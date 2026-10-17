package io.github.traelmyx.iefixpack.mixin;

import blusunrize.immersiveengineering.common.fluids.ConcreteFluid;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.traelmyx.iefixpack.IEFixPack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Immersive Engineering #6201. A liquid concrete source only sets once no liquid concrete is next to it, so the flow
 * around it sets first. Two sources next to each other wait for each other forever and never set. Neighbouring sources
 * no longer hold a source back; neighbouring flowing concrete still does.
 */
@Mixin(value = ConcreteFluid.class, remap = false)
public abstract class ConcreteFluidMixin {
	@WrapOperation(method = "tick", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"))
	private FluidState ie_fix_pack$ignoreNeighbourSources(BlockState neighbour, Operation<FluidState> original) {
		FluidState fluid = original.call(neighbour);
		if (IEFixPack.CONCRETE_SOURCES_SET.on() && fluid.isSource() && fluid.getType() instanceof ConcreteFluid)
			return Fluids.EMPTY.defaultFluidState();
		return fluid;
	}
}
