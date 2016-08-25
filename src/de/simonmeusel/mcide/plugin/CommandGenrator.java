package de.simonmeusel.mcide.plugin;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.json.simple.JSONObject;

public class CommandGenrator {

	private World world;
	private String[] lines;

	private int x = 0;
	private int y = 1;
	private int z = 0;
	private boolean conditional;
	
	public CommandGenrator(JSONObject data) {
		getWorld(data.get("world").toString());

		lines = data.get("commands").toString().split("\n");
		
		processLines();
	}
	
	/**
	 * Processes the lines and generates Command blocks
	 */
	private void processLines() {

		generateCommandBlock(x, 0, z, Material.COMMAND_REPEATING, "");
		
		for (String line : lines) {
			// Check if the line is an annotation
			if (processAnnotation(line)) continue;
			
			// Generate command block
			generateCommandBlock(x, y, z, Material.COMMAND_CHAIN, line);
			y += 1;
			
			checkPosition();
		}

		// Stop after last command block
		world.getBlockAt(new Location(world, x, y, z)).setType(Material.AIR);
	}
	
	/**
	 * Checks the position and updates it it is out of bounds
	 */
	private void checkPosition() {
		if (y > 255) {
			y = 1;
			x += 1;
			generateCommandBlock(x, 0, z, Material.COMMAND_REPEATING, "");
		}

		if (x > 64) {
			x = 0;
			z += 1;
			generateCommandBlock(x, 0, z, Material.COMMAND_REPEATING, "");
		}
	}

	private boolean processAnnotation(String line) {
		if (!line.startsWith("@")) return false;
		
		try {
			String annotation = line.substring(1).trim().split(" ")[0];
			switch (annotation) {
			case "conditional":
				conditional = true;
				break;

			case "unconditional":
				conditional = false;
				break;

			default:
				throw new Exception();
			}
		} catch (Exception e) {
			System.out.println("[McIDE] Annotation " + line + " is invalid!");
		}
		
		return true;
	}
	
	private void generateCommandBlock(int x, int y, int z, Material material, String command) {
		Block block = new Location(world, x, y, z).getBlock();
		block.setType(material);
		if (material.equals(Material.COMMAND_REPEATING)) {
			setRepeating(x, y, z);
			CommandBlock commandBlock = (CommandBlock) block.getState();
			commandBlock.setCommand(command);
			
			setConditional(commandBlock, false);
			
			commandBlock.update();
		} else {
			postProgress(x, y, z, command);
		}
	}
	
	private void postProgress(int x, int y, int z, String command) {
		Block block = new Location(world, x, y, z).getBlock();
		CommandBlock commandBlock = (CommandBlock) block.getState();
		commandBlock.setCommand(command);
		
		setConditional(commandBlock, conditional);
		
		commandBlock.update();
	}
	
	private void setRepeating(int x, int y, int z) {
		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
				"blockdata " + x + " 0 " + z + " {auto:1b}");
	}
	
	@SuppressWarnings("deprecation")
	private void setConditional(CommandBlock commandBlock, boolean conditional) {
		if (conditional) {
			commandBlock.setRawData((byte) 9);
		} else {
			commandBlock.setRawData((byte) 1);
		}
	}

	/**
	 * Computes world by given world name or world UUID. If no world was found, the first world will be used.
	 * 
	 * @param worldString String witch describes the world
	 */
	private void getWorld(String worldString) {
		world = null;

		if ((world = Bukkit.getWorld(worldString)) == null) {
			if ((world = Bukkit.getWorld(UUID.fromString(worldString))) == null) {
				world = Bukkit.getWorlds().get(0);
			}
		}
	}

}
