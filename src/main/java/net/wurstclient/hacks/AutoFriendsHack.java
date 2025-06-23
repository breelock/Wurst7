/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;

import java.util.Comparator;

@SearchTags({"auto leave", "AutoDisconnect", "auto disconnect", "AutoQuit",
	"auto quit"})
public final class AutoFriendsHack extends Hack implements UpdateListener
{
	private final CheckboxSetting clearFriends = new CheckboxSetting("Clear friends list",
			"Clear friends list", true);

	public AutoFriendsHack()
	{
		super("AutoFriends");
		setCategory(Category.COMBAT);
		addSetting(clearFriends);
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
	public void onUpdate()
	{

	}

	public void trigger()
	{
		if (clearFriends.isChecked())
			WURST.getFriends().removeAllAndSave();

		if (WurstClient.MC.player != null && WurstClient.MC.world != null) {
			AbstractClientPlayerEntity nearest = WurstClient.MC.world.getPlayers().stream()
					.filter(p -> !p.getName().getString().equals(WurstClient.MC.player.getName().getString()))
					.filter(p -> p.squaredDistanceTo(WurstClient.MC.player) <= 9.0)
					.min(Comparator.comparingDouble(p -> p.squaredDistanceTo(WurstClient.MC.player)))
					.orElse(null);

			if (nearest != null) {
				String name = nearest.getEntityName();
				if (!WURST.getFriends().contains(name)) {
					WURST.getFriends().addAndSave(name);
					ChatUtils.message("Detected friend \"" + name + "\".");
				}
			}
		}
	}
}
