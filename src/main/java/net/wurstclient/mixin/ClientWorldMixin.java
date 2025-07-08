/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.world.GameMode;
import net.wurstclient.WurstClient;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldMixin
{
	@Inject(at = @At("TAIL"), method = "getTimeOfDay", cancellable = true)
	private void getTimeOfDay(CallbackInfoReturnable<Long> ci) {
		if (WurstClient.INSTANCE.getHax().timeChangerHack.isEnabled() &&
				WurstClient.INSTANCE.getHax().timeChangerHack.changeTime.isChecked()) {
			ci.setReturnValue((long) WurstClient.INSTANCE.getHax().timeChangerHack.time.getValueI());
			return;
		}
	}

	@Inject(at = @At("TAIL"), method = "isRaining", cancellable = true)
	private void isRaining(CallbackInfoReturnable<Boolean> ci) {
		if (WurstClient.INSTANCE.getHax().timeChangerHack.isEnabled() &&
				WurstClient.INSTANCE.getHax().timeChangerHack.changeWeather.isChecked()) {
			ci.setReturnValue(WurstClient.INSTANCE.getHax().timeChangerHack.enableRain.isChecked());
			return;
		}
	}

	@Inject(at = @At("TAIL"), method = "isThundering", cancellable = true)
	private void isThundering(CallbackInfoReturnable<Boolean> ci) {
		if (WurstClient.INSTANCE.getHax().timeChangerHack.isEnabled() &&
				WurstClient.INSTANCE.getHax().timeChangerHack.enableRain.isChecked()) {
			ci.setReturnValue(WurstClient.INSTANCE.getHax().timeChangerHack.enableThunder.isChecked());
			return;
		}
	}
}
