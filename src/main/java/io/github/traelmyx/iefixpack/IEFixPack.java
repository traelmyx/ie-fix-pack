package io.github.traelmyx.iefixpack;

import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Unofficial fixes for long-standing Immersive Engineering bugs (by BluSunrize; this pack is not), each with its own
 * switch. A fix whose bug Immersive Engineering has fixed itself switches off on that version and later.
 */
@Mod(IEFixPack.MOD_ID)
public class IEFixPack {
	public static final String MOD_ID = "ie_fix_pack";
	public static final Logger LOGGER = LoggerFactory.getLogger("Immersive Engineering: Fix Pack");

	/** Every fix, in config file order. */
	public static final Map<String, Fix> FIXES = new LinkedHashMap<>();

	public static final Fix WORKBENCH_NO_QUICK_MOVE_DUPE = fix("workbenchNoQuickMoveDupe", "6393", null,
		"Shift-clicking a stack into the Engineer's Workbench no longer drops a copy of what didn't fit on the ground.");

	public static final class Fix {
		public final String key, issue, description;
		/** First Immersive Engineering version that fixes the bug itself, or null while it is still open upstream. */
		public final String fixedUpstreamIn;
		public volatile boolean enabled = true;
		volatile boolean fixedUpstream;

		Fix(String key, String issue, String fixedUpstreamIn, String description) {
			this.key = key;
			this.issue = issue;
			this.fixedUpstreamIn = fixedUpstreamIn;
			this.description = description;
		}

		public boolean on() {
			return enabled && !fixedUpstream;
		}
	}

	/** Fixes without a passing test start switched off. */
	private static Fix experimental(Fix fix) {
		fix.enabled = false;
		return fix;
	}

	private static Fix fix(String key, String issue, String fixedUpstreamIn, String description) {
		Fix fix = new Fix(key, issue, fixedUpstreamIn, description);
		FIXES.put(key, fix);
		return fix;
	}

	public IEFixPack() {
		loadConfig(FMLPaths.CONFIGDIR.get());
		ArtifactVersion ie = ModList.get().getModContainerById("immersiveengineering").map(c -> c.getModInfo().getVersion()).orElse(null);
		for (Fix fix : FIXES.values()) {
			if (ie != null && fix.fixedUpstreamIn != null && ie.compareTo(new DefaultArtifactVersion(fix.fixedUpstreamIn)) >= 0) {
				fix.fixedUpstream = true;
				LOGGER.info("Immersive Engineering {} fixes #{} itself; {} is off", ie, fix.issue, fix.key);
			}
		}
	}

	static void loadConfig(Path configDir) {
		Path file = configDir.resolve(MOD_ID + ".properties");
		Properties props = new Properties();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				props.load(reader);
			} catch (IOException e) {
				LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		StringBuilder out = new StringBuilder("# Immersive Engineering: Fix Pack. true = fixed, false = the original behaviour.\n# Fixes switch themselves off on Immersive Engineering versions that fix the bug.\n");
		for (Fix fix : FIXES.values()) {
			String value = props.getProperty(fix.key);
			if (value != null) fix.enabled = Boolean.parseBoolean(value.trim());
			out.append("\n# Immersive Engineering #").append(fix.issue).append(": ").append(fix.description).append('\n')
				.append(fix.key).append('=').append(fix.enabled).append('\n');
		}
		try {
			Files.createDirectories(configDir);
			try (Writer writer = Files.newBufferedWriter(file)) {
				writer.write(out.toString());
			}
		} catch (IOException e) {
			LOGGER.warn("Could not write {}", file, e);
		}
	}
}
