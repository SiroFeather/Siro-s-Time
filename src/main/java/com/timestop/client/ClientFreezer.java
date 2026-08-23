package com.timestop.client;

import com.timestop.TimeStopMod;
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
 * 豁免判定使用状态包同步的白名单（与服务端一致）。
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
        if (ClientTimeStopState.isExemptEntity(entity)) {
            return; // FTB 同队玩家及其仆从不冻结
        }
        if (ClientTimeStopState.isWhitelisted(entity.getType())) {
            return;
        }
        event.setCanceled(true);
    }
}
