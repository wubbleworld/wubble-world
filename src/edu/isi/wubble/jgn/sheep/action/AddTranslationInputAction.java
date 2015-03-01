package edu.isi.wubble.jgn.sheep.action;

import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.math.Vector3f;
import com.jme.scene.Node;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.entity.SDynamicEntity;
import edu.isi.wubble.jgn.sheep.entity.SEntity;

public class AddTranslationInputAction extends InputAction {
	
	public void performAction(InputActionEvent evt) {
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		SEntity ce = w.getCurEntity();
		
		// Right now we can only apply this to DynEntities.
		if (!(ce instanceof SDynamicEntity)) { return; }
		SDynamicEntity de = (SDynamicEntity)ce;
		
//		if (evt.getTriggerPressed() == true) {w.p("xlation button DOWN");}
//		else {w.p("xlation button UP");}
		
//		w.p("trigger event: " + evt.getTriggerName());
		

//		if (w != null) { return; }
		
		if (ce != null) {

			Node p = ce.getNode();

			Vector3f l1 = p.getLocalTranslation();
			Vector3f l2 = Vector3f.UNIT_Y; // de.getIndicator().getWorldTranslation();
			Vector3f impulse = l2.subtract(l1);
			impulse.setY(0);			
			impulse.normalizeLocal();
			
			Vector3f lt = p.getLocalTranslation();
			p.getLocalTranslation().set(lt.add(impulse));
		}
	}
}
