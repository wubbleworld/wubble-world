package edu.isi.wubble.jgn.sheep.audio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.Wubble;

public class RegistrationManager extends Thread {
	AudioServer _server;
	DatagramSocket _registrationSocket;
	byte _currID = 0;
	
	public RegistrationManager(AudioServer as) {
		_server = as;
		
		try {
			_registrationSocket = new DatagramSocket(12001);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		while (true) {
			byte[] b = new byte[32];
			DatagramPacket p = new DatagramPacket(b, b.length);
			
			try {
				_registrationSocket.receive(p);
				
				String name = new String(p.getData()).trim();
				SocketAddress sa = p.getSocketAddress();
				
				System.out.println(name + " " + sa);

				// The response goes out over JGN because it is reliable
				Wubble player = (Wubble) SEntity.GetEntityForName(name);
				if (player == null) {
					System.out.println("INVALID NAME");
				} else {
					Byte newID = new Byte(++_currID);
					_server.registerPlayer(name, sa, newID);
					InvokeMessage im = new InvokeMessage();
					im.setArguments(new Object[]{newID});
					im.setMethodName("audioRegister");
					im.sendTo(player.getID());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
