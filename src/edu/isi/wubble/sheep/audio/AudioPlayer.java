package edu.isi.wubble.sheep.audio;

import java.util.Stack;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.xiph.speex.SpeexDecoder;

import edu.isi.wubble.jgn.sheep.audio.AudioConfig;

public class AudioPlayer extends Thread {
	// For now we will only store one buffer
	byte[] _buffer;
//	
	Stack<byte[]> _stack = new Stack<byte[]>();
	byte _newest = Byte.MIN_VALUE;
	
	SourceDataLine _dataLine;
	
	SpeexDecoder _decoder;
	
	boolean _playing = true;
	
	public AudioPlayer() {
		_buffer = null;
		
		AudioFormat lineFormat = AudioConfig.createAudioFormat(AudioFormat.Encoding.PCM_SIGNED);
		lineFormat = AudioConfig.createMasterFormat();
		
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, lineFormat, AudioSystem.NOT_SPECIFIED);
		try	{
			_dataLine = (SourceDataLine) AudioSystem.getLine(info);
			_dataLine.open(lineFormat, _dataLine.getBufferSize());
			_dataLine.start();
		} catch(LineUnavailableException e) {
			e.printStackTrace();
		}
		
		_decoder = AudioConfig.createDecoder();
	}
	
	public void write(byte[] b) {
		if (b != null && b.length == AudioConfig.getSpeexBufferSize() + 1) {
			byte oldNewest = _newest;
			_newest = b[0];
			
			if (_newest > oldNewest || _newest == Byte.MIN_VALUE) {
//				_stack.push(b);
				_buffer = b;
			} else {
				System.out.println("NOT THE NEWEST " + oldNewest + " " + _newest);
			}
		} else {
			System.out.println("REJECTED INVALID BUFFER");
		}
	}
	
	public void cleanup() {
		_playing = false;
	}
	
	public void run() {
		while (_playing && _dataLine.isOpen()) { 
			if (_buffer == null) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
//					e.printStackTrace();
				}
			} else {
				byte[] speex = _buffer;
				_buffer = null;
				
				// process the byte[]
				try {
					byte[] pcm = new byte[AudioConfig.getPcmBufferSize()];
					
					_decoder.processData(speex, 1, speex.length - 1);				
					
					int length = _decoder.getProcessedDataByteSize();
					_decoder.getProcessedData(pcm, 0);
				
					if (length >= 0) {
						_dataLine.write(pcm, 0, length);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		// cleanup
		_dataLine.stop();
		_dataLine.close();
		
		System.out.println("PLAYER DEAD");
	}
}
