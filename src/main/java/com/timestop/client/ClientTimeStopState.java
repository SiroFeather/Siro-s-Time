package com.timestop.client;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 客户端侧的时停状态镜像（由 {@link ClientPacketHandler} 通过状态包更新）。
 * <p>
 * 用于驱动客户端表现：动画冻结、被冻结玩家输入锁定。
 */
public final class ClientTimeStopState {
    private static volatile boolean active = false;
    @Nullable
    private static volatile UUID stopperUUID = null;

    private ClientTimeStopState() {
    }

    public static void set(boolean active, @Nullable UUID stopperUUID) {
        ClientTimeStopState.active = active;
        ClientTimeStopState.stopperUUID = stopperUUID;
    }

    public static boolean isActive() {
        return active;
    }

    /** 该玩家是否就是时停者本人（可自由行动）。 */
    public static boolean isStopper(Player player) {
        return active && stopperUUID != null && stopperUUID.equals(player.getUUID());
    }
}
