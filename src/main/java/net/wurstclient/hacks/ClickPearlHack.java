/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.Rand;

@SearchTags({"pearl", "click pearl"})
public final class ClickPearlHack extends Hack
{
	private final SliderSetting minDelay = new SliderSetting("Min delay (ms)",
			84, 0, 1000, 1, SliderSetting.ValueDisplay.INTEGER);

	private final SliderSetting maxDelay = new SliderSetting("Max delay (ms)",
			100, 0, 1000, 1, SliderSetting.ValueDisplay.INTEGER);

	public ClickPearlHack()
	{
		super("ClickPearl");
		setCategory(Category.ITEMS);
		addSetting(minDelay);
		addSetting(maxDelay);
	}
	
	@Override
	protected void onEnable()
	{
		boolean skipSleep;

		if (minDelay.getValueI() > maxDelay.getValueI()) {
			int mind = minDelay.getValueI();
			int maxd = maxDelay.getValueI();

			maxDelay.setValue(mind);
			minDelay.setValue(maxd);
		}

        skipSleep = maxDelay.getValueI() <= 0;

        new Thread(() -> {
			MinecraftClient client = MC;
			if (client == null || client.player == null || client.interactionManager == null) return;

			int currentSlot = client.player.getInventory().selectedSlot;

			for (int i = 0; i < 9; i++) {
				if (client.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
					if (currentSlot != i) {
						client.player.getInventory().selectedSlot = i;
						if (!skipSleep) {
							try {Thread.sleep(Rand.Int(minDelay.getValueI(), maxDelay.getValueI()));} catch (InterruptedException ignored) {}
						}
					}

					client.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, client.player.getInventory().selectedSlot));
					client.player.swingHand(Hand.MAIN_HAND);

					if (currentSlot != i) {
						if (!skipSleep) {
							try {Thread.sleep(Rand.Int(minDelay.getValueI(), maxDelay.getValueI()));} catch (InterruptedException ignored) {}
						}
						client.player.getInventory().selectedSlot = currentSlot;
					}
					break;
				}
			}
			this.setEnabled(false);
		}).start();
	}
}
