package com.timestop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

/**
 * 车万女仆（Touhou Little Maid）软依赖适配（M4）。
 * <p>
 * 只在运行时通过 ModList 检测与实体注册名判断，<b>绝不直接引用该模组的类</b>，
 * 因此未安装该模组时本模组可正常运行（无硬依赖）。
 */
@SuppressWarnings("deprecation") // 1.20.1 标准注册表访问，Mojang 建议用 RegistryAccess
public final class TimeStopCompat {
    /** 车万女仆模组 ID（软依赖）。 */
    public static final String MAID_MOD_ID = "touhou_little_maid";
    /** 女仆实体注册路径。 */
    public static final String MAID_ENTITY_PATH = "maid";

    private static Boolean maidModLoaded;

    private TimeStopCompat() {
    }

    /** 车万女仆是否已安装（结果缓存）。 */
    public static boolean isMaidModLoaded() {
        if (maidModLoaded == null) {
            ModList modList = ModList.get();
            maidModLoaded = modList != null && modList.isLoaded(MAID_MOD_ID);
        }
        return maidModLoaded;
    }

    /** 判断实体是否为车万女仆的女仆实体（仅依据注册名，避免硬依赖）。 */
    public static boolean isMaid(Entity entity) {
        if (!isMaidModLoaded()) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null
                && key.getNamespace().equals(MAID_MOD_ID)
                && key.getPath().equals(MAID_ENTITY_PATH);
    }
}
