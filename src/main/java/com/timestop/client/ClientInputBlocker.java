package com.timestop.client;

import com.timestop.TimeStopMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 被冻结玩家的输入锁定（仅客户端）：
 * 时停中且自己不是停时者时，清零移动/跳跃/疾跑输入，
 * 防止"客户端本地预测在动、服务端拉回"造成的橡皮筋抖动。
 * <p>
 * 1.20.1 的 {@link Input} 使用 forwardImpulse/leftImpulse 与 up/down/left/right，
 * 在 {@link MovementInputUpdateEvent}（input.tick 之后触发）中清零即可覆盖计算后的输入。
 */
@Mod.EventBusSubscriber(modid = TimeStopMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientInputBlocker {
    private ClientInputBlocker() {
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientTimeStopState.isActive()) {
            return;
        }
        Player local = Minecraft.getInstance().player;
        if (local == null || event.getEntity() != local) {
            return;
        }
        if (ClientTimeStopState.isStopper(local) || ClientTimeStopState.isExemptPlayer(local)) {
            return; // 停时者 / FTB 同队玩家可自由移动
        }
        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }
}
