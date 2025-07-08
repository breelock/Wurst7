/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.TimeChangerHack;

@Mixin(World.class)
@Environment(EnvType.CLIENT)
public abstract class WorldMixin implements WorldAccess
{
	@Override
	public int getMoonPhase() {
		if(WurstClient.INSTANCE.getHax().timeChangerHack.isEnabled() &&
				WurstClient.INSTANCE.getHax().timeChangerHack.changeMoonPhase.isChecked() && this.getServer() == null)
			return WurstClient.INSTANCE.getHax().timeChangerHack.moonPhase.getValueI();
		return this.getDimension().getMoonPhase(this.getLunarTime());
	}

	@Override
	public float getMoonSize() {
		return DimensionType.MOON_SIZES[this.getMoonPhase()];
	}

	@Inject(at = @At("TAIL"), method = "getRainGradient", cancellable = true)
	private void getRainGradient(float delta, CallbackInfoReturnable<Float> ci) {
		if(this.getServer() != null) return;
		if(WurstClient.INSTANCE.getHax().timeChangerHack.isEnabled() &&
				WurstClient.INSTANCE.getHax().timeChangerHack.changeWeather.isChecked())
		{
			ci.setReturnValue(WurstClient.INSTANCE.getHax().timeChangerHack.enableRain.isChecked() ? 1f : 0f);
			return;
		}
	}

	@Inject(at = @At("TAIL"), method = "getThunderGradient", cancellable = true)
	private void getThunderGradient(float delta, CallbackInfoReturnable<Float> ci) {
		if(this.getServer() != null) return;
		if(WurstClient.INSTANCE.getHax().timeChangerHack.isEnabled() &&
				WurstClient.INSTANCE.getHax().timeChangerHack.changeWeather.isChecked())
		{
			ci.setReturnValue(WurstClient.INSTANCE.getHax().timeChangerHack.enableThunder.isChecked() ? 1f : 0f);
			return;
		}
	}

	@Inject(at = @At("TAIL"), method = "isRaining", cancellable = true)
	private void isRaining(CallbackInfoReturnable<Boolean> ci) {
		if(this.getServer() != null) return;
		if (WurstClient.INSTANCE.getHax().timeChangerHack.isEnabled() &&
				WurstClient.INSTANCE.getHax().timeChangerHack.changeWeather.isChecked()) {
			ci.setReturnValue(WurstClient.INSTANCE.getHax().timeChangerHack.enableRain.isChecked());
			return;
		}
	}

	@Inject(at = @At("TAIL"), method = "isThundering", cancellable = true)
	private void isThundering(CallbackInfoReturnable<Boolean> ci) {
		if(this.getServer() != null) return;
		if (WurstClient.INSTANCE.getHax().timeChangerHack.isEnabled() &&
				WurstClient.INSTANCE.getHax().timeChangerHack.changeWeather.isChecked()) {
			ci.setReturnValue(WurstClient.INSTANCE.getHax().timeChangerHack.enableThunder.isChecked());
			return;
		}
	}
}
