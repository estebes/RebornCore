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

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.Validate;
import reborncore.api.items.InventoryBase;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InventoryItem extends InventoryBase {

	@Nonnull
	ItemStack stack;
	int size;

	private InventoryItem(
		@Nonnull
			ItemStack stack, int size) {
		super(size);
		Validate.notNull(stack);
		Validate.isTrue(!stack.isEmpty());
		this.size = size;
		this.stack = stack;
	}

	public static InventoryItem getItemInvetory(ItemStack stack, int size) {
		return new InventoryItem(stack, size);
	}

	public ItemStack getStack() {
		return stack;
	}

	public CompoundTag getInvData() {
		Validate.isTrue(!stack.isEmpty());
		if (!stack.hasTag()) {
			stack.setTag(new CompoundTag());
		}
		if (!stack.getTag().contains("inventory")) {
			stack.getTag().put("inventory", new CompoundTag());
		}
		return stack.getTag().getCompound("inventory");
	}

	public CompoundTag getSlotData(int slot) {
		validateSlotIndex(slot);
		CompoundTag invData = getInvData();
		if (!invData.contains("slot_" + slot)) {
			invData.put("slot_" + slot, new CompoundTag());
		}
		return invData.getCompound("slot_" + slot);
	}

	public void setSlotData(int slot, CompoundTag tagCompound) {
		validateSlotIndex(slot);
		Validate.notNull(tagCompound);
		CompoundTag invData = getInvData();
		invData.put("slot_" + slot, tagCompound);
	}

	public List<ItemStack> getAllStacks() {
		return IntStream.range(0, size)
			.mapToObj(this::getInvStack)
			.collect(Collectors.toList());
	}

	public int getSlots() {
		return size;
	}

	@Nonnull
	@Override
	public ItemStack getInvStack(int slot) {
		return ItemStack.fromTag(getSlotData(slot));
	}

	@Override
	public void setInvStack(int slot,
	                           @Nonnull
		                           ItemStack stack) {
		setSlotData(slot, stack.toTag(new CompoundTag()));
	}

	public int getSlotLimit(int slot) {
		return 64;
	}

	public void validateSlotIndex(int slot) {
		if (slot < 0 || slot >= size) {
			throw new RuntimeException("Slot " + slot + " not in valid range - [0," + size + ")");
		}

	}

	public int getStackLimit(int slot,
	                         @Nonnull
		                         ItemStack stack) {
		return Math.min(getSlotLimit(slot), stack.getMaxCount());
	}

	@Override
	public boolean isValidInvStack(int slot, ItemStack stack) {
		return true;
	}

}
