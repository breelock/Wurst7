/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.google.gson.JsonArray;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

@SearchTags({"auto leave", "AutoDisconnect", "auto disconnect", "AutoQuit",
	"auto quit"})
public final class AutoFriendsHack extends Hack implements UpdateListener
{
	private TreeSet<String> tempFriends = new TreeSet<>();
	private final Path path = MC.runDirectory.toPath().normalize().resolve("wurst").resolve("tempFriends.json");

	private final SliderSetting perimeter = new SliderSetting("Detect perimeter",
			"Detect perimeter",
			25, 0, 100, 1, SliderSetting.ValueDisplay.DECIMAL);

	public AutoFriendsHack()
	{
		super("AutoFriends");
		setCategory(Category.COMBAT);
		addSetting(perimeter);
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
        try { // getting list of temp frens
			tempFriends.clear();
            tempFriends.addAll(JsonUtils.parseFileToArray(path).getAllStrings());
        } catch (IOException | JsonException e) {
			tempFriends = new TreeSet<>();
        }

		if (tempFriends != null && !tempFriends.isEmpty()) {
			// del temp frens
			ArrayList<String> tmpFren = new ArrayList<>(tempFriends);
			for (int i = 0; i < tempFriends.size(); i++) {
				String name = tmpFren.get(i);
				if (WURST.getFriends().contains(name)) {
					WURST.getFriends().removeAndSave(name);
					// ChatUtils.message("Deleted temp fren \"" + name + "\".");
				}
			}

			// clear temp frens
			tempFriends.clear();
            try {
                JsonUtils.toJson(new JsonArray(), path);
            } catch (IOException | JsonException e) {
                //
            }
        }

		if (WurstClient.MC.player != null && WurstClient.MC.world != null) {
			AbstractClientPlayerEntity nearest = WurstClient.MC.world.getPlayers().stream()
					.filter(p -> !p.getName().getString().equals(WurstClient.MC.player.getName().getString()))
					.filter(p -> p.squaredDistanceTo(WurstClient.MC.player) <= perimeter.getValue())
					.min(Comparator.comparingDouble(p -> p.squaredDistanceTo(WurstClient.MC.player)))
					.orElse(null);

			if (nearest != null) {
				String name = nearest.getEntityName();
				if (!WURST.getFriends().contains(name)) {
					WURST.getFriends().addAndSave(name);

					// add temp fren
					if (tempFriends != null)
						tempFriends.add(name);

					// save to json
                    try {
                        JsonUtils.toJson(createJson(), path);
                    } catch (IOException | JsonException e) {
                        //
                    }

                    // ChatUtils.message("Detected temp fren \"" + name + "\".");
				}
			}
		}
	}

	private JsonArray createJson()
	{
		JsonArray json = new JsonArray();
		tempFriends.forEach(json::add);
		return json;
	}
}
