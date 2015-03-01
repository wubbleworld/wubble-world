package edu.isi.wubble.physics.entity.controller;

import edu.isi.wubble.physics.entity.DynamicEntity;

public class SheepMovementController extends edu.isi.wubble.physics.entity.controller.MovementController {

	private static final long serialVersionUID = 1L;
	
	private float _speedModPowerUp;
	private float _sheepInGarden;
	
	public float getBaseSpeed() { return super.getSpeed(); }
	
	public float getSpeedPowerUp() { return _speedModPowerUp; }
	public float getSpeedGarden()  { return _sheepInGarden;  }
	
	public void setSpeedPowerUp(float s) { _speedModPowerUp = s; }
	public void setSheepInGarden(float s)  { _sheepInGarden  = s; }
	
	public SheepMovementController(DynamicEntity de, int defSpeed) {
		super(de, defSpeed);
		_sheepInGarden   = 0;
		_speedModPowerUp = 1;
	}
	
	@Override
	public float getSpeed() {
		double gardenMod = (double)super.getSpeed() * (Math.pow(.9, _sheepInGarden));
		gardenMod = Math.max(gardenMod, super.getSpeed() / 2);
		
		double speed = gardenMod * _speedModPowerUp;
		return (float)speed;
	}
}
