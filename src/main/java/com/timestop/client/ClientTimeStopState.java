package com.timestop.client;

import com.timestop.TimeStopCompat;
import com.timestop.TimeStopWhitelist;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端侧的时停状态镜像（由 {@link ClientPacketHandler} 通过状态包更新）。
 * <p>
 * 包含 active、停时者 UUID、同步来的白名单与 FTB 同队豁免玩家，
 * 驱动客户端表现：动画冻结、输入锁定、滤镜。
 */
public final class ClientTimeStopState {
    private static volatile boolean active = false;
    @Nullable
    private static volatile UUID stopperUUID = null;
    private static volatile TimeStopWhitelist.Whitelist whitelist = TimeStopWhitelist.Whitelist.EMPTY;
    private static volatile Set<UUID> exemptPlayers = Set.of();

    private ClientTimeStopState() {
    }

    public static void set(boolean active, @Nullable UUID stopperUUID, List<String> whitelistIds, List<UUID> exemptUuids) {
        ClientTimeStopState.active = active;
        ClientTimeStopState.stopperUUID = stopperUUID;
        ClientTimeStopState.whitelist = TimeStopWhitelist.build(whitelistIds);
        ClientTimeStopState.exemptPlayers = Set.copyOf(new HashSet<>(exemptUuids));
    }

    public static boolean isActive() {
        return active;
    }

    /** 该玩家是否就是时停者本人（可自由行动）。 */
    public static boolean isStopper(Player player) {
        return active && stopperUUID != null && stopperUUID.equals(player.getUUID());
    }

    /** 该玩家是否在 FTB 同队豁免名单内（服务端同步）。 */
    public static boolean isExemptPlayer(Player player) {
        return exemptPlayers.contains(player.getUUID());
    }

    /** 实体是否属于 FTB 同队豁免玩家（玩家自身或某人的仆从/被驯服实体）。 */
    public static boolean isExemptEntity(Entity entity) {
        if (exemptPlayers.isEmpty()) {
            return false;
        }
        UUID owner = TimeStopCompat.resolveOwner(entity);
        return owner != null && exemptPlayers.contains(owner);
    }

    /** 实体是否在（服务端同步来的）白名单内。 */
    public static boolean isWhitelisted(EntityType<?> type) {
        return TimeStopWhitelist.contains(type, whitelist);
    }
}
