package edu.isi.wubble.physics.entity.controller;

import java.util.TreeMap;

import com.jme.input.controls.GameControlManager;
import com.jme.scene.Controller;

import edu.isi.wubble.physics.TimeManager;

public abstract class EntityController extends Controller {

	protected static GameControlManager _manager;

	static {
		_manager = new GameControlManager();
	}
	
	protected TreeMap<String,Float> _modifiers; 
	protected long _lastUpdate;
	
	public EntityController() {
		_lastUpdate = -1;
		_modifiers = new TreeMap<String,Float>();
	}
	
	public float getModifier(String name) {
		if (_modifiers.containsKey(name)) {
			return _modifiers.get(name);
		} else {
			return 1.0f;
		}
	}
	
	public void updateModifier(String name, float value) {
		_modifiers.put(name, value);
	}
	
	public float getSpeed() {
		float speed = super.getSpeed();
		for (float value : _modifiers.values()) 
			speed *= value;
		return speed;
		
	}
	
	public void cleanup() {
		// override if you have some fun stuff to do.
	}
	
	public boolean canUpdate() {
		if (_lastUpdate == -1) {
			_lastUpdate = TimeManager.inst().getLogicalTime();
			return true;
		}
		
		if (_lastUpdate == TimeManager.inst().getLogicalTime())  {
			return false;
		}
		
		_lastUpdate = TimeManager.inst().getLogicalTime();
		return true;
	}
}
