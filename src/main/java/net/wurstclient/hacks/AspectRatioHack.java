/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"aspect", "ratio", "screen", "resolution"})
public final class AspectRatioHack extends Hack
{
	public final SliderSetting aspectRatio = new SliderSetting("Aspect ratio", "Aspect ratio",
			1.3, 0.1, 10, 0.01, SliderSetting.ValueDisplay.DECIMAL);

	public AspectRatioHack()
	{
		super("AspectRatio");
		setCategory(Category.RENDER);
		addSetting(aspectRatio);
	}
}
