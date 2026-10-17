package io.github.traelmyx.iefixpack.mixin;

import blusunrize.immersiveengineering.common.gui.IEContainerMenu;
import blusunrize.immersiveengineering.common.gui.ToolboxMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.traelmyx.iefixpack.IEFixPack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Immersive Engineering #6177. Shift-clicking items onto a stack in a toolbox held in your hand lost them, or duplicated
 * some. The usual shift-click code adds to a stack by growing the slot's item in place, but a held toolbox's slots hand
 * out copies of what is stored in the item, so the growth never reaches the toolbox while the items leave your
 * inventory. Items now go into toolbox slots through the slot itself, which writes them back to the toolbox.
 */
@Mixin(value = IEContainerMenu.class, remap = false)
public abstract class IEContainerMenuMixin {
	@WrapOperation(method = "quickMoveStack", require = 0, at = @At(value = "INVOKE",
		target = "Lblusunrize/immersiveengineering/common/gui/IEContainerMenu;moveItemStackToWithMayPlace(Lnet/minecraft/world/item/ItemStack;II)Z"))
	private boolean ie_fix_pack$insertThroughSlots(IEContainerMenu menu, ItemStack stack, int start, int end, Operation<Boolean> original) {
		if (!(menu instanceof ToolboxMenu) || !IEFixPack.TOOLBOX_NO_SHIFT_CLICK_LOSS.on())
			return original.call(menu, stack, start, end);
		AbstractContainerMenu self = (AbstractContainerMenu) (Object) menu;
		boolean moved = false;
		// fill matching stacks first, then empty slots, like the normal shift-click
		for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
			for (int i = start; i < end && !stack.isEmpty(); i++) {
				Slot slot = self.getSlot(i);
				if (!slot.mayPlace(stack)) continue;
				ItemStack current = slot.getItem();
				boolean fits = pass == 0 ? !current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack) : current.isEmpty();
				if (!fits) continue;
				int before = stack.getCount();
				slot.safeInsert(stack);
				if (stack.getCount() < before) moved = true;
			}
		}
		return moved;
	}
}
