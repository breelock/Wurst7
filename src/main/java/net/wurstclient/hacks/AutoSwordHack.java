/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ItemUtils;

import java.util.AbstractMap;

@SearchTags({"auto sword"})public final class AutoSwordHack extends Hack implements UpdateListener, PacketOutputListener
{
	private final CheckboxSetting swapWhileMoving = new CheckboxSetting(
			"Swap while moving",
			"Whether or not to swap while the player is moving.\n\n"
					+ "\u00a7c\u00a7lWARNING:\u00a7r This would not be possible without cheats. It may raise suspicion.",
			false);
	
	private final SliderSetting delay = new SliderSetting("Delay",
		"Amount of ticks to wait before swapping.",
		2, 0, 20, 1,
		ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(1, "1 tick"));

	private final SliderSetting swordSlot = new SliderSetting("Sword slot",
			"Slot in the hot bar where the sword will be placed.",
			1, 1, 9, 1, ValueDisplay.INTEGER);

	private int timer;
	
	public AutoSwordHack()
	{
		super("AutoSword");
		setCategory(Category.COMBAT);
		addSetting(swapWhileMoving);
		addSetting(delay);
		addSetting(swordSlot);
	}
	
	@Override
	protected void onEnable()
	{
		timer = 0;
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
	}

	@Override
	public void onUpdate() {
		if (timer > 0) {
			timer--;
			return;
		}

		if (MC.currentScreen instanceof HandledScreen && !(MC.currentScreen instanceof InventoryScreen))
			return;

		ClientPlayerEntity player = MC.player;
		PlayerInventory inventory = player.getInventory();

		if (!swapWhileMoving.isChecked() && (player.input.movementForward != 0 || player.input.movementSideways != 0))
			return;

		AbstractMap.SimpleEntry<Integer, Float> swrd = getBestSword();
		float bestSwordValue = swrd.getValue();
		int bestSwordSlot = swrd.getKey();

		ItemStack currentStack = inventory.getStack(swordSlot.getValueI() - 1);
		float currentSwordValue = -1;

		if (!currentStack.isEmpty() && currentStack.getItem() instanceof SwordItem currentSword)
			currentSwordValue = getSwordValue(currentStack, currentSword);

		if (bestSwordSlot == -1
				|| bestSwordSlot == swordSlot.getValueI() - 1
				|| currentSwordValue >= bestSwordValue
				|| inventory.getEmptySlot() == -1) {
			return;
		}

		if (!currentStack.isEmpty()) {
			IMC.getInteractionManager().windowClick_QUICK_MOVE(swordSlot.getValueI() + 35);
		}

		int slotId = bestSwordSlot < 9 ? bestSwordSlot + 36 : bestSwordSlot;
		Int2ObjectMap<ItemStack> stackMap = new Int2ObjectOpenHashMap<>();
		int revision = player.currentScreenHandler.getRevision();
		WURST.openServInv(true);
		player.networkHandler.sendPacket(new ClickSlotC2SPacket(
				0, revision, slotId, 0, SlotActionType.PICKUP,
				player.currentScreenHandler.getSlot(slotId).getStack(), stackMap));

		player.networkHandler.sendPacket(new ClickSlotC2SPacket(
				0, revision, swordSlot.getValueI() + 35, 0, SlotActionType.PICKUP,
				player.currentScreenHandler.getSlot(swordSlot.getValueI() + 35).getStack(), stackMap));
		WURST.openServInv(false);
	}

	public float getSwordValue(ItemStack stack, SwordItem item) {
		float dmg = item.getAttackDamage();
		int enchantmentBonus = 0;

		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack) * 3;
		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.FIRE_ASPECT, stack);
		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, stack);
		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.MENDING, stack);
		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);

		return dmg * 5 + enchantmentBonus;
	}

	public AbstractMap.SimpleEntry<Integer, Float> getBestSword() {
		float bestSwordValue = -1;
		int bestSwordSlot = -1;

		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = MC.player.getInventory().getStack(slot);
			if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem sword)) continue;

			float value = getSwordValue(stack, sword);
			if (value > bestSwordValue) {
				bestSwordValue = value;
				bestSwordSlot = slot;
			}
		}

		return new AbstractMap.SimpleEntry<>(bestSwordSlot, bestSwordValue);
	}

	@Override
	public void onSentPacket(PacketOutputListener.PacketOutputEvent event)
	{
		if(event.getPacket() instanceof ClickSlotC2SPacket)
			timer = delay.getValueI();
	}

	public void setSlot(Entity entity) {

	}
}
