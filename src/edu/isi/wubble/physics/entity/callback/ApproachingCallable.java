package edu.isi.wubble.physics.entity.callback;

import java.util.HashMap;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

public abstract class ApproachingCallable extends PairwiseCallable {
	public static final float ERROR = 0.001f;

	protected String _towards;
	protected String _away;
	protected String _distanceTo;
	
	protected HashMap<Integer,Float> _previousMap;

	public ApproachingCallable(String prefix, DynamicEntity e) {
		super(e);
		
		_towards = prefix + "Towards";
		_away    = prefix + "Away";
		_distanceTo = prefix + "DistanceTo";
		
		_previousMap = new HashMap<Integer,Float>();
	}

	public abstract float getDistance(Entity b);
	
	public void call(Entity b) {
		if (b instanceof DynamicEntity && _entity.getId() > b.getId())
			return;
		
//		if (!_entity.isChanging() && !b.isChanging())
//			return;
		
		float distance = getDistance(b);
		_entity.setDistance(b, distance);
		_entity.recordSample(b, _distanceTo, distance);

		Object prevValue = _previousMap.get(b.getId());

		boolean towards = false;
		boolean away = false;
		if (prevValue != null) {
			float oldDistance = ((Float) prevValue).floatValue();
			float delta = oldDistance - distance;
			if (Math.abs(delta) < ERROR) {
				// the change is too small, neither approaching nor moving away
				// do nothing... (already false)
				if (_entity.getName().equals("") && b.getName().equals(""))
					System.out.println("...distance: " + distance);
			} else if (delta > 0) {
				// previousDistance is greater than new distance... we are closing in
				towards = true;
			} else if (delta < 0) {
				// previousDistance is smaller than new distance... moving away
				away = true;
			}
		}
		_entity.record(b, _towards, towards);
		_entity.record(b, _away, away);
		
		_previousMap.put(b.getId(), new Float(distance));
	}

}
