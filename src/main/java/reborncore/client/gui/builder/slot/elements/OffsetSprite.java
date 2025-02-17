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

package reborncore.client.gui.builder.slot.elements;

import reborncore.common.blockentity.MachineBaseBlockEntity;

public class OffsetSprite {
	public ISprite sprite;
	public int offsetX = 0;
	public int offsetY = 0;

	public OffsetSprite(ISprite sprite, int offsetX, int offsetY) {
		this.sprite = sprite;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	public OffsetSprite(ISprite sprite) {
		this.sprite = sprite;
	}

	public OffsetSprite(Sprite sprite, MachineBaseBlockEntity provider) {
		this.sprite = sprite;
	}

	public ISprite getSprite() {
		return sprite;
	}

	public int getOffsetX(MachineBaseBlockEntity provider) {
		return offsetX + sprite.getSprite(provider).offsetX;
	}

	public OffsetSprite setOffsetX(int offsetX) {
		this.offsetX = offsetX;
		return this;
	}

	public int getOffsetY(MachineBaseBlockEntity provider) {
		return offsetY + sprite.getSprite(provider).offsetY;
	}

	public OffsetSprite setOffsetY(int offsetY) {
		this.offsetY = offsetY;
		return this;
	}
}
