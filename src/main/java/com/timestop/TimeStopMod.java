package com.timestop;

import com.mojang.logging.LogUtils;
import com.timestop.config.TimeStopConfig;
import com.timestop.network.ModNetworking;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * 时停模组主类。
 * <p>
 * M0：工程骨架（物品/创造标签/配置框架）。
 * M1：注册时停核心事件处理器（冻结逻辑）。
 * M2：注册网络通道（时停状态同步到客户端）与多人交互拦截。
 * M3：解除后无敌帧移除窗口（连击）。
 * M4：白名单通配支持 + 车万女仆软依赖适配。
 */
@Mod(TimeStopMod.MODID)
public class TimeStopMod {
    public static final String MODID = "timestop";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TimeStopMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // M0：注册物品与创造标签
        ModRegistries.register(modEventBus);

        // M0：注册服务端配置文件（白名单、无敌帧窗口等）
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TimeStopConfig.SERVER_SPEC);

        // M1：注册时停冻结逻辑到 Forge 事件总线
        MinecraftForge.EVENT_BUS.register(TimeStopFreezer.class);

        // M2：注册网络通道（时停状态同步到客户端）
        ModNetworking.register();

        LOGGER.info("[{}] 初始化完成（M5：打磨与发布准备）", MODID);
    }
}
