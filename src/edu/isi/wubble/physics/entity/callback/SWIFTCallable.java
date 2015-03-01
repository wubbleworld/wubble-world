package edu.isi.wubble.physics.entity.callback;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

public class SWIFTCallable extends ApproachingCallable {
	
	public SWIFTCallable(DynamicEntity e) {
		super("SWIFT", e);
	}
	
	public float getDistance(Entity b) {
		return _entity.getDistance(b);
	}
}
