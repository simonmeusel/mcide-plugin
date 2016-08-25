package de.simonmeusel.mcide.plugin;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import de.simonmeusel.mcide.server.Server;

public class Plugin extends JavaPlugin {

	Server server;
	JSONParser jsonParser;
	Plugin plugin = this;

	@Override
	public void onEnable() {

		getConfig().options().copyDefaults(true);
		saveDefaultConfig();

		getDataFolder().mkdirs();

		boolean allowNoSSL = getConfig().getBoolean("allowNoSSL");
		boolean allowSSL = getConfig().getBoolean("allowSSL");

		// Is the keystore generated
		if (!new File(getDataFolder(), "keystore").isFile()) {
			if (getConfig().getBoolean("keystore.autogenerate.enabled") == true) {
				System.out.println("[Mcide] Generting keystore file");
				SecureRandom random = new SecureRandom();
				String password = new BigInteger(130, random).toString(32);
				System.out.println(password);
				try {
					Process process = Runtime.getRuntime().exec("keytool -genkey -dname CN= -keypass " + password
							+ " -storepass " + password + " -alias mcide -keystore keystore", null, getDataFolder());
					process.waitFor();

					getConfig().set("keystore.password", password);
					saveConfig();
				} catch (Exception e) {
					allowSSL = false;
					System.err.println("[Mcide] Failed to generate the keystore file! Check the config file");
					e.printStackTrace();
				}
			} else {
				allowSSL = false;
				System.err.println("[Mcide] Keystore not generated! Check the config file");
			}
		}
		
		server = new Server(this, getConfig().getString("keystore.password"), allowSSL, allowNoSSL, getConfig().getInt("port"));
		jsonParser = new JSONParser();

	}

	@Override
	public void onDisable() {
		server.stop();
		Bukkit.getScheduler().cancelTasks(this);
	}

	public void setCommands(String dataString, String address) {

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			public void run() {
				try {
					JSONObject data = (JSONObject) jsonParser.parse(dataString);
					
					if (!data.get("password").equals(getConfig().getString("password"))) {
						System.out.println("[Mcide] Incorrect password!");
						return;
					}

					new CommandGenrator(data);

					System.out.println("[" + address + "] Mcide generated commands!");

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
