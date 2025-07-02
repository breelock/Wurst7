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

@SearchTags({"RainbowGUI", "rainbow ui", "rainbow gui", "rgb"})
public final class RainbowUiHack extends Hack
{
	public final CheckboxSetting rainbowAccentColor = new CheckboxSetting("Rainbow accent color",
			"Rainbow accent color", false);

	public RainbowUiHack()
	{
		super("RainbowUI");
		setCategory(Category.FUN);
		addSetting(rainbowAccentColor);
	}
	
	// See ClickGui.updateColors()
}
