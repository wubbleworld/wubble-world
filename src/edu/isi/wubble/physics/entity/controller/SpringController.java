package edu.isi.wubble.physics.entity.controller;

import com.jme.math.Vector3f;
import com.jmex.physics.DynamicPhysicsNode;

import edu.isi.wubble.physics.entity.DynamicEntity;

public class SpringController extends EntityController {
	
	private static final long serialVersionUID = 1L;

	public static float THRESHOLD = 0.5f;
	public static float THRESHOLD_SQUARED = 1.25f;
	
	public float STIFFNESS = 30.0f;
	public float DAMPING = 3.0f; 
	
	protected DynamicEntity      _entity;
	protected DynamicPhysicsNode _node;

	protected Vector3f _pos;

	public SpringController(DynamicEntity entity) {
		_entity = entity;
		_node = _entity.getPhysicsNode();
		
		setActive(false);
	}
	
	public boolean isClose() {
		return _entity.getPosition().distanceSquared(_pos) <= THRESHOLD_SQUARED;
	}
	
	public void setStiffness(float stiffness) {
		STIFFNESS = stiffness;
	}
	
	public void setDamping(float damping) {
		DAMPING = damping;
	}
	
	public void applyController(Vector3f pos) {
		_pos = pos;
		_node = _entity.getPhysicsNode();
		
		setActive(true);
	}
	
	public void stopController() {
		_pos = null;
		setActive(false);
	}
	
	public void update(float tpf) {
		if (_node.isResting()) {
			_node.unrest();
		}
		
		Vector3f currVel = _node.getLinearVelocity(null);
		Vector3f currPos = _entity.getPosition();

		Vector3f forceSpring = _pos.subtract(currPos).mult(STIFFNESS);
		Vector3f dampSpring = currVel.mult(DAMPING); 

		_node.addForce(forceSpring.subtract(dampSpring));
	}
}