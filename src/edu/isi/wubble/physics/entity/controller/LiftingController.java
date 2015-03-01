package edu.isi.wubble.physics.entity.controller;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.util.GameTaskQueueManager;
import com.jmex.physics.DynamicPhysicsNode;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.PhysicsEntity;

public class LiftingController extends EntityController {
	private static final long serialVersionUID = 1L;

	protected FSMState           _state;  

	protected Vector3f           _maintainOffset;

	protected DynamicEntity      _entity;
	protected DynamicPhysicsNode _carrier;
	
	protected DynamicEntity      _carryingEntity;
	protected VelocityController _carryingController;
	
	protected Timer _forceDrop;

	public LiftingController(DynamicEntity entity) {
		super();
		_entity = entity;
		_carrier = _entity.getPhysicsNode();
		
		setActive(false);
	}
	
	public void cleanup() {
		if (_forceDrop != null)
			_forceDrop.cancel();
	}
	
	public boolean applyController(DynamicEntity entity) {
		if (_state != null) {
			System.out.println("[applyController] state not ready for pickup " + _state);
			return false;
		}
		
		_carrier = _entity.getPhysicsNode();
		
		Controller c = entity.getController(LookAtController.class.getName());
		((LookAtController) c).applyController(entity);
		
		_carryingEntity = entity;
		_carryingController = (VelocityController) _carryingEntity.getController(VelocityController.class.getName());
		_state = new LiftState();
		
		//TODO if i am near something... then (and only then) I can pick it up
		
		Vector3f pos = _carryingEntity.getPosition().add(new Vector3f(0,5,0));
		_carryingController.applyController(pos);
		_maintainOffset = new Vector3f(0, 1.2f + _carryingEntity.getPhysicsNode().getLocalScale().y, 0);
		
		// schedule a timer for moving on to state 2 in case
		// we can't reach the top point.
		final Callable<?> callable = new Callable<Object>() {
			public Object call() {
				_state = new MaintainState();
				return null;
			}
		};
		
		_forceDrop = new Timer("forceDrop", false);
		_forceDrop.schedule(new TimerTask() {
			public void run() {
				GameTaskQueueManager.getManager().update(callable);
			}
		}, 5000);
		
		setActive(true);
		return true;
	}
	
	/**
	 * release whatever object we were carrying...
	 */
	public void release() {
		if (_state != null && _state.isCarrying()) {
			_forceDrop.cancel();
			_state = new DropState();
			
			final Callable<?> callable = new Callable<Object>() {
				public Object call() {
					drop();
					return null;
				}
			};
			
			_forceDrop = new Timer("forceDrop", false);
			_forceDrop.schedule(new TimerTask() {
				public void run() {
					GameTaskQueueManager.getManager().update(callable);
				}
			}, 1000);
		} else {
			System.out.println("[Lifting] tried to release in unknown state: " + _state);
		}
	}
	
	public void update(float tpf) {
		_state.update(tpf);
	}
	
	protected void drop() {
		System.out.println("dropping: " + _carryingEntity);
		if (_carryingEntity == null) 
			return;

		_entity.record(_carryingEntity, "Carrying", false);
		_forceDrop.cancel();
		_carryingController.stopController();
		_carryingEntity = null;
		_state = null;
		setActive(false);
	}
	
	public PhysicsEntity getCarriedEntity() {
		return _carryingEntity;
	}
	
	abstract class FSMState {
		public abstract boolean isCarrying();
		public abstract void update(float tpf);
	}

	class LiftState extends FSMState {
		public LiftState() {
			_entity.record(_carryingEntity, "Lifting", true);
		}
		
		public boolean isCarrying() {
			return true;
		}
		
		public void update(float tpf) {
			if (_carryingController.isClose()) {
				_maintainOffset.setY(_maintainOffset.y - 0.8f);
				_state = new MaintainState();
			} 
			Vector3f pos = _carrier.getLocalTranslation().add(_maintainOffset);
			_carryingController.applyController(pos);
		}
	}


	class MaintainState extends FSMState {
		public MaintainState() {
			_entity.record(_carryingEntity, "Lifting", false);
			_entity.record(_carryingEntity, "Carrying", true);
		}

		public boolean isCarrying() {
			return true;
		}
		
		public void update(float tpf) {
			Vector3f pos = _carrier.getLocalTranslation().add(_maintainOffset);
			_carryingController.applyController(pos);
		}
	}

	class DropState extends FSMState {
		public boolean isCarrying() {
			return false;
		}
		
		public void update(float tpf) {
			if (_carryingController.isClose()) {
				drop();
			}
		}
	}
}

