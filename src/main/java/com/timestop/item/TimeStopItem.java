package com.timestop.item;

import com.timestop.TimeStopState;
import com.timestop.client.ClientTimeStopState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 时停物品：手持右键切换时停状态（服务端裁决）。
 * <p>
 * M5：Tooltip 显示当前时停状态（客户端）。
 */
public class TimeStopItem extends Item {
    public TimeStopItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            boolean toggled = TimeStopState.toggle(serverLevel, serverPlayer);
            if (!toggled) {
                // 时停已在进行且不是停时者本人
                serverPlayer.displayClientMessage(Component.translatable("message.timestop.denied"), true);
            }
        }
        // 客户端返回 sidedSuccess 以播放使用动画
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.timestop.time_stop_item.tooltip"));
        // M5：客户端根据状态包显示当前时停状态
        if (level != null && level.isClientSide()) {
            if (ClientTimeStopState.isActive()) {
                tooltip.add(Component.translatable("item.timestop.time_stop_item.status_active")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                tooltip.add(Component.translatable("item.timestop.time_stop_item.status_ready")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
