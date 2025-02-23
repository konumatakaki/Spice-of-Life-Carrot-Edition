package com.cazsius.solcarrot;

import com.cazsius.solcarrot.communication.ConstructFoodsMessage;
import com.cazsius.solcarrot.tracking.CapabilityHandler;
import com.cazsius.solcarrot.tracking.FoodList;
import com.google.common.collect.Lists;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static net.neoforged.fml.common.EventBusSubscriber.Bus.MOD;

@EventBusSubscriber(modid = SOLCarrot.MOD_ID, bus = MOD)
public final class SOLCarrotConfig {
	private static String localizationPath(String path) {
		return "config." + SOLCarrot.MOD_ID + "." + path;
	}
	
	public static final Server SERVER;
	public static final ModConfigSpec SERVER_SPEC;
	
	static {
		Pair<Server, ModConfigSpec> specPair = new Builder().configure(Server::new);
		SERVER = specPair.getLeft();
		SERVER_SPEC = specPair.getRight();
	}
	
	public static final Client CLIENT;
	public static final ModConfigSpec CLIENT_SPEC;
	
	static {
		Pair<Client, ModConfigSpec> specPair = new Builder().configure(Client::new);
		CLIENT = specPair.getLeft();
		CLIENT_SPEC = specPair.getRight();
	}
	
	public static void setUp(ModContainer container, Dist dist) {
		container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
		container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);

		NeoForge.EVENT_BUS.addListener(SOLCarrotConfig::onLoggedIn);

		if (dist.isClient()) {
			container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		}
	}

	public static void onLoggedIn(PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.send(new ConstructFoodsMessage());
		}
	}
	
	@SubscribeEvent
	public static void onConfigReload(ModConfigEvent.Reloading event) {
		MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
		if (currentServer == null) return;
		
		var players = currentServer.getPlayerList();
		for (var player : players.getPlayers()) {
			FoodList.get(player).invalidateProgressInfo();
			CapabilityHandler.syncFoodList(player);
		}
	}
	
	public static int getBaseHearts() {
		return SERVER.baseHearts.get();
	}
	
	public static int getHeartsPerMilestone() {
		return SERVER.heartsPerMilestone.get();
	}
	
	public static List<Integer> getMilestones() {
		return new ArrayList<>(SERVER.milestones.get());
	}
	
	public static List<String> getBlacklist() {
		return new ArrayList<>(SERVER.blacklist.get());
	}
	
	public static List<String> getWhitelist() {
		return new ArrayList<>(SERVER.whitelist.get());
	}
	
	public static int getMinimumFoodValue() {
		return SERVER.minimumFoodValue.get();
	}
	
	public static boolean shouldResetOnDeath() {
		return SERVER.shouldResetOnDeath.get();
	}
	
	public static boolean limitProgressionToSurvival() {
		return SERVER.limitProgressionToSurvival.get();
	}
	
	public static class Server {
		public final IntValue baseHearts;
		public final IntValue heartsPerMilestone;
		public final ModConfigSpec.ConfigValue<List<? extends Integer>> milestones;
		
		public final ModConfigSpec.ConfigValue<List<? extends String>> blacklist;
		public final ModConfigSpec.ConfigValue<List<? extends String>> whitelist;
		public final IntValue minimumFoodValue;
		
		public final BooleanValue shouldResetOnDeath;
		public final BooleanValue limitProgressionToSurvival;
		
		Server(Builder builder) {
			builder.push("milestones");
			
			baseHearts = builder
				.translation(localizationPath("base_hearts"))
				.comment("Number of hearts you start out with.")
				.defineInRange("baseHearts", 10, 0, 1000);
			
			heartsPerMilestone = builder
				.translation(localizationPath("hearts_per_milestone"))
				.comment("Number of hearts you gain for reaching a new milestone.")
				.defineInRange("heartsPerMilestone", 2, 0, 1000);
			
			milestones = builder
				.translation(localizationPath("milestones"))
				.comment("A list of numbers of unique foods you need to eat to unlock each milestone, in ascending order. Naturally, adding more milestones lets you earn more hearts.")
				.defineList("milestones", Lists.newArrayList(5, 10, 15, 20, 25), () -> 0, e -> e instanceof Integer);
			
			builder.pop();
			builder.push("filtering");
			
			blacklist = builder
				.translation(localizationPath("blacklist"))
				.comment("Foods in this list won't affect the player's health nor show up in the food book.")
				.defineListAllowEmpty("blacklist", Lists.newArrayList(), () -> "", e -> e instanceof String);
			
			whitelist = builder
				.translation(localizationPath("whitelist"))
				.comment("When this list contains anything, the blacklist is ignored and instead only foods from here count.")
				.defineListAllowEmpty("whitelist", Lists.newArrayList(), () -> "", e -> e instanceof String);
			
			minimumFoodValue = builder
				.translation(localizationPath("minimum_food_value"))
				.comment("The minimum hunger value foods need to provide in order to count for milestones, in half drumsticks.")
				.defineInRange("minimumFoodValue", 1, 0, 1000);
			
			builder.pop();
			builder.push("miscellaneous");
			
			shouldResetOnDeath = builder
				.translation(localizationPath("reset_on_death"))
				.comment("Whether or not to reset the food list on death, effectively losing all bonus hearts.")
				.define("resetOnDeath", false);
			
			limitProgressionToSurvival = builder
				.translation(localizationPath("limit_progression_to_survival"))
				.comment("If true, eating foods outside of survival mode (e.g. creative/adventure) is not tracked and thus does not contribute towards progression.")
				.define("limitProgressionToSurvival", false);
			
			builder.pop();
		}
	}
	
	public static boolean shouldPlayMilestoneSounds() {
		return CLIENT.shouldPlayMilestoneSounds.get();
	}
	
	public static boolean shouldSpawnIntermediateParticles() {
		return CLIENT.shouldSpawnIntermediateParticles.get();
	}
	
	public static boolean shouldSpawnMilestoneParticles() {
		return CLIENT.shouldSpawnMilestoneParticles.get();
	}
	
	public static boolean isFoodTooltipEnabled() {
		return CLIENT.isFoodTooltipEnabled.get();
	}
	
	public static boolean shouldShowProgressAboveHotbar() {
		return CLIENT.shouldShowProgressAboveHotbar.get();
	}
	
	public static boolean shouldShowUneatenFoods() {
		return CLIENT.shouldShowUneatenFoods.get();
	}
	
	public static class Client {
		public final BooleanValue shouldPlayMilestoneSounds;
		public final BooleanValue shouldSpawnIntermediateParticles;
		public final BooleanValue shouldSpawnMilestoneParticles;
		
		public final BooleanValue isFoodTooltipEnabled;
		public final BooleanValue shouldShowProgressAboveHotbar;
		public final BooleanValue shouldShowUneatenFoods;
		
		Client(Builder builder) {
			builder.push("milestone celebration");
			
			shouldPlayMilestoneSounds = builder
				.translation(localizationPath("should_play_milestone_sounds"))
				.comment("If true, reaching a new milestone plays a ding sound.")
				.define("shouldPlayMilestoneSounds", true);
			
			shouldSpawnIntermediateParticles = builder
				.translation(localizationPath("should_spawn_intermediate_particles"))
				.comment("If true, trying a new food spawns particles.")
				.define("shouldSpawnIntermediateParticles", true);
			
			shouldSpawnMilestoneParticles = builder
				.translation(localizationPath("should_spawn_milestone_particles"))
				.comment("If true, reaching a new milestone spawns particles.")
				.define("shouldSpawnMilestoneParticles", true);
			
			builder.pop();
			builder.push("miscellaneous");
			
			isFoodTooltipEnabled = builder
				.translation(localizationPath("is_food_tooltip_enabled"))
				.comment("If true, foods indicate in their tooltips whether or not they have been eaten.")
				.define("isFoodTooltipEnabled", true);
			
			shouldShowProgressAboveHotbar = builder
				.translation(localizationPath("should_show_progress_above_hotbar"))
				.comment("Whether the messages notifying you of reaching new milestones should be displayed above the hotbar or in chat.")
				.define("shouldShowProgressAboveHotbar", true);
			
			shouldShowUneatenFoods = builder
				.translation(localizationPath("should_show_uneaten_foods"))
				.comment("If true, the food book also lists foods that you haven't eaten, in addition to the ones you have.")
				.define("shouldShowUneatenFoods", true);
			
			builder.pop();
		}
	}
	
	// TODO: investigate performance of all these get() calls
	
	public static int milestone(int i) {
		return SERVER.milestones.get().get(i);
	}
	
	public static int getMilestoneCount() {
		return SERVER.milestones.get().size();
	}
	
	public static int highestMilestone() {
		return milestone(getMilestoneCount() - 1);
	}
	
	public static boolean hasWhitelist() {
		return !SERVER.whitelist.get().isEmpty();
	}
	
	public static boolean isAllowed(Item food) {
		String id = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(food)).toString();
		if (hasWhitelist()) {
			return matchesAnyPattern(id, getWhitelist());
		} else {
			return !matchesAnyPattern(id, getBlacklist());
		}
	}

	public static boolean shouldCount(Item food) {
		return shouldCount(food.getDefaultInstance());
	}

	public static boolean shouldCount(ItemStack food) {
		return isHearty(food) && isAllowed(food.getItem());
	}

	public static boolean isHearty(ItemStack food) {
		var foodInfo = food.getFoodProperties(null);
		if (foodInfo == null) return false;
		return foodInfo.nutrition() >= SERVER.minimumFoodValue.get();
	}

	public static boolean isHearty(Item food) {
		return isHearty(food.getDefaultInstance());
	}
	
	private static boolean matchesAnyPattern(String query, Collection<? extends String> patterns) {
		for (String glob : patterns) {
			StringBuilder pattern = new StringBuilder(glob.length());
			for (String part : glob.split("\\*", -1)) {
				if (!part.isEmpty()) { // not necessary
					pattern.append(Pattern.quote(part));
				}
				pattern.append(".*");
			}
			
			// delete extraneous trailing ".*" wildcard
			pattern.delete(pattern.length() - 2, pattern.length());
			
			if (Pattern.matches(pattern.toString(), query)) {
				return true;
			}
		}
		return false;
	}
}
