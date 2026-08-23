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
 * 时停白名单解析与判定（M4 支持 modid:* 通配；M5 客户端通过状态包复用同一套解析）。
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
    private static Whitelist cachedWhitelist = Whitelist.EMPTY;

    private TimeStopWhitelist() {
    }

    /** 服务端/本机配置判定（带缓存）。 */
    public static boolean contains(EntityType<?> type) {
        List<? extends String> config = TimeStopConfig.whitelist();
        if (!Objects.equals(config, cachedConfig)) {
            cachedConfig = List.copyOf(config);
            cachedWhitelist = build(cachedConfig);
        }
        return contains(type, cachedWhitelist);
    }

    /** 使用给定的白名单集合判定（客户端用状态包同步的列表）。 */
    public static boolean contains(EntityType<?> type, Whitelist whitelist) {
        if (whitelist.exact().contains(type)) {
            return true;
        }
        if (!whitelist.namespaces().isEmpty()) {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            return key != null && whitelist.namespaces().contains(key.getNamespace());
        }
        return false;
    }

    /** 把配置/包里的字符串列表解析为 {@link Whitelist}。 */
    public static Whitelist build(List<? extends String> ids) {
        Set<EntityType<?>> exact = new HashSet<>();
        Set<String> namespaces = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            if (id.endsWith(":*")) {
                String namespace = id.substring(0, id.length() - 2);
                if (ResourceLocation.isValidNamespace(namespace)) {
                    namespaces.add(namespace);
                }
                continue;
            }
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                BuiltInRegistries.ENTITY_TYPE.getOptional(rl).ifPresent(exact::add);
            }
        }
        return new Whitelist(Set.copyOf(exact), Set.copyOf(namespaces));
    }

    /** 精确实体集合 + 通配命名空间集合。 */
    public record Whitelist(Set<EntityType<?>> exact, Set<String> namespaces) {
        public static final Whitelist EMPTY = new Whitelist(Set.of(), Set.of());
    }
}
