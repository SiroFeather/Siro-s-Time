package com.timestop;

import com.timestop.config.TimeStopConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 时停白名单缓存（M4：支持 modid:* 通配）。
 * <p>
 * 把配置里的实体注册名解析为 {@link EntityType} 集合与通配命名空间集合并缓存，
 * 仅在配置列表内容变化时重建，避免每 tick 反复解析字符串。
 * <p>
 * 配置示例：
 * <ul>
 *   <li>{@code "minecraft:zombie"} —— 精确匹配单个实体；</li>
 *   <li>{@code "touhou_little_maid:*"} —— 匹配某 mod 的所有实体（命名空间通配）。</li>
 * </ul>
 */
@SuppressWarnings("deprecation") // 1.20.1 标准注册表访问，Mojang 建议用 RegistryAccess
public final class TimeStopWhitelist {
    private static List<? extends String> cachedConfig = List.of();
    private static Set<EntityType<?>> cachedTypes = Set.of();
    private static Set<String> cachedWildcardNamespaces = Set.of();

    private TimeStopWhitelist() {
    }

    public static boolean contains(EntityType<?> type) {
        List<? extends String> config = TimeStopConfig.whitelist();
        if (!Objects.equals(config, cachedConfig)) {
            cachedConfig = List.copyOf(config);
            rebuild(config);
        }
        if (cachedTypes.contains(type)) {
            return true;
        }
        if (!cachedWildcardNamespaces.isEmpty()) {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (key != null && cachedWildcardNamespaces.contains(key.getNamespace())) {
                return true;
            }
        }
        return false;
    }

    private static void rebuild(List<? extends String> ids) {
        Set<EntityType<?>> exact = new HashSet<>();
        Set<String> wildcards = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            if (id.endsWith(":*")) {
                String namespace = id.substring(0, id.length() - 2);
                if (ResourceLocation.isValidNamespace(namespace)) {
                    wildcards.add(namespace);
                }
                continue;
            }
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                BuiltInRegistries.ENTITY_TYPE.getOptional(rl).ifPresent(exact::add);
            }
        }
        cachedTypes = Set.copyOf(exact);
        cachedWildcardNamespaces = Set.copyOf(wildcards);
    }
}
