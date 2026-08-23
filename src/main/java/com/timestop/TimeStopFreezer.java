package com.timestop;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 时停冻结逻辑（M1：单机核心）。
 * <p>
 * 两条主线：
 * <ul>
 *   <li>{@link #onLivingTick(LivingEvent.LivingTickEvent)}：取消被冻结生物实体的 tick。
 *       Forge 1.20.1 中该事件在 {@code LivingEntity.tick()} 第一行、{@code super.tick()} 之前触发，
 *       取消后重力/移动/AI（都在 aiStep）全部不执行 → 位置天然静止，无抽动。</li>
 *   <li>{@link #onServerTick(TickEvent.ServerTickEvent)}：每 tick 兜底——清空被冻结实体的遗留速度；
 *       对没有 LivingTickEvent 的非生物实体做快照复位。</li>
 * </ul>
 */
public final class TimeStopFreezer {
    private TimeStopFreezer() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        if (!TimeStopState.isActive()) {
            TimeStopState.clearSnapshots();
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!TimeStopState.shouldFreeze(entity)) {
                    continue;
                }

                // TACZ/投掷物适配：先记录原始速度（解除时恢复，子弹续飞）
                TimeStopState.captureMotion(entity);

                // 1) 清空遗留速度（防止解除瞬间残留速度造成位移）
                entity.setDeltaMovement(Vec3.ZERO);

                if (entity instanceof LivingEntity living) {
                    if (living instanceof Mob mob) {
                        mob.setJumping(false);
                    }
                    // 生物实体：tick 已被取消，位置天然静止，不做每 tick 复位（避免抽动）
                } else {
                    // 非生物实体（箭/船/矿车/掉落物/经验球…）：快照复位兜底
                    FrozenSnapshot snap = TimeStopState.snapshotFor(entity);
                    entity.moveTo(snap.pos().x, snap.pos().y, snap.pos().z, snap.yRot(), snap.xRot());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (TimeStopState.shouldFreeze(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTickEnd(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // M3（按需求调整）：无敌帧移除窗口 = 整个时停期间 + 时停结束后 invulnWindowTicks(0.5s)
        boolean active = TimeStopState.isActive();
        if (!active && TimeStopState.getInvulnWindowTicks() <= 0) {
            return;
        }
        if (!active) {
            TimeStopState.tickInvulnWindow(); // 仅在收尾阶段递减
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) {
                    living.invulnerableTime = 0;
                    living.hurtTime = 0;
                }
            }
        }
    }
}
