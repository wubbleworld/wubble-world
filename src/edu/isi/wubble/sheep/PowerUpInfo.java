package edu.isi.wubble.sheep;

import java.util.HashMap;

import com.jme.renderer.ColorRGBA;

public class PowerUpInfo {

	public static enum PowerUpType { POWERDOWN,  JUMPER,  INVISIBLE,  SHEEP,  BOUNCE,  SPEEDY,  STICKY,  EATER, MULTI, NONE }

	private static HashMap<PowerUpType, ColorRGBA> _powerColors = new HashMap<PowerUpType, ColorRGBA>();
	static {
		_powerColors.put(PowerUpType.EATER, new ColorRGBA(1, 0, 0, 0.6f));
		_powerColors.put(PowerUpType.JUMPER, new ColorRGBA(0, 1, 0, 0.6f));
		_powerColors.put(PowerUpType.SPEEDY, new ColorRGBA(0, 0, 1, 0.6f));
		_powerColors.put(PowerUpType.BOUNCE, new ColorRGBA(1, (140f / 255f), 0, 0.6f));
		_powerColors.put(PowerUpType.SHEEP, new ColorRGBA(1, 1, 0, 0.6f));
		_powerColors.put(PowerUpType.INVISIBLE, new ColorRGBA(1, 1, 1, 0.6f));
		_powerColors.put(PowerUpType.STICKY, new ColorRGBA((140f / 255f), (40f / 255f), (140f / 255f), 0.6f));
		_powerColors.put(PowerUpType.MULTI, new ColorRGBA(0, 0, 0, .75f));
		_powerColors.put(PowerUpType.POWERDOWN, new ColorRGBA(0, 0, 0, 0));
		_powerColors.put(PowerUpType.NONE, new ColorRGBA(0, 0, 0, 0));
		
	}
	
	//public static HashMap<PowerUpType, ColorRGBA> GetPowerColors() { return _powerColors; }
	public static ColorRGBA GetPUColor(PowerUpType p) { return _powerColors.get(p); }
	
	

}
