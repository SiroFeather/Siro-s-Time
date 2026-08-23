package com.timestop;

import com.mojang.logging.LogUtils;
import com.timestop.config.TimeStopConfig;
import com.timestop.network.ModNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /** 冻结时记录的实体原始速度（用于时停解除后恢复，使子弹/箭矢等续飞）。 */
    private static final Map<UUID, Vec3> savedMotions = new HashMap<>();
    /** 冻结时记录的实体 tickCount（年龄），冻结期间钉住以防生存时间/自动消失推进。 */
    private static final Map<UUID, Integer> savedTickCounts = new HashMap<>();

    /// 解除后移除受伤无敌帧的剩余窗口（tick）
    private static int invulnWindowTicks = 0;
    /** FTB Teams：与时停者同队伍的成员 UUID（null 表示未启用/未安装/不在队伍）。 */
    @org.jetbrains.annotations.Nullable
    private static Set<UUID> ftbTeamMembers = null;

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
        savedMotions.clear();
        savedTickCounts.clear();
        // FTB Teams：记录同队成员，时停中豁免
        ftbTeamMembers = TimeStopConfig.ftbTeamFreezeExemption()
                ? TimeStopCompat.getFtbTeamMembers(player) : null;
        player.displayClientMessage(Component.translatable("message.timestop.activated"), true);
        ModNetworking.broadcastState(true, stopperUUID, List.copyOf(TimeStopConfig.whitelist()), ftbExemptList());
        // M3（按需求调整）：无敌帧移除在“整个时停期间”生效，解除后再延续 0.5 秒
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
        ftbTeamMembers = null;
        snapshots.clear();
        // TACZ/投掷物适配：时停解除时恢复被冻结实体的速度（子弹续飞）
        if (TimeStopConfig.resumeProjectilesOnRelease()) {
            restoreMotions(level.getServer());
        }
        savedMotions.clear();
        savedTickCounts.clear();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.displayClientMessage(Component.translatable("message.timestop.deactivated"), true);
        }
        ModNetworking.broadcastState(false, null, List.copyOf(TimeStopConfig.whitelist()), List.of());
        // M3：时停结束后继续移除无敌帧 0.5 秒（invulnWindowTicks）
        startInvulnWindow(TimeStopConfig.invulnWindowTicks());
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
        // FTB Teams：豁免与时停者同队伍的玩家及其仆从/被驯服实体
        if (ftbTeamMembers != null) {
            UUID owner = TimeStopCompat.resolveOwner(entity);
            if (owner != null && ftbTeamMembers.contains(owner)) {
                return false;
            }
        }
        // SlashBlade 适配：豁免停时者（或同队玩家）发射的召唤剑/斩击投射物——
        // 其命中依赖 deltaMovement 射线长度，冻结清零速度会导致射线零长度而无法造成伤害
        if (TimeStopCompat.isSlashBladeProjectile(entity)) {
            UUID owner = TimeStopCompat.getProjectileOwnerUuid(entity);
            if (owner != null && (owner.equals(stopperUUID)
                    || (ftbTeamMembers != null && ftbTeamMembers.contains(owner)))) {
                return false;
            }
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

    /** 记录实体被冻结时的原始速度（仅在首次冻结时记录）。 */
    public static void captureMotion(Entity entity) {
        savedMotions.putIfAbsent(entity.getUUID(), entity.getDeltaMovement());
    }

    /** 记录实体被冻结时的 tickCount（年龄），用于冻结期间钉住以暂停生存时间。 */
    public static void captureTickCount(Entity entity) {
        savedTickCounts.putIfAbsent(entity.getUUID(), entity.tickCount);
    }

    /** 返回实体被冻结时记录的 tickCount；未记录返回 null。 */
    @org.jetbrains.annotations.Nullable
    public static Integer getSavedTickCount(Entity entity) {
        return savedTickCounts.get(entity.getUUID());
    }

    /** 时停解除时，把记录的速度恢复到对应实体上（子弹/箭矢/掉落物续飞）。 */
    public static void restoreMotions(MinecraftServer server) {
        if (savedMotions.isEmpty()) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                Vec3 motion = savedMotions.remove(entity.getUUID());
                if (motion != null) {
                    entity.setDeltaMovement(motion);
                }
            }
        }
        savedMotions.clear();
    }

    public static void clearSnapshots() {
        snapshots.clear();
        savedMotions.clear();
        savedTickCounts.clear();
    }

    /** FTB 同队豁免玩家 UUID 列表（广播给客户端用）。 */
    public static List<UUID> ftbExemptList() {
        return ftbTeamMembers == null ? List.of() : List.copyOf(ftbTeamMembers);
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
