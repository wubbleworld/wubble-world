package edu.isi.wubble.physics.entity.callback;

import static com.jme.math.FastMath.RAD_TO_DEG;
import static com.jme.math.FastMath.atan2;

import com.jme.math.Vector3f;

import edu.isi.wubble.physics.CollisionInfo;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.util.SWIFTContactInfo;

public class PrepositionCallable extends PairwiseCallable {
	public static final float ERROR = 0.1f;
	public static final float THRESHOLD = 0.05f;
	
	public PrepositionCallable(DynamicEntity e) {
		super(e);
	}

	@Override
	public void call(Entity b) {
		SWIFTContactInfo info = _entity.getSWIFT(b);
		if (info == null) {
			System.out.println("WEIRD: " + _entity.getName() + " " + b.getName());
			return;
		}
		
		_entity.record(b, "DistanceTo", info.getDistance());
		
		boolean aOrB = checkAboveOrBelow(b, info);
		
		if (!aOrB)
			predCheck(b, info);
	}
	
	protected boolean checkAboveOrBelow(Entity b, SWIFTContactInfo info) {
		String aWatch = "";
		String bWatch = "";
		
		if (_entity.isCollisionWith(b)) {
			// Get the ContactInfo for this entity.
			CollisionInfo cInfo = _entity.getCollision(b);
			
			Vector3f normalVec = cInfo.getNormal(_entity, b);
			Vector3f delta = normalVec.subtract(Vector3f.UNIT_Y.mult(-1));
			if (delta.length() < 3*ERROR) {
				if (_entity.getName().equals(aWatch) && b.getName().equals(bWatch))
					System.out.println("Old fashion collision: " + normalVec + " SWIFTNormal: " + info.getNormal(_entity, b));
				_entity.recordAuto(b, "Above", true);
				return true;
			} 
			
			delta = normalVec.subtract(Vector3f.UNIT_Y);
			if (delta.length() < 3*ERROR) {
				if (_entity.getName().equals(aWatch) && b.getName().equals(bWatch))
					System.out.println("Old fashion UD: " + normalVec + " SWIFTNormal: " + info.getNormal(_entity, b));
				_entity.recordAuto(b, "Below", true);
				return true;
			}
		}		
		float d = _entity.getDistance(b);
		if (Float.compare(d, 0.0f) == 0) {
			// we are still colliding even though ODE didn't pick it up.  Above is
			// determined by seeing if _entity's lowest point from it's bounding
			// box is higher than or equal to the highest point in entity b (with tolerance)
			float aLowSpot = _entity.getPosition().y - (_entity.getSize().y * 0.5f);
			float bHighSpot = b.getPosition().y + (b.getSize().y / 2.0f);
			float delta = Math.abs(aLowSpot - bHighSpot);
			if (delta < THRESHOLD) {
				if (_entity.getName().equals(aWatch) && b.getName().equals(bWatch))
					System.out.println("SWIFT found collision");
				_entity.recordAuto(b, "Above", true);
				return true;
			}
			
			// else we could be below it instead of above it....
			float aHighSpot = _entity.getPosition().y - (_entity.getSize().y * 0.5f);
			float bLowSpot = b.getPosition().y + (b.getSize().y * 0.5f);
			delta = Math.abs(aHighSpot - bLowSpot);
			if (delta < THRESHOLD) {
				if (_entity.getName().equals(aWatch) && b.getName().equals(bWatch))
					System.out.println("SWIFT found collision UD");
				_entity.recordAuto(b, "Below", true);
				return true;
			}
		} else {
			Vector3f normal = info.getNormal(_entity, b);
			Vector3f nDelta = normal.subtract(Vector3f.UNIT_Y.mult(-1));
			if (Math.abs(nDelta.x) < ERROR && Math.abs(nDelta.y) < ERROR && Math.abs(nDelta.z) < ERROR) {
				if (_entity.getName().equals(aWatch) && b.getName().equals(bWatch))
					System.out.println("Above: " + normal + " " + nDelta);
				_entity.recordAuto(b, "Above", true);
				return true;
			}
			Vector3f pDelta = normal.subtract(Vector3f.UNIT_Y);
			if (Math.abs(pDelta.x) < ERROR && Math.abs(pDelta.y) < ERROR && Math.abs(pDelta.z) < ERROR) {
				if (_entity.getName().equals(aWatch) && b.getName().equals(bWatch)) {
					System.out.println("Below: " + normal + " " + pDelta);
				}
				_entity.recordAuto(b, "Below", true);
				return true;
			}
		}
		
		if (_entity.getName().equals(aWatch) && b.getName().equals(bWatch))
			System.out.println("Not Above or Below: " + info.getNormal(_entity, b));
		
		return false;
	}
	
	protected void predCheck(Entity b, SWIFTContactInfo info) {
		Vector3f heading = _entity.getRotation().getRotationColumn(2).normalize();
		Vector3f normal = info.getNormal(_entity, b);
		
		if (_entity.isCollisionWith(b)) {
			// Get the ContactInfo for this entity.
			CollisionInfo cInfo = _entity.getCollision(b);
			normal = cInfo.getNormal(_entity, b);
		}		

		float xzOur = RAD_TO_DEG * atan2(heading.z, heading.x);
		float xzRel = RAD_TO_DEG * atan2(normal.z, normal.x);
		float diff = xzRel - xzOur;
		diff += (diff < 0.0f) ? 360.0f : 0.0f;
		
		if (diff < 45 || diff > 315) {
			_entity.recordAuto(b, "InFrontOf", true);
		} else if (diff < 135) {
			_entity.recordAuto(b, "RightOf", true);
		} else if (diff < 225) {
			_entity.recordAuto(b, "InBackOf", true);
		} else {
			_entity.recordAuto(b, "LeftOf", true);
		}
	}	
}
