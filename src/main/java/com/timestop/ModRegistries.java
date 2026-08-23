package com.timestop;

import com.timestop.item.TimeStopItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 注册表入口：物品、创造标签、音效（M0/M5）。
 */
public class ModRegistries {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, TimeStopMod.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TimeStopMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, TimeStopMod.MODID);

    /** 时停物品：右键切换时停状态。 */
    public static final RegistryObject<Item> TIME_STOP_ITEM =
            ITEMS.register("time_stop_item", () -> new TimeStopItem(new Item.Properties().stacksTo(1)));

    /** 时停开启音效（DIO voice）。 */
    public static final RegistryObject<SoundEvent> TIMESTOP_ACTIVATE = SOUND_EVENTS.register("timestop.activate",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TimeStopMod.MODID, "timestop.activate")));

    /** 时停解除音效。 */
    public static final RegistryObject<SoundEvent> TIMESTOP_DEACTIVATE = SOUND_EVENTS.register("timestop.deactivate",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TimeStopMod.MODID, "timestop.deactivate")));

    /** 模组创造标签。 */
    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register("timestop",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.timestop"))
                    .icon(() -> new ItemStack(TIME_STOP_ITEM.get()))
                    .displayItems((params, output) -> output.accept(TIME_STOP_ITEM.get()))
                    .build());

    private ModRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
    }
}
