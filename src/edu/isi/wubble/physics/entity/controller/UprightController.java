package edu.isi.wubble.physics.entity.controller;

import static com.jme.math.FastMath.atan2;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import edu.isi.wubble.physics.entity.PhysicsEntity;

public class UprightController extends EntityController {
	private static final long serialVersionUID = 1L;

	protected PhysicsEntity _entity;
	protected Quaternion _upright;
	
	public UprightController(PhysicsEntity de) {
		_entity = de;
		
		Vector3f ourDir = _entity.getRotation().getRotationColumn(2).normalize();
		float xzOur = atan2(ourDir.z, ourDir.x);

		_upright = new Quaternion();
		_upright.fromAngleNormalAxis(xzOur, new Vector3f(0,1,0));
	}
	
	@Override
	public void update(float time) {

		Vector3f ourDir = _entity.getRotation().getRotationColumn(2).normalize();
		float xzOur = atan2(ourDir.z, ourDir.x);
		
		_upright.fromAngleAxis(xzOur, Vector3f.UNIT_Y);
		_entity.getRotation().fromAngleNormalAxis(xzOur, Vector3f.UNIT_Y);
	}

}
