package edu.isi.wubble.physics.entity.controller;

import com.jme.math.Matrix3f;
import com.jme.math.Vector3f;

import edu.isi.wubble.physics.entity.DynamicEntity;

public class RotationController extends BinaryController {

	private static final long serialVersionUID = 1L;

    //temporary variables to handle rotation
    private static final Matrix3f _incr = new Matrix3f();
    private static final Matrix3f _tempMa = new Matrix3f();
    private static final Matrix3f _tempMb = new Matrix3f();

    private Vector3f lockAxis = new Vector3f(0,1,0);
    
    public RotationController(DynamicEntity entity) {
    	this(entity, 3.0f);
    }
    
    public RotationController(DynamicEntity entity, float speed) {
    	super(entity, "left", "right", speed);
		setSpeed(speed);
    }
    
	public void update(float tpf) {
		if (!canUpdate())
			return;
		
		float value = _positive.getValue() - _negative.getValue();

		if (value != 0.0f) {
//			System.out.println("rotController: updating " + _entity.getName());
//			System.out.println("  getSpeed returns " + getSpeed() + " value is " + value);
//			System.out.println("entity.getRotation: " + _entity.getRotation());
			
			if (value < 0)
				_entity.recordAuto("Right", true);
			else 
				_entity.recordAuto("Left", true);

//			float param = value * getSpeed() * tpf;
//			System.out.println("  param is " + param);
			
			_incr.fromAngleNormalAxis(value * getSpeed() * tpf, lockAxis);
			_entity.getRotation().fromRotationMatrix(
					_incr.mult(_entity.getRotation().toRotationMatrix(_tempMa),
							_tempMb));
			_entity.getRotation().normalize();
			
			if (_physicsNode != null) { _physicsNode.unrest(); }
		}
	}
}