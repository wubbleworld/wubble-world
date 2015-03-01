package edu.isi.wubble.physics.entity.controller;

import com.jme.input.controls.GameControl;
import com.jme.input.controls.binding.KeyboardBinding;
import com.jmex.physics.DynamicPhysicsNode;

import edu.isi.wubble.AutoBinding;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.util.Globals;

public abstract class BinaryController extends EntityController {

	private static final long serialVersionUID = 1L;

	// use the visual node to get the direction and 
	// the physics node to set the velocity.
	protected DynamicEntity      _entity;
	protected DynamicPhysicsNode _physicsNode;
	
	protected GameControl _positive;
	protected GameControl _negative;
	String _pName;
	String _nName;
	
	public BinaryController(DynamicEntity entity, String pName, String nName, float speed) {
		_entity      = entity;
		_physicsNode = _entity.getPhysicsNode();
		_pName = pName;
		_nName = nName;

		if (Globals.SHANE_PRINTING)	
			System.out.println("Adding binary controller to " + entity.getName() + " w/pName " + pName + " and nName " + nName);
		_positive = _manager.addControl(pName);
		_negative = _manager.addControl(nName);

		assert(_positive != null);
		assert(_negative != null);
		setSpeed(speed);
	}
	
	public void addAutoBinding(String pad) {
		_positive.addBinding(AutoBinding.createBinding(_entity.getName() + pad, _pName));
		_negative.addBinding(AutoBinding.createBinding(_entity.getName() + pad, _nName));
	}

	public void addAutoBinding() {
		_positive.addBinding(AutoBinding.createBinding(_entity.getName(), _pName));
		_negative.addBinding(AutoBinding.createBinding(_entity.getName(), _nName));
	}

	public void addKeyBinding(int p, int n) {
		KeyboardBinding pos = new KeyboardBinding(p);
		KeyboardBinding neg = new KeyboardBinding(n);

		_positive.addBinding(pos); 
		_negative.addBinding(neg);
	}
}
