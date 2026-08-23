package com.timestop;

import net.minecraft.world.phys.Vec3;

/**
 * 实体冻结快照：记录冻结时刻的位置、朝向与速度。
 * <p>
 * 仅用于"非生物实体"（箭/船/矿车/掉落物/经验球等没有 LivingTickEvent 的实体）
 * 以及受外部推力实体的兜底复位；生物实体的静止由取消 LivingTickEvent 保证，
 * 不做每 tick 复位（避免与客户端插值打架造成视觉抽动）。
 */
public record FrozenSnapshot(Vec3 pos, float yRot, float xRot, Vec3 motion) {
}
