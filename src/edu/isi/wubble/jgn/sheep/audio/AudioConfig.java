/*
 * Lucane - a collaborative platform
 * Copyright (C) 2004  Vincent Fiack <vfiack@mail15.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.isi.wubble.jgn.sheep.audio;

import java.io.Serializable;

import javax.sound.sampled.AudioFormat;

import org.xiph.speex.SpeexDecoder;
import org.xiph.speex.SpeexEncoder;

/**
 * Configuration for audio stream
 */
@SuppressWarnings("serial")
public class AudioConfig implements Serializable
{
	//-- speex buffer size
	private static final int[][] SPEEX_BUFFERS = {
		{8, 12, 17, 23, 23, 30, 30, 40, 40, 48, 64},
		{12, 17, 22, 27, 35, 45, 54, 62, 72, 88, 108},
		{13, 21, 26, 32, 39, 49, 59, 67, 77, 93, 113}
	};
	
	//-- speex modes - frame rates
	public static final int NARROWBAND = 1;
	public static final int WIDEBAND = 2;
	public static final int ULTRA_WIDEBAND = 3;
	
	//-- attributes
//	private int mode;
//	private int quality;
//	private int channels;

	static int mode = ULTRA_WIDEBAND;
	static int quality = 10;
	static int channels = 2;
	
	/**
	 * Constructor
	 * 
	 * @param mode the audio mode (NARROWBAND, WIDEBAND or ULTRA_WIDEBAND)
	 * @param quality the quality (1 - 10)
	 */
	private AudioConfig(int mode, int quality)
	{
		this.mode = mode;
		this.quality = quality;
		this.channels = 2;
	}
	
	/**
	 * Return the frame rate
	 * 
	 * @return the frame rate
	 */
	public static int getFrameRate()
	{
		if(mode == NARROWBAND)
			return 8000;
		if(mode == WIDEBAND)
			return 16000;
		if(mode == ULTRA_WIDEBAND)
			return 32000;
		
		throw new IllegalStateException("unknown mode: " + mode);
	}	
	
	/**
	 * Return the number of channels
	 * 
	 * @return the number of channels
	 */
	public static int getChannels()
	{
		return channels;
	}
		
	public static int getQuality()
	{
		return quality;
	}

	public static int getSpeexMode()
	{
		return mode -1;
	}
		
	public static int getPcmBufferSize()
	{
		if(mode == NARROWBAND)
			return 640;
		if(mode == WIDEBAND)
			return 1280;
		if(mode == ULTRA_WIDEBAND)
			return 2560;
		
		throw new IllegalStateException("unknown mode: " + mode);
	}
	
	public static int getSpeexBufferSize()
	{
		return SPEEX_BUFFERS[mode - 1][quality];
	}			
	
	public String toString()
	{
		return "" + mode;
	}
	
	//-- factories
	
	/**
	 * Factory to create AudioFormat with this configuration
	 * 
	 * @param type the audio encoding (PCM or SPEEX)
	 * @return the audio format
	 */
	public static AudioFormat createAudioFormat(AudioFormat.Encoding type)
	{
		return new AudioFormat(
				type,
				getFrameRate(), 
				16, 
				getChannels(), 
				getChannels()*2, 
				getFrameRate(), 
				false);
	}
	
	public static AudioFormat createMasterFormat() {
		return new AudioFormat(44100.0f, 16, 2, true, false);
	}
	
	public static SpeexEncoder createEncoder()
	{
		SpeexEncoder encoder = new SpeexEncoder();
		encoder.init(getSpeexMode(), getQuality(), getFrameRate(), getChannels());
		return encoder;
	}
	
	public static SpeexDecoder createDecoder()
	{
		SpeexDecoder decoder = new SpeexDecoder();
		decoder.init(getSpeexMode(), getFrameRate(), getChannels(), false);
		return decoder;
	}
}