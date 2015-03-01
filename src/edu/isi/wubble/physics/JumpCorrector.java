package edu.isi.wubble.physics;

import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import com.jmex.physics.DynamicPhysicsNode;


public class JumpCorrector extends Corrector {
	private static final long serialVersionUID = 1L;

	protected int   _state;
	protected float _elapsedTime;
	
	public JumpCorrector(DynamicPhysicsNode node, Spatial orienting) {
		super(node, orienting);
	}
	
	public void setPosition(Vector3f desiredPos) {
		super.setPosition(desiredPos);
		_state = 0;
	}
	
	/**
	 * when we are active we need to move towards the 
	 * desired position unless we have achieved our goal.
	 * @param tpf
	 */
	public void update(float tpf) {
		if (_node.isResting())
			_node.unrest();
		
		switch (_state) {
		case 0:
			initializeJump();
			break;
		case 1:
			_elapsedTime += tpf;
			if (_elapsedTime > 0.5) {
				_lastDistance = 100;
				_state = 2;
			}
			break;
		case 2:
			super.update(tpf);
			break;
		}
	}
	
	private void initializeJump() {
		Vector3f dir = _desiredPos.subtract(_node.getLocalTranslation());
		Vector3f lookAt = new Vector3f(dir.x, 0, dir.z);
		_orienting.lookAt(lookAt.normalizeLocal(), Vector3f.UNIT_Y);
		
		Vector3f vel = _node.getLinearVelocity(null);
		vel.setY(vel.y + 10);
		_node.setLinearVelocity(vel);
	
		_elapsedTime = 0;
		_state = 1;
	}
}
