package com.timestop;

import com.mojang.logging.LogUtils;
import com.timestop.config.TimeStopConfig;
import com.timestop.network.ModNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 时停状态机（服务端权威）。
 * <p>
 * M1：右键切换、激活/解除状态、冻结判定（停时者豁免 / 白名单豁免）。
 * M2：激活/解除时通过状态包广播到客户端。
 */
public final class TimeStopState {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean active = false;
    private static volatile UUID stopperUUID = null;
    private static final Map<UUID, FrozenSnapshot> snapshots = new HashMap<>();

    /// 解除后移除受伤无敌帧的剩余窗口（tick）
    private static int invulnWindowTicks = 0;

    private TimeStopState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static UUID getStopperUUID() {
        return stopperUUID;
    }

    public static boolean isStopper(Player player) {
        return stopperUUID != null && stopperUUID.equals(player.getUUID());
    }

    /**
     * 右键切换时停。
     *
     * @return true 表示切换成功（启动或解除）；false 表示时停已在进行且不是停时者本人（被拒绝）。
     */
    public static boolean toggle(ServerLevel level, ServerPlayer player) {
        if (!active) {
            activate(level, player);
            return true;
        }
        if (isStopper(player)) {
            deactivate(level);
            return true;
        }
        return false;
    }

    private static void activate(ServerLevel level, ServerPlayer player) {
        active = true;
        stopperUUID = player.getUUID();
        snapshots.clear();
        player.displayClientMessage(Component.translatable("message.timestop.activated"), true);
        ModNetworking.broadcastState(true, stopperUUID);
        // M5：时停开启音效（DIO voice）+ 金色粒子爆发
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModRegistries.TIMESTOP_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                120, 2.0D, 1.0D, 2.0D, 0.05D);
        LOGGER.info("Time stop activated by {}", player.getGameProfile().getName());
    }

    private static void deactivate(ServerLevel level) {
        ServerPlayer stopper = null;
        if (stopperUUID != null) {
            stopper = level.getServer().getPlayerList().getPlayer(stopperUUID);
        }
        active = false;
        stopperUUID = null;
        snapshots.clear();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.displayClientMessage(Component.translatable("message.timestop.deactivated"), true);
        }
        ModNetworking.broadcastState(false, null);
        startInvulnWindow(com.timestop.config.TimeStopConfig.invulnWindowTicks());
        // M5：时停解除音效 + 传送门粒子
        if (stopper != null) {
            level.playSound(null, stopper.getX(), stopper.getY(), stopper.getZ(),
                    ModRegistries.TIMESTOP_DEACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.PORTAL,
                    stopper.getX(), stopper.getY() + 1.0D, stopper.getZ(),
                    100, 1.5D, 1.0D, 1.5D, 0.05D);
        }
        LOGGER.info("Time stop released");
    }

    /**
     * 判断某实体在时停期间是否应被冻结。
     * <p>
     * 豁免：停时者本人、停时者的坐骑/载具、白名单实体。
     */
    public static boolean shouldFreeze(Entity entity) {
        if (!active) {
            return false;
        }
        if (entity instanceof Player player && isStopper(player)) {
            return false;
        }
        // 豁免停时者骑乘的载具（坐骑/船/矿车）
        if (entity instanceof LivingEntity
                && entity.getControllingPassenger() instanceof Player rider
                && isStopper(rider)) {
            return false;
        }
        // M4：女仆跟随模式——开启且已安装车万女仆时，女仆保持正常 AI（可跟随主人）
        if (TimeStopConfig.maidFollowDuringTimeStop() && TimeStopCompat.isMaid(entity)) {
            return false;
        }
        if (TimeStopWhitelist.contains(entity.getType())) {
            return false;
        }
        return true;
    }

    /** 获取（或惰性创建）某非生物实体的冻结快照。 */
    public static FrozenSnapshot snapshotFor(Entity entity) {
        return snapshots.computeIfAbsent(entity.getUUID(), k -> new FrozenSnapshot(
                entity.position(), entity.getYRot(), entity.getXRot(), entity.getDeltaMovement()));
    }

    public static void clearSnapshots() {
        snapshots.clear();
    }

    public static void startInvulnWindow(int ticks) {
        invulnWindowTicks = Math.max(0, ticks);
    }

    public static int getInvulnWindowTicks() {
        return invulnWindowTicks;
    }

    public static void tickInvulnWindow() {
        if (invulnWindowTicks > 0) {
            invulnWindowTicks--;
        }
    }
}
