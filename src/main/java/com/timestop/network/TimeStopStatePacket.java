package com.timestop.network;

import com.timestop.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 时停状态包（服务端 -> 客户端）：同步 active、停时者 UUID、白名单与 FTB 同队豁免玩家。
 * <p>
 * 白名单与豁免玩家随包下发，保证客户端动画冻结/输入锁定的豁免判定与服务端一致。
 */
public class TimeStopStatePacket {
    private final boolean active;
    @Nullable
    private final UUID stopperUUID;
    private final List<String> whitelist;
    private final List<UUID> exemptPlayers;

    public TimeStopStatePacket(boolean active, @Nullable UUID stopperUUID, List<String> whitelist, List<UUID> exemptPlayers) {
        this.active = active;
        this.stopperUUID = stopperUUID;
        this.whitelist = whitelist;
        this.exemptPlayers = exemptPlayers;
    }

    public boolean isActive() {
        return active;
    }

    @Nullable
    public UUID getStopperUUID() {
        return stopperUUID;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public List<UUID> getExemptPlayers() {
        return exemptPlayers;
    }

    public static void encode(TimeStopStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.stopperUUID != null);
        if (msg.stopperUUID != null) {
            buf.writeUUID(msg.stopperUUID);
        }
        buf.writeVarInt(msg.whitelist.size());
        for (String id : msg.whitelist) {
            buf.writeUtf(id);
        }
        buf.writeVarInt(msg.exemptPlayers.size());
        for (UUID uuid : msg.exemptPlayers) {
            buf.writeUUID(uuid);
        }
    }

    public static TimeStopStatePacket decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        UUID stopper = buf.readBoolean() ? buf.readUUID() : null;
        int wlSize = buf.readVarInt();
        List<String> whitelist = new ArrayList<>(wlSize);
        for (int i = 0; i < wlSize; i++) {
            whitelist.add(buf.readUtf());
        }
        int exSize = buf.readVarInt();
        List<UUID> exempt = new ArrayList<>(exSize);
        for (int i = 0; i < exSize; i++) {
            exempt.add(buf.readUUID());
        }
        return new TimeStopStatePacket(active, stopper, whitelist, exempt);
    }

    public static void handle(TimeStopStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                ClientPacketHandler.handleState(msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
