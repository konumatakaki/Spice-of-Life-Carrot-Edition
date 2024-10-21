package com.cazsius.solcarrot.communication;

import com.cazsius.solcarrot.SOLCarrot;
import com.cazsius.solcarrot.tracking.FoodList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FoodListMessage(CompoundTag capabilityNBT) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, FoodListMessage> CODEC = CustomPacketPayload.codec(
			FoodListMessage::write,
			FoodListMessage::new);
	public static final Type<FoodListMessage> ID = new Type<>(SOLCarrot.resourceLocation("food_list_message"));

	public FoodListMessage(FriendlyByteBuf buffer) {
		this(buffer.readNbt());
	}

	public FoodListMessage(FoodList foodList, HolderLookup.Provider provider) {
		this(foodList.serializeNBT(provider));
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeNbt(capabilityNBT);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
	
	public static class Handler {
		public static void handle(FoodListMessage message, IPayloadContext context) {
			context.enqueueWork(() -> {
				Player player = Minecraft.getInstance().player;
				assert player != null;
				FoodList.get(player).deserializeNBT(player.registryAccess(), message.capabilityNBT);
			});
		}
	}
}
