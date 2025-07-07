/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.text.Text;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.PlayerMethods;
import net.wurstclient.util.Rand;

@SearchTags({"auto clicker", "clicker", "pvp", "click", "AutoClicker", "left", "mouse"})
public final class AutoClickerLeftHack extends Hack implements UpdateListener
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
			"\u00a7lOldPvP\u00a7r click spam (1.8).\n\n"
					+ "\u00a7lNewPvP\u00a7r wait for delay (1.9+).",
			Mode.values(), Mode.OldPvP);

	private final SliderSetting minDelay = new SliderSetting("Minimal delay (ticks)",
			0, 0, 20, 1, SliderSetting.ValueDisplay.INTEGER);

	private final SliderSetting maxDelay = new SliderSetting("Maximum delay (ticks)",
			0, 0, 20, 1, SliderSetting.ValueDisplay.INTEGER);

	public final CheckboxSetting interrupt = new CheckboxSetting(
			"Interrupt", "Interrupt the item use when attack.", true);

	private final CheckboxSetting showMessage = new CheckboxSetting(
			"Show message", "Show enable/disable message.", true);

	public final CheckboxSetting autoJump = new CheckboxSetting(
			"Auto jump", "Hack will jump when targeting an entity.", false);

	public final CheckboxSetting onlyEntity = new CheckboxSetting(
			"Only entity", "Hack will only attack the entity.", false);

	public AutoClickerLeftHack()
	{
		super("AutoClickerLeft");
		setCategory(Category.ITEMS);
		addSetting(mode);
		addSetting(interrupt);
		addSetting(showMessage);
		addSetting(autoJump);
		addSetting(onlyEntity);
		addSetting(minDelay);
		addSetting(maxDelay);
	}
	
	@Override
	protected void onEnable()
	{
		BtnIsNowEnabled = true;
		currentDelay = 0;

		String isNewPvPTranslate = mode.getSelected() == Mode.NewPvP ? "New PvP (1.9+)" : "Old PvP (1.8)";
		if (showMessage.isChecked())
			MC.player.sendMessage(Text.literal("[Left mouse button] AutoClicker " + "enabled" + " [" + isNewPvPTranslate + "]"), true);

		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		BtnIsNowEnabled = false;
		currentDelay = 0;

		String isNewPvPTranslate = mode.getSelected() == Mode.NewPvP ? "New PvP (1.9+)" : "Old PvP (1.8)";
		if (showMessage.isChecked())
			MC.player.sendMessage(Text.literal("[Left mouse button] AutoClicker " + "disabled" + " [" + isNewPvPTranslate + "]"), true);

		EVENTS.remove(UpdateListener.class, this);
	}

	private static boolean BtnIsNowEnabled = false;
	private static int currentDelay = 0;

	@Override
	public void onUpdate()
	{
		if (minDelay.getValueI() > maxDelay.getValueI()) {
			int mind = minDelay.getValueI();
			int maxd = maxDelay.getValueI();

			maxDelay.setValue(mind);
			minDelay.setValue(maxd);
		}

		if (BtnIsNowEnabled && MC.player != null && MC.currentScreen == null) {
			if (currentDelay <= 0) {
				PlayerMethods.attack(MC, mode.getSelected() == Mode.NewPvP);
				currentDelay = Rand.Int(minDelay.getValueI(), maxDelay.getValueI());
			}
			else
				currentDelay--;
		}
	}

	private enum Mode
	{
		OldPvP,
		NewPvP
	}
}
