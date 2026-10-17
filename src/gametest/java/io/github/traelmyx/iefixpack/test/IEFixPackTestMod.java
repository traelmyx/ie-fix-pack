package io.github.traelmyx.iefixpack.test;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(WorkbenchTests.MOD_ID)
public class IEFixPackTestMod {
	public IEFixPackTestMod(Dist dist) {
		if (dist.isClient() && ClientSmoke.start()) {
			NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientSmoke.tick(Minecraft.getInstance()));
		}
	}
}
