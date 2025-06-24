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

@SearchTags({"auto sword"})
public final class AutoSwordHack extends Hack implements UpdateListener, PacketOutputListener
{
	private final EnumSetting<Priority> priority =
			new EnumSetting<>("Priority", Priority.values(), Priority.SPEED);

	private final CheckboxSetting swapWhileMoving = new CheckboxSetting(
			"Swap while moving",
			"Whether or not to swap while the player is moving.\n\n"
					+ "\u00a7c\u00a7lWARNING:\u00a7r This would not be possible without cheats. It may raise suspicion.",
			false);
	
	private final SliderSetting delay = new SliderSetting("Delay",
		"Amount of ticks to wait before swapping.",
		2, 1, 20, 1,
		ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(1, "1 tick"));

	private final SliderSetting releaseTime = new SliderSetting("Release time",
			"Time until AutoSword will switch back from the weapon to the"
					+ " previously selected slot.\n\n"
					+ "Only works when \u00a7lSwitch back\u00a7r is checked.",
			10, 1, 200, 1,
			ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(1, "1 tick"));

	private final CheckboxSetting dropSwords = new CheckboxSetting("Drop swords", "Throw away the worst swords", true);

	private int oldSlot;
	private int timer;
	
	public AutoSwordHack()
	{
		super("AutoSword");
		setCategory(Category.COMBAT);
		addSetting(swapWhileMoving);
		addSetting(delay);
		addSetting(releaseTime);
		addSetting(dropSwords);
	}
	
	@Override
	protected void onEnable()
	{
		oldSlot = -1;
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

		do {
			float bestSwordValue = -1;
			int bestSwordSlot = -1;

			for (int slot = 0; slot < 36; slot++) {
				ItemStack stack = inventory.getStack(slot);
				if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem sword)) continue;

				float value = getSwordValue(stack, sword);
				if (value > bestSwordValue) {
					bestSwordValue = value;
					bestSwordSlot = slot;
				}
			}

			ItemStack currentStack = inventory.getStack(0);
			float currentSwordValue = -1;

			if (!currentStack.isEmpty() && currentStack.getItem() instanceof SwordItem currentSword)
				currentSwordValue = getSwordValue(currentStack, currentSword);

			if (bestSwordSlot == -1
					|| bestSwordSlot == 0
					|| currentSwordValue >= bestSwordValue
					|| (!currentStack.isEmpty() && inventory.getEmptySlot() == -1)) {
				break;
			}

			if (!currentStack.isEmpty()) {
				IMC.getInteractionManager().windowClick_QUICK_MOVE(36);
			}

			int slotId = bestSwordSlot < 9 ? bestSwordSlot + 36 : bestSwordSlot;
			Int2ObjectMap<ItemStack> stackMap = new Int2ObjectOpenHashMap<>();
			int revision = player.currentScreenHandler.getRevision();

			player.networkHandler.sendPacket(new ClickSlotC2SPacket(
					0, revision, slotId, 0, SlotActionType.PICKUP,
					player.currentScreenHandler.getSlot(slotId).getStack(), stackMap));

			player.networkHandler.sendPacket(new ClickSlotC2SPacket(
					0, revision, 36, 0, SlotActionType.PICKUP,
					player.currentScreenHandler.getSlot(36).getStack(), stackMap));
		} while (false);

		// Throw away the worst swords
		if (dropSwords.isChecked())
		{
			for (int slot = 9; slot < 45; slot++) {
				int adjusted = slot >= 36 ? slot - 36 : slot;
				if (adjusted == 0) continue;
				ItemStack stack = inventory.getStack(adjusted);

				if (!stack.isEmpty() && isWorseOrSameSword(stack)) {
					IMC.getInteractionManager().windowClick_THROW(slot);
				}
			}
		}
	}


	public boolean isWorseOrSameSword(ItemStack candidate)
	{
		// If candidate is not sword
		if(!(candidate.getItem() instanceof SwordItem candidateItem))
			return false;

		var player = MinecraftClient.getInstance().player;
		if(player == null)
			return false;

		ItemStack equipped = player.getInventory().getStack(0);

		// If nothing is equipped
		if(equipped.isEmpty())
			return false;

		// If equipped not an armor
		if(!(equipped.getItem() instanceof SwordItem equippedItem))
			return false;

		float candidateValue = getSwordValue(candidate, candidateItem);
		float equippedValue = getSwordValue(equipped, equippedItem);
		return candidateValue <= equippedValue;
	}

	public static float getSwordValue(ItemStack stack, SwordItem item) {
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

	@Override
	public void onSentPacket(PacketOutputListener.PacketOutputEvent event)
	{
		if(event.getPacket() instanceof ClickSlotC2SPacket)
			timer = delay.getValueI();
	}

	public void setSlot(Entity entity)
	{
		// check if active
		if(!isEnabled())
			return;

		// wait for AutoEat
		if(WURST.getHax().autoEatHack.isEating())
			return;

		// find best weapon
		float bestValue = Integer.MIN_VALUE;
		int bestSlot = -1;
		for(int i = 0; i < 9; i++)
		{
			// skip empty slots
			if(MC.player.getInventory().getStack(i).isEmpty())
				continue;

			// get weapon value
			ItemStack stack = MC.player.getInventory().getStack(i);
			float value = getValue(stack, entity);

			// compare with previous best weapon
			if(value > bestValue)
			{
				bestValue = value;
				bestSlot = i;
			}
		}

		// check if any weapon was found
		if(bestSlot == -1)
			return;

		// save old slot
		if(oldSlot == -1)
			oldSlot = MC.player.getInventory().selectedSlot;

		// set slot
		MC.player.getInventory().selectedSlot = bestSlot;

		// start timer
		timer = releaseTime.getValueI();
	}

	private float getValue(ItemStack stack, Entity entity)
	{
		Item item = stack.getItem();
		if(!(item instanceof ToolItem || item instanceof TridentItem))
			return Integer.MIN_VALUE;

		switch(priority.getSelected())
		{
			case SPEED:
				return ItemUtils.getAttackSpeed(item);

			case DAMAGE:
				EntityGroup group = entity instanceof LivingEntity le
						? le.getGroup() : EntityGroup.DEFAULT;
				float dmg = EnchantmentHelper.getAttackDamage(stack, group);
				if(item instanceof SwordItem sword)
					dmg += sword.getAttackDamage();
				if(item instanceof MiningToolItem tool)
					dmg += tool.getAttackDamage();
				if(item instanceof TridentItem)
					dmg += TridentItem.ATTACK_DAMAGE;
				return dmg;
		}

		return Integer.MIN_VALUE;
	}

	private enum Priority
	{
		SPEED("Speed (swords)"),
		DAMAGE("Damage (axes)");

		private final String name;

		private Priority(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}
