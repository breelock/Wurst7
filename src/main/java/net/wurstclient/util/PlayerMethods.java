package net.wurstclient.util;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoClickerLeftHack;
import net.wurstclient.hacks.AutoClickerRightHack;

import java.util.Set;

public class PlayerMethods {
    public static void attack(MinecraftClient client, boolean isNewPvP) {
        if (client.player != null && client.crosshairTarget != null && !client.player.isSpectator() && client.interactionManager != null && client.world != null) {
            ItemStack itemStack = client.player.getStackInHand(Hand.MAIN_HAND);
            if (!itemStack.isItemEnabled(client.world.getEnabledFeatures()))
                return;

            boolean isInLava = client.world.getBlockState(client.player.getBlockPos()).getBlock() == Blocks.LAVA;
            boolean isOnGround = client.player.isOnGround() && !client.player.isTouchingWater() && !isInLava;

            float jumpCooldown = 400f;
            float cooldownTime = getAttackSpeedInTicks(client.player) * 50;
            float attackCooldown = getAttackCooldownInTicks(client.player) * 50;

            AutoClickerLeftHack hack = WurstClient.INSTANCE.getHax().autoClickerLeftHack;

            if (isNewPvP) {
                if (client.crosshairTarget.getType() == HitResult.Type.ENTITY && !hack.dontAttackEntities.isChecked() && attackCooldown >= cooldownTime - jumpCooldown && hack.autoJump.isChecked() && client.player.isOnGround() && !client.player.isTouchingWater() && !isInLava) {
                    if (!targetIsProtectedByShield(client, ((EntityHitResult) client.crosshairTarget).getEntity()))
                        client.player.jump();
                }

                if (attackCooldown >= cooldownTime) {
                    if (client.crosshairTarget.getType() == HitResult.Type.ENTITY && !hack.dontAttackEntities.isChecked()) {
                        Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
                        if (hack.dontAttackFrens.isChecked() && entity.isPlayer() && WurstClient.INSTANCE.getFriends().contains(entity.getEntityName()))
                            return;

                        if (!targetIsProtectedByShield(client, entity)) {
                            if (!isOnGround && client.player.getVelocity().y < -0.1 || client.player.isOnGround() || client.player.getAbilities().flying || client.player.isTouchingWater() || isInLava) {
                                if (interrupt(client, true)) return;
                                PlayerMethods.attackEntity(client);
                            }
                        }
                    }
                    else if (client.crosshairTarget.getType() == HitResult.Type.BLOCK && !hack.dontAttackBlocks.isChecked()) {
                        if (interrupt(client, true)) return;
                        PlayerMethods.breakBlock(client);
                    }

                    else if (client.crosshairTarget.getType() == HitResult.Type.MISS && !hack.dontAttackAir.isChecked()) {
                        if (interrupt(client, true)) return;
                        client.player.swingHand(Hand.MAIN_HAND);
                        resetAttackCooldown(client);
                    }
                }
            }
            else {
                if (client.crosshairTarget.getType() == HitResult.Type.ENTITY && !hack.dontAttackEntities.isChecked()) {
                    Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
                    if (hack.dontAttackFrens.isChecked() && entity.isPlayer() && WurstClient.INSTANCE.getFriends().contains(entity.getEntityName()))
                        return;

                    if (hack.autoJump.isChecked() && client.player.isOnGround() && !client.player.isTouchingWater() && !isInLava)
                        client.player.jump();

                    if (interrupt(client, false)) return;
                    PlayerMethods.attackEntity(client);
                }

                else if (client.crosshairTarget.getType() == HitResult.Type.BLOCK && !hack.dontAttackBlocks.isChecked()) {
                    if (interrupt(client, false)) return;
                    PlayerMethods.breakBlock(client);
                }

                else if (client.crosshairTarget.getType() == HitResult.Type.MISS && !hack.dontAttackAir.isChecked()) {
                    if (interrupt(client, false)) return;
                    client.player.swingHand(Hand.MAIN_HAND);
                    resetAttackCooldown(client);
                }
            }
        }
    }

    public static void interact(MinecraftClient client) {
        if (client.player != null && !client.player.isSpectator() && client.interactionManager != null && !client.interactionManager.isBreakingBlock() && !client.player.isRiding()) {
            AutoClickerRightHack hack = WurstClient.INSTANCE.getHax().autoClickerRightHack;
            for (Hand hand : Hand.values()) {
                if (client.crosshairTarget != null) {
                    if (client.crosshairTarget.getType() == HitResult.Type.ENTITY && !hack.dontInteractWithEntities.isChecked()) {
                        if (PlayerMethods.interactEntity(client, hand)) return;
                    }
                    else if (client.crosshairTarget.getType() == HitResult.Type.BLOCK && !hack.dontInteractWithBlocks.isChecked()) {
                        if (hack.dontInteractWithInteractiveBlocks.isChecked()) {
                            BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
                            Block block = client.world.getBlockState(blockHit.getBlockPos()).getBlock();

                            if (INTERACTIVE_BLOCKS.contains(block.getClass()))
                                return;
                        }

                        if (PlayerMethods.interactBlock(client, hand)) return;
                    }

                }
                if (!hack.dontInteractWithItems.isChecked())
                    interactItem(client, hand);
            }
        }
    }

    private static boolean interrupt(MinecraftClient client, boolean isNewPvP) {
        if (client != null && client.player != null && client.interactionManager != null && client.player.isUsingItem()) {
            AutoClickerLeftHack hack = WurstClient.INSTANCE.getHax().autoClickerLeftHack;
            if (hack.interrupt.isChecked())
                client.interactionManager.stopUsingItem(client.player);
            else
                return true;
        }

        return false;
    }

    private static void resetAttackCooldown(MinecraftClient client) {
        if (client.player != null) {
            client.player.resetLastAttackedTicks();
        }
    }

    private static void attackEntity(MinecraftClient client) {
        if (client != null && client.interactionManager != null && client.player != null) {
            EntityHitResult entityHitResult = (EntityHitResult) client.crosshairTarget;
            if (entityHitResult != null)
                client.interactionManager.attackEntity(client.player, entityHitResult.getEntity());

            client.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private static boolean targetIsProtectedByShield(MinecraftClient client, Entity targetEntity) {
        if (client.player != null) {
            if (targetEntity instanceof PlayerEntity) {
                PlayerEntity targetPlayer = (PlayerEntity) targetEntity;
                ItemStack heldItem = client.player.getMainHandStack();
                if (heldItem.getItem() instanceof AxeItem)
                    return false;

                return targetPlayer.isUsingItem() && targetPlayer.getActiveItem().getItem() == Items.SHIELD;
            }
        }
        return false;
    }

    public static int getAttackCooldownInTicks(PlayerEntity player) {
        float cooldown = player.getAttackCooldownProgress(0.0f);
        return MathHelper.ceil(cooldown * getAttackSpeedInTicks(player));
    }

    public static float getAttackSpeedInTicks(PlayerEntity player) {
        float attackSpeed = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
        return (1.0f / attackSpeed) * 20.0f;
    }

    private static void breakBlock(MinecraftClient client) {
        if (client != null && client.interactionManager != null && client.player != null && client.world != null) {
            BlockHitResult blockHitResult = (BlockHitResult) client.crosshairTarget;
            if (blockHitResult != null) {
                BlockPos blockPos = blockHitResult.getBlockPos();

                if (!client.world.getBlockState(blockPos).isAir())
                    client.interactionManager.attackBlock(blockPos, blockHitResult.getSide());

                client.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private static boolean interactEntity(MinecraftClient client, Hand hand) {
        if (client != null && client.interactionManager != null && client.player != null && client.world != null) {
            EntityHitResult entityHitResult = (EntityHitResult) client.crosshairTarget;
            if (entityHitResult != null) {
                Entity entity = entityHitResult.getEntity();
                if (!client.world.getWorldBorder().contains(entity.getBlockPos())) {
                    return true;
                }

                ActionResult actionResult = client.interactionManager.interactEntityAtLocation(client.player, entity, entityHitResult, hand);
                if (!actionResult.isAccepted()) {
                    actionResult = client.interactionManager.interactEntity(client.player, entity, hand);
                }

                if (actionResult.isAccepted()) {
                    if (actionResult.shouldSwingHand()) {
                        client.player.swingHand(hand);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private static boolean interactBlock(MinecraftClient client, Hand hand) {
        if (client != null && client.interactionManager != null && client.player != null) {
            ItemStack itemStack = client.player.getStackInHand(hand);
            BlockHitResult blockHitResult = (BlockHitResult)client.crosshairTarget;
            int i = itemStack.getCount();
            ActionResult actionResult = client.interactionManager.interactBlock(client.player, hand, blockHitResult);
            if (actionResult.isAccepted()) {
                if (actionResult.shouldSwingHand()) {
                    client.player.swingHand(hand);
                    if (!itemStack.isEmpty() && (itemStack.getCount() != i || client.interactionManager.hasCreativeInventory())) {
                        client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                    }
                }

                return true;
            }

            return actionResult == ActionResult.FAIL;
        }

        return false;
    }

    private static void interactItem(MinecraftClient client, Hand hand) {
        if (client != null && client.interactionManager != null && client.player != null) {
            ItemStack itemStack = client.player.getStackInHand(hand);
            if (!itemStack.isEmpty()) {
                ActionResult actionResult = client.interactionManager.interactItem(client.player, hand);
                if (actionResult.isAccepted()) {
                    if (actionResult.shouldSwingHand()) {
                        client.player.swingHand(hand);
                    }

                    client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                }
            }
        }
    }

    private static final Set<Class<? extends Block>> INTERACTIVE_BLOCKS = Set.of(
            AnvilBlock.class, BarrelBlock.class, BeaconBlock.class, BrewingStandBlock.class, CartographyTableBlock.class, ChestBlock.class,
            CraftingTableBlock.class, EnchantingTableBlock.class, EnderChestBlock.class, FurnaceBlock.class, BlastFurnaceBlock.class,
            SmokerBlock.class, GrindstoneBlock.class, LecternBlock.class, LoomBlock.class, ShulkerBoxBlock.class, SignBlock.class,
            HangingSignBlock.class, SmithingTableBlock.class, StonecutterBlock.class
    );

}