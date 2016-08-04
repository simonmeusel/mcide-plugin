package de.simonmeusel.mcide.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.bukkit.Bukkit;

import de.simonmeusel.mcide.plugin.Plugin;

public class Server {

	SSLServerSocket server;
	public int serverTask;

	@SuppressWarnings("deprecation")
	public Server(Plugin plugin, String password) {
		serverTask = Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {

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
					server = (SSLServerSocket) ssf.createServerSocket(25564);

					System.out.println("Mcide Server started!");

					while (!server.isClosed()) {
						try {
							SSLSocket client = (SSLSocket) server.accept();

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
