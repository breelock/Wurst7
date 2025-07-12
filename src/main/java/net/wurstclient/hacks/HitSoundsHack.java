/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.TickScheduler;

import java.util.*;

import static net.wurstclient.util.PlayerMethods.getAttackCooldownInTicks;
import static net.wurstclient.util.PlayerMethods.getAttackSpeedInTicks;

@SearchTags({"hit sound", "hitsound", "sound", "sfx", "hit", "crit", "hot", "hotsound"})
public final class HitSoundsHack extends Hack
{
	private final CheckboxSetting onlyInCrit = new CheckboxSetting("Only in crit",
			"Play hit sound only when you attack is critical", true);

	private final CheckboxSetting oldPvpMode = new CheckboxSetting("1.8 pvp mode",
			"Do not take weapon readiness into account when checking critical hits.", true);

	private final SliderSetting pitch = new SliderSetting("Pitch", "Hit sounds pitch",
			1f, 0.5f, 2f, 0.01f, SliderSetting.ValueDisplay.DECIMAL);

	private final SliderSetting volume = new SliderSetting("Volume", "Hit sounds volume",
			1f, 0f, 1f, 0.01f, SliderSetting.ValueDisplay.DECIMAL);

	private final SliderSetting sfxDelay = new SliderSetting("Sfx delay (in seconds)", "Delay between sfx playbacks",
			0.45f, 0f, 3f, 0.01f, SliderSetting.ValueDisplay.DECIMAL);

	private final Random rand = new Random();
	private boolean sfxEnded = true;

	public HitSoundsHack()
	{
		super("HitSounds");
		setCategory(Category.COMBAT);
		addSetting(volume);
		addSetting(pitch);
		addSetting(onlyInCrit);
		addSetting(oldPvpMode);
		addSetting(sfxDelay);
	}
	
	@Override
	protected void onEnable()
	{
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!this.isEnabled() || !world.isClient)
				return ActionResult.PASS;

			if (onlyInCrit.isChecked() && isCriticalHit() || !onlyInCrit.isChecked())
				playHitSound(player);

			return ActionResult.PASS;
		});
	}

	private boolean isCriticalHit() {
		boolean isInLava = MC.world.getBlockState(MC.player.getBlockPos()).getBlock() == Blocks.LAVA;
		boolean isOnGround = MC.player.isOnGround() && !MC.player.isTouchingWater() && !isInLava;
		float cooldownTime = getAttackSpeedInTicks(MC.player) * 50;
		float attackCooldown = getAttackCooldownInTicks(MC.player) * 50;
		boolean kd = !oldPvpMode.isChecked() && attackCooldown >= cooldownTime || oldPvpMode.isChecked();

		return kd && !isOnGround && MC.player.getVelocity().y < -0.1;
	}

	private void playHitSound(PlayerEntity player) {
		if (WURST.hitSounds.isEmpty())
			return;

		if (sfxDelay.getValueF() > 0f && !sfxEnded)
			return;

		int indx = rand.nextInt(WURST.hitSounds.size());
		player.playSound(WURST.hitSounds.get(indx), SoundCategory.PLAYERS, volume.getValueF(), pitch.getValueF());

		if (sfxDelay.getValueF() > 0f) {
			sfxEnded = false;
			TickScheduler.schedule(Math.round((sfxDelay.getValueF() / pitch.getValueF()) * 20), () -> sfxEnded = true);
		}
	}
}

