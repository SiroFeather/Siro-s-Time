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
 * 时停状态包（服务端 -> 客户端）：同步 active、停时者 UUID 与白名单。
 * <p>
 * 白名单随包下发，保证客户端动画冻结的豁免判定与服务端一致（服务端配置对客户端不可见）。
 */
public class TimeStopStatePacket {
    private final boolean active;
    @Nullable
    private final UUID stopperUUID;
    private final List<String> whitelist;

    public TimeStopStatePacket(boolean active, @Nullable UUID stopperUUID, List<String> whitelist) {
        this.active = active;
        this.stopperUUID = stopperUUID;
        this.whitelist = whitelist;
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
    }

    public static TimeStopStatePacket decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        UUID stopper = buf.readBoolean() ? buf.readUUID() : null;
        int size = buf.readVarInt();
        List<String> whitelist = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            whitelist.add(buf.readUtf());
        }
        return new TimeStopStatePacket(active, stopper, whitelist);
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
