package io.github.gjum.mc.tradex.mixin;

import io.github.gjum.mc.tradex.TradexMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class PacketHandlerMixin {
	@Inject(method = "handleLogin", at = @At("RETURN"))
	protected void handleGameJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
		if (!Minecraft.getInstance().isSameThread()) return; // will be called again on mc thread in a moment
		TradexMod.mod.handleJoinGame(packet);
	}

	// HEAD so our "find similar" message appears before the final cti message
	@Inject(method = "handleSystemChat", at = @At("HEAD"))
	protected void handleChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		if (!Minecraft.getInstance().isSameThread()) return; // will be called again on mc thread in a moment
		TradexMod.mod.handleReceivedChat(packet.content());
	}

	@Inject(method = "handleCommandSuggestions", at = @At("HEAD"))
	protected void handleChat(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
		if (!Minecraft.getInstance().isSameThread()) return; // will be called again on mc thread in a moment
		TradexMod.mod.handleReceivedTabComplete(packet.id());
	}
}