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

@SearchTags({"auto clicker", "clicker", "pvp", "click", "AutoClicker", "right", "mouse"})
public final class AutoClickerRightHack extends Hack implements UpdateListener
{
	private final SliderSetting minDelay = new SliderSetting("Minimal delay (ticks)",
			0, 0, 20, 1, SliderSetting.ValueDisplay.INTEGER);

	private final SliderSetting maxDelay = new SliderSetting("Maximum delay (ticks)",
			0, 0, 20, 1, SliderSetting.ValueDisplay.INTEGER);

	private final CheckboxSetting showMessage = new CheckboxSetting(
			"Show message", "Show enable/disable message.", true);

	public final CheckboxSetting dontInteractWithEntities = new CheckboxSetting(
			"Don't interact with entities", "Don't interact with entities.", false);

	public final CheckboxSetting dontInteractWithBlocks = new CheckboxSetting(
			"Don't interact with blocks", "Don't interact with blocks.", false);

	public final CheckboxSetting dontInteractWithItems = new CheckboxSetting(
			"Don't interact with items", "Don't interact with items.", false);

	public AutoClickerRightHack()
	{
		super("AutoClickerRight");
		setCategory(Category.ITEMS);
		addSetting(showMessage);
		addSetting(dontInteractWithEntities);
		addSetting(dontInteractWithBlocks);
		addSetting(dontInteractWithItems);
		addSetting(minDelay);
		addSetting(maxDelay);
	}

	@Override
	protected void onEnable()
	{
		BtnIsNowEnabled = true;
		currentDelay = 0;

		if (showMessage.isChecked())
			MC.player.sendMessage(Text.literal("[Right mouse button] AutoClicker " + "enabled"), true);

		EVENTS.add(UpdateListener.class, this);
	}

	@Override
	protected void onDisable()
	{
		BtnIsNowEnabled = true;
		currentDelay = 0;

		if (showMessage.isChecked())
			MC.player.sendMessage(Text.literal("[Right mouse button] AutoClicker " + "disabled"), true);

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
				PlayerMethods.interact(MC);
				currentDelay = Rand.Int(minDelay.getValueI(), maxDelay.getValueI());
			}
			else
				currentDelay--;
		}
	}
}
