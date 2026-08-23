package com.timestop.item;

import com.mojang.logging.LogUtils;
import com.timestop.ModRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 时间瓶充能机制（服务端）：
 * <ul>
 *   <li>手持玻璃瓶（主手或副手）连续 10 分钟 → 变为时间瓶；</li>
 *   <li>使用快捷键切换主/副手不打断计时（物品仍在手部槽位）；</li>
 *   <li>物品离开主/副手槽位则打断计时（重置）。</li>
 * </ul>
 */
public final class TimeBottleHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** 10 分钟 = 600 秒 = 12000 tick。 */
    public static final int BOTTLE_TICKS = 20 * 60 * 10;

    private static final Map<UUID, Integer> PROGRESS = new HashMap<>();

    private TimeBottleHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            PROGRESS.remove(event.getEntity().getUUID());
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        Inventory inv = player.getInventory();
        boolean holdingBottle = inv.getItem(inv.selected).is(Items.GLASS_BOTTLE)
                || inv.offhand.get(0).is(Items.GLASS_BOTTLE);

        UUID id = player.getUUID();
        if (!holdingBottle) {
            // 离开主/副手槽位 → 打断计时
            PROGRESS.remove(id);
            return;
        }

        int ticks = PROGRESS.getOrDefault(id, 0) + 1;
        if (ticks >= BOTTLE_TICKS) {
            convert(player, inv);
            PROGRESS.remove(id);
        } else {
            PROGRESS.put(id, ticks);
            // 每 1 分钟提示一次进度（1200 tick = 1 分钟）
            if (ticks % 1200 == 0) {
                player.displayClientMessage(
                        Component.translatable("message.timestop.time_bottle_progress", ticks / 1200, 10), true);
            }
        }
    }

    private static void convert(ServerPlayer player, Inventory inv) {
        ItemStack main = inv.getItem(inv.selected);
        ItemStack off = inv.offhand.get(0);
        ItemStack bottle = new ItemStack(ModRegistries.TIME_BOTTLE.get());

        boolean converted = false;
        if (main.is(Items.GLASS_BOTTLE)) {
            if (main.getCount() == 1) {
                inv.setItem(inv.selected, bottle);
            } else {
                main.shrink(1);
                give(player, bottle);
            }
            converted = true;
        } else if (off.is(Items.GLASS_BOTTLE)) {
            if (off.getCount() == 1) {
                inv.offhand.set(0, bottle);
            } else {
                off.shrink(1);
                give(player, bottle);
            }
            converted = true;
        }

        if (converted) {
            player.displayClientMessage(Component.translatable("message.timestop.time_bottle_complete"), true);
            player.playNotifySound(SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
            LOGGER.info("Time bottle created for {}", player.getGameProfile().getName());
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack.copy())) {
            player.drop(stack, false);
        }
    }
}
