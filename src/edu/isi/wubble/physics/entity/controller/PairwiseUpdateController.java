package edu.isi.wubble.physics.entity.controller;

import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.physics.entity.callback.CollisionCallable;
import edu.isi.wubble.physics.entity.callback.InFOVCallable;
import edu.isi.wubble.physics.entity.callback.PairwiseCallable;
import edu.isi.wubble.physics.entity.callback.PrepositionCallable;
import edu.isi.wubble.physics.entity.callback.SWIFTCallable;
import edu.isi.wubble.physics.entity.callback.SWIFTOnCallable;
import edu.isi.wubble.util.Globals;

public class PairwiseUpdateController extends EntityController {
	private static final long serialVersionUID = 1L;

	protected ArrayList<PairwiseCallable> _callbacks;
	HashMap<String, PairwiseCallable> _callbackMap;

	protected DynamicEntity _entity;
	protected EntityManager _entityManager;
	
	public PairwiseUpdateController(DynamicEntity e) {
		_entity = e;
		_entityManager = e.getManager();
		
		_callbacks = new ArrayList<PairwiseCallable>();
		_callbackMap = new HashMap<String, PairwiseCallable>();
		
		addDefaults();
	}
	
	
	/**
	 * this may not be ideal for Sheep.  We need to discuss what you 
	 * would like to see here and then we can decide how things will
	 * be added to this controller.
	 */
	public void addDefaults() {
		addCallable(new CollisionCallable(_entity));
//		addCallable(new OnCallable(_entity));
		
		if (!Globals.IN_SHEEP_GAME) {
			addCallable(new PrepositionCallable(_entity));
			addCallable(new SWIFTOnCallable(_entity));
			addCallable(new SWIFTCallable(_entity));
			addCallable(new InFOVCallable(_entity));
		}
		
			// For the release, sheep game is never using swift, so this is all moot
//		} else { 
			// If the sheep game is not using swift, don't add these at all
//			if (SheepPhysicsState.SWIFT != SwiftMode.OFF) {
//				addCallable(new PrepositionCallable(_entity));
//				addCallable(new SWIFTOnCallable(_entity));
//				addCallable(new SWIFTCallable(_entity));
			// TODO: test this to see what it is
			// If it's a wubble, add the push controller.
			//			if (_entity instanceof Wubble) {
			//				System.out.println("Added pushController to " + _entity.getName());
			//				addCallable(new PushCallable(_entity));
			//			}
//			}
//		}
	}
	
	
	public void addCallable(PairwiseCallable p) {
		_callbacks.add(p);
		String pcName = p.getClass().getSimpleName();
		// System.out.println("Adding to " + _entity.getName() + " pairwise callable " + pcName);
		_callbackMap.put(pcName, p);
	}
	
	
	public PairwiseCallable getCallable(String name) { return _callbackMap.get(name); }

	
	
	@Override
	public void update(float time) {
		if (!canUpdate())
			return;
		
		// this is this way because static entities may
		// have a lower value than the current dynamic entity
		// A better way may be to actually maintain separate
		// sets and iterate over only the dynamic entities with
		// higher id's and iterate over all static entities.
		
		// that will probably come next week.
		for (Entity e : _entityManager.getAllEntities()) {
			if (_entity.equals(e)) 
				continue;
			
			for (PairwiseCallable p : _callbacks) 
				p.call(e);
		}
		
		// If I have the lookAtEach controller, execute it.
		//if (_lookAtEach!= null) { _lookAtEach.go(0.0f); }
	}
}
