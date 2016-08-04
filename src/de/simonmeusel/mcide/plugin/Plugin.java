package de.simonmeusel.mcide.plugin;

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

	@Override
	public void onEnable() {

		server = new Server(this);
		jsonParser = new JSONParser();

	}

	@Override
	public void onDisable() {
		server.stop();
		Bukkit.getScheduler().cancelTask(server.serverTask);
	}
	
	public void setCommands(String dataString, String address) {

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			public void run() {
				try {
					JSONObject data = (JSONObject) jsonParser.parse(dataString);

					World world = getWorld(data.get("world").toString());
					
					String[] commands = data.get("commands").toString().split("\n");

					generateCommands(commands, world);
					
					System.out.println("[" + address + "] Mcide generated "
							+ commands.length + " commands in World "
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

		for (String command : commands) {
			generateCommandBlock(new Location(world, x, y, z), Material.COMMAND_CHAIN, command);

			y += 1;

			if (y > 255) {
				y = 1;
				x += 1;
				generateCommandBlock(new Location(world, x, 0, z), Material.COMMAND_CHAIN, "");
			}

			if (x > 64) {
				x = 0;
				z += 1;
				generateCommandBlock(new Location(world, x, 0, z), Material.COMMAND_CHAIN, "");
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	private void generateCommandBlock(Location location, Material material, String command) {
		Block block = location.getBlock();
		block.setType(material);
		CommandBlock commandBlock = (CommandBlock) block.getState();
		commandBlock.setCommand(command);
		commandBlock.setRawData((byte) 1);
		commandBlock.update();
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
