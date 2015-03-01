package edu.isi.wubble.login;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import edu.isi.wubble.jgn.db.Database;
import edu.isi.wubble.jgn.db.DatabaseManager;

public class LoginServer {
	
	public static final int PORT = 12000;
	protected ServerSocket _serverSocket;
	
	public static String _sheepServer = "butters.isi.edu";
	public static String _rpgServer   = "butters.isi.edu";
	public static String _roomServer  = "butters.isi.edu";
	
	public LoginServer() {
		DatabaseManager.inst();
	}
	
	public void acceptConnections() {
		try {
			_serverSocket = new ServerSocket(PORT);
			
			while (true) {
				Socket s = _serverSocket.accept();
				SocketHandler handler = new SocketHandler(s);
				handler.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private class SocketHandler extends Thread {
		protected Socket _socket;
		
		protected BufferedReader _in;
		protected PrintWriter _out;

		protected boolean _loginSuccessful;
		
		public SocketHandler(Socket s) {
			_socket = s;
			_loginSuccessful = false;
		}
		
		public void run() {
			try {
				_in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
				_out = new PrintWriter(_socket.getOutputStream());
				
				while (true) {
					String action = _in.readLine();
					String message = _in.readLine();
					
					if (action == null)
						return;
					
					System.out.println("action: " + action + " message: " + message);
					Method method = getClass().getMethod(action, new Class[] { String.class });
					method.invoke(this, message);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			
		}
		
		public void login(String credentials) {
			Database mgr = new Database();
			try {
				StringTokenizer str = new StringTokenizer(credentials, " ");
				String userName = str.nextToken();
				String password = str.nextToken();
				
				if (mgr.authenticate(userName, password)) {
					_loginSuccessful = true;
					
					_out.write("true\n");
					_out.write(_sheepServer + "\n");
					_out.write(_rpgServer + "\n");
					_out.write(_roomServer + "\n");
				} else {
					_out.write("false\n");
				}
				_out.flush();
			} catch (Exception e) {
				System.out.println("[login] " + e.getMessage());
				e.printStackTrace();
			} finally {
				mgr.disconnect();
			}
		}
		
		public void create(String credentials) {
			Database mgr = new Database();
			try {
				StringTokenizer str = new StringTokenizer(credentials, " ");
				String userName = str.nextToken();
				String password = str.nextToken();
				
				String result = mgr.create(userName, password);
				
				if (result.equals("SUCCESS")) {
					_loginSuccessful = true;
					
					_out.write(result + "\n");
					_out.write(_sheepServer + "\n");
					_out.write(_rpgServer + "\n");
					_out.write(_roomServer + "\n");
				} else {
					_out.write(result + "\n");
				}
				_out.flush();
			} catch (Exception e) {
				System.out.println("[login] " + e.getMessage());
				e.printStackTrace();
			} finally {
				mgr.disconnect();
			}
		}
		
		public void changeServer(String message) {
			if (!_loginSuccessful)
				return;
			
			try {
				StringTokenizer str = new StringTokenizer(message, " ");
				String game       = str.nextToken();
				String serverName = str.nextToken();
				
				if ("sheep".equals(game)) 
					_sheepServer = serverName;
				if ("rpg".equals(game))
					_rpgServer = serverName;
				if ("room".equals(game))
					_roomServer = serverName;
			} catch (Exception e) {
				System.out.println("[login] " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		LoginServer ls = new LoginServer();
		ls.acceptConnections();
	}

	
}

