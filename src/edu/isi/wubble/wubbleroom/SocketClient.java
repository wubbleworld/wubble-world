package edu.isi.wubble.wubbleroom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;


public class SocketClient {
	
	public static SocketClient _socketClient;
	
	protected Socket _socket;
	protected int _port = 10020;
	
	protected BufferedReader _in;
	protected PrintWriter _out;
	
	protected boolean _notDone = true;
	
	private SocketClient() {
	}
	
	public static SocketClient inst() {
		if (_socketClient == null) {
			_socketClient = new SocketClient();
		}
		return _socketClient;
	}
	
	public void close() {
		System.out.println("closing socket");
		_notDone = false;
		_out.write("quit");
		_out.flush();
		_out.close();

		try {
			_in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public void connect(String serverName) {
		System.out.println("Connecting: " + serverName);
		_notDone = true;
		try {
			_socket = new Socket(serverName, _port);
		
			_in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
			_out = new PrintWriter(_socket.getOutputStream());

			Runnable r = new Runnable() {
				public void run() {
					mainLoop();
				}
			};
			Thread t = new Thread(r, "NetworkThread");
			t.start();
		} catch (IOException e) {
			System.out.println("ERROR " + e.getMessage());
		}
	}	
	
	public void sendMessage(String message) {
		if (_out == null) {
			return;
		}
		_out.write(message + "\n");
		_out.flush();
	}
	
	/**
	 * messages come in pairs.  The first message is the header and the
	 * second message is the actual payload for that message.
	 */
	protected void mainLoop() {
		try {
			while (_notDone) {
				String header = _in.readLine();
				if (header == null) {
					_notDone = false;
					continue;
				}
				
				String payload = _in.readLine();
				if (payload == null) {
					_notDone = false;
					continue;
				}
				
				RoomManager.dispatchMessage(header, payload);
			}
		} catch (Exception e) {
			if (_notDone) {
				System.out.println("[mainLoop] + " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		try {
			_in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("finished main loop");
	}
	
	
}
