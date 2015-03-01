package edu.isi.wubble.physics.entity.callback;

import static com.jme.math.FastMath.RAD_TO_DEG;
import static com.jme.math.FastMath.atan2;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.entity.Wubble;
import edu.isi.wubble.physics.CollisionInfo;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

public class PushCallable extends PairwiseCallable {

	public PushCallable(DynamicEntity e) {
		super(e);
	}

	@Override
	public void call(Entity e2) {
		
		// I already know _entity is a DynamicEntity - otherwise it makes no sense to have
		// a push controller.  Make sure the collided entity is also dynamic.
		if (_entity.isCollisionWith(e2) && e2 instanceof DynamicEntity) {
			// See if the collision is within the 45 degree slice 
			// in the current direction of movement.
			CollisionInfo colInfo = _entity.getCollision(e2);
			Vector3f normal = colInfo.getNormal(_entity, e2);

			
			Vector3f ourDir = _entity.getRotation().getRotationColumn(2).normalize();
			float angleBetween = normal.angleBetween(ourDir) * RAD_TO_DEG;

			// If the contact is within a certain region of the Wubble's heading, then give
			// an impetus to the second entity along the appropriate direction.
			if (angleBetween > -45.0f && angleBetween < 45.0f) {
				DynamicEntity de = (DynamicEntity)e2;
				de.getPhysicsNode().addForce(new Vector3f(normal).mult(800)); // new Vector3f(0, 500, 0));
			}
				
//			Vector3f direction = colInfo.getPosition().subtract(_entity.getPosition()).normalize();
//			Vector3f direction = e1.getPosition().subtract(_entity.getPosition()).normalize();
//			float xyRel  = RAD_TO_DEG * atan2(direction.y, direction.x);
			
//			System.out.println(_entity.getName() + " collided with " + e2.getName() + " at " 
//					+ Utils.vs(normal) + " angleBetween " + angleBetween);
			
		}
		

		// Push the the thing.
	}
}
