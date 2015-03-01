package edu.isi.wubble.physics.entity.callback;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

public abstract class PairwiseCallable {

	protected DynamicEntity _entity;
	
	public PairwiseCallable(DynamicEntity e) {
		_entity = e;
	}
	
	public abstract void call(Entity e2);
}
