package com.cazsius.solcarrot.communication;

import com.cazsius.solcarrot.SOLCarrot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConstructFoodsMessage() implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ConstructFoodsMessage> CODEC = CustomPacketPayload.codec(
			ConstructFoodsMessage::write,
			ConstructFoodsMessage::new);
	public static final Type<ConstructFoodsMessage> ID = new Type<>(SOLCarrot.resourceLocation("construct_foods_message"));

	public ConstructFoodsMessage(FriendlyByteBuf buffer) {
		this();
	}

	public void write(FriendlyByteBuf buffer) {
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
