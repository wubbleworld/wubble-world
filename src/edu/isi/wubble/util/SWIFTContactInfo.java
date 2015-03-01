package edu.isi.wubble.util;

import com.jme.math.Vector3f;

import edu.isi.wubble.physics.entity.Entity;


public class SWIFTContactInfo {
	
	private Entity _a;
	private Entity _b;
	
	private float _distance;
	
	private Vector3f _nearestPtA;
	private Vector3f _nearestPtB;
	
	private Vector3f _normal;
	
	public SWIFTContactInfo(Entity a, Entity b) {
		_a = a;
		_b = b;
		
		_nearestPtA = new Vector3f();
		_nearestPtB = new Vector3f();

		_normal = new Vector3f();
	}
	
	public void setNormal(double x, double y, double z) {
		setNormal((float) x, (float) y, (float) z);
	}
	
	public void setNormal(float x, float y, float z) {
		_normal.set(x,y,z);
	}
	
	public void setNearestPoint(Entity e, double x, double y, double z) {
		setNearestPoint(e, (float) x, (float) y, (float) z);
	}
	
	public void setNearestPoint(Entity e, float x, float y, float z) {
		if (_a.getId() == e.getId()) {
			_nearestPtA.set(x,y,z);
			translateAndRotate(e, _nearestPtA);
		} else if (_b.getId() == e.getId()) {
			_nearestPtB.set(x,y,z);
			translateAndRotate(e, _nearestPtB);
		} else {
			System.out.println("bad request: " + e.getName() + " not part of this SWIFT info");
		}
		
	}
	
	public void setDistance(float d) {
		_distance = d;
	}
	
	public float getDistance() {
		return _distance;
	}
	
	public Vector3f getNearestPt(Entity e) {
		if (_a.getId() == e.getId())
			return _nearestPtA;
		else if (_b.getId() == e.getId())
			return _nearestPtB;
		
		System.out.println("bad request: " + e.getName() + " not part of SWIFT info");
		return null;
	}

	public Vector3f getNormal(Entity from, Entity to) {
		if (_a.getId() == from.getId()) {
			return _normal.mult(-1);
		} else {
			return _normal;
		}
	}

	private void translateAndRotate(Entity a, Vector3f v) {
		if ("static".equals(a.getEntityType()))
			return;
		
		Vector3f store = a.getRotation().toRotationMatrix().mult(v);
		store.addLocal(a.getPosition());

		v.set(store);
	}	
}
