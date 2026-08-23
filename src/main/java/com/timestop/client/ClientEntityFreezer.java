package com.timestop.client;

import com.mojang.logging.LogUtils;
import com.timestop.TimeStopMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 方案 A：非生物实体的客户端冻结器（仅客户端）。
 * <p>
 * Forge 1.20.1 没有通用的 {@code EntityTickEvent}，生物实体由 {@link ClientFreezer}
 * 通过取消 {@code LivingTickEvent} 停住；这里在客户端每 tick 遍历渲染实体，
 * 对"非生物实体"（掉落物、船、矿车、箭、经验球等）做快照复位：
 * 位置、朝向、速度、{@code tickCount}（从而停住物品旋转等按 tick 计时的渲染动画）。
 * <p>
 * 白名单判定使用状态包同步的列表（与服务端一致）。
 */
@Mod.EventBusSubscriber(modid = TimeStopMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEntityFreezer {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** key = 实体 id。 */
    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();

    private ClientEntityFreezer() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!ClientTimeStopState.isActive()) {
            if (!SNAPSHOTS.isEmpty()) {
                SNAPSHOTS.clear();
            }
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LivingEntity) {
                continue; // 生物实体由 ClientFreezer 处理
            }
            if (entity == Minecraft.getInstance().player) {
                continue; // 防御性：本地玩家永不被冻结
            }
            if (ClientTimeStopState.isExemptEntity(entity)) {
                continue; // FTB 同队玩家及其仆从不冻结
            }
            if (ClientTimeStopState.isWhitelisted(entity.getType())) {
                continue;
            }
            freeze(entity);
        }
    }

    private static void freeze(Entity entity) {
        Snapshot snap = SNAPSHOTS.computeIfAbsent(entity.getId(),
                id -> new Snapshot(entity.position(),
                        new Vec3(entity.xOld, entity.yOld, entity.zOld),
                        entity.getYRot(), entity.getXRot(), entity.tickCount));

        entity.setDeltaMovement(Vec3.ZERO);
        entity.moveTo(snap.pos().x, snap.pos().y, snap.pos().z, snap.yRot(), snap.xRot());
        entity.xOld = snap.oldPos().x;
        entity.yOld = snap.oldPos().y;
        entity.zOld = snap.oldPos().z;
        entity.tickCount = snap.tickCount();
    }

    /** 非生物实体冻结快照（位置/上一帧位置/朝向/计时刻）。 */
    private record Snapshot(Vec3 pos, Vec3 oldPos, float yRot, float xRot, int tickCount) {
    }
}
