package de.simonmeusel.mcide.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.bukkit.Bukkit;

import de.simonmeusel.mcide.plugin.Plugin;

public class Server {

	ServerSocket server;
	public int serverTask;

	@SuppressWarnings("deprecation")
	public Server(Plugin plugin) {
		serverTask = Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {

			@Override
			public void run() {
				try {
					
					System.out.println("Mcide Server started!");

					while (!server.isClosed()) {
						try {
							Socket client = server.accept();
							
							String address = client.getLocalAddress().getHostAddress();
							System.out.println("Mcide accepted a client from " + address + "!");

							BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

							String dataString = "";
							String line;

							while (!client.isClosed()
									&& !(line = br.readLine()).contains("------***endofsequence***-------")) {
								line = line.trim();
								if (line.equals(""))
									continue;
								dataString += line;
							}

							System.out.println(dataString);

							plugin.setCommands(dataString, address);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					System.out.println("[Mcide] Server closed!");
				}
			}
		});

	}

	public void stop() {
		try {
			server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
