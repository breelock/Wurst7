package net.wurstclient.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoFriendsHack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class GetScreenTitleMixin {
	private boolean detected = false;
	AutoFriendsHack h = WurstClient.INSTANCE.getHax().autoFriendsHack;

	@Inject(method = "onTitle", at = @At("HEAD"))
	public void onTitle(TitleS2CPacket packet, CallbackInfo ci) {
		Text text = packet.getTitle();
		if (h.isEnabled() && text != null && text.getString().toLowerCase().trim().equals(h.titleText.getValue())) {
			if (!detected) {
				detected = true;
				h.trigger();
			}
		}
		else {
			detected = false;
		}
	}
}
