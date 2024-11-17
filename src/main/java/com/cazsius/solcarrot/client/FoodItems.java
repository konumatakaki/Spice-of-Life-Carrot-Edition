package com.cazsius.solcarrot.client;

import com.cazsius.solcarrot.SOLCarrot;
import com.cazsius.solcarrot.SOLCarrotConfig;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static net.neoforged.fml.common.EventBusSubscriber.Bus.MOD;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = SOLCarrot.MOD_ID, bus = MOD)
public final class FoodItems {
	private static List<Item> foodsBeforeBlacklist;
	private static List<Item> foods;
	private static boolean isConfigLoaded = false;
	
	/** @return a list of all item stacks that can be eaten, including blacklisted/hidden ones */
	public static List<Item> getAllFoodsIgnoringBlacklist() {
		return new ArrayList<>(foodsBeforeBlacklist);
	}
	
	/** @return a list of all item stacks that can be eaten */
	public static List<Item> getAllFoods() {
		return new ArrayList<>(foods);
	}
	
	@SubscribeEvent
	public static void setUp(FMLLoadCompleteEvent event) {
		foodsBeforeBlacklist = BuiltInRegistries.ITEM.stream()
			.filter((item) -> item.getDefaultInstance().getFoodProperties(null) != null)
			// sort by name
			.sorted(Comparator.comparing(food -> I18n.get(food.getDescriptionId() + ".name")))
			.collect(Collectors.toList());
		
		// depending on the other mods involved, config might be loaded before or after this event
		tryApplyBlacklist();
	}
	
	@SubscribeEvent
	public static void onConfigUpdate(ModConfigEvent event) {
		if (event.getConfig().getType() == ModConfig.Type.CLIENT) return;
		
		isConfigLoaded = true;
		
		tryApplyBlacklist();
	}
	
	private static void tryApplyBlacklist() {
		if (foodsBeforeBlacklist == null) return;
		if (!isConfigLoaded) return;
		
		foods = foodsBeforeBlacklist.stream()
			.filter(SOLCarrotConfig::isAllowed)
			.collect(Collectors.toList());
	}
}
