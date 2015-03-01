package edu.isi.wubble.physics;

import com.jme.math.Vector3f;

import edu.isi.wubble.physics.entity.Entity;

public class CollisionInfo {
	private Entity _a;
	private Entity _b;
	
	private Vector3f _position;
	private Vector3f _normal;
	
	public CollisionInfo(Entity a, Entity b) {
		_a = a;
		_b = b;

		_normal = new Vector3f();
		_position = new Vector3f();
	}
	
	public void setNormal(Vector3f normal) {
		_normal.set(normal);
	}
	
	public Vector3f getNormal(Entity from, Entity to) {
		if (_a.getId() == from.getId()) {
			return _normal.mult(-1);
		} else if (_b.getId() == from.getId()) {
			return _normal;
		} else {
			System.out.println("unknown get normal: " + from.getName() + " " + to.getName());
			return _normal;
		}
	}
	
	public void setPosition(Vector3f pos) {
		_position.set(pos);
	}
	
	public Vector3f getPosition() {
		return _position;
	}
}
