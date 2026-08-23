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
                .comment("无敌帧移除窗口：整个时停期间生效，并在时停结束后再延续该时长（tick，20=1 秒）。",
                        "默认 10 = 0.5 秒。窗口内连续攻击每次都能造成满额伤害。")
                .defineInRange("invulnWindowTicks", 10, 0, 200);

        VALUES.maidFollowDuringTimeStop = builder
                .comment("若为 true 且安装了车万女仆（Touhou Little Maid），无论是否在白名单，",
                        "女仆实体在时停中都保持正常 AI（可继续跟随主人）。默认 false。")
                .define("maidFollowDuringTimeStop", false);

        VALUES.resumeProjectilesOnRelease = builder
                .comment("时停解除时恢复被冻结实体的原始速度（子弹/箭矢/掉落物等继续飞行）。",
                        "用于适配 TACZ 等枪械模组：时停期间开枪，子弹定在空中，解除后续飞并造成伤害。默认 true。")
                .define("resumeProjectilesOnRelease", true);

        VALUES.ftbTeamFreezeExemption = builder
                .comment("FTB Teams 兼容：时停时自动豁免与时停者同队伍的玩家及其仆从/被驯服实体。",
                        "需安装 FTB Teams；默认 true。")
                .define("ftbTeamFreezeExemption", true);

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

    public static boolean resumeProjectilesOnRelease() {
        return VALUES.resumeProjectilesOnRelease.get();
    }

    public static boolean ftbTeamFreezeExemption() {
        return VALUES.ftbTeamFreezeExemption.get();
    }

    private static final class ConfigValues {
        private ForgeConfigSpec.ConfigValue<List<? extends String>> whitelist;
        private ForgeConfigSpec.IntValue invulnWindowTicks;
        private ForgeConfigSpec.BooleanValue maidFollowDuringTimeStop;
        private ForgeConfigSpec.BooleanValue resumeProjectilesOnRelease;
        private ForgeConfigSpec.BooleanValue ftbTeamFreezeExemption;
    }
}
