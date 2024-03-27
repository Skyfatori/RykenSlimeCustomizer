package org.lins.mmmjjkx.rykenslimefuncustomizer.objects.customs.item.exts;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactivity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.customs.item.CustomDefaultItem;

public class CustomDefaultRadiationItem extends CustomDefaultItem implements Radioactive {
    private final Radioactivity radioactivity;

    private final Object[] constructorArgs;

    public CustomDefaultRadiationItem(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, Radioactivity radioactivity) {
        super(itemGroup, item, recipeType, recipe);

        this.constructorArgs = new Object[] {itemGroup, item, recipeType, recipe, radioactivity};
        this.radioactivity = radioactivity;
    }

    @NotNull
    @Override
    public Radioactivity getRadioactivity() {
        return radioactivity;
    }

    @Override
    public Object[] constructorArgs() {
        return constructorArgs;
    }
}
