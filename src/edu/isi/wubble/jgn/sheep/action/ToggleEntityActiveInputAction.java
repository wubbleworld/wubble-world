package edu.isi.wubble.jgn.sheep.action;

import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.entity.SEntity;

public class ToggleEntityActiveInputAction extends InputAction {

	public ToggleEntityActiveInputAction() {}

	public void performAction(InputActionEvent evt) {
		// Only do this when the button was pushed, not relesaed.
		if (!evt.getTriggerPressed()) { return; }

		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		SEntity ce = w.getCurEntity();
		if (ce == null) { return; }
		
		Boolean isActive = ce.isBehaviorActive();

		// Toggle this entity's behavior.
		if (isActive == true) { isActive = false; } else { isActive = true; }
		ce.setBehaviorActive(isActive);
		System.out.println(ce.getName() + "'s behavior activity set to " + isActive);

		// Now stop movement, if it's the sort of entity that moves.
		if (ce != null) { ce.stopMoving(); }

	}
}
