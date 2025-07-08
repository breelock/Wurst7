/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"NoWeather", "weather", "time", "ClientTime", "WorldTime", "change", "changeworldtime"})
public final class TimeChangerHack extends Hack
{
	public final CheckboxSetting changeTime =
		new CheckboxSetting("Change World Time", true);

	public final SliderSetting time =
		new SliderSetting("Time", 12000, 0, 23900, 1, ValueDisplay.INTEGER);

	public final CheckboxSetting changeMoonPhase =
		new CheckboxSetting("Change Moon Phase", true);

	public final SliderSetting moonPhase =
		new SliderSetting("Moon Phase", 0, 0, 7, 1, ValueDisplay.INTEGER);

	public final CheckboxSetting changeWeather =
			new CheckboxSetting("Change Weather", true);

	public final CheckboxSetting enableRain =
			new CheckboxSetting("Enable Rain", false);

	public final CheckboxSetting enableThunder =
			new CheckboxSetting("Enable Thunder", false);
	
	public TimeChangerHack()
	{
		super("TimeChanger");
		setCategory(Category.RENDER);

		addSetting(changeTime);
		addSetting(time);
		addSetting(changeMoonPhase);
		addSetting(moonPhase);
		addSetting(changeWeather);
		addSetting(enableRain);
		addSetting(enableThunder);
	}

	// See WorldMixin & ClientWorldMixin
}
