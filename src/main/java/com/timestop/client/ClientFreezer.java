package com.timestop.client;

import com.timestop.TimeStopMod;
import com.timestop.TimeStopWhitelist;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端动画冻结（仅客户端）：时停期间取消非本地玩家实体的客户端 tick，
 * 让被冻结实体停在当前姿势（停止走路摆动/挥臂/使用物品动画）。
 * <p>
 * 注意：不取消本地玩家自己的 tick（避免影响相机/渲染）。
 */
@Mod.EventBusSubscriber(modid = TimeStopMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientFreezer {
    private ClientFreezer() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ClientTimeStopState.isActive()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity == Minecraft.getInstance().player) {
            return; // 本地玩家不冻结
        }
        if (TimeStopWhitelist.contains(entity.getType())) {
            return;
        }
        event.setCanceled(true);
    }
}
