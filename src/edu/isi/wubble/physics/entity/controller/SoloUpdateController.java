package edu.isi.wubble.physics.entity.controller;

import static com.jme.math.FastMath.atan2;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.util.Swift;

/**
 * added by default to all dynamic entities.  This controller
 * just watches the state of the entity and when it is changing
 * it records updates.
 * @author wkerr
 *
 */
public class SoloUpdateController extends EntityController {
	private static final long serialVersionUID = 1L;
	
	protected static Vector3f _tmpVec;
	
	protected static final int SURFACE_MOTION = 0;
	protected static final int VERTICAL_MOTION = 1;
	protected static final int XZROTATION = 2;
	
	protected static final int COUNT = 3;
	
	public static final float ERROR = 0.007f;
	
	protected DynamicEntity _entity;
	
	// string is fluent name 
	protected Object[] _history;
	
	public SoloUpdateController(DynamicEntity e) {
		_entity = e;
		_tmpVec = new Vector3f();
		_history = new Object[COUNT];
	}
	
	public void update(float time) {
		if (!canUpdate())
			return;
		
		testSurfaceMotion();
		testVerticalMotion();
		testXZRotation();
	}
	
	//--------------------------------------------------------------------------
	// single entity relationships
	//--------------------------------------------------------------------------
	
	public void testSurfaceMotion() {
		String fluent = "SurfaceMotion";
		
		Vector3f prevValue = (Vector3f) _history[SURFACE_MOTION];
		if (prevValue == null) {
			_entity.recordAuto(fluent, false);
			_history[SURFACE_MOTION] = new Vector3f(_entity.getPosition());
			return;
		}

		prevValue.y = 0;
		_tmpVec.set(_entity.getPosition());
		_tmpVec.y = 0;
		
		float distance = _tmpVec.distance(prevValue);
		if (distance > ERROR) {
			// we are moving, so we need to update SWIFT
			Swift.inst().update(_entity);
			
			_entity.recordAuto(fluent, true);
			_entity.recordAuto("Motion", true);
			_entity.recordSample("Position", _entity.getPosition());
			_entity.setChanging(true);
		} else {
			_entity.recordAuto(fluent, false);
		}
		
		((Vector3f) _history[SURFACE_MOTION]).set(_tmpVec);
	}
	
	/**
	 * vertical motion is different that surface motion.
	 * vertical occurs when we jump or attempt to climb something
	 * therefore it's worth maintaining.
	 */
	public void testVerticalMotion() {
		Vector3f prevValue = (Vector3f) _history[VERTICAL_MOTION];
		if (prevValue == null) {
			_history[VERTICAL_MOTION] = new Vector3f(_entity.getPosition());
			return;
		}

		float prevY = prevValue.y;
		float currentY = _entity.getPosition().y;
		
		
		float distance = prevY - currentY;
		if (distance < -ERROR) {
			// we are moving, so we need to update SWIFT
			Swift.inst().update(_entity);

			_entity.recordAuto("PositiveVerticalMotion", true);
			_entity.recordAuto("Motion", true);
			_entity.recordSample("Position", _entity.getPosition());
			_entity.setChanging(true);
		} else if (distance > ERROR) {
			// we are moving, so we need to update SWIFT
			Swift.inst().update(_entity);
			
			_entity.recordAuto("NegativeVerticalMotion", true);
			_entity.recordAuto("Motion", true);
			_entity.recordSample("Position", _entity.getPosition());
			_entity.setChanging(true);
		} 
		
		((Vector3f) _history[VERTICAL_MOTION]).set(_entity.getPosition());
	}
	
	public void testXZRotation() {
		String fluent = "XZRotation";

		Vector3f ourDir = _entity.getRotation().getRotationColumn(2).normalize();
		float newAngle = atan2(ourDir.z, ourDir.x);
		
		Float prevValue = (Float) _history[XZROTATION];
		if (prevValue == null) {
			_entity.record(fluent, false);
			_history[XZROTATION] = new Float(newAngle);
			return;
		}

		float delta = Math.abs(prevValue.floatValue() - newAngle);
		if (delta > ERROR) {
			// we are moving, so we need to update SWIFT
			Swift.inst().update(_entity);
			
//			if (_entity.getName().equals("auto")) {
//				System.out.println("...[auto] previous: " + prevValue + " " + newAngle);
//			}
			
			_entity.recordSample("Rotation", newAngle);
			_entity.record(fluent, true);
			_entity.setChanging(true);
		} else {
			_entity.record(fluent, false);
		}
		
		_history[XZROTATION] = new Float(newAngle);
	}
}