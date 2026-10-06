package com.qxiane;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * The mod's config file, written to {@code config/meowify.json} on first launch.
 *
 * <p>Fabric has no built-in config system, so this is a plain JSON file read with the Gson that
 * Fabric Loader already ships. It is loaded once during mod initialisation and kept in memory;
 * values are read through the static accessors below.</p>
 */
public final class MeowifyConfig {

	/** Published config file name inside the loader's config directory. */
	private static final String FILE_NAME = "meowify.json";

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private static Values values = new Values();

	private MeowifyConfig() {
	}

	/** Whether item names outside of Jade get the suffix. */
	public static boolean isGlobalSuffixEnabled() {
		return values.enableGlobalSuffix;
	}

	/** Whether the block and entity names in Jade's overlay get the suffix. */
	public static boolean isJadeSuffixEnabled() {
		return values.enableJadeSuffix;
	}

	/**
	 * The custom suffix the player typed, with surrounding whitespace stripped, or {@code null} when it
	 * is not set. The leading space of a value like {@code " meow~"} is therefore trimmed away, and
	 * {@link MeowifyText} supplies exactly one space instead.
	 */
	public static String customSuffix() {
		String custom = values.customSuffix;
		if (custom == null) {
			return null;
		}
		custom = custom.trim();
		return custom.isEmpty() ? null : custom;
	}

	/** Reads the config file, writing a default one when it does not exist or cannot be parsed. */
	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		if (Files.isRegularFile(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				Values loaded = GSON.fromJson(reader, Values.class);
				if (loaded != null) {
					values = loaded;
					return;
				}
			} catch (IOException | RuntimeException e) {
				Meowify.LOGGER.warn("Could not read {}, falling back to the defaults", file, e);
			}
		}
		values = new Values();
		save(file);
	}

	private static void save(Path file) {
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(values, writer);
			}
		} catch (IOException e) {
			Meowify.LOGGER.warn("Could not write {}", file, e);
		}
	}

	/** The serialised shape of the config file. */
	private static final class Values {

		private boolean enableGlobalSuffix = true;

		/** Only meaningful when Jade is installed; has no effect otherwise. */
		private boolean enableJadeSuffix = true;

		/** Empty means "use the translated suffix", which follows the language selected in game. */
		private String customSuffix = "";
	}
}
