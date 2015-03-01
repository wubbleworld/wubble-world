package edu.isi.wubble.sheep.audio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.xiph.speex.SpeexEncoder;

import edu.isi.wubble.Main;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.sheep.audio.AudioConfig;

public class AudioSender extends Thread {
	AudioFormat _format;
	TargetDataLine _dataLine;
	AudioInputStream _stream;
	
	DatagramSocket _udp;
	
	// we won't be sending until player presses shift
	boolean _sending = false;
	boolean _alive = true;
	
	InetAddress _serverAddress;
	int _serverPort;
	
	public AudioSender(DatagramSocket sendSocket) {
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, AudioConfig.createMasterFormat());
		try {
			_dataLine = (TargetDataLine) AudioSystem.getLine(info);
			_dataLine.open();
			_dataLine.start();

			_format = AudioConfig.createAudioFormat(AudioFormat.Encoding.PCM_SIGNED);
			
			_stream = AudioSystem.getAudioInputStream(_format, new AudioInputStream(_dataLine));
		} catch (LineUnavailableException lue) {
			lue.printStackTrace();
		}	
		
		try {
			_serverPort = 11000;
			_serverAddress = InetAddress.getByName(Main.inst().getServer("sheep"));
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		try {
			if (sendSocket == null) {
				_udp = new DatagramSocket();
			} else {
				_udp = sendSocket;
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void cleanup() {
		_alive = false;
	}
	
	public void run() {
		byte[] pcm = new byte[AudioConfig.getPcmBufferSize()];
		byte[] speex = new byte[AudioConfig.getSpeexBufferSize() + 1];
		SpeexEncoder encoder = AudioConfig.createEncoder();
		byte counter = 0;
		
		while(_alive && _dataLine.isOpen()) {	
			if (_sending) {
				_dataLine.read(pcm, 0, pcm.length);
				encoder.processData(pcm, 0, pcm.length);
				encoder.getProcessedDataByteSize();
				encoder.getProcessedData(speex, 1);
				
				speex[0] = ++counter;
				
				try {
					DatagramPacket p = new DatagramPacket(speex, speex.length, _serverAddress, _serverPort);
					_udp.send(p);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}	
		
		// cleanup
		_udp.close();
		_dataLine.stop();
		_dataLine.close();
		try {
			_stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("SENDER DEAD");
	}
	
	public void startSending() {
		InvokeMessage im = new InvokeMessage();
		im.setArguments(new Object[]{Main.inst().getName()});
		im.setMethodName("startAudio");
		im.sendToServer();
		
		_sending = true;
	}
	
	public void stopSending() {
		InvokeMessage im = new InvokeMessage();
		im.setArguments(new Object[]{Main.inst().getName()});
		im.setMethodName("stopAudio");
		im.sendToServer();
		
		_sending = false;
	}
	
	public static void main(String[] args) throws Exception {
		Thread t = new AudioSender(null);
		t.run();
	}

}
