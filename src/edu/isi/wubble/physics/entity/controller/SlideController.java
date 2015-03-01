package edu.isi.wubble.physics.entity.controller;

import com.jme.input.KeyInput;
import com.jme.input.controls.GameControl;
import com.jme.input.controls.binding.KeyboardBinding;
import com.jme.math.Vector3f;
import com.jmex.physics.DynamicPhysicsNode;

import edu.isi.wubble.AutoBinding;
import edu.isi.wubble.physics.entity.DynamicEntity;

public class SlideController extends EntityController {

	private static final long serialVersionUID = 1L;

	// use the visual node to get the direction and 
	// the physics node to set the velocity.
	protected DynamicEntity      _entity;
	protected DynamicPhysicsNode _physicsNode;
	
	protected GameControl _positive;
	protected GameControl _negative;
	
	protected int _count;
	

    public SlideController(DynamicEntity entity) {
    	this(entity, 4.0f);
    }
    
    public SlideController(DynamicEntity entity, float speed) {
		_entity      = entity;
		_physicsNode = _entity.getPhysicsNode();
		
		_positive = _manager.addControl("forward");
		_negative = _manager.addControl("backward");

		_count = 0;
		setSpeed(speed);
    }
    
    public void addAutoBinding() {
		_positive.addBinding(AutoBinding.createBinding(_entity.getName(), "forward"));
		_negative.addBinding(AutoBinding.createBinding(_entity.getName(), "backward"));
    }
    
    public void addKeyBinding() {
		_positive.addBinding(new KeyboardBinding(KeyInput.KEY_W));
		_negative.addBinding(new KeyboardBinding(KeyInput.KEY_S));
    }
    
	public void update(float tpf) {
		if (!canUpdate())
			return;
		
		float value = _positive.getValue() - _negative.getValue();
		
		if (value != 0.0f) {
			if (value < 0) 
				_entity.recordAuto("Backward", true);
			else 
				_entity.recordAuto("Forward", true);
			
			Vector3f direction = _entity.getRotation().getRotationColumn(2);
			direction.normalizeLocal();
			direction = direction.mult(getSpeed() * value);

			_physicsNode.getMaterial().setSurfaceMotion( direction );
				
			if (_physicsNode.isResting()) {
				_physicsNode.unrest();
			} 
		} else {
			Vector3f currentVelocity = _physicsNode.getLinearVelocity(null);
			currentVelocity.x = currentVelocity.x * 0.1f;
			currentVelocity.z = currentVelocity.z * 0.1f;
			_physicsNode.setLinearVelocity(currentVelocity);
		}
	}
}
