package edu.isi.wubble.physics.entity.controller;

import static com.jme.input.InputHandler.AXIS_NONE;

import com.jme.input.KeyInput;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.controls.GameControl;
import com.jme.input.controls.binding.KeyboardBinding;
import com.jme.input.util.SyntheticButton;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.PhysicsUpdateCallback;
import com.jmex.physics.contact.ContactInfo;

import edu.isi.wubble.AutoBinding;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.PhysicsEntity;

public class JumpController extends EntityController {

	private static final long serialVersionUID = 1L;
	
	protected DynamicEntity      _entity;
	protected DynamicPhysicsNode _physicsNode;
	
	protected boolean       _onFloor;
	protected boolean       _restrict;

	protected InputAction _floorCollisionAction;
	protected PhysicsUpdateCallback _resetCallback;

	protected GameControl _positive;
	protected boolean     _autobinding;

	public JumpController(DynamicEntity entity) {
		this(entity, false);
	}
	
	public JumpController(DynamicEntity entity, boolean restricted) {
		_entity      = entity;
		_physicsNode = _entity.getPhysicsNode();
		
		_positive = _manager.addControl("jump");

		_restrict = restricted;
		setSpeed(4.0f);
		
		monitorFloor();
	}
	
	public void addAutoBinding(String pad) {
		_autobinding = true;
		_positive.addBinding(AutoBinding.createBinding(_entity.getName() + pad, "jump"));
	}
	
	public void addAutoBinding() {
		_autobinding = true;
		_positive.addBinding(AutoBinding.createBinding(_entity.getName(), "jump"));
	}
	
	public void addKeyBinding() {
		_autobinding = false;
		_positive.addBinding(new KeyboardBinding(KeyInput.KEY_SPACE));
	}

	/**
	 * turn on a monitor that checks when the object hits the floor.
	 * Really should be called "touching something."
	 *
	 */
	public void monitorFloor() {
		SyntheticButton eventHandler = _entity.getPhysicsNode().getCollisionEventHandler();
		
		_floorCollisionAction = new InputAction() {
			public void performAction( InputActionEvent evt ) {
				final ContactInfo contactInfo = ((ContactInfo) evt.getTriggerData());
				
				// we need to make sure that we are checking to see if we are
				// in fact colliding with another entity.
				if (_entity.getManager().getEntity(contactInfo.getNode2().getName()) == null) 
					return;
				
				if (_restrict) {
					Vector3f normal = contactInfo.getContactNormal(null);
					float angle = FastMath.RAD_TO_DEG * Vector3f.UNIT_Y.angleBetween(normal);
					if (angle <= 60 || angle >= 120) 
						_onFloor = true;
				} else {
					_onFloor = true;
				}
			}
		};
		_entity.getManager().getInputHandler().addAction(_floorCollisionAction, 
				eventHandler.getDeviceName(), eventHandler.getIndex(), AXIS_NONE, false ); 

		// and a very simple callback to set the variable to false before each step
		_resetCallback = new PhysicsUpdateCallback() {
			public void beforeStep( PhysicsSpace space, float time ) { _onFloor = false; }
			public void afterStep( PhysicsSpace space, float time ) {}
		}; 
		_entity.getManager().getPhysicsSpace().addToUpdateCallbacks(_resetCallback); 
	}

	
	// Unhook this controller from all the places where it's hooked.
	public void remove() {
		_entity.getManager().getInputHandler().removeAction(_floorCollisionAction);
		_entity.getManager().getPhysicsSpace().removeFromUpdateCallbacks(_resetCallback);
		// System.out.println("Removed JumpController from node " + _physicsNode.getName());
	}
    
	public void update(float tpf) {	
		if (!canUpdate())
			return;
		
		if (_positive.getValue() > 0.0f && _onFloor) {
			_entity.recordAuto("Jump", true);

			Vector3f vel = _physicsNode.getLinearVelocity(null);
			vel.y = getSpeed();
			_physicsNode.setLinearVelocity(vel);
			if (_physicsNode.isResting()) {
				_physicsNode.unrest();
			}
			// turn off the jump, since it only needs to last 1
			// time tick anyways
//			if (_autobinding)
//				AutoBinding.bindingMsg(_entity.getName(), "jump", false);
		}
	}
}
