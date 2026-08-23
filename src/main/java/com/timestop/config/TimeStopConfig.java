package com.timestop.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * 时停模组服务端配置（M0：框架）。
 * <p>
 * 已定义（供后续里程碑使用）：
 * <ul>
 *   <li>whitelist —— 时停时不被冻结的实体注册名列表；</li>
 *   <li>invulnWindowTicks —— 解除后移除无敌帧的持续 tick 数；</li>
 *   <li>maidFollowDuringTimeStop —— 车万女仆在时停中是否继续跟随（软依赖）。</li>
 * </ul>
 */
public class TimeStopConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    private static final ConfigValues VALUES = new ConfigValues();

    static {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("timestop");

        VALUES.whitelist = builder
                .comment("实体注册名白名单：时停期间仍保持正常活动的实体。",
                        "支持精确 ID，如 \"minecraft:zombie\"、\"touhou_little_maid:maid\"；",
                        "也支持命名空间通配 modid:*，如 \"touhou_little_maid:*\" 豁免该 mod 全部实体。",
                        "默认已包含车万女仆的女仆实体。")
                .defineList("whitelist",
                        Arrays.asList("touhou_little_maid:maid"),
                        obj -> obj instanceof String);

        VALUES.invulnWindowTicks = builder
                .comment("时停解除后移除默认受伤无敌帧的持续时长（tick，20=1 秒）。默认 10 = 0.5 秒。")
                .defineInRange("invulnWindowTicks", 10, 0, 200);

        VALUES.maidFollowDuringTimeStop = builder
                .comment("若为 true 且安装了车万女仆（Touhou Little Maid），无论是否在白名单，",
                        "女仆实体在时停中都保持正常 AI（可继续跟随主人）。默认 false。")
                .define("maidFollowDuringTimeStop", false);

        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private TimeStopConfig() {
    }

    public static List<? extends String> whitelist() {
        return VALUES.whitelist.get();
    }

    public static int invulnWindowTicks() {
        return VALUES.invulnWindowTicks.get();
    }

    public static boolean maidFollowDuringTimeStop() {
        return VALUES.maidFollowDuringTimeStop.get();
    }

    private static final class ConfigValues {
        private ForgeConfigSpec.ConfigValue<List<? extends String>> whitelist;
        private ForgeConfigSpec.IntValue invulnWindowTicks;
        private ForgeConfigSpec.BooleanValue maidFollowDuringTimeStop;
    }
}
