/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.Registries;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.settings.SliderSetting;

import java.util.HashMap;
import java.util.Map;

@SearchTags({"auto drop", "AutoEject", "auto-eject", "auto eject",
	"InventoryCleaner", "inventory cleaner", "InvCleaner", "inv cleaner"})
public final class AutoDropHack extends Hack implements UpdateListener
{
	public ItemListSetting items = new ItemListSetting("Items",
		"Unwanted items that will be dropped.", "minecraft:allium",
		"minecraft:azure_bluet", "minecraft:blue_orchid",
		"minecraft:cornflower", "minecraft:dandelion", "minecraft:lilac",
		"minecraft:lily_of_the_valley", "minecraft:orange_tulip",
		"minecraft:oxeye_daisy", "minecraft:peony", "minecraft:pink_tulip",
		"minecraft:poisonous_potato", "minecraft:poppy", "minecraft:red_tulip",
		"minecraft:rose_bush", "minecraft:rotten_flesh", "minecraft:sunflower",
		"minecraft:wheat_seeds", "minecraft:white_tulip");

	private final CheckboxSetting disableAfterDrop = new CheckboxSetting("Disable after drop", "Disable after drop", true);
	private final CheckboxSetting dropSwords = new CheckboxSetting("Drop swords", "Throw away the worst swords", true);
	private final CheckboxSetting dropArmor = new CheckboxSetting("Drop armor", "Throw away the worst armor", true);
	private final CheckboxSetting dropTools = new CheckboxSetting("Drop tools", "Throw away the worst tools", true);
	private final SliderSetting delay = new SliderSetting("Delay",
			"Amount of ticks to wait before drop.", 2,
			0, 20, 1, SliderSetting.ValueDisplay.INTEGER);

	private boolean isDropped = false;
	private int timer;
	
	public AutoDropHack()
	{
		super("AutoDrop");
		setCategory(Category.ITEMS);
		addSetting(items);
		addSetting(disableAfterDrop);
		addSetting(delay);
		addSetting(dropSwords);
		addSetting(dropArmor);
		addSetting(dropTools);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		if (enabled) {
			super.setEnabled(true);
		}
		else {
			if (disableAfterDrop.isChecked()) {
				if (isDropped) {
					isDropped = false;
					super.setEnabled(false);
				}
			}
			else {
				super.setEnabled(false);
			}
		}
	}
	
	@Override
	public void onUpdate()
	{
		if(timer > 0)
		{
			timer--;
			return;
		}

		AutoSwordHack swordH = WurstClient.INSTANCE.getHax().autoSwordHack;
		AutoArmorHack armorH = WurstClient.INSTANCE.getHax().autoArmorHack;
		AutoToolHack toolDH = WurstClient.INSTANCE.getHax().autoToolHack;

		int bestSwordSlot = swordH.getBestSword().getKey();
		int[] bestArmorSlots = armorH.getBestArmor().getKey();
		Map<AutoToolHack.MCTool, Integer> bestToolsSlots = toolDH.getBestTools().getKey();

		for(int slot = 9; slot < 45; slot++)
		{
			if(MC.currentScreen instanceof HandledScreen && !(MC.currentScreen instanceof InventoryScreen))
				return;

			if(MC.player.input.movementForward != 0 || MC.player.input.movementSideways != 0)
				return;

			int adjustedSlot = slot;
			if(adjustedSlot >= 36)
				adjustedSlot -= 36;

			ItemStack stack = MC.player.getInventory().getStack(adjustedSlot);
			if(stack.isEmpty())
				continue;
			
			Item item = stack.getItem();
			String itemName = Registries.ITEM.getId(item).toString();

			if (dropSwords.isChecked() && item instanceof SwordItem) {
				if (adjustedSlot != bestSwordSlot) {
					drop(slot);
					return;
				}
			}

			if (dropArmor.isChecked() && item instanceof ArmorItem armorItem) {
				int armorType = armorItem.getSlotType().getEntitySlotId();
				if (adjustedSlot != bestArmorSlots[armorType]) {
					drop(slot);
					return;
				}
			}

			AutoToolHack.MCTool toolT = toolDH.getMCTool(item);
			if (dropTools.isChecked() && toolT != AutoToolHack.MCTool.Null) {
				if (adjustedSlot != (bestToolsSlots.get(toolT) == null ? -1 : bestToolsSlots.get(toolT))) {
					drop(slot);
					return;
				}
			}

			if(items.getItemNames().contains(itemName)) {
				drop(slot);
				return;
			}
		}

		if (disableAfterDrop.isChecked()) {
			isDropped = true;
			this.setEnabled(false);
		}
	}

	private void drop(int slot)
	{
		WURST.openServInv(true);
		IMC.getInteractionManager().windowClick_THROW(slot);
		WURST.openServInv(false);
		timer = delay.getValueI();
	}
}
