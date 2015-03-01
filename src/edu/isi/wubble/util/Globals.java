package edu.isi.wubble.util;

import java.util.Random;

public class Globals {
	
	public static boolean IN_SHEEP_GAME = false;
	public static boolean INSTANCED_SERVER = false;

	public static boolean LOG_DYNAMIC = false;
	public static boolean USE_DATABASE = false;
	
	public static boolean SHANE_PRINTING = true;
	
	public static Random random;
	
	static {
		random = new Random();
	}
}
