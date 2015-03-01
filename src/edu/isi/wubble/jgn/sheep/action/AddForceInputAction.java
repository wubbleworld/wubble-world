package edu.isi.wubble.jgn.sheep.action;

import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.math.Vector3f;
import com.jmex.physics.DynamicPhysicsNode;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.entity.SDynamicEntity;
import edu.isi.wubble.jgn.sheep.entity.SEntity;

public class AddForceInputAction extends InputAction {
	private final Vector3f direction;
	private final Vector3f appliedForce = new Vector3f();

	public AddForceInputAction(Vector3f direction) {
		this.direction = direction;
	}

	public void performAction(InputActionEvent evt) {
		appliedForce.set(direction); // .multLocal(5);   // (evt.getTime());
		SheepPhysicsState w = SheepPhysicsState.getWorldState();

		SEntity ce = w.getCurEntity();
		// Right now we can only apply this to DynEntities.
		if (ce == null || !(ce instanceof SDynamicEntity)) { return; }
		SDynamicEntity de = (SDynamicEntity)ce;

		DynamicPhysicsNode p = (DynamicPhysicsNode)ce.getNode();

		Vector3f oldForce = new Vector3f();
		p.getForce(oldForce);

		Vector3f vel = new Vector3f();
		p.getLinearVelocity(vel);

		// Add a burst in the appropriate direction.
		Vector3f l1 = p.getLocalTranslation();
		//Vector3f l2 = de.getIndicator().getWorldTranslation();
		//Vector3f l2 = de.getRotation().
		
		Vector3f impulse = Vector3f.UNIT_Z; // l2.subtract(l1);
		
		//w.p("l1 --> " + w.vs(l1) + " l2 --> " + w.vs(l2) + " impulse --> " + w.vs(impulse));
//		impulse.y = 1;

		//impulse = direction;

		impulse.normalizeLocal();
		impulse.setY(0);
		impulse.multLocal(500);

		// This adds a force based on the heading.
		//p.addForce(impulse);

		// This adds force based on the direction supplied in the constructor.
		p.addForce(appliedForce);
	}
}
