package edu.isi.wubble.jgn.sheep.action;

import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.math.Vector3f;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsNode;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.entity.SEntity;

public class ToggleEntitiesActiveInputAction extends InputAction {
	
	protected boolean _isActive = false;
	
	public ToggleEntitiesActiveInputAction() {}
	
	public void performAction(InputActionEvent evt) {
		// Only do this when the button was pushed, not relesaed.
		if (!evt.getTriggerPressed()) { return; }
		
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		System.out.println("Toggling all entities to: " + _isActive);
		
		for (SEntity ce: w.getEntities().values()) {
			// Set this entity to the proper activity. 
			ce.setBehaviorActive(_isActive);
			ce.stopMoving();
		}
		
		// Toggle activity.
		if (_isActive == true) { _isActive = false; } else { _isActive = true; }
	}
}

