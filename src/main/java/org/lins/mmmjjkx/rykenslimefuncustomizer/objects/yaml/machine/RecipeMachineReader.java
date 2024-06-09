package org.lins.mmmjjkx.rykenslimefuncustomizer.objects.yaml.machine;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.ProjectAddon;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.customs.CustomMenu;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.customs.machine.CustomRecipeMachine;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.machine.CustomMachineRecipe;
import org.lins.mmmjjkx.rykenslimefuncustomizer.objects.yaml.YamlReader;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.CommonUtils;
import org.lins.mmmjjkx.rykenslimefuncustomizer.utils.ExceptionHandler;

public class RecipeMachineReader extends YamlReader<CustomRecipeMachine> {
    public RecipeMachineReader(YamlConfiguration config, ProjectAddon addon) {
        super(config, addon);
    }

    @Override
    public CustomRecipeMachine readEach(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        String id = section.getString(s + ".id_alias", s);

        ExceptionHandler.HandleResult result = ExceptionHandler.handleIdConflict(id);

        if (result == ExceptionHandler.HandleResult.FAILED) return null;

        String igId = section.getString("item_group");
        Pair<ExceptionHandler.HandleResult, ItemGroup> group = ExceptionHandler.handleItemGroupGet(addon, igId);
        if (group.getFirstValue() == ExceptionHandler.HandleResult.FAILED) return null;

        SlimefunItemStack slimefunItemStack = getPreloadItem(id);
        if (slimefunItemStack == null) return null;

        ItemStack[] recipe = CommonUtils.readRecipe(section.getConfigurationSection("recipe"), addon);
        String recipeType = section.getString("recipe_type", "NULL");

        Pair<ExceptionHandler.HandleResult, RecipeType> rt = ExceptionHandler.getRecipeType(
                "在附属" + addon.getAddonId() + "中加载配方机器" + s + "时遇到了问题: " + "错误的配方类型" + recipeType + "!", recipeType);

        if (rt.getFirstValue() == ExceptionHandler.HandleResult.FAILED) return null;

        CustomMenu menu = CommonUtils.getIf(addon.getMenus(), m -> m.getID().equalsIgnoreCase(s));

        List<Integer> input = section.getIntegerList("input");
        List<Integer> output = section.getIntegerList("output");

        if (input.isEmpty()) {
            ExceptionHandler.handleError("在附属" + addon.getAddonId() + "中加载配方机器" + s + "时遇到了问题: " + "输入槽为空");
            return null;
        }

        if (output.isEmpty()) {
            ExceptionHandler.handleError("在附属" + addon.getAddonId() + "中加载配方机器" + s + "时遇到了问题: " + "输出槽为空");
            return null;
        }

        ConfigurationSection recipes = section.getConfigurationSection("recipes");

        int capacity = section.getInt("capacity");

        if (capacity < 0) {
            ExceptionHandler.handleError("在附属" + addon.getAddonId() + "中加载配方机器" + s + "时遇到了问题: " + "能源容量小于0");
            return null;
        }

        int energy = section.getInt("energyPerCraft");

        if (energy <= 0) {
            ExceptionHandler.handleError(
                    "在附属" + addon.getAddonId() + "中加载配方机器" + s + "时遇到了问题: " + "合成一次的消耗能量未设置或小于等于0");
            return null;
        }

        int speed = section.getInt("speed");

        if (speed <= 0) {
            ExceptionHandler.handleError("在附属" + addon.getAddonId() + "中加载配方机器" + s + "时遇到了问题: " + "合成速度未设置或小于等于0");
            return null;
        }

        List<CustomMachineRecipe> mr = readRecipes(s, input.size(), output.size(), recipes, addon);

        return new CustomRecipeMachine(
                group.getSecondValue(),
                slimefunItemStack,
                rt.getSecondValue(),
                recipe,
                input.stream().mapToInt(x -> x).toArray(),
                output.stream().mapToInt(x -> x).toArray(),
                mr,
                energy,
                capacity,
                menu,
                speed);
    }

    @Override
    public List<SlimefunItemStack> preloadItems(String s) {
        ConfigurationSection section = configuration.getConfigurationSection(s);
        if (section == null) return null;
        String id = section.getString(s + ".id_alias", s);

        ConfigurationSection item = section.getConfigurationSection("item");
        ItemStack stack = CommonUtils.readItem(item, false, addon);

        if (stack == null) {
            ExceptionHandler.handleError("在附属" + addon.getAddonId() + "中加载配方机器" + s + "时遇到了问题: " + "物品为空或格式错误");
            return null;
        }

        return List.of(new SlimefunItemStack(id, stack));
    }

    private List<CustomMachineRecipe> readRecipes(
            String s, int inputSize, int outputSize, ConfigurationSection section, ProjectAddon addon) {
        List<CustomMachineRecipe> list = new ArrayList<>();
        if (section == null) {
            return list;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection recipes = section.getConfigurationSection(key);
            if (recipes == null) continue;
            int seconds = recipes.getInt("seconds");
            if (seconds < 0) {
                ExceptionHandler.handleError(
                        "在附属" + addon.getAddonId() + "中加载配方机器" + s + "的工作配方" + key + "时遇到了问题: " + "间隔时间未设置或不能小于0");
                continue;
            }
            ConfigurationSection inputs = recipes.getConfigurationSection("input");
            if (inputs == null) {
                ExceptionHandler.handleError(
                        "在附属" + addon.getAddonId() + "中加载配方机器" + s + "的工作配方" + key + "时遇到了问题: " + "没有输入物品");
                continue;
            }
            ItemStack[] input = CommonUtils.readRecipe(inputs, addon, inputSize);
            if (input == null) {
                ExceptionHandler.handleError(
                        "在附属" + addon.getAddonId() + "中加载配方机器" + s + "的工作配方" + key + "时遇到了问题: " + "输入物品为空或格式错误");
                continue;
            }
            ConfigurationSection outputs = recipes.getConfigurationSection("output");
            if (outputs == null) {
                ExceptionHandler.handleError(
                        "在附属" + addon.getAddonId() + "中加载配方机器" + s + "的工作配方" + key + "时遇到了问题: " + "没有输出物品");
                continue;
            }

            List<Integer> chances = new ArrayList<>();

            ItemStack[] output = new ItemStack[outputSize];
            for (int i = 0; i < outputSize; i++) {
                ConfigurationSection section1 = outputs.getConfigurationSection(String.valueOf(i + 1));
                var item = CommonUtils.readItem(section1, true, addon);
                if (item != null) {
                    int chance = section1.getInt("chance", 100);

                    if (chance < 1) {
                        ExceptionHandler.handleError("在附属" + addon.getAddonId() + "中加载配方机器" + s + "的工作配方" + key
                                + "时遇到了问题: " + "概率不应该小于1，已转为1");
                        chance = 1;
                    }

                    output[i] = item;
                    chances.add(chance);
                } else {
                    output[i] = null;
                }
            }

            boolean chooseOne = recipes.getBoolean("chooseOne", false);
            boolean forDisplay = recipes.getBoolean("forDisplay", false);

            input = removeNulls(input);
            output = removeNulls(output);

            list.add(new CustomMachineRecipe(seconds, input, output, chances, chooseOne, forDisplay));
        }
        return list;
    }

    private ItemStack[] removeNulls(ItemStack[] origin) {
        int count = 0;
        for (ItemStack element : origin) {
            if (element != null) {
                count++;
            }
        }
        ItemStack[] newArray = new ItemStack[count];

        int index = 0;
        for (ItemStack element : origin) {
            if (element != null) {
                newArray[index] = element;
                index++;
            }
        }

        return newArray;
    }
}
