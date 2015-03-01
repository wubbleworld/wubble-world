package edu.isi.wubble.physics.entity.controller;

import static com.jme.math.FastMath.RAD_TO_DEG;
import static com.jme.math.FastMath.atan2;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import com.jme.bounding.BoundingSphere;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.MaterialState;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.AutoBinding;
import edu.isi.wubble.physics.entity.PhysicsEntity;

public class MoveToController extends EntityController {

	private static final long serialVersionUID = 1L;

	protected FSMState _state;
	
	protected PhysicsEntity _entity;
	protected Sphere        _complex;
	
	protected Vector3f _desiredPos;
	protected Timer    _giveUpTimer;
	
	protected ArrayList<Vector3f> _waypoints;
	protected int                 _currWaypoint;
	
	public MoveToController(PhysicsEntity de) {
		super();
		
		_entity = de;
		
		AlphaState as = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
		as.setBlendEnabled(true);
		as.setTestEnabled(true);
		as.setTestFunction(AlphaState.TF_GEQUAL);

		MaterialState ms = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
		ms.setDiffuse(new ColorRGBA(1.0f,0f,0f,1.0f));
		ms.setAmbient(ColorRGBA.black);
		ms.setEmissive(ColorRGBA.black);
		
		CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
		cs.setCullMode(CullState.CS_BACK);

		_complex = new Sphere(_entity.getName() + "_moveTo", 16, 16, 0.125f);
		_complex.setModelBound(new BoundingSphere());
		_complex.updateModelBound();
		_complex.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		_complex.setRenderState(as);
		_complex.setRenderState(ms);
		_complex.setRenderState(cs);

		_entity.getManager().getRootNode().attachChild(_complex);
		
		setActive(false);
	}
	
	public void cleanup() {
		if (_giveUpTimer != null)
			_giveUpTimer.cancel();
	}
	
	public void applyController(ArrayList<Vector3f> waypoints) {
		if (waypoints.isEmpty())
			return;
		
		_waypoints = waypoints;
		_currWaypoint = 0;
		
		applyController(_waypoints.get(_currWaypoint));
	}
	
	/**
	 * update the counter and if successful set the next location
	 * to apply the controller to.
	 */
	private void willContinue() {
		if (_waypoints != null && _waypoints.size() > 0) {
			++_currWaypoint;
			if (_currWaypoint >= _waypoints.size())
				return;
			applyController(_waypoints.get(_currWaypoint));
		}
	}
	
	/**
	 * turn this controller on and begin moving towards 
	 * the desired location.
	 * @param position
	 */
	public void applyController(Vector3f position) {
		if (_state != null || isActive()) {
			System.err.println("Still moving to " + _desiredPos + "!");
			return;
		}
		
		System.out.println("moving to: " + position);
		
		_complex.setLocalTranslation(position);
		_desiredPos = position;
		_state = new TurnState();

		// schedule a timer for moving on to state 2 in case
		// we can't reach the top point.
		final MoveToController mtc = this;
		final Callable<?> callable = new Callable<Object>() {
			public Object call() {
				System.out.println("we are giving up: " + _desiredPos);
				if (mtc.isActive()) {
					if (_state != null)
						_state.stopEarly();
					_state = null;
					setActive(false);
				}
				
				return null;
			}
		};
		
		_giveUpTimer = new Timer(_entity.getName() + "-giveUp", false);
		_giveUpTimer.schedule(new TimerTask() {
			public void run() {
				GameTaskQueueManager.getManager().update(callable);
			}
		}, 5000);
		
		setActive(true);
	}
	
	@Override
	public void update(float tpf) {
		_state.update(tpf);
	}
	
	public void setActive(boolean active) {
		super.setActive(active);
		
		if (!active) 
			_complex.setCullMode(Spatial.CULL_ALWAYS);
		else
			_complex.setCullMode(Spatial.CULL_NEVER);
	}
	
	abstract class FSMState {
		public abstract void update(float tpf);
		public abstract void stopEarly();
	}

	class TurnState extends FSMState {
		private float _prevDelta;
		private float _desiredAngle;
		private String _action;
		
		public TurnState() {
			Vector3f ourDir = _entity.getRotation().getRotationColumn(2).normalize();
			Vector3f direction = _desiredPos.subtract(_entity.getPosition()).normalize();

			_desiredAngle = RAD_TO_DEG * atan2(direction.z, direction.x);
			_desiredAngle = _desiredAngle < 0 ? (_desiredAngle + 360.0f) : _desiredAngle;
			
			float xzOur = RAD_TO_DEG * atan2(ourDir.z, ourDir.x);
			xzOur = xzOur < 0 ? (xzOur + 360.0f) : xzOur;
			
			float rightTurn = Math.abs(((xzOur - _desiredAngle)+360) % 360);
			float leftTurn = Math.abs(((_desiredAngle - xzOur)+360) % 360);
			
			if (leftTurn > rightTurn) 
				_action = "left";
			else 
				_action = "right";
			AutoBinding.bindingMsg(_entity.getName(), _action, true);
			
			_prevDelta = Float.MAX_VALUE;
		}
		
		public void update(float tpf) {
			Vector3f ourDir = _entity.getRotation().getRotationColumn(2).normalize();
			float xzOur = RAD_TO_DEG * atan2(ourDir.z, ourDir.x);
			xzOur = xzOur < 0 ? (xzOur + 360.0f) : xzOur;

			float delta = Math.abs(_desiredAngle - xzOur);
			
			// if this turn our delta actually started growing then 
			// we should stop turning.
			if (delta < 8.5) {
				_prevDelta = delta;
				stop();
				return;
			}

			_prevDelta = delta;
		}
		
		public void stop() {
			AutoBinding.bindingMsg(_entity.getName(), _action, false);
			_state = new ForwardState();
			willContinue();
		}
		
		public void stopEarly() {
			_giveUpTimer.cancel();
			AutoBinding.bindingMsg(_entity.getName(), _action, false);
			willContinue();
		}
	}


	class ForwardState extends FSMState {
		
		private Vector3f _flatPos;
		private Vector3f _flatDesiredPos;
		
		public ForwardState() {
			_flatPos= new Vector3f();
			_flatDesiredPos = new Vector3f(_desiredPos);
			_flatDesiredPos.y = 0;
			
			AutoBinding.bindingMsg(_entity.getName(), "forward", true);
		}
		
		public void update(float tpf) {
			Vector3f ourDir = _entity.getRotation().getRotationColumn(2).normalize();
			Vector3f direction = _desiredPos.subtract(_entity.getPosition()).normalize();

			float directAngle = RAD_TO_DEG * atan2(direction.z, direction.x);
			directAngle = directAngle < 0 ? (directAngle + 360.0f) : directAngle;
			
			float xzOur = RAD_TO_DEG * atan2(ourDir.z, ourDir.x);
			xzOur = xzOur < 0 ? (xzOur + 360.0f) : xzOur;
			
			float delta = Math.abs(directAngle-xzOur);
			if (delta > 90) {
				AutoBinding.bindingMsg(_entity.getName(), "forward", false);
				_state = new TurnState();
				return;
			}

			_flatPos.set(_entity.getPosition());
			_flatPos.y = 0;
			
			float distance = _flatPos.distance(_flatDesiredPos);
			if (distance < 0.25) {
				System.out.println("we have arrived " + _desiredPos);
				AutoBinding.bindingMsg(_entity.getName(), "forward", false);
				_desiredPos = null;
				_state = null;
				_giveUpTimer.cancel();
				
				setActive(false);
				willContinue();
			}
			
		}
		
		public void stopEarly() {
			_giveUpTimer.cancel();
			AutoBinding.bindingMsg(_entity.getName(), "forward", false);
			willContinue();
		}
		
	}
}	
	