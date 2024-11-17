package com.cazsius.solcarrot.item;

import com.cazsius.solcarrot.client.gui.FoodBookScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

public final class FoodBookItem extends Item {
	public FoodBookItem() {
		super(new Properties());
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		if (player.isLocalPlayer()) {
			if (FMLEnvironment.dist.isClient()) {
				FoodBookScreen.open(player);
			}
		}
		
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
	}
}
