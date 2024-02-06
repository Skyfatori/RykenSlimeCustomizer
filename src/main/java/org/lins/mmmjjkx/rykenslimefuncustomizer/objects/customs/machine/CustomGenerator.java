package org.lins.mmmjjkx.rykenslimefuncustomizer.objects.customs.machine;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.operations.FuelOperation;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lins.mmmjjkx.rykenslimefuncustomizer.RykenSlimefunCustomizer;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.customs.CustomMenu;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomGenerator extends AGenerator implements MachineProcessHolder<FuelOperation>, EnergyNetProvider {
    @Getter
    private final MachineProcessor<FuelOperation> processor = new MachineProcessor<>(this);
    private final int capacity;
    private final List<Integer> input;
    private final List<Integer> output;
    private final CustomMenu menu;
    private final int production;

    public CustomGenerator(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, @NotNull CustomMenu menu, int capacity, List<Integer> input, List<Integer> output, int production, List<MachineFuel> machineFuels) {
        super(itemGroup, item, recipeType, recipe);

        menu.setInvb(this);
        menu.reInit();

        this.capacity = capacity;
        this.input = input;
        this.output = output;
        this.production = production;
        this.menu = menu;

        setCapacity(capacity);
        setEnergyProduction(production);

        this.processor.setProgressBar(menu.getProgress());

        for (MachineFuel fuel : machineFuels) {
            registerFuel(fuel);
        }



        register(RykenSlimefunCustomizer.INSTANCE);
    }

    @Override
    public int getGeneratedOutput(@NotNull Location l, @NotNull SlimefunBlockData data) {
        BlockMenu inv = StorageCacheUtils.getMenu(l);
        FuelOperation operation = processor.getOperation(l);

        if (inv != null) {
            if (operation != null) {
                if (!operation.isFinished()) {
                    processor.updateProgressBar(inv, menu.getProgressSlot(), operation);

                    if (isChargeable()) {
                        int charge = getCharge(l, data);

                        if (getCapacity() - charge >= getEnergyProduction()) {
                            operation.addProgress(1);
                            return getEnergyProduction();
                        }

                        return 0;
                    } else {
                        operation.addProgress(1);
                        return getEnergyProduction();
                    }
                } else {
                    ItemStack fuel = operation.getIngredient();

                    if (isBucket(fuel)) {
                        inv.pushItem(new ItemStack(Material.BUCKET), getOutputSlots());
                    }

                    if (operation.getResult() != null) {
                        inv.pushItem(operation.getResult().clone(), getOutputSlots());
                    }

                    inv.replaceExistingItem(menu.getProgressSlot(), new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));

                    processor.endOperation(l);
                    return 0;
                }
            } else {
                Map<Integer, Integer> found = new HashMap<>();
                MachineFuel fuel = findRecipe(inv, found);

                if (fuel != null) {
                    for (Map.Entry<Integer, Integer> entry : found.entrySet()) {
                        inv.consumeItem(entry.getKey(), entry.getValue());
                    }

                    processor.startOperation(l, new FuelOperation(fuel));
                }

                return 0;
            }
        }
        return 0;
    }

    private boolean isBucket(ItemStack item) {
        if (item == null) {
            return false;
        }

        ItemStackWrapper wrapper = ItemStackWrapper.wrap(item);
        return item.getType() == Material.LAVA_BUCKET || SlimefunUtils.isItemSimilar(wrapper, SlimefunItems.FUEL_BUCKET, true) || SlimefunUtils.isItemSimilar(wrapper, SlimefunItems.OIL_BUCKET, true);
    }

    private MachineFuel findRecipe(BlockMenu menu, Map<Integer, Integer> found) {
        for (MachineFuel fuel : fuelTypes) {
            for (int slot : getInputSlots()) {
                if (fuel.test(menu.getItemInSlot(slot))) {
                    found.put(slot, fuel.getInput().getAmount());
                    return fuel;
                }
            }
        }

        return null;
    }

    @Nonnull
    protected BlockBreakHandler onBlockBreak() {
        return new SimpleBlockBreakHandler() {
            public void onBlockBreak(@NotNull Block b) {
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
                if (inv != null) {
                    inv.dropItems(b.getLocation(), CustomGenerator.this.getInputSlots());
                    inv.dropItems(b.getLocation(), CustomGenerator.this.getOutputSlots());
                }

                CustomGenerator.this.processor.endOperation(b);
            }
        };
    }

    @NotNull
    @Override
    public String getInventoryTitle() {
        return "";
    }

    @NotNull
    @Override
    //outside init
    public ItemStack getProgressBar() {
        return new ItemStack(Material.STONE, 1);
    }

    @Override
    public int getEnergyProduction() {
        return production;
    }

    @Override
    public @NotNull MachineProcessor<FuelOperation> getMachineProcessor() {
        return processor;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int[] getInputSlots() {
        if (this.input == null) return new int[0];

        int[] input = new int[this.input.size()];
        for (int i = 0; i < this.input.size(); i ++) {
            input[i] = this.input.get(i);
        }
        return input;
    }

    @Override
    public int[] getOutputSlots() {
        if (this.output == null) return new int[0];

        int[] output = new int[this.output.size()];
        for (int i = 0; i < this.output.size(); i ++) {
            output[i] = this.output.get(i);
        }
        return output;
    }

    /**
     * Outside init
     */
    @Override
    public void registerDefaultFuelTypes() {}
}
