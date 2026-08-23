package com.timestop;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 多人安全（M2）：拦截被冻结玩家的交互与攻击。
 * <p>
 * 服务端权威判定：时停中且非停时者/非白名单 → 禁止使用物品、破坏/放置方块、
 * 交互实体、攻击实体。防止冻结玩家"动不了却能操作"。
 */
@Mod.EventBusSubscriber(modid = TimeStopMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeStopPlayerEvents {
    private TimeStopPlayerEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelIfFrozen(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfFrozen(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelIfFrozen(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfFrozen(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelIfFrozen(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        cancelIfFrozen(event.getEntity(), event);
    }

    private static void cancelIfFrozen(Player player, net.minecraftforge.eventbus.api.Event event) {
        // 仅在逻辑服务端裁决（客户端交给输入锁定/服务端同步处理）
        if (player.level().isClientSide()) {
            return;
        }
        if (TimeStopState.shouldFreeze(player)) {
            event.setCanceled(true);
        }
    }
}
