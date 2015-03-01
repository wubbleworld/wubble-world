package edu.isi.wubble.physics.entity.callback;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;

import edu.isi.wubble.physics.CollisionInfo;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

public class OnCallable extends PairwiseCallable {

	public OnCallable(DynamicEntity e) {
		super(e);
	}

	@Override
	public void call(Entity b) {
		if (!_entity.isCollisionWith(b)) {
			_entity.record(b, "On", false);
			return;
		}
		
		// Get the ContactInfo for this entity.
		CollisionInfo cInfo = _entity.getCollision(b);
		
		//Vector3f normalVec = cInfo.getNormal(_entity, b);
		Vector3f normalVec = cInfo.getNormal(b, _entity);
		
		float angle = normalVec.angleBetween(Vector3f.UNIT_Z) * FastMath.RAD_TO_DEG;
		float angleY = normalVec.angleBetween(Vector3f.UNIT_Y) * FastMath.RAD_TO_DEG;
		float angleX = normalVec.angleBetween(Vector3f.UNIT_X) * FastMath.RAD_TO_DEG;
		
		//if (angle > 85.0 && angle < 95.0 && (angleY < 0.01 && angleY > -0.01)) {	

		if (angle > 85.0 && angle < 95.0 && _entity.getPosition().y > b.getPosition().y && (angleY < .1 && angleY > -.1)) {	
			_entity.record(b, "On", true); 
			if (!b.getName().equals("floor")) {
//				System.out.println(_entity.getName() + " is on " + b.getName() + " angle is " + angle 
//							+ " angleY is " + angleY + " angleX is " + angleX);
			}
		} else {
			_entity.record(b, "On", false);
		}
	}
}
