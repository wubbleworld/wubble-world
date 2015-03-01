package edu.isi.wubble.physics.entity.callback;

import static com.jme.math.FastMath.RAD_TO_DEG;
import static com.jme.math.FastMath.atan2;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import com.jme.math.Vector3f;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

public class InFOVCallable extends PairwiseCallable {

	public HashMap<Entity, Boolean> _inFOV;
	
	public InFOVCallable(DynamicEntity e) {
		super(e);
		
		_inFOV = new HashMap<Entity, Boolean>();
	}
	
	public HashMap<Entity, Boolean> getFOV() { return _inFOV; }
	
	
	public void printFOV() {
		Set<Entry<Entity, Boolean>> es = _inFOV.entrySet();
		for (Entry<Entity, Boolean> e : es) {
			Entity entity = e.getKey();
			boolean val = e.getValue();
			if (val) {
				System.out.println(_entity.getName() + " can see " + entity.getName());
			}
		}
	}
	
	
	// Right now the entities are populated in the _inFOV map to mimic the fluents; this means
	// they are NOT automatically cleared per-update, as they really should be.  For now.
	public void call(Entity b) {
		Vector3f ourDir = _entity.getRotation().getRotationColumn(2).normalize();
		Vector3f direction = b.getPosition().subtract(_entity.getPosition()).normalize();
		float xzOur = RAD_TO_DEG * atan2(ourDir.z, ourDir.x);
		float xzRel = RAD_TO_DEG * atan2(direction.z, direction.x);
		
		float diff = xzRel - xzOur;
		if (diff < 0.0f) 
			diff += 360.0f;
		if (diff < 45 || diff > 315) {
			_entity.record(b, "InFOV", true);
			_inFOV.put(b, true);
		}
		else { 
			_entity.record(b, "InFOV", false);
			_inFOV.remove(b);
		}
	}

	// Returns whether or not the provided entity is in _entity's FOV.
	public boolean isInFOV(Entity e) { return _inFOV.get(e); }
}
