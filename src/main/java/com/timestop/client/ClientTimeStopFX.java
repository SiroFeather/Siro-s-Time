package com.timestop.client;

import com.mojang.blaze3d.platform.Window;
import com.timestop.TimeStopMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 时停画面滤镜（仅客户端）：时停期间叠加一层半透明金色滤镜，
 * 模拟 "The World" 时停氛围。纯表现，不影响服务端逻辑。
 */
@Mod.EventBusSubscriber(modid = TimeStopMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientTimeStopFX {
    /** 半透明金色（ARGB）。 */
    private static final int FILTER_COLOR = 0x3CFFD000;

    private ClientTimeStopFX() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientTimeStopState.isActive()) {
            return;
        }
        Window window = event.getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        guiGraphics.fill(0, 0, width, height, FILTER_COLOR);
    }
}
