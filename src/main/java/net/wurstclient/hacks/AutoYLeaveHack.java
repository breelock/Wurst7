/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"auto leave", "AutoDisconnect", "auto disconnect", "AutoQuit",
	"auto quit"})
public final class AutoYLeaveHack extends Hack implements UpdateListener
{
	private boolean hasLeft = false;

	private final SliderSetting yAxis = new SliderSetting("Y Axis",
		"Leaves the server when the Y-axis reaches this value or falls below it.",
		-60, -100, 500, 1, ValueDisplay.DECIMAL);

	public AutoYLeaveHack()
	{
		super("AutoYLeave");
		setCategory(Category.COMBAT);
		addSetting(yAxis);
	}
	
	@Override
	protected void onEnable()
	{
		hasLeft = false;
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// check gamemode
		if(MC.player == null || MC.player.getAbilities().creativeMode)
			return;

		// check if player in water
		if (MC.player.isTouchingWater())
			return;

		// check Y-Axis
		if (MC.player.getY() <= yAxis.getValue()) {
			if (!hasLeft) {
				hasLeft = true;
				left();
			}
		} else {
			hasLeft = false;
		}
	}

	private void left()
	{
		new Thread(() -> {
			try {
				MC.player.networkHandler.sendChatMessage("/next");
				Thread.sleep(322);
				MC.player.networkHandler.sendChatMessage("/next");
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}
}
