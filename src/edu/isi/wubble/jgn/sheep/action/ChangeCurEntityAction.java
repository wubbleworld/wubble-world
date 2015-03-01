package edu.isi.wubble.jgn.sheep.action;

import java.util.ArrayList;
import java.util.Hashtable;

import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.entity.SDynamicEntity;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.Wubble;

public class ChangeCurEntityAction extends InputAction {
	private boolean _fwd;
	public ChangeCurEntityAction(boolean fwd) {
		_fwd = fwd;
	}

	public void performAction(InputActionEvent evt) {
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		
		// Only do this when the button was pushed, not relesaed.
		if (!evt.getTriggerPressed()) { return; }
		
		// Show the entities in the system.
		//w.printAllEntities();
		
		// Remove the controller from the old entity.  The curEntity should ALWAYS have these controllers.
		SDynamicEntity oldEntity = w.getCurEntity();
		if (oldEntity != null) {
			// System.out.println("Yanking curEntity " + oldEntity.getName());

			// Remove the movement controls.
			oldEntity.removeMovementControllers();

			// If the entity is a wubble, add new movement controllers, network side.  
			// (Will this ever break stuff?)  
			if (oldEntity instanceof Wubble) { oldEntity.addMovementControls(false); }
			
			// Reactivate the entity's behaviors.
			oldEntity.setBehaviorActive(true);

			w.setCurEntity(null);
		}
		
		
		SEntity e = getNext(oldEntity);
		while (!(e instanceof SDynamicEntity)) {
			e = getNext(e);
			
			// If there are no entities, quit.
			if (e == null) { return; }
			
			// FIXME: If there are no dynamic entities, this will loop forever.  
			// However, if that happens, you've got bigger problems.
			//System.out.println("Prospective next ce: " + e.getName());
		}
		
		SDynamicEntity de = (SDynamicEntity)e;
		System.out.println("-- Changed curEntity to " + de.getName());
		w.setCurEntity(de);
		
		// Add local movement controls.
		de.addMovementControls(true);
		
		// Turn this entity's behaviors off, so it doesn't spin around and do stupid shit.
		de.setBehaviorActive(false);
	}
	
	
	protected SEntity getNext(SEntity ce) {
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		Hashtable<String, SEntity> h = w.getEntities();
		ArrayList<SEntity> entities = new ArrayList<SEntity>(h.values());
		
		int size = entities.size();
		
		// Return if there's no entities to get.
		if (size == 0) { System.out.println("No entities!"); return null; }
		
		// Find the entity in question.
		// System.out.println("There are " + size + " entities.");
		
		int index = entities.indexOf(ce);
		int nextIndex = -1;
		
		if (index == -1) { nextIndex = 0; }
		else if (_fwd == true) { nextIndex = index + 1; }
		else { nextIndex = index - 1; }
		
		// Check for wrap.
		if (nextIndex < 0) { nextIndex = size - 1; }
		else if (nextIndex >= size) { nextIndex = 0; }
		
		SEntity nextEntity = entities.get(nextIndex);
		
		return nextEntity;
	}
}
