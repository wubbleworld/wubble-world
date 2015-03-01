package edu.isi.wubble.physics.entity.callback;

import java.util.HashMap;

import com.jme.math.FastMath;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.util.gjk.GJK;

public class GJKCallable extends ApproachingCallable {
	protected HashMap<String,GJK> _gjkMap;
	
	public GJKCallable(DynamicEntity e1) {
		super("GJK", e1);
		
		_gjkMap = new HashMap<String,GJK>();
	}
	
	public float getDistance(Entity b) {
		GJK gjk = _gjkMap.get(b.getName());
		if (gjk == null) {
			gjk = new GJK(_entity, b);
			_gjkMap.put(b.getName(), gjk);
		}
		
		float distance = gjk.getDistance(null, null, null, 0);
		return FastMath.sqrt(distance);
	}
}
