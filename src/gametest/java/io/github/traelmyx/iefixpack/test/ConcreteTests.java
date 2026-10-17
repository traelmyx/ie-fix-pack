package io.github.traelmyx.iefixpack.test;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.register.IEBlocks;
import blusunrize.immersiveengineering.common.register.IEFluids;
import io.github.traelmyx.iefixpack.IEFixPack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Immersive Engineering #6201: two liquid concrete sources side by side in a stone trough, so they can't flow anywhere.
 * They start almost done (drying timer 30 of 31), so the next fluid tick decides whether they set.
 */
@GameTestHolder(WorkbenchTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class ConcreteTests {
	private static final BlockPos A = new BlockPos(3, 1, 3), B = new BlockPos(4, 1, 3);

	private static void run(GameTestHelper helper, boolean fixed, Check check) {
		IEFixPack.CONCRETE_SOURCES_SET.enabled = fixed;
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
			helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
		}
		BlockState liquid = IEFluids.CONCRETE.getBlock().defaultBlockState().setValue(IEProperties.INT_32, 30);
		helper.setBlock(A, liquid);
		helper.setBlock(B, liquid);
		helper.runAtTickTime(80, () -> {
			boolean aSet = helper.getBlockState(A).is(IEBlocks.StoneDecoration.CONCRETE.get());
			boolean bSet = helper.getBlockState(B).is(IEBlocks.StoneDecoration.CONCRETE.get());
			IEFixPack.CONCRETE_SOURCES_SET.enabled = true;
			System.out.printf("[ie_fix_pack] concrete, fix %s: first source set %s, second set %s (%s / %s)%n", fixed ? "on" : "off", aSet, bSet,
				helper.getBlockState(A).getBlock(), helper.getBlockState(B).getBlock());
			check.check(aSet, bSet);
			helper.succeed();
		});
	}

	private interface Check {
		void check(boolean aSet, boolean bSet);
	}

	@GameTest(template = "empty", timeoutTicks = 120, batch = "concrete_fixed")
	public static void adjacentConcreteSourcesSet(GameTestHelper helper) {
		run(helper, true, (a, b) -> helper.assertTrue(a && b, "the two sources did not both set"));
	}

	@GameTest(template = "empty", timeoutTicks = 120, batch = "concrete_control")
	public static void concreteControlReproducesBug(GameTestHelper helper) {
		// without the fix the sources must stay liquid, or this test can't tell the fix works
		run(helper, false, (a, b) -> helper.assertTrue(!a && !b, "the sources set without the fix: the bug did not reproduce"));
	}
}
