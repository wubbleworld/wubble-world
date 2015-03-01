package edu.isi.wubble;

import com.jme.renderer.ColorRGBA;


public interface Constants {
	public static Integer DIR_FORWARD    = 1;
	public static Integer DIR_BACKWARD   = 2;
	public static Integer DIR_LEFT_TURN  = 3;
	public static Integer DIR_RIGHT_TURN = 4;
	public static Integer DIR_JUMP = 4;

	public static final int NUM_TEAMS = 2;
	public static final int BLUE_TEAM = 0;
	public static final int RED_TEAM  = 1;
	
	public static final ColorRGBA WUBBLE_BLUE = new ColorRGBA(0f, (126f / 255f), 1.0f, 0f);
	public static final ColorRGBA WUBBLE_RED = new ColorRGBA((215f / 255f), (58f / 255f), (5f / 255f), 0f);
	
	public static final ColorRGBA[] COLORS = new ColorRGBA[]{ WUBBLE_BLUE, WUBBLE_RED };
	
	public static final ColorRGBA COLOR_OFF = new ColorRGBA(0,0,0,0);
}
