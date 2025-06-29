/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMultiplayerScreen;
import net.wurstclient.serverfinder.CleanUpScreen;
import net.wurstclient.serverfinder.ServerFinderScreen;
import net.wurstclient.util.LastServerRememberer;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen implements IMultiplayerScreen
{
	@Shadow
	protected MultiplayerServerListWidget serverListWidget;
	
	private MultiplayerScreenMixin(WurstClient wurst, Text title)
	{
		super(title);
	}
	
	@Override
	public MultiplayerServerListWidget getServerListSelector()
	{
		return serverListWidget;
	}
	
	@Override
	public void connectToServer(ServerInfo server)
	{
		connect(server);
	}
	
	@Shadow
	private void connect(ServerInfo entry)
	{
		
	}
}
