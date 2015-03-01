package edu.isi.wubble.jgn.sheep.audio;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import org.tritonus.share.sampled.AudioSystemShadow;
import org.tritonus.share.sampled.file.AudioOutputStream;
import org.tritonus.share.sampled.file.TDataOutputStream;
import org.xiph.speex.SpeexDecoder;

import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.Wubble;
import edu.isi.wubble.physics.entity.Entity;

public class AudioServer extends Thread {
	// We don't need the data line unless we want to eavesdrop
//	SourceDataLine _dataLine;
	
	// users will send to the registration socket to 
	// get themselves registered - we need this to know what the port is
	// to send to
	DatagramSocket _udp;
	RegistrationManager _reg;

	HashMap<String,SocketAddress> _nameMap = new HashMap<String, SocketAddress>();
	HashMap<SocketAddress,String> _reverseNameMap = new HashMap<SocketAddress,String>();
	HashMap<SocketAddress,Byte> _addressMap = new HashMap<SocketAddress, Byte>();
	HashMap<Byte,AudioOutputStream> _writers = new HashMap<Byte, AudioOutputStream>();
	
	public AudioServer() {
//		AudioFormat lineFormat = AudioConfig.createAudioFormat(AudioFormat.Encoding.PCM_SIGNED);
//		lineFormat = AudioConfig.createMasterFormat();
//		DataLine.Info info = new DataLine.Info(SourceDataLine.class, lineFormat, AudioSystem.NOT_SPECIFIED);
//		try	{
//			_dataLine = (SourceDataLine) AudioSystem.getLine(info);
//			_dataLine.open(lineFormat, _dataLine.getBufferSize());
//			_dataLine.start();
//		} catch(LineUnavailableException e) {
//			e.printStackTrace();
//		}
//		System.out.println(_dataLine.getFormat());
		
		try {
			_udp = new DatagramSocket(11000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		_reg = new RegistrationManager(this);
		_reg.start();
	}
	
	public static void main(String[] args) {
		Thread t = new Thread(new AudioServer());
		t.start();
	}

	// This will be called by the registration manager
	public synchronized void registerPlayer(String name, SocketAddress addr, Byte id) {
		// get the ID of the new player
		Wubble w = (Wubble) SEntity.GetEntityForName(name);
		short newID = w.getID();
		
		// first, we tell the new player to subscribe to all the old players
		for (Byte otherID : _addressMap.values()) {
			InvokeMessage im = new InvokeMessage();
			im.setMethodName("audioSubscribe");
			im.setArguments(new Object[]{otherID});
			im.sendTo(newID);
		}
		
		_nameMap.put(name, addr);
		_reverseNameMap.put(addr, name);
		_addressMap.put(addr, id);
		System.out.println("AUDIO: REGISTERED " + name);
		
		// This tells all players to subscribe to the new player
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("audioSubscribe");
		im.setArguments(new Object[]{id});
		im.sendToAll(); // this should be allExcept
	}
	
	public synchronized void removePlayer(String name) {
		// Hopefully this doesn't throw an NPE?
		Byte id = _addressMap.remove(_nameMap.remove(name));
		
		if (id != null) {
			InvokeMessage im = new InvokeMessage();
			im.setMethodName("audioUnsubscribe");
			im.setArguments(new Object[]{id});
			im.sendToAll();
		}
	}
	
	// This will need to create the file writing thread
	public void startAudio(String name) {
		Byte id = _addressMap.get(_nameMap.get(name));
		
		if (id == null) {
			System.out.println("ID IS NULL");
			return;
		}
		
		// eventually we will need the game ID here - the game start time
		String speechName = name + "-" + System.currentTimeMillis() + ".wav"; 
		File testFile = new File("audio-data/" + speechName); 
		
		// Turn speaking fluent on.
		System.out.println("startAudio: here's the ID.");
		
		Entity entity = SEntity.GetEntityForName(name);
		if (entity == null) {
			System.out.println("StartAudio: Who the fuck is " + name);
			assert(false);
		}
		System.out.println("startAudio: Starting audio for entity " + entity.getName());
		entity.record("Speaking", true);
		entity.record("SpeakingFile", speechName);
		
		TDataOutputStream	dataOutputStream = null;
		try {
			dataOutputStream = AudioSystemShadow.getDataOutputStream(testFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		AudioOutputStream	audioOutputStream = AudioSystemShadow.getAudioOutputStream(
			AudioFileFormat.Type.WAVE,
			AudioConfig.createMasterFormat(),
			AudioSystem.NOT_SPECIFIED,
			dataOutputStream);
		
		_writers.put(id, audioOutputStream);
	}
	
	// This will close the file writing
	public void stopAudio(String name) {
		Byte id = _addressMap.get(_nameMap.get(name));
		
		if (id == null) {
			System.out.println("ID IS NULL");
			return;
		}
		
		// Turn speaking fluent off.
		Entity entity = SEntity.GetEntityForName(name); 
		System.out.println("stopAudio: Stopping audio for entity " + entity.getName());
		entity.record("Speaking", false);
		entity.closeFluent("SpeakingFile");
		
		// might this result in closing while still writing? probably
		AudioOutputStream audioOutputStream = _writers.remove(id);
		if (audioOutputStream != null) {
			try {
				audioOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// NB: the incoming packets are counter (1) + speex (113) = 114 bytes
	// outgoing are id (1) + counter (1) + speex (113) = 115 bytes
	public void run() {
		byte[] speex = new byte[AudioConfig.getSpeexBufferSize() + 1];
		byte[] pcm = new byte[AudioConfig.getPcmBufferSize()];
		SpeexDecoder decoder = AudioConfig.createDecoder();
			
		while (true)
		{
			// Sometimes this goes bugshit.
			if (_udp == null) {
				Logger.getLogger("").warning("AudioServer::run - no udp!  Quitting audio thread.");
				return; 
			}
			
			try	{
				DatagramPacket p = new DatagramPacket(speex, speex.length);
				_udp.receive(p);
				SocketAddress sa = p.getSocketAddress();
				
				Byte sender = _addressMap.get(sa);
				if (sender == null) {
					System.out.println("WHO SENT THIS?");
				} else {
					// 1 more byte for the sender ID
					byte[] sendBytes = new byte[speex.length + 1];
					ByteBuffer sendBuffer = ByteBuffer.wrap(sendBytes);
					sendBuffer.put(sender.byteValue());
					sendBuffer.put(speex);
					
					for (SocketAddress a : _addressMap.keySet()) {
						// send it right back to all the clients
						DatagramPacket p2 = new DatagramPacket(sendBytes, sendBytes.length, a);
						_udp.send(p2);
					}
					
					// Probably want to put decoding and writing in a separate thread, no?
					decoder.processData(speex, 1, speex.length - 1);				
					decoder.getProcessedDataByteSize();
					decoder.getProcessedData(pcm, 0);

					AudioOutputStream audioOutputStream = _writers.get(sender);
					if (audioOutputStream != null) {
						audioOutputStream.write(pcm, 0, pcm.length);
					} else {
						System.out.println("NULL AUDIO");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
