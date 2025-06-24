/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"auto steal", "ChestStealer", "chest stealer",
	"steal store buttons", "Steal/Store buttons"})
public final class AutoStealHack extends Hack
{
	private final SliderSetting delay = new SliderSetting("Delay",
		"Delay between moving stacks of items.\n"
			+ "Should be at least 70ms for NoCheat+ servers.",
		100, 0, 500, 10, ValueDisplay.INTEGER.withSuffix("ms"));
	
	private final CheckboxSetting buttons =
		new CheckboxSetting("Steal/Store buttons", true);
	
	private final CheckboxSetting reverseSteal =
		new CheckboxSetting("Reverse steal order", false);

	private final CheckboxSetting dontStealShit =
			new CheckboxSetting("Don't steal shit", true);

	private final CheckboxSetting autoClose =
			new CheckboxSetting("Auto close chest after steal", true);
	
	private Thread thread;
	private List<ItemStack> shit = new ArrayList<>();
	
	public AutoStealHack()
	{
		super("AutoSteal");
		setCategory(Category.ITEMS);
		addSetting(buttons);
		addSetting(delay);
		addSetting(reverseSteal);
		addSetting(dontStealShit);
		addSetting(autoClose);
	}
	
	public void steal(HandledScreen<?> screen, int rows)
	{
		startClickingSlots(screen, 0, rows * 9, true);
	}
	
	public void store(HandledScreen<?> screen, int rows)
	{
		startClickingSlots(screen, rows * 9, rows * 9 + 36, false);
	}
	
	private void startClickingSlots(HandledScreen<?> screen, int from, int to,
		boolean steal)
	{
		if(thread != null && thread.isAlive())
			return;
		
		thread = new Thread(() -> shiftClickSlots(screen, from, to, steal),
			"AutoSteal");
		thread.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
		thread.setDaemon(true);
		thread.start();
	}
	
	private void shiftClickSlots(HandledScreen<?> screen, int from, int to,
		boolean steal)
	{
		shit.clear();
		List<Slot> slots = IntStream.range(from, to)
			.mapToObj(i -> screen.getScreenHandler().slots.get(i)).toList();
		
		if(reverseSteal.isChecked() && steal)
			Collections.reverse(slots);

		AutoDropHack dropH = WurstClient.INSTANCE.getHax().autoDropHack;
		AutoSwordHack swordH = WurstClient.INSTANCE.getHax().autoSwordHack;
		AutoArmorHack armorH = WurstClient.INSTANCE.getHax().autoArmorHack;

		float bestSwordValue = -1;

		// find best sword
		for (int slt = 0; slt < 36; slt++) {
			ItemStack stack = MC.player.getInventory().getStack(slt);
			if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem sword)) continue;

			float value = swordH.getSwordValue(stack, sword);
			if (value > bestSwordValue) {
				bestSwordValue = value;
			}
		}

		int[] bestArmorValues = new int[4];

		// initialize with currently equipped armor
		for(int type = 0; type < 4; type++)
		{
			ItemStack stack = MC.player.getInventory().getArmorStack(type);
			if(stack.isEmpty() || !(stack.getItem() instanceof ArmorItem))
				continue;

			ArmorItem item = (ArmorItem)stack.getItem();
			bestArmorValues[type] = armorH.getArmorValue(item, stack);
		}

		// search inventory for better armor
		for(int slot = 0; slot < 36; slot++)
		{
			ItemStack stack = MC.player.getInventory().getStack(slot);

			if(stack.isEmpty() || !(stack.getItem() instanceof ArmorItem))
				continue;

			ArmorItem item = (ArmorItem)stack.getItem();
			int armorType = item.getSlotType().getEntitySlotId();
			int armorValue = armorH.getArmorValue(item, stack);

			if(armorValue > bestArmorValues[armorType])
				bestArmorValues[armorType] = armorValue;
		}

		for (Slot slot : slots)
			try
			{
				if (slot.getStack().isEmpty())
					continue;

				ItemStack stack = slot.getStack();
				Item item = stack.getItem();
				String itemName = Registries.ITEM.getId(item).toString();

				if (dontStealShit.isChecked())
				{
					if (dropH.items.getItemNames().contains(itemName))
					{
						shit.add(stack);
						continue;
					}

					if (stack.getItem() instanceof SwordItem sword)
					{
						if (swordH.getSwordValue(stack, sword) <= bestSwordValue)
						{
							shit.add(stack);
							continue;
						}
					}

					if (stack.getItem() instanceof ArmorItem armorItem)
					{
						int armorType = armorItem.getSlotType().getEntitySlotId();
						int armorValue = armorH.getArmorValue(armorItem, stack);

						if (armorValue <= bestArmorValues[armorType])
						{
							shit.add(stack);
							continue;
						}
					}

				}
				
				Thread.sleep(delay.getValueI());
				
				if(MC.currentScreen == null)
					break;
				
				screen.onMouseClick(slot, slot.id, 0,
					SlotActionType.QUICK_MOVE);
				
			}catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
				break;
			}


		boolean allEmpty = true;

		for (Slot slot : slots) {
			if (!slot.getStack().isEmpty() && !shit.contains(slot.getStack())) {
				allEmpty = false;
				break;
			}
		}

		if (allEmpty) {
			if (autoClose.isChecked() && MC.currentScreen != null)
				MC.execute(() -> MC.player.closeHandledScreen());
		}
		else {
			shiftClickSlots(screen, from, to, steal);
		}
	}
	
	public boolean areButtonsVisible()
	{
		return buttons.isChecked();
	}
	
	// See GenericContainerScreenMixin and ShulkerBoxScreenMixin
}
