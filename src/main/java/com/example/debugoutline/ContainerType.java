package com.example.debugoutline;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;

/** Types de conteneurs suivis, avec leur couleur d'affichage. */
public enum ContainerType {
    CHEST("Coffres", 1.00f, 0.50f, 0.00f),
    TRAPPED_CHEST("Coffres piégés", 1.00f, 0.00f, 0.00f),
    BARREL("Barils", 1.00f, 1.00f, 0.00f),
    SHULKER("Shulkers", 0.80f, 0.20f, 1.00f);

    public final String label;
    public final float r, g, b;

    ContainerType(String label, float r, float g, float b) {
        this.label = label;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int argb() {
        return 0xFF000000
                | ((int) (r * 255) << 16)
                | ((int) (g * 255) << 8)
                | (int) (b * 255);
    }

    /** Retourne le type du conteneur, ou null si la block entity n'en est pas un. */
    public static ContainerType of(BlockEntity be) {
        // TrappedChestBlockEntity hérite de ChestBlockEntity : il faut le tester en premier.
        if (be instanceof TrappedChestBlockEntity) return TRAPPED_CHEST;
        if (be instanceof ChestBlockEntity) return CHEST;
        if (be instanceof BarrelBlockEntity) return BARREL;
        if (be instanceof ShulkerBoxBlockEntity) return SHULKER;
        return null;
    }
}
