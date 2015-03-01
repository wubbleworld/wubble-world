package edu.isi.wubble.sheep.audio;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import edu.isi.wubble.Main;
import edu.isi.wubble.jgn.sheep.audio.AudioConfig;

public class AudioClient extends Thread {
	// We need a way to send audio...
	AudioSender _sender;
	
	// And a way to play the audio streams we will receive
	HashMap<Byte,AudioPlayer> _players = new HashMap<Byte,AudioPlayer>();
	
	// We need the address of the server
	InetAddress _serverAddress;
	int _serverPort;
	int _registerPort;
	
	byte _myID;
	Byte _myByte;
	boolean _registered = false;
	boolean _alive = true;
	
	DatagramSocket _udp;
	
	public AudioClient() {
		try {
			_serverAddress = InetAddress.getByName(Main.inst().getServer("sheep"));
			
			_serverPort = 11000;
			_registerPort = 12001;
			
			_udp = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	// This adds a new player with the given id
	public void subscribe(Byte id) {
		AudioPlayer player = new AudioPlayer();
		_players.put(id, player);
		player.start();
	}
	
	public void unsubscribe(Byte id) {
		_players.remove(id);
	}
	
	public void cleanup() {
		_alive = false;
	}
	
	// This tells you which Byte is your ID (if you want to listen to yourself)
	public void register(Byte b) {
		// only accept one registration
		if (!_registered) {
			_myByte = b;
			_myID = b.byteValue();
			_registered = true;

			AudioPlayer selfPlayer = new AudioPlayer();
			_players.put(_myByte, selfPlayer);
			selfPlayer.start();
		}
	}
	
	// The jobs of this run are: 
	// 1. start the sending thread
	// 2. listen for UDP packets -
	// route each one to the corresponding player
	// We will probably send messages over the JGN as well
	public void run() {
		// This section is for registration
		String s = new String(Main.inst().getName());
		byte[] name = s.getBytes();
		byte[] fullName = new byte[32]; 
		ByteBuffer b = ByteBuffer.wrap(fullName);
		b.put(name);
		
		try {
			_udp.setSoTimeout(300);
		} catch (SocketException e2) {
			e2.printStackTrace();
		}
		
		while (!_registered) {
			DatagramPacket r = new DatagramPacket(fullName, fullName.length, _serverAddress, _registerPort);
			try {
				_udp.send(r);

				// Otherwise we send out too many of these
				Thread.sleep(1000);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
		try {
			// I think this disables the timeout?
			_udp.setSoTimeout(1000);
		} catch (SocketException e2) {
			e2.printStackTrace();
		}		
		// This is where we start the sending thread
		_sender = new AudioSender(_udp);
		_sender.start();
		
		// This is the main receiving loop
		byte[] rec = new byte[AudioConfig.getSpeexBufferSize() + 2];
		while (_alive) {
			try	{
				DatagramPacket p = new DatagramPacket(rec, rec.length);
				try {
					_udp.receive(p);
					
					AudioPlayer player = _players.get(new Byte(rec[0]));

					if (player != null) {
						ByteBuffer temp = ByteBuffer.allocate(AudioConfig.getSpeexBufferSize() + 1);
						temp.put(rec, 1, rec.length - 1);
						
						player.write(temp.array());
						player.interrupt();
					}
				} catch (SocketTimeoutException timeout) {
					//timeout.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// This is the cleanup phase
		_sender.cleanup();
		for (AudioPlayer p : _players.values()) {
			p.cleanup();
		}
		_udp.close();
		
		System.out.println("CLIENT DEAD");
	}
	
	// These will also need to send message to the JGN server
	// Which will create the files for saving
	public void startAudio() {
		if (_sender != null)
			_sender.startSending();
	}

	public void stopAudio() {
		if (_sender != null)
			_sender.stopSending();
	}
}
