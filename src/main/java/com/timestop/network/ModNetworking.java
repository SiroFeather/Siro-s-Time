package com.timestop.network;

import com.timestop.TimeStopMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;

/**
 * 模组网络通道（M2）。
 * <p>
 * 用于把时停状态同步到客户端，驱动客户端动画冻结与输入锁定。
 */
public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TimeStopMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetworking() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, TimeStopStatePacket.class,
                TimeStopStatePacket::encode,
                TimeStopStatePacket::decode,
                TimeStopStatePacket::handle);
    }

    /** 向服务器上所有玩家广播时停状态（须在 Server 线程调用）。 */
    public static void broadcastState(boolean active, UUID stopperUUID) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new TimeStopStatePacket(active, stopperUUID));
    }
}
