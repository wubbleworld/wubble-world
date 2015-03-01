package edu.isi.wubble.physics.entity.controller;

import com.jme.math.Vector3f;

import edu.isi.wubble.physics.entity.DynamicEntity;

public class MovementController extends BinaryController {

	private static final long serialVersionUID = 1L;
	protected int _count;

    public MovementController(DynamicEntity entity) {
    	this(entity, 4.0f);
    }
    
    public MovementController(DynamicEntity entity, float speed) {
    	super(entity, "forward", "backward", speed);
		_count = 0;
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

			Vector3f currVelocity = _physicsNode.getLinearVelocity(null);
			direction.y = currVelocity.y;

			_physicsNode.setLinearVelocity(direction);
			_physicsNode.setAngularVelocity(new Vector3f(0,0,0));
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
