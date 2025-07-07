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
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.TextFieldSetting;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@SearchTags({"auto friends", "auto frens", "frens", "friends", "AutoFrens"})
public final class AutoFriendsHack extends Hack
{
	private TreeSet<String> tempFriends = new TreeSet<>();
	private final Path path = MC.runDirectory.toPath().normalize().resolve("wurst").resolve("tempFriends.json");

	public final TextFieldSetting titleText = new TextFieldSetting("Title text to trigger", "Title text to trigger", "1");

	private final SliderSetting maxTempFrens = new SliderSetting("Max temp frens",
			"Max temp frens", 3, 1, 10, 1, SliderSetting.ValueDisplay.DECIMAL);

	private final SliderSetting area = new SliderSetting("Detect area",
			"If area 25 frens be detected in 5x5 area",
			25, 0, 1000, 1, SliderSetting.ValueDisplay.DECIMAL);

	private final SliderSetting delay = new SliderSetting("Trigger delay (ms)",
			0, 0, 5000, 1, SliderSetting.ValueDisplay.INTEGER);

	public AutoFriendsHack()
	{
		super("AutoFriends");
		setCategory(Category.COMBAT);
		addSetting(area);
		addSetting(maxTempFrens);
		addSetting(titleText);
		addSetting(delay);
	}

	public void trigger()
	{
		new Thread(() -> {
			if (delay.getValueI() > 0)
				try { Thread.sleep(delay.getValueI()); } catch (InterruptedException ignored) { }

			// getting list of temp frens
			try {
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
					}
				}

				// clear temp frens
				tempFriends.clear();
				try { JsonUtils.toJson(new JsonArray(), path); } catch (IOException | JsonException ignored) { }
			}

			if (WurstClient.MC.player != null && WurstClient.MC.world != null) {
				List<AbstractClientPlayerEntity> nearestPlayers = WurstClient.MC.world.getPlayers().stream()
						.filter(p -> !p.equals(WurstClient.MC.player))
						.filter(p -> p.squaredDistanceTo(WurstClient.MC.player) <= area.getValue())
						.sorted(Comparator.comparingDouble(p -> p.squaredDistanceTo(WurstClient.MC.player)))
						.limit(maxTempFrens.getValueI()).toList();

				for (AbstractClientPlayerEntity player : nearestPlayers) {
					String name = player.getEntityName();
					if (!WURST.getFriends().contains(name)) {
						WURST.getFriends().addAndSave(name);

						if (tempFriends != null)
							tempFriends.add(name);

						try { JsonUtils.toJson(createJson(), path); } catch (IOException | JsonException ignored) { }
					}
				}
			}
		}).start();
	}

	private JsonArray createJson()
	{
		JsonArray json = new JsonArray();
		tempFriends.forEach(json::add);
		return json;
	}
}
