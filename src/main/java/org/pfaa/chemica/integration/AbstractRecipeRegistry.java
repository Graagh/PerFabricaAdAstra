package org.pfaa.chemica.integration;

import java.util.Collections;
import java.util.List;

import org.pfaa.chemica.model.Strength;
import org.pfaa.chemica.processing.TemperatureLevel;
import org.pfaa.chemica.registration.RecipeRegistry;
import org.pfaa.chemica.util.ChanceStack;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public abstract class AbstractRecipeRegistry implements RecipeRegistry {
	@Override
	public void registerCastingRecipe(ItemStack input, ItemStack output, ItemStack flux, int temp) {}

	@Override
	public void registerCastingRecipe(FluidStack input, ItemStack output) {}

	@Override
	public void registerMeltingRecipe(ItemStack input, FluidStack output, int temp) {}

	@Override
	public void registerSmeltingRecipe(ItemStack input, ItemStack output, ItemStack flux, TemperatureLevel temp) {}

	@Override
	public void registerSmeltingRecipe(ItemStack input, FluidStack output, ItemStack flux, TemperatureLevel temp) {}

	@Override
	public void registerGrindingRecipe(ItemStack input, ItemStack output, List<ChanceStack> secondaries,
			Strength strength) {}

	@Override
	public void registerCrushingRecipe(ItemStack input, ItemStack output, ChanceStack dust, Strength strength) {
		this.registerGrindingRecipe(input, output, 
				dust != null ? Collections.singletonList(dust) : Collections.<ChanceStack>emptyList(), strength);
	}

	@Override
	public void registerAlloyingRecipe(ItemStack output, ItemStack base, List<ItemStack> solutes, int temp) {}

	@Override
	public void registerAlloyingRecipe(FluidStack output, List<FluidStack> inputs) {}

	@Override
	public void registerRoastingRecipe(List<ItemStack> inputs, ItemStack output, int temp) {}

	@Override
	public void registerAbsorptionRecipe(List<ItemStack> inputs, FluidStack additive, ItemStack output, int temp) {}

	@Override
	public void registerMixingRecipe(FluidStack input, List<ItemStack> additives, FluidStack output, int temp) {}
	
	private static Strength strengthFromOutputs(List<ChanceStack> outputs) {
		return Strength.values()[Math.min(Math.max(outputs.size() - 1, 1), Strength.values().length) - 1];
	}
	
	@Override
	public void registerMixingRecipe(List<ItemStack> inputs, ItemStack output) {}

	@Override
	public void registerPhysicalSeparationRecipe(ItemStack input, List<ChanceStack> outputs) {
		this.registerGrindingRecipe(input, outputs.get(0).itemStack, outputs.subList(1, outputs.size()), 
				strengthFromOutputs(outputs));
	}
}
