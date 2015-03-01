package edu.isi.wubble.jgn.sheep;

import com.jme.math.Vector3f;
import com.jmex.physics.DynamicPhysicsNode;

import edu.isi.wubble.jgn.sheep.entity.SDynamicEntity;

public class SheepUpdateStrategy extends EntityUpdateStrategy {
	
	public SheepUpdateStrategy(SDynamicEntity e) {super(e); }

	// The update strategy for sheep wanders around aimlessly.
	public boolean updateEntity() {
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
//		int x = w.halfRand(75);
//		int y = w.halfRand(40);
//		int z = w.halfRand(75);
		
		Vector3f f = new Vector3f(10, 0, -10);
		DynamicPhysicsNode n = (DynamicPhysicsNode)getEntity().getNode();
		
		n.addForce(f);
		
		// getEntity().applyImpulse(500);
		
		return true;
	}
}
