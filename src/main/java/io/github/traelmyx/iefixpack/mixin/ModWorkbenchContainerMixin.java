package io.github.traelmyx.iefixpack.mixin;

import blusunrize.immersiveengineering.common.gui.IESlot;
import blusunrize.immersiveengineering.common.gui.ModWorkbenchContainer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.traelmyx.iefixpack.IEFixPack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Immersive Engineering #6393. After a shift-click, whatever is still left in the clicked slot is dropped on the ground.
 * That is meant for blueprint results that don't fit into the inventory, but it happens for every slot: shift-click a
 * stack from your inventory into a workbench with room for only part of it, and the rest is dropped while it also stays
 * in your inventory. Only blueprint result slots drop their leftovers now.
 */
@Mixin(value = ModWorkbenchContainer.class, remap = false)
public abstract class ModWorkbenchContainerMixin {
	@WrapOperation(method = "quickMoveStack", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"))
	private ItemEntity ie_fix_pack$dropOnlyBlueprintResults(Player player, ItemStack stack, boolean includeThrower, Operation<ItemEntity> original,
															@Local(argsOnly = true) int slot) {
		if (IEFixPack.WORKBENCH_NO_QUICK_MOVE_DUPE.on() && !(((AbstractContainerMenu) (Object) this).getSlot(slot) instanceof IESlot.BlueprintOutput))
			return null;
		return original.call(player, stack, includeThrower);
	}
}
