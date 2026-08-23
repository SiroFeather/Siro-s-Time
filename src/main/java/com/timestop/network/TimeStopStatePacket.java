package com.timestop.network;

import com.timestop.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 时停状态包（服务端 -> 客户端）：同步 active 状态与停时者 UUID。
 */
public class TimeStopStatePacket {
    private final boolean active;
    @Nullable
    private final UUID stopperUUID;

    public TimeStopStatePacket(boolean active, @Nullable UUID stopperUUID) {
        this.active = active;
        this.stopperUUID = stopperUUID;
    }

    public boolean isActive() {
        return active;
    }

    @Nullable
    public UUID getStopperUUID() {
        return stopperUUID;
    }

    public static void encode(TimeStopStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.stopperUUID != null);
        if (msg.stopperUUID != null) {
            buf.writeUUID(msg.stopperUUID);
        }
    }

    public static TimeStopStatePacket decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        UUID stopper = buf.readBoolean() ? buf.readUUID() : null;
        return new TimeStopStatePacket(active, stopper);
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
