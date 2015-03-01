package edu.isi.wubble.physics.entity.callback;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

public class CollisionCallable extends PairwiseCallable {

	public CollisionCallable(DynamicEntity e) {
		super(e);
	}

	@Override
	public void call(Entity b) {
		if (b instanceof DynamicEntity && _entity.getId() > b.getId()) { 
			return;
		}
		
		if (_entity.isCollisionWith(b) || _entity.getDistance(b) == 0) 
			_entity.record(b, "Collision", true);
		else
			_entity.record(b, "Collision", false);
	}

}
