/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.TextFieldSetting;

@SearchTags({"auto leave", "AutoDisconnect", "auto disconnect", "AutoQuit",
	"auto quit", "AutoLeave"})
public final class AutoYLeaveHack extends Hack implements UpdateListener
{
	private boolean hasLeft = false;

	private final SliderSetting yAxis = new SliderSetting("Y Axis",
		"Leaves the server when the Y-axis reaches this value or falls below it.",
		-60, -100, 500, 1, ValueDisplay.DECIMAL);

	public final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
			"\u00a7lQuit\u00a7r mode just quits the game normally.\n\n"
					+ "\u00a7lCommand\u00a7r mode sends a command to the chat (/leave, /hub, /spawn, etc.).\n\n"
					+ "\u00a7lDoubleCommand\u00a7r same as command mode, but sends the command twice.\n\n"
					+ "\u00a7lChars\u00a7r mode sends a special chat message that causes the server to kick you.\n\n"
					+ "\u00a7lSelfHurt\u00a7r mode sends a packet that attacks itself, causing the server to kick you.\n",
			Mode.values(), Mode.Command);

	private final TextFieldSetting leaveCmd = new TextFieldSetting("Command sent when leaving", "[Only Command/DoubleCommand mode] Examples: /leave, /hub, /spawn, etc.", "/leave");

	private final TextFieldSetting secondLeaveCmd = new TextFieldSetting("Second command sent when leaving", "[Only DoubleCommand mode] Examples: /leave, /hub, /spawn, etc.", "/leave");

	private final SliderSetting delay = new SliderSetting("Delay between commands",
			"[Only DoubleCommand mode] Delay between sending command in ms",
			322, 0, 1000, 1, ValueDisplay.DECIMAL);

	private final CheckboxSetting checkWater = new CheckboxSetting("Check water", "Don't leave if player in water", true);

	private final CheckboxSetting disableAutoReconnect = new CheckboxSetting(
			"Disable AutoReconnect", "Automatically turns off AutoReconnect when"
			+ " AutoYLeave makes you leave the server.",
			true);

	public AutoYLeaveHack()
	{
		super("AutoYLeave");
		setCategory(Category.COMBAT);
		addSetting(mode);
		addSetting(yAxis);
		addSetting(leaveCmd);
		addSetting(secondLeaveCmd);
		addSetting(delay);
		addSetting(checkWater);
		addSetting(disableAutoReconnect);
	}
	
	@Override
	protected void onEnable()
	{
		hasLeft = false;
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
		// check gamemode
		if(MC.player == null || MC.player.getAbilities().creativeMode)
			return;

		// check if player in water
		if (checkWater.isChecked() && MC.player.isTouchingWater())
			return;

		// check Y-Axis
		if (MC.player.getY() <= yAxis.getValue()) {
			if (!hasLeft) {
				hasLeft = true;

				if(disableAutoReconnect.isChecked())
					WURST.getHax().autoReconnectHack.setEnabled(false);

				left();
			}
		} else {
			hasLeft = false;
		}
	}

	private void left()
	{
		mode.getSelected().leave(this);
	}

	private enum Mode
	{
		Quit("Quit") {
			@Override
			public void leave(AutoYLeaveHack hack) {
				MC.world.disconnect();
			}
		},
		Command("Command") {
			@Override
			public void leave(AutoYLeaveHack hack) {
				MC.player.networkHandler.sendChatMessage(hack.leaveCmd.getValue());
			}
		},
		DoubleCommand("DoubleCommand") {
			@Override
			public void leave(AutoYLeaveHack hack) {
				new Thread(() -> {
					try {
						MC.player.networkHandler.sendChatMessage(hack.leaveCmd.getValue());
						Thread.sleep(hack.delay.getValueI());
						MC.player.networkHandler.sendChatMessage(hack.secondLeaveCmd.getValue());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}).start();
			}
		},
		Chars("Chars") {
			@Override
			public void leave(AutoYLeaveHack hack) {
				MC.player.networkHandler.sendChatMessage("\u00a7");
			}
		},
		SelfHurt("SelfHurt") {
			@Override
			public void leave(AutoYLeaveHack hack) {
				MC.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(MC.player, MC.player.isSneaking()));
			}
		};

		private final String name;

		Mode(String name) {
			this.name = name;
		}

		public abstract void leave(AutoYLeaveHack hack);

		@Override
		public String toString() {
			return name;
		}
	}

}
