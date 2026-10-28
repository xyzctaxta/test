package clearlag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * File config: <server>/config/clearlag.json
 * Dibuat otomatis saat server pertama kali jalan. Edit lalu jalankan /clearlag reload.
 */
public class ClearLagConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("clearlag.json");

	// ===== Timer =====
	/** Hidupkan/matikan auto clear. */
	public boolean autoClear = true;
	/** Jeda antar pembersihan, dalam detik. 300 = 5 menit. */
	public int intervalSeconds = 300;

	// ===== Peringatan =====
	/** Pada sisa detik berapa saja peringatan muncul. */
	public List<Integer> warnAtSeconds = new ArrayList<>(List.of(60, 30, 10, 5, 4, 3, 2, 1));
	/** Lokasi peringatan: "ACTIONBAR" (di atas hotbar) atau "CHAT". */
	public String warnDisplay = "ACTIONBAR";
	/** Lokasi pesan hasil pembersihan: "ACTIONBAR" atau "CHAT". */
	public String resultDisplay = "ACTIONBAR";

	// ===== Yang dibersihkan =====
	public boolean clearItems = true;
	public boolean clearExperienceOrbs = true;
	/** Mob hostile tanpa name tag dan tidak persistent. */
	public boolean clearHostileMobs = false;
	/** ID item yang TIDAK akan dihapus, contoh: ["minecraft:diamond", "minecraft:netherite_ingot"]. */
	public List<String> protectedItems = new ArrayList<>();

	// ===== Pesan =====
	/** Placeholder: %time% = sisa detik. */
	public String warnMessage = "Item di tanah akan dihapus dalam %time% detik!";
	/** Placeholder: %items%, %xp%, %mobs%. */
	public String clearedMessage = "ClearLag: %items% item, %xp% XP orb, %mobs% mob dihapus.";

	public boolean warnInActionBar() {
		return !"CHAT".equalsIgnoreCase(warnDisplay);
	}

	public boolean resultInActionBar() {
		return !"CHAT".equalsIgnoreCase(resultDisplay);
	}

	public static ClearLagConfig load() {
		ClearLagConfig cfg = null;
		try {
			if (Files.exists(PATH)) {
				cfg = GSON.fromJson(Files.readString(PATH), ClearLagConfig.class);
			}
		} catch (Exception e) {
			ClearLagMod.LOGGER.error("Gagal membaca config, memakai default", e);
		}
		if (cfg == null) cfg = new ClearLagConfig();

		if (cfg.intervalSeconds < 5) cfg.intervalSeconds = 5;
		if (cfg.warnAtSeconds == null) cfg.warnAtSeconds = new ArrayList<>();
		if (cfg.protectedItems == null) cfg.protectedItems = new ArrayList<>();

		cfg.save(); // tulis ulang supaya opsi baru otomatis muncul di file
		return cfg;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			ClearLagMod.LOGGER.error("Gagal menyimpan config", e);
		}
	}
}
