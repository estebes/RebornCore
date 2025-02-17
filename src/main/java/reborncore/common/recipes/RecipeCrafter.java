/*
 * Copyright (c) 2018 modmuss50 and Gigabit101
 *
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package reborncore.common.recipes;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import reborncore.RebornCore;
import reborncore.api.recipe.IRecipeCrafterProvider;
import reborncore.common.blocks.BlockMachineBase;
import reborncore.common.crafting.RebornRecipe;
import reborncore.common.crafting.RebornRecipeType;
import reborncore.common.crafting.ingredient.RebornIngredient;
import reborncore.common.util.ItemUtils;
import reborncore.common.util.RebornInventory;
import team.reborn.energy.Energy;
import team.reborn.energy.EnergySide;
import team.reborn.energy.EnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Use this in your blockEntity entity to craft things
 */
public class RecipeCrafter implements IUpgradeHandler {

	/**
	 * This is the recipe type to use
	 */
	public RebornRecipeType<?> recipeType;

	/**
	 * This is the parent blockEntity
	 */
	public BlockEntity blockEntity;

	/**
	 * This is the place to use the power from
	 */
	public EnergyStorage energy;

	public Optional<IUpgradeHandler> parentUpgradeHandler = Optional.empty();

	/**
	 * This is the amount of inputs that the setRecipe has
	 */
	public int inputs;

	/**
	 * This is the amount of outputs that the recipe has
	 */
	public int outputs;

	/**
	 * This is the inventory to use for the crafting
	 */
	public RebornInventory<?> inventory;

	/**
	 * This is the list of the slots that the crafting logic should look for the
	 * input item stacks.
	 */
	public int[] inputSlots;

	/**
	 * This is the list for the slots that the crafting logic should look fot
	 * the output item stacks.
	 */
	public int[] outputSlots;
	public RebornRecipe currentRecipe;
	public int currentTickTime = 0;
	public int currentNeededTicks = 1;// Set to 1 to stop rare crashes
	double lastEnergy;

	int ticksSinceLastChange;

	@Nullable
	public static ICrafterSoundHanlder soundHanlder = (firstRun, blockEntity) -> {};

	public RecipeCrafter(RebornRecipeType<?> recipeType, BlockEntity blockEntity, int inputs, int outputs, RebornInventory<?> inventory,
	                     int[] inputSlots, int[] outputSlots) {
		this.recipeType = recipeType;
		this.blockEntity = blockEntity;
		if (blockEntity instanceof EnergyStorage) {
			energy = (EnergyStorage) blockEntity;
		}
		if (blockEntity instanceof IUpgradeHandler) {
			parentUpgradeHandler = Optional.of((IUpgradeHandler) blockEntity);
		}
		this.inputs = inputs;
		this.outputs = outputs;
		this.inventory = inventory;
		this.inputSlots = inputSlots;
		this.outputSlots = outputSlots;
		if (!(blockEntity instanceof IRecipeCrafterProvider)) {
			RebornCore.LOGGER.error(blockEntity.getClass().getName() + " does not use IRecipeCrafterProvider report this to the issue tracker!");
		}
	}

	/**
	 * Call this on the blockEntity tick
	 */
	public void updateEntity() {
		if (blockEntity.getWorld().isClient) {
			return;
		}
		ticksSinceLastChange++;
		// Force a has chanced every second
		if (ticksSinceLastChange == 20) {
			setInvDirty(true);
			ticksSinceLastChange = 0;
			setIsActive();
		}
		// It will now look for new recipes.
		if (currentRecipe == null && isInvDirty()) {
			updateCurrentRecipe();
		}
		if (currentRecipe != null) {
			// If it doesn't have all the inputs reset
			if (isInvDirty() && !hasAllInputs()) {
				currentRecipe = null;
				currentTickTime = 0;
				setIsActive();
			}
			// If it has reached the recipe tick time
			if (currentRecipe != null && currentTickTime >= currentNeededTicks && hasAllInputs()) {
				boolean canGiveInvAll = true;
				// Checks to see if it can fit the output
				for (int i = 0; i < currentRecipe.getOutputs().size(); i++) {
					if (!canFitOutput(currentRecipe.getOutputs().get(i), outputSlots[i])) {
						canGiveInvAll = false;
					}
				}
				// The slots that have been filled
				ArrayList<Integer> filledSlots = new ArrayList<>();
				if (canGiveInvAll && currentRecipe.onCraft(blockEntity)) {
					for (int i = 0; i < currentRecipe.getOutputs().size(); i++) {
						// Checks it has not been filled
						if (!filledSlots.contains(outputSlots[i])) {
							// Fills the slot with the output stack
							fitStack(currentRecipe.getOutputs().get(i).copy(), outputSlots[i]);
							filledSlots.add(outputSlots[i]);
						}
					}
					// This uses all the inputs
					useAllInputs();
					// Reset
					currentRecipe = null;
					currentTickTime = 0;
					updateCurrentRecipe();
					//Update active sate if the blockEntity isnt going to start crafting again
					if (currentRecipe == null) {
						setIsActive();
					}
				}
			} else if (currentRecipe != null && currentTickTime < currentNeededTicks) {
				double useRequirement = getEuPerTick(currentRecipe.getPower());
				if(Energy.of(energy).use(useRequirement)){
					currentTickTime++;
					if (currentTickTime == 1 || currentTickTime % 20 == 0 && soundHanlder != null) {
						soundHanlder.playSound(false, blockEntity);
					}
				}
			}
		}
		setInvDirty(false);
	}

	public void updateCurrentRecipe() {
		currentTickTime = 0;
		for (RebornRecipe recipe : recipeType.getRecipes(blockEntity.getWorld())) {
			// This checks to see if it has all of the inputs
			if (recipe.canCraft(blockEntity) && hasAllInputs(recipe)) {
				// This checks to see if it can fit all of the outputs
				for (int i = 0; i < recipe.getOutputs().size(); i++) {
					if (!canFitOutput(recipe.getOutputs().get(i), outputSlots[i])) {
						currentRecipe = null;
						this.currentTickTime = 0;
						setIsActive();
						return;
					}
				}
				// Sets the current recipe then syncs
				setCurrentRecipe(recipe);
				this.currentNeededTicks = Math.max((int) (currentRecipe.getTime() * (1.0 - getSpeedMultiplier())), 1);
				this.currentTickTime = 0;
				setIsActive();
				return;
			}
		}
	}

	public boolean hasAllInputs() {
		return hasAllInputs(currentRecipe);
	}

	public boolean hasAllInputs(RebornRecipe recipeType) {
		if (recipeType == null) {
			return false;
		}
		for (RebornIngredient ingredient : recipeType.getRebornIngredients()) {
			boolean hasItem = false;
			for (int slot : inputSlots) {
				if (ingredient.test(inventory.getInvStack(slot))) {
					hasItem = true;
				}
			}
			if (!hasItem) {
				return false;
			}
		}
		return true;
	}

	public void useAllInputs() {
		if (currentRecipe == null) {
			return;
		}
		for (RebornIngredient ingredient : currentRecipe.getRebornIngredients()) {
			for (int inputSlot : inputSlots) {// Uses all of the inputs
				if (ingredient.test(inventory.getInvStack(inputSlot))) {
					inventory.shrinkSlot(inputSlot, ingredient.getCount());
					break;
				}
			}
		}
	}

	public boolean canFitOutput(ItemStack stack, int slot) {// Checks to see if it can fit the stack
		if (stack.isEmpty()) {
			return true;
		}
		if (inventory.getInvStack(slot).isEmpty()) {
			return true;
		}
		if (ItemUtils.isItemEqual(inventory.getInvStack(slot), stack, true, true)) {
			if (stack.getCount() + inventory.getInvStack(slot).getCount() <= stack.getMaxCount()) {
				return true;
			}
		}
		return false;
	}

	public void fitStack(ItemStack stack, int slot) {// This fits a stack into a slot
		if (stack.isEmpty()) {
			return;
		}
		if (inventory.getInvStack(slot).isEmpty()) {// If the slot is empty set the contents
			inventory.setInvStack(slot, stack);
			return;
		}
		if (ItemUtils.isItemEqual(inventory.getInvStack(slot), stack, true)) {// If the slot has stuff in
			if (stack.getCount() + inventory.getInvStack(slot).getCount() <= stack.getMaxCount()) {// Check to see if it fits
				ItemStack newStack = stack.copy();
				newStack.setCount(inventory.getInvStack(slot).getCount() + stack.getCount());// Sets
				// the
				// new
				// stack
				// size
				inventory.setInvStack(slot, newStack);
			}
		}
	}

	public void read(CompoundTag tag) {
		CompoundTag data = tag.getCompound("Crater");

		if (data.contains("currentTickTime")) {
			currentTickTime = data.getInt("currentTickTime");
		}

		if (blockEntity != null && blockEntity.getWorld() != null && blockEntity.getWorld().isClient) {
			blockEntity.getWorld().updateListeners(blockEntity.getPos(),
			                                  blockEntity.getWorld().getBlockState(blockEntity.getPos()),
			                                  blockEntity.getWorld().getBlockState(blockEntity.getPos()), 3);
		}
	}

	public void write(CompoundTag tag) {

		CompoundTag data = new CompoundTag();

		data.putDouble("currentTickTime", currentTickTime);

		tag.put("Crater", data);
	}

	private boolean isActive() {
		return currentRecipe != null && energy.getStored(EnergySide.UNKNOWN) >= currentRecipe.getPower();
	}

	public boolean canCraftAgain() {
		for (RebornRecipe recipe : recipeType.getRecipes(blockEntity.getWorld())) {
			if (recipe.canCraft(blockEntity) && hasAllInputs(recipe)) {
				boolean canGiveInvAll = true;
				for (int i = 0; i < recipe.getOutputs().size(); i++) {
					if (!canFitOutput(recipe.getOutputs().get(i), outputSlots[i])) {
						canGiveInvAll = false;
						return false;
					}
				}
				if (energy.getStored(EnergySide.UNKNOWN) < recipe.getPower()) {
					return false;
				}
				return canGiveInvAll;
			}
		}
		return false;
	}

	public void setIsActive() {
		if (blockEntity.getWorld().getBlockState(blockEntity.getPos()).getBlock() instanceof BlockMachineBase) {
			BlockMachineBase blockMachineBase = (BlockMachineBase) blockEntity.getWorld()
				.getBlockState(blockEntity.getPos()).getBlock();
			boolean isActive = isActive() || canCraftAgain();
			blockMachineBase.setActive(isActive, blockEntity.getWorld(), blockEntity.getPos());
		}
		blockEntity.getWorld().updateListeners(blockEntity.getPos(),
		                                  blockEntity.getWorld().getBlockState(blockEntity.getPos()),
		                                  blockEntity.getWorld().getBlockState(blockEntity.getPos()), 3);
	}

	public void setCurrentRecipe(RebornRecipe recipe) {
		this.currentRecipe = recipe;
	}

	public boolean isInvDirty() {
		return inventory.hasChanged();
	}

	public void setInvDirty(boolean isDiry) {
		inventory.setChanged(isDiry);
	}

	public boolean isStackValidInput(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		for (RebornRecipe recipe : recipeType.getRecipes(blockEntity.getWorld())) {
			for (RebornIngredient ingredient : recipe.getRebornIngredients()) {
				if (ingredient.test(stack)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void resetSpeedMulti() {
		parentUpgradeHandler.ifPresent(IUpgradeHandler::resetSpeedMulti);
	}

	@Override
	public double getSpeedMultiplier() {
		return Math.min(parentUpgradeHandler.map(IUpgradeHandler::getSpeedMultiplier).orElse(0D), 0.975);
	}

	@Override
	public void addPowerMulti(double amount) {
		parentUpgradeHandler.ifPresent(iUpgradeHandler -> iUpgradeHandler.addPowerMulti(amount));
	}

	@Override
	public void resetPowerMulti() {
		parentUpgradeHandler.ifPresent(IUpgradeHandler::resetPowerMulti);
	}

	@Override
	public double getPowerMultiplier() {
		return parentUpgradeHandler.map(IUpgradeHandler::getPowerMultiplier).orElse(1D);
	}

	@Override
	public double getEuPerTick(double baseEu) {
		double power = parentUpgradeHandler.map(iUpgradeHandler -> iUpgradeHandler.getEuPerTick(baseEu)).orElse(1D);
		return Math.min(power, energy.getMaxStoredPower());
	}

	@Override
	public void addSpeedMulti(double amount) {
		parentUpgradeHandler.ifPresent(iUpgradeHandler -> iUpgradeHandler.addSpeedMulti(amount));
	}
}
