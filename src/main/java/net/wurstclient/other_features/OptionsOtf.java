/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.options.KeybindManagerScreen;
import net.wurstclient.options.WurstOptionsScreen;
import net.wurstclient.other_feature.OtherFeature;

@SearchTags({"WurstOptions", "Settings"})
@DontBlock
public final class OptionsOtf extends OtherFeature
{
	public OptionsOtf()
	{
		super("Options", "Options");
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "Open Options";
	}
	
	@Override
	public void doPrimaryAction()
	{
		MC.setScreen(new WurstOptionsScreen(MC.currentScreen));
	}
}
