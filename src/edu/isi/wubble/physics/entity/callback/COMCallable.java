package edu.isi.wubble.physics.entity.callback;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

/**
 * this is a Center-Of-Mass approaching callable.
 * @author wkerr
 *
 */
public class COMCallable extends ApproachingCallable {

	public COMCallable(DynamicEntity e) {
		super("COM", e);
	}
	
	public float getDistance(Entity b) {
		return _entity.getPosition().distance(b.getPosition());
	}
}
