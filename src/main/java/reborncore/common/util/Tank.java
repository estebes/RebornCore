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

package reborncore.common.util;

import reborncore.common.fluid.container.FluidInstance;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import reborncore.client.containerBuilder.builder.Syncable;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import net.minecraft.fluid.Fluid;
import reborncore.common.fluid.container.GenericFluidContainer;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Tank implements GenericFluidContainer<Direction>, Syncable {

	private final String name;
	@NonNull
	private FluidInstance fluidInstance = new FluidInstance();
	private final int capacity;

	@Nullable
	private Direction side = null;

	private final MachineBaseBlockEntity blockEntity;

	public Tank(String name, int capacity, MachineBaseBlockEntity blockEntity) {
		super();
		this.name = name;
		this.capacity = capacity;
		this.blockEntity = blockEntity;
	}

	@NonNull
	public FluidInstance getFluidInstance(){
		return getFluidInstance(side);
	}

	@NonNull
	public Fluid getFluid(){
		return getFluidInstance().getFluid();
	}

	public int getCapacity(){
		return capacity;
	}

	public int getFreeSpace(){
		return getCapacity() - getFluidAmount();
	}

	public boolean canFit(Fluid fluid, int amount) {
		return (getFluid() == Fluids.EMPTY || getFluid() == fluid) && getFreeSpace() > amount;
	}

	public boolean isEmpty() {
		return getFluidInstance().isEmpty();
	}

	public boolean isFull() {
		return !getFluidInstance().isEmpty() && getFluidInstance().getAmount() >= getCapacity();
	}

	public final CompoundTag write(CompoundTag nbt) {
		CompoundTag tankData = fluidInstance.write();
		nbt.put(name, tankData);
		return nbt;
	}

	public void setFluidAmount(int amount) {
		if (!fluidInstance.isEmpty()) {
			fluidInstance.setAmount(amount);
		}
	}

	public final Tank read(CompoundTag nbt) {
		if (nbt.contains(name)) {
			// allow to read empty tanks
			setFluid(Fluids.EMPTY);

			CompoundTag tankData = nbt.getCompound(name);
			fluidInstance = new FluidInstance(tankData);
		}
		return this;
	}

	public void setFluid(@NonNull Fluid f) {
		Validate.notNull(f);
		fluidInstance.setFluid(f);
	}

	@Nullable
	public Direction getSide() {
		return side;
	}

	public void setSide(
		@Nullable
			Direction side) {
		this.side = side;
	}

	@Override
	public void getSyncPair(List<Pair<Supplier, Consumer>> pairList) {
		pairList.add(Pair.of(() -> fluidInstance.getAmount(), o -> fluidInstance.setAmount((Integer) o)));
		pairList.add(Pair.of(() -> Registry.FLUID.getId(fluidInstance.getFluid()).toString(), (Consumer<String>) o -> fluidInstance.setFluid(Registry.FLUID.get(new Identifier(o)))));
	}

	public int getFluidAmount() {
		return getFluidInstance().getAmount();
	}

	@Override
	public void setFluid(@Nullable Direction type, @NonNull FluidInstance instance) {
		fluidInstance = instance;
	}

	@NonNull
	@Override
	public FluidInstance getFluidInstance(@Nullable Direction type) {
		return fluidInstance;
	}

	@Override
	public int getCapacity(@Nullable Direction type) {
		return capacity;
	}


}
