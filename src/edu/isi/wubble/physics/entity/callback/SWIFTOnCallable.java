package edu.isi.wubble.physics.entity.callback;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;

import edu.isi.wubble.physics.CollisionInfo;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

public class SWIFTOnCallable extends PairwiseCallable {
	public static final float ERROR = 0.1f;
	public static final float THRESHOLD = 0.05f;

	public SWIFTOnCallable(DynamicEntity e) {
		super(e);
	}

	public void call(Entity b) {
		if (_entity.isCollisionWith(b)) {
			// Get the ContactInfo for this entity.
			CollisionInfo cInfo = _entity.getCollision(b);
			Vector3f normalVec = cInfo.getNormal(_entity, b);
			Vector3f delta = normalVec.subtract(Vector3f.UNIT_Y.mult(-1));
			if (delta.length() < 3*ERROR) {
				_entity.record(b, "On", true);
				return;
			} 
		} else {
			float d = _entity.getDistance(b);
			if (Float.compare(d, 0.0f) == 0) {
				// we are still colliding even though ODE didn't pick it up.  On is
				// determined by seeing if _entity's lowest point from it's bounding
				// box is higher than or equal to the highest point in entity b (with tolerance)
				float aLowSpot = _entity.getPosition().y - (_entity.getSize().y * 0.5f);
				float bHighSpot = b.getPosition().y + (b.getSize().y / 2.0f);
				float delta = Math.abs(aLowSpot - bHighSpot);
				if (delta < THRESHOLD) {
					_entity.record(b, "On", true);
					return;
				}
			}
		}
		_entity.record(b, "On", false);
	}
}
