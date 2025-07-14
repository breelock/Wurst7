/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.*;
import java.util.stream.IntStream;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.*;
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
import net.wurstclient.util.Rand;

@SearchTags({"auto steal", "ChestStealer", "chest stealer",
	"steal store buttons", "Steal/Store buttons"})
public final class AutoStealHack extends Hack
{
	private final SliderSetting minDelay = new SliderSetting("Min delay",
			"Minimal delay between moving stacks of items.",
			110, 0, 500, 10, ValueDisplay.INTEGER.withSuffix("ms"));

	private final SliderSetting maxDelay = new SliderSetting("Max delay",
		"Maximal delay between moving stacks of items.",
		150, 0, 500, 10, ValueDisplay.INTEGER.withSuffix("ms"));

	private final CheckboxSetting buttons =
		new CheckboxSetting("Steal/Store buttons", true);
	
	private final CheckboxSetting reverseSteal =
		new CheckboxSetting("Reverse steal order", false);

	private final CheckboxSetting dontStealShit =
			new CheckboxSetting("Don't steal shit", true);

	private final CheckboxSetting autoClose =
			new CheckboxSetting("Auto close chest after steal", true);

	public final CheckboxSetting checkTitle =
			new CheckboxSetting("Check chest title, if it is not default, don't steal", true);

	private final SliderSetting checkDelay = new SliderSetting("Delay to ensure",
			"Delay to ensure that the items were stolen.\n",
			100, 0, 1000, 10, ValueDisplay.INTEGER.withSuffix("ms"));
	
	private Thread thread;
	private final List<ItemStack> shit = new ArrayList<>();
	
	public AutoStealHack()
	{
		super("AutoSteal");
		setCategory(Category.ITEMS);
		addSetting(minDelay);
		addSetting(maxDelay);
		addSetting(buttons);
		addSetting(reverseSteal);
		addSetting(dontStealShit);
		addSetting(autoClose);
		addSetting(checkTitle);
		addSetting(checkDelay);
	}
	
	public void steal(HandledScreen<?> screen, int rows, boolean fromBtn)
	{
		startClickingSlots(screen, 0, rows * 9, true, fromBtn);
	}
	
	public void store(HandledScreen<?> screen, int rows, boolean fromBtn)
	{
		startClickingSlots(screen, rows * 9, rows * 9 + 36, false, fromBtn);
	}
	
	private void startClickingSlots(HandledScreen<?> screen, int from, int to,
		boolean steal, boolean fromBtn)
	{
		if (thread != null && thread.isAlive()) {
			if (fromBtn && !steal)
				return;
			else
				thread.interrupt();
		}
		
		thread = new Thread(() -> shiftClickSlots(screen, from, to, steal),
			"AutoSteal");
		thread.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
		thread.setDaemon(true);
		thread.start();
	}
	
	private void shiftClickSlots(HandledScreen<?> screen, int from, int to, boolean steal)
	{
		shit.clear();
		boolean isShitChest = true;
		boolean firstRun = true;

		List<Slot> slots = IntStream.range(from, to).mapToObj(i -> screen.getScreenHandler().slots.get(i)).toList();
		
		if(reverseSteal.isChecked() && steal)
			Collections.reverse(slots);

		AutoDropHack dropH = WurstClient.INSTANCE.getHax().autoDropHack;
		AutoSwordHack swordH = WurstClient.INSTANCE.getHax().autoSwordHack;
		AutoArmorHack armorH = WurstClient.INSTANCE.getHax().autoArmorHack;
		AutoToolHack toolDH = WurstClient.INSTANCE.getHax().autoToolHack;

		float bestSwordValue = swordH.getBestSword().getValue();
		int[] bestArmorValues = armorH.getBestArmor().getValue();
		Map<AutoToolHack.MCTool, Float> bestToolsValues = toolDH.getBestTools().getValue();

		if (minDelay.getValueI() > maxDelay.getValueI()) {
			int mind = minDelay.getValueI();
			int maxd = maxDelay.getValueI();

			maxDelay.setValue(mind);
			minDelay.setValue(maxd);
		}

        while (MC.currentScreen == screen) {
            for (Slot slot : slots)
                try {
                    if (slot.getStack().isEmpty())
                        continue;

					if (MC.currentScreen != screen)
						return;

                    ItemStack stack = slot.getStack();
                    Item item = stack.getItem();
                    String itemName = Registries.ITEM.getId(item).toString();

                    if (dontStealShit.isChecked()) {
                        if (dropH.items.getItemNames().contains(itemName)) {
                            shit.add(stack);
                            continue;
                        }

                        if (item instanceof SwordItem sword) {
                            if (swordH.getSwordValue(stack, sword) <= bestSwordValue) {
                                shit.add(stack);
                                continue;
                            }
                        }

                        if (item instanceof ArmorItem armorItem) {
                            int armorType = armorItem.getSlotType().getEntitySlotId();
                            int armorValue = armorH.getArmorValue(armorItem, stack);

                            if (armorValue <= bestArmorValues[armorType]) {
                                shit.add(stack);
                                continue;
                            }
                        }

						AutoToolHack.MCTool toolT = toolDH.getMCTool(item);
						if (toolT != AutoToolHack.MCTool.Null) {
							if (toolDH.getToolValue(toolT, stack, item) <= (bestToolsValues.get(toolT) == null ? -1 : bestToolsValues.get(toolT))) {
								shit.add(stack);
								continue;
							}
						}
                    }

					Thread.sleep(Rand.Int(minDelay.getValueI(), maxDelay.getValueI()));

					if (MC.currentScreen != screen)
						return;

                    screen.onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
                    isShitChest = false;

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

			try {
				Thread.sleep(checkDelay.getValueI());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}

            boolean allEmpty = true;
			boolean fullEmpty = true;

            for (Slot slot : slots) {
                if (!slot.getStack().isEmpty()) {
                    fullEmpty = false;
					if (!shit.contains(slot.getStack())) {
						allEmpty = false;
						break;
					}
                }
            }

			if (fullEmpty && firstRun && isShitChest)
				continue;

            if (allEmpty) {
                if (autoClose.isChecked() && MC.currentScreen == screen && !isShitChest)
                    MC.execute(() -> MC.player.closeHandledScreen());
                return;
            }
			firstRun = false;
        }
	}
	
	public boolean areButtonsVisible()
	{
		return buttons.isChecked();
	}
	
	// See GenericContainerScreenMixin and ShulkerBoxScreenMixin
}
