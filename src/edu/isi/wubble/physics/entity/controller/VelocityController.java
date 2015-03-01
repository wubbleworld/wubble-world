package edu.isi.wubble.physics.entity.controller;

import com.jme.math.Vector3f;
import com.jmex.physics.DynamicPhysicsNode;

import edu.isi.wubble.physics.entity.DynamicEntity;

/**
 * use this controller to keep the object moving towards a destination using
 * velocity updates rather than spring (forces) updates
 * @author wkerr
 *
 */

public class VelocityController extends EntityController {
	
	private static final long serialVersionUID = 1L;

	public static float THRESHOLD = 0.5f;
	public static float THRESHOLD_SQUARED = 1.25f;
	
	protected DynamicEntity      _entity;
	protected DynamicPhysicsNode _node;

	protected float    _lastDistance;
	protected Vector3f _pos;

	public VelocityController(DynamicEntity entity) {
		_entity = entity;
		_node = _entity.getPhysicsNode();
		
		setSpeed(3.0f);
		setActive(false);
	}
	
	public boolean isClose() {
		return _entity.getPosition().distanceSquared(_pos) <= THRESHOLD_SQUARED;
	}
	
	public void applyController(Vector3f pos) {
		_pos = pos;
		_node = _entity.getPhysicsNode();
		_lastDistance = _pos.subtract(_entity.getPosition()).length();
		
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
		
		Vector3f dir = _pos.subtract(_entity.getPosition());
		float distance = dir.length();
		if (distance < 0.1 || distance > _lastDistance) {
			_node.setLinearVelocity(new Vector3f(0,0,0));
			_lastDistance = distance;
			return;
		}
		
		_lastDistance = distance;
		_node.setLinearVelocity(dir.normalize().mult(getSpeed()));
	}
}