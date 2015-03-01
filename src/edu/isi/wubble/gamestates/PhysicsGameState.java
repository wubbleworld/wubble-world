package edu.isi.wubble.gamestates;

import com.jme.system.DisplaySystem;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.PhysicsDebugger;

import edu.isi.wubble.physics.TimeManager;

public abstract class PhysicsGameState extends VisualGameState {

	protected PhysicsSpace _physics;
	public PhysicsSpace getPhysics() { return _physics; }
	
	protected boolean _drawPhysics = true;
	
	public PhysicsGameState() {
		super();
		_physics = PhysicsSpace.create();
		setDelayInputUpdate(true);
	}
	
	/**
	 * override the underlying method in order to update physics
	 * as well as the rest.
	 * @param tpf
	 * 		elapsed milliseconds.
	 */
	public void update(float tpf) {
		super.update(tpf);
		
		TimeManager.inst().update();

        if ( tpf > 0.2 || Float.isNaN( tpf ) ) {
            tpf = 0.2f;
        }
		_physics.update(tpf);
		_input.update(tpf);
	}
	
	public void render(float tpf) {
		super.render(tpf);
		
		if (_drawPhysics) {
			PhysicsDebugger.drawPhysics(_physics, DisplaySystem.getDisplaySystem().getRenderer());
		}
	}
}
