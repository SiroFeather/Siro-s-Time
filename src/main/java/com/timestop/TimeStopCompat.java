package com.timestop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 外部模组软依赖适配。
 * <p>
 * 只在运行时通过 ModList 检测与实体注册名判断，<b>绝不直接引用对方模组的类</b>，
 * 因此未安装这些模组时本模组可正常运行（无硬依赖）。
 * <ul>
 *   <li>车万女仆（Touhou Little Maid）：女仆实体默认豁免 / 时停中跟随。</li>
 *   <li>TACZ（Timeless and Classics: Zero）：时停中可开枪，子弹定在空中，解除后续飞并造成伤害——
 *       该行为由通用"解除时恢复被冻结实体速度"机制覆盖，这里仅做检测与标识。</li>
 * </ul>
 */
@SuppressWarnings("deprecation") // 1.20.1 标准注册表访问，Mojang 建议用 RegistryAccess
public final class TimeStopCompat {
    /** 车万女仆模组 ID（软依赖）。 */
    public static final String MAID_MOD_ID = "touhou_little_maid";
    /** 女仆实体注册路径。 */
    public static final String MAID_ENTITY_PATH = "maid";
    /** TACZ 模组 ID（软依赖）。 */
    public static final String TACZ_MOD_ID = "tacz";
    /** FTB Teams 模组 ID（软依赖）。 */
    public static final String FTB_TEAMS_MOD_ID = "ftbteams";
    /** SlashBlade Resharped 模组 ID（软依赖）。 */
    public static final String SLASHBLADE_MOD_ID = "slashblade";

    private static Boolean maidModLoaded;
    private static Boolean taczLoaded;
    private static Boolean ftbTeamsLoaded;
    private static Boolean slashBladeLoaded;

    private TimeStopCompat() {
    }

    /** 车万女仆是否已安装（结果缓存）。 */
    public static boolean isMaidModLoaded() {
        if (maidModLoaded == null) {
            ModList modList = ModList.get();
            maidModLoaded = modList != null && modList.isLoaded(MAID_MOD_ID);
        }
        return maidModLoaded;
    }

    /** 判断实体是否为车万女仆的女仆实体（仅依据注册名，避免硬依赖）。 */
    public static boolean isMaid(Entity entity) {
        if (!isMaidModLoaded()) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null
                && key.getNamespace().equals(MAID_MOD_ID)
                && key.getPath().equals(MAID_ENTITY_PATH);
    }

    /** TACZ 是否已安装（结果缓存）。 */
    public static boolean isTaczLoaded() {
        if (taczLoaded == null) {
            ModList modList = ModList.get();
            taczLoaded = modList != null && modList.isLoaded(TACZ_MOD_ID);
        }
        return taczLoaded;
    }

    /** 判断实体是否来自 TACZ（按命名空间识别，含其子弹等实体；避免硬依赖）。 */
    public static boolean isTaczEntity(Entity entity) {
        if (!isTaczLoaded()) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && key.getNamespace().equals(TACZ_MOD_ID);
    }

    /** FTB Teams 是否已安装（结果缓存）。 */
    public static boolean isFtbTeamsLoaded() {
        if (ftbTeamsLoaded == null) {
            ModList modList = ModList.get();
            ftbTeamsLoaded = modList != null && modList.isLoaded(FTB_TEAMS_MOD_ID);
        }
        return ftbTeamsLoaded;
    }

    /**
     * 通过反射获取某玩家所属 FTB 队伍的全部成员 UUID；未安装/不在队伍时返回 null。
     * 不直接引用 FTB Teams 类，避免硬依赖。
     */
    @SuppressWarnings("unchecked")
    @javax.annotation.Nullable
    public static Set<UUID> getFtbTeamMembers(ServerPlayer player) {
        if (!isFtbTeamsLoaded()) {
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object api = apiClass.getMethod("api").invoke(null);
            Object manager = api.getClass().getMethod("getManager").invoke(api);

            Optional<?> teamOpt;
            try {
                Method byUuid = manager.getClass().getMethod("getPlayerTeamFor", UUID.class);
                teamOpt = (Optional<?>) byUuid.invoke(manager, player.getUUID());
            } catch (NoSuchMethodException e) {
                Method byPlayer = manager.getClass().getMethod("getPlayerTeamFor", ServerPlayer.class);
                teamOpt = (Optional<?>) byPlayer.invoke(manager, player);
            }
            if (teamOpt == null || !teamOpt.isPresent()) {
                return null;
            }
            Object team = teamOpt.get();
            Object members = team.getClass().getMethod("getMembers").invoke(team);
            return new HashSet<>((Set<UUID>) members);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析实体的"归属者"UUID：玩家返回自身；被驯服/可拥有实体返回主人；
     * 其余尝试读取常见仆从 NBT 字段。找不到返回 null。
     */
    @javax.annotation.Nullable
    public static UUID resolveOwner(Entity entity) {
        if (entity instanceof Player player) {
            return player.getUUID();
        }
        if (entity instanceof OwnableEntity ownable) {
            UUID owner = ownable.getOwnerUUID();
            if (owner != null) {
                return owner;
            }
        }
        if (entity.getPersistentData().isEmpty()) {
            return null;
        }
        CompoundTag tag = entity.getPersistentData();
        for (String key : new String[]{"OwnerUUID", "Owner", "owner", "OwnerID", "Summoner", "BoundPlayer"}) {
            if (tag.contains(key, Tag.TAG_STRING)) {
                try {
                    return UUID.fromString(tag.getString(key));
                } catch (IllegalArgumentException ignored) {
                }
            } else if (tag.contains(key, Tag.TAG_INT_ARRAY)) {
                int[] arr = tag.getIntArray(key);
                if (arr.length == 4) {
                    long msb = ((long) arr[0] << 32) | (arr[1] & 0xFFFFFFFFL);
                    long lsb = ((long) arr[2] << 32) | (arr[3] & 0xFFFFFFFFL);
                    return new UUID(msb, lsb);
                }
            }
        }
        return null;
    }

    /** SlashBlade Resharped 是否已安装（结果缓存）。 */
    public static boolean isSlashBladeLoaded() {
        if (slashBladeLoaded == null) {
            ModList modList = ModList.get();
            slashBladeLoaded = modList != null && modList.isLoaded(SLASHBLADE_MOD_ID);
        }
        return slashBladeLoaded;
    }

    /**
     * 判断是否为 SlashBlade 的投射类伤害实体（召唤剑/居合斩/斩击效果）。
     * 这些实体用 getDeltaMovement() 作为命中射线长度，冻结清零速度会导致射线零长度而无法命中。
     */
    public static boolean isSlashBladeProjectile(Entity entity) {
        if (!isSlashBladeLoaded()) {
            return false;
        }
        if (!(entity instanceof Projectile)) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && key.getNamespace().equals(SLASHBLADE_MOD_ID);
    }

    /** 获取投射物的归属者 UUID（Projectile.getOwner()）。 */
    @javax.annotation.Nullable
    public static UUID getProjectileOwnerUuid(Entity entity) {
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            return owner != null ? owner.getUUID() : null;
        }
        return null;
    }
}
