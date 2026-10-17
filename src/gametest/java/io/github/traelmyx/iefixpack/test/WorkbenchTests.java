package io.github.traelmyx.iefixpack.test;

import blusunrize.immersiveengineering.common.blocks.wooden.ModWorkbenchBlockEntity;
import blusunrize.immersiveengineering.common.gui.ModWorkbenchContainer;
import blusunrize.immersiveengineering.common.register.IEBlocks;
import blusunrize.immersiveengineering.common.register.IEMenuTypes;
import io.github.traelmyx.iefixpack.IEFixPack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Immersive Engineering #6393, as in the report: the workbench's six input slots are full of shovels except one with 63
 * diamonds. The player shift-clicks a stack of 64 diamonds into it. One diamond fits; there must still be exactly
 * 64 + 63 diamonds in total afterwards.
 */
@GameTestHolder(WorkbenchTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class WorkbenchTests {
	public static final String MOD_ID = "ie_fix_pack_gametest";

	private static final BlockPos BENCH = new BlockPos(3, 1, 3);

	static ServerPlayer player(GameTestHelper helper, String name) {
		MinecraftServer server = helper.getLevel().getServer();
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name), false);
		var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
		player.setPos(at.x, at.y, at.z);
		return player;
	}

	private static int diamondsOnGround(GameTestHelper helper) {
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		int n = 0;
		for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(o.getX(), o.getY(), o.getZ(), o.getX() + 8, o.getY() + 8, o.getZ() + 8)))
			if (e.getItem().is(Items.DIAMOND)) n += e.getItem().getCount();
		return n;
	}

	private static void run(GameTestHelper helper, boolean fixed, Check check) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		helper.setBlock(BENCH, IEBlocks.WoodenDevices.WORKBENCH.defaultBlockState());
		ModWorkbenchBlockEntity bench = (ModWorkbenchBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BENCH));
		helper.assertTrue(bench != null, "no workbench block entity");
		for (int i = 1; i <= 6; i++) bench.getInventory().set(i, new ItemStack(Items.IRON_SHOVEL));
		bench.getInventory().set(3, new ItemStack(Items.DIAMOND, 63));
		ServerPlayer player = player(helper, fixed ? "test-bench" : "test-bench-v");
		player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 64));
		ModWorkbenchContainer menu = IEMenuTypes.MOD_WORKBENCH.create(1, player.getInventory(), bench);
		int hotbar0 = -1;
		for (int i = 0; i < menu.slots.size(); i++)
			if (menu.slots.get(i).container == player.getInventory() && menu.slots.get(i).getContainerSlot() == 0) hotbar0 = i;
		helper.assertTrue(hotbar0 >= 0, "the workbench menu has no slot for the player's hotbar");
		IEFixPack.WORKBENCH_NO_QUICK_MOVE_DUPE.enabled = fixed;
		menu.quickMoveStack(player, hotbar0);
		IEFixPack.WORKBENCH_NO_QUICK_MOVE_DUPE.enabled = true;
		int inPlayer = 0, inBench = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++)
			if (player.getInventory().getItem(i).is(Items.DIAMOND)) inPlayer += player.getInventory().getItem(i).getCount();
		for (ItemStack s : bench.getInventory()) if (s.is(Items.DIAMOND)) inBench += s.getCount();
		int finalPlayer = inPlayer, finalBench = inBench;
		helper.runAfterDelay(2, () -> {
			int ground = diamondsOnGround(helper);
			player.getInventory().clearContent();
			helper.getLevel().getServer().getPlayerList().remove(player);
			System.out.printf("[ie_fix_pack] workbench, fix %s: %d diamonds in the player, %d in the bench, %d on the ground (127 exist)%n",
				fixed ? "on" : "off", finalPlayer, finalBench, ground);
			check.check(finalPlayer + finalBench + ground);
			helper.succeed();
		});
	}

	private interface Check {
		void check(int total);
	}

	@GameTest(template = "empty", timeoutTicks = 40, batch = "workbench_fixed")
	public static void workbenchQuickMoveKeepsCount(GameTestHelper helper) {
		run(helper, true, total -> helper.assertTrue(total == 127, total + " diamonds after the shift-click, 127 existed"));
	}

	@GameTest(template = "empty", timeoutTicks = 40, batch = "workbench_control")
	public static void workbenchControlReproducesDupe(GameTestHelper helper) {
		// without the fix diamonds must be duplicated, or this test can't tell the fix works
		run(helper, false, total -> helper.assertTrue(total > 127, "no diamonds were duplicated without the fix (" + total + "): the bug did not reproduce"));
	}
}
