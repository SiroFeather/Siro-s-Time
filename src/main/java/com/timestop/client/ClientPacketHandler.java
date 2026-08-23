package com.timestop.client;

import com.timestop.network.TimeStopStatePacket;

/**
 * 客户端收包处理入口（仅客户端加载）。
 */
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleState(TimeStopStatePacket packet) {
        ClientTimeStopState.set(packet.isActive(), packet.getStopperUUID(), packet.getWhitelist());
    }
}
