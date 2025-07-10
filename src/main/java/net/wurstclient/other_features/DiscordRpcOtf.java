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
import net.wurstclient.settings.TextFieldSetting;

@DontBlock
@SearchTags({"RPC", "Discord", "Ds", "dsrpc", "dc", "dcrpc"})
public final class DiscordRpcOtf extends OtherFeature
{
	private final CheckboxSetting enableDiscordRpc =
		new CheckboxSetting("Enable DiscordRPC", true);

	private final TextFieldSetting detailsRPC = new TextFieldSetting("Details",
			"Text displayed in DiscordRPC as details", "by breelock");

	public DiscordRpcOtf()
	{
		super("DiscordRPC", "Enable DiscordRpc");
		addSetting(enableDiscordRpc);
		addSetting(detailsRPC);
	}
	
	@Override
	public boolean isEnabled()
	{
		return enableDiscordRpc.isChecked();
	}

	public String getDetailsRPC()
	{
		return detailsRPC.getValue();
	}

	@Override
	public void doPrimaryAction()
	{
		enableDiscordRpc.setChecked(!enableDiscordRpc.isChecked());
	}

	@Override
	public String getPrimaryAction()
	{
		if (enableDiscordRpc.isChecked())
			return "Disable DiscordRPC";
		else
			return "Enable DiscordRPC";
	}
}
