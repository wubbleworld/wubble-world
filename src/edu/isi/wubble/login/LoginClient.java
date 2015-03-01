package edu.isi.wubble.login;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import edu.isi.wubble.Main;

public class LoginClient {

	protected String _userName;
	protected String _password;
	
	public LoginClient(String userName, String password) {
		_userName = userName;
		_password = password;
		
	}
	
	public boolean authenticate() {
		try {
			// This should move to Main
			Socket s = new Socket(Main.inst().getServer("login"), LoginServer.PORT);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			PrintWriter out = new PrintWriter(s.getOutputStream());
			
			out.write("login\n");
			out.write(_userName + " " + _password + "\n");
			out.flush();
			
			boolean success = Boolean.parseBoolean(in.readLine());
			if (success) {
				String sheep = in.readLine();
				String rpg   = in.readLine();
				String room  = in.readLine();
				
				Main.inst().setServers(sheep, rpg, room);
			}
			
			return success;
		} catch (Exception e) {
			System.out.println("[authenticate] " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
	
	public String create() {
		String result = "FAILURE";
		
		try {
			Socket s = new Socket(Main.inst().getServer("login"), LoginServer.PORT);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			PrintWriter out = new PrintWriter(s.getOutputStream());
			
			out.write("create\n");
			out.write(_userName + " " + _password + "\n");
			out.flush();
			
			result = in.readLine();
			if (result.equals("SUCCESS")) {
				String sheep = in.readLine();
				String rpg   = in.readLine();
				String room  = in.readLine();
				
				Main.inst().setServers(sheep, rpg, room);
			} 
			
		} catch (Exception e) {
			System.out.println("[authenticate] " + e.getMessage());
			e.printStackTrace();
		}
		
		return result;
	}
}
