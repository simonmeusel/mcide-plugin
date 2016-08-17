package de.simonmeusel.mcide.plugin;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
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

					World world = getWorld(data.get("world").toString());

					String[] commands = data.get("commands").toString().split("\n");

					generateCommands(commands, world);

					System.out.println("[" + address + "] Mcide generated " + commands.length + " commands in World "
							+ world.getName() + "!");

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void generateCommands(String[] commands, World world) {
		int x = 0;
		int y = 1;
		int z = 0;

		generateCommandBlock(new Location(world, x, 0, z), Material.COMMAND_REPEATING, "");

		for (String command : commands) {
			generateCommandBlock(new Location(world, x, y, z), Material.COMMAND_CHAIN, command);

			y += 1;

			if (y > 255) {
				y = 1;
				x += 1;
				generateCommandBlock(new Location(world, x, 0, z), Material.COMMAND_REPEATING, "");
			}

			if (x > 64) {
				x = 0;
				z += 1;
				generateCommandBlock(new Location(world, x, 0, z), Material.COMMAND_REPEATING, "");
			}
		}
		world.getBlockAt(new Location(world, x, y, z)).setType(Material.AIR);
		
	}

	@SuppressWarnings("deprecation")
	private void generateCommandBlock(Location location, Material material, String command) {
		Block block = location.getBlock();
		block.setType(material);
		CommandBlock commandBlock = (CommandBlock) block.getState();
		commandBlock.setCommand(command);
		commandBlock.setRawData((byte) 1);
		commandBlock.update();
		if (material.equals(Material.COMMAND_REPEATING)) {
			getServer().dispatchCommand(getServer().getConsoleSender(),
					"blockdata " + location.getBlockX() + " 0 " + location.getBlockZ() + " {auto:1b}");
		}
	}

	private World getWorld(String worldString) {
		World world = null;

		if ((world = Bukkit.getWorld(worldString)) == null) {
			if ((world = Bukkit.getWorld(UUID.fromString(worldString))) == null) {
				world = Bukkit.getWorlds().get(0);
			}
		}

		return world;
	}

}
