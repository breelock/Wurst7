/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;

@DontBlock
@SearchTags({"RPC", "Discord", "Ds", "dsrpc", "dc"})
public final class DiscordRpcOtf extends OtherFeature
{
	private final CheckboxSetting enableDiscordRpc =
		new CheckboxSetting("Enable DiscordRPC", true);

	public DiscordRpcOtf()
	{
		super("DiscordRPC", "Enable DiscordRpc");
		addSetting(enableDiscordRpc);
	}
	
	@Override
	public boolean isEnabled()
	{
		return enableDiscordRpc.isChecked();
	}
}
