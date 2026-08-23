package com.timestop.network;

import com.timestop.TimeStopMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.UUID;

/**
 * 模组网络通道（M2/M5/FTB 适配）。
 * <p>
 * 把时停状态、白名单与 FTB 同队豁免玩家同步到客户端。
 */
public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "2";
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

    /** 向服务器上所有玩家广播时停状态、白名单与 FTB 豁免玩家（须在 Server 线程调用）。 */
    public static void broadcastState(boolean active, UUID stopperUUID, List<String> whitelist, List<UUID> exemptPlayers) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new TimeStopStatePacket(active, stopperUUID, whitelist, exemptPlayers));
    }
}
