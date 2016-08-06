package de.simonmeusel.mcide.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.bukkit.Bukkit;

import de.simonmeusel.mcide.plugin.Plugin;

public class Server {
	
	Plugin plugin;

	SSLServerSocket secureServer;
	ServerSocket server;
	public int serverTask;
	public int secureServerTask;

	@SuppressWarnings("deprecation")
	public Server(Plugin plugin, String password, boolean allowSSL, boolean allowNoSSL, int port) {
		this.plugin = plugin;
		
		if (allowSSL) {
			secureServerTask = Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {

				@Override
				public void run() {
					try {
						KeyStore ks = KeyStore.getInstance("JKS");
						ks.load(new FileInputStream(new File(plugin.getDataFolder(), "keystore")), password.toCharArray());
						KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
						kmf.init(ks, password.toCharArray());
						SSLContext sc = SSLContext.getInstance("TLS");
						sc.init(kmf.getKeyManagers(), null, null);
						SSLServerSocketFactory ssf = sc.getServerSocketFactory();
						secureServer = (SSLServerSocket) ssf.createServerSocket(port + 1);

						System.out.println("Mcide SSL Server started!");

						while (!secureServer.isClosed()) {
							try {
								acceptClient(secureServer.accept());
							} catch (Exception e) {
								System.err.println("[Mcide] Connection with client failed");
							}
						}
					} catch (Exception e) {
						System.out.println("[Mcide] Server closed!");
						e.printStackTrace();
					}
				}
			});
		}
		
		if (allowNoSSL) {
			serverTask = Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {

				@Override
				public void run() {
					try {
						server = new ServerSocket(port);

						System.out.println("Mcide no SSL Server started!");

						while (!server.isClosed()) {
							try {
								acceptClient(server.accept());
							} catch (InterruptedException e) {
							} catch (Exception e) {
								System.err.println("[Mcide] Connection with client failed");
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						System.out.println("[Mcide] Server closed!");
						e.printStackTrace();
					}
				}
			});
		}

	}
	
	private void acceptClient(Socket client) throws Exception {

		String address = client.getLocalAddress().getHostAddress();
		System.out.println("Mcide accepted a client from " + address + "!");

		BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

		String dataString = "";
		String line;
		
		System.out.println(br);

		while (!client.isClosed()) {
			line = br.readLine();
			if (line == null) continue;
			if (line.contains("------***endofsequence***-------")) break;
			line = line.trim();
			if (line.equals(""))
				continue;
			dataString += line;
		}
		System.out.println(dataString);

		plugin.setCommands(dataString, address);
		
		client.close();
	}

	public void stop() {
		try {
			secureServer.close();
			server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
