package edu.isi.wubble.physics;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jmex.physics.PhysicsSpace;

import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.Wubble;
import edu.isi.wubble.physics.entity.DynamicEntity;

public class FollowCorrector extends SpringCorrector {

	DynamicEntity _follower;
	DynamicEntity _followee;
	String _followeeName;
	
	int _ticksBetweenUpdates = 10;
	int _ticksSinceUpdate;
	public void setTicksBetweenUpdates(int t) { _ticksBetweenUpdates = t; }
	
	Vector3f _offsetToFollowee;
	public void setOffsetToFollowee(Vector3f o) { _offsetToFollowee = o; }
	Vector3f _chasePosition;
	
	public static FollowCorrector Create(String name, PhysicsSpace ps, DynamicEntity follower, String followeeName) {
		FollowCorrector fc = null;
		// I don't see this ever happening in actual usage, but what the hell.
		if (_correctorMap.containsKey(name)) {
			fc = (FollowCorrector) _correctorMap.get(name);
			fc._ticksSinceUpdate = 0;
			return fc;
		}
		DynamicEntity f = (DynamicEntity)SEntity.GetEntityForName(followeeName);
	

		// It's possible that entity could have disappeared by the time this call gets executed.
		// If that's happened, then don't create anything.
		if (f != null) { fc = new FollowCorrector(name, ps, follower, f); }

		// Make the following a bit more urgent.
		fc.STIFFNESS = 40.0f;
		// fc.STIFFNESS = 5.0f;

		_correctorMap.put(name, fc);
		ps.addToUpdateCallbacks(fc);
		
		return fc;
	}


	protected FollowCorrector(String name, PhysicsSpace ps, DynamicEntity follower, DynamicEntity followee) {
		super(name, ps, follower.getPhysicsNode(), new Vector3f());
		_ticksSinceUpdate = 0;
		_follower = follower;
		_followeeName = followee.getName();
		
		// Create a default offset from the followee the follower should try to attain.
		_offsetToFollowee = new Vector3f(2,0,-1.5f);
		_chasePosition    = new Vector3f();
	}

	public void afterStep(PhysicsSpace space, float time) {
		super.afterStep(space, time);
	}

	public void beforeStep(PhysicsSpace space, float time) {
		super.beforeStep(space, time);
		_ticksSinceUpdate++;
		if (_ticksSinceUpdate >= _ticksBetweenUpdates) {
			_ticksSinceUpdate = 0;
			_followee = (Wubble)SEntity.GetEntityForName(_followeeName);
			if (_followee != null) { 
				_chasePosition.set(_followee.getPosition());
				_chasePosition.add(_offsetToFollowee, _chasePosition);
				setLocation(_chasePosition);
			}
			else { deactivate(); }
		}
	}
}
