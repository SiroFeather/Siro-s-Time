package com.timestop.item;

import net.minecraft.world.item.Item;

/**
 * 时间瓶：无实际效果，仅作为时停怀表的合成材料。
 * <p>
 * 由手持玻璃瓶（主手或副手）连续 10 分钟充能获得（见 {@link TimeBottleHandler}）。
 */
public class TimeBottleItem extends Item {
    public TimeBottleItem(Properties properties) {
        super(properties);
    }
}
