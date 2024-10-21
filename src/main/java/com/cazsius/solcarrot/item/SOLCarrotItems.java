package com.cazsius.solcarrot.item;

import com.cazsius.solcarrot.SOLCarrot;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.neoforged.fml.common.EventBusSubscriber.Bus.MOD;

@EventBusSubscriber(modid = SOLCarrot.MOD_ID, bus = MOD)
public final class SOLCarrotItems {
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SOLCarrot.MOD_ID);
	
	public static final DeferredItem<FoodBookItem> FOOD_BOOK = ITEMS.register("food_book", FoodBookItem::new);
	
	public static void setUp(IEventBus eventBus) {
		ITEMS.register(eventBus);
	}
	
	@SubscribeEvent
	public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(FOOD_BOOK);
		}
	}
}
