package com.github.alexthe666.rats;

import com.github.alexthe666.rats.client.ClientProxy;
import com.github.alexthe666.rats.server.CommonProxy;
import com.github.alexthe666.rats.server.items.RatsItemRegistry;
import com.github.alexthe666.rats.server.potion.PotionConfitByaldi;
import com.github.alexthe666.rats.server.potion.PotionPlague;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RatsMod.MODID)
public class RatsMod {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "diversebiomes";
    public static final String NAME = "Diverse Biomes";
    public static final String VERSION = "1.0";
    public static ItemGroup TAB = new ItemGroup(MODID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(RatsItemRegistry.CHEESE);
        }
    };
    public static ItemGroup TAB_UPGRADES = new ItemGroup("rats.upgrades") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(RatsItemRegistry.RAT_UPGRADE_BASIC);
        }
    };
    public static CommonProxy PROXY = DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    public static Potion CONFIT_BYALDI_POTION = new PotionConfitByaldi();
    public static Potion PLAGUE_POTION = new PotionPlague();

    public RatsMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
        MinecraftForge.EVENT_BUS.register(this);
        final ModLoadingContext modLoadingContext = ModLoadingContext.get();
        modLoadingContext.registerConfig(ModConfig.Type.CLIENT, ConfigHolder.CLIENT_SPEC);
        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ConfigHolder.SERVER_SPEC);
    }

    private void preInit(final FMLCommonSetupEvent event) {

    }
}
