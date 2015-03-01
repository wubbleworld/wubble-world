package edu.isi.wubble.physics.entity;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.jme.intersection.PickData;
import com.jme.intersection.PickResults;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.Quaternion;
import com.jme.math.Ray;
import com.jme.math.Vector3f;
import com.jme.scene.Node;

import edu.isi.wubble.physics.Corrector;
import edu.isi.wubble.physics.JumpCorrector;
import edu.isi.wubble.physics.MoveNotifyI;
import edu.isi.wubble.physics.entity.controller.JumpController;
import edu.isi.wubble.physics.entity.controller.LiftingController;
import edu.isi.wubble.physics.entity.controller.MovementController;
import edu.isi.wubble.physics.entity.controller.RotationController;

public class ActiveEntity extends DynamicEntity implements MoveNotifyI {

	public static ArrayList<Vector3f> _directions;

	static {
		_directions = new ArrayList<Vector3f>();
		_directions.add(new Vector3f(-1,0,0));
		_directions.add(new Vector3f(1,0,0));
		_directions.add(new Vector3f(0,0,-1));
		_directions.add(new Vector3f(0,0,1));
	}
	
	protected Node _visualNode;

	protected LiftingController _lifting;
	protected Corrector         _corrector;
	protected JumpCorrector     _jumpCorrector;

	protected Timer _timeout;
	protected float _diameter;


	public ActiveEntity(EntityManager em, String name, boolean makeUnique) {
		super(em, name, makeUnique);
	}

	public Vector3f getPosition() {
		return _physicsNode.getLocalTranslation();
	}

	public void setPosition(Vector3f pos) {
		_physicsNode.setLocalTranslation(pos);
		_visualNode.setLocalTranslation(pos);
	}

	public Quaternion getRotation() {
		return _visualNode.getLocalRotation();
	}
	
	public Node getVisualNode() {
		return _visualNode;
	}
	
	public Node getAttachPoint() {
		return getVisualNode();
	}

	public void update(float tpf) {
		super.update(tpf);
		if (_visualNode != _physicsNode) {
			_visualNode.setLocalTranslation(_physicsNode.getLocalTranslation());
		}
	}

	/**
	 * add the proper nodes and controllers so that when
	 * we are told to pick something up we can do it.
	 * @param ps
	 */
	public void addCarryingAbility() {
		_lifting = new LiftingController(this);
		addController(_lifting);
	}
	
	public void addMoveAbility() {
		_corrector = new Corrector(getPhysicsNode(), _visualNode);
//		addController(_corrector);
	}
	
	public void addJumpAbility() {
		_jumpCorrector = new JumpCorrector(getPhysicsNode(), _visualNode);
//		addController(_jumpCorrector);
	}

	public boolean pickUp(String name) {
		DynamicEntity e = _entityManager.getDynamicEntity(name);
		if (_lifting == null || e == null) {
			System.out.println("Unable to pick-up: " + name + "[" + e + "] using " + _lifting);
			return false;
		}
		return _lifting.applyController(e);
	}

	public void putDown() {
		_lifting.release();
	}

	/** 
	 * move the wubble to the desired position.  This is accomplished
	 * with correctors and there is a default timer that will stop
	 * the movement if it doesn't finish in 2 seconds.
	 * @param pos
	 * @param sendResponse
	 */
	public void moveTo(Vector3f pos, final boolean sendResponse) {
	//		_movement.setActive(false);
			_corrector.setPosition(pos);
			_corrector.setActive(true);
			_corrector.registerInterest(this, sendResponse);
			
			TimerTask tt = new TimerTask() {
				public void run() {
					moveFinished(_corrector, sendResponse);
				}
			};
			_timeout = new Timer("moveFinished", false);
			_timeout.schedule(tt, 2000);
		}

	/**
	 * jump to the desired position.  For more details
	 * see the moveTo method.  Just uses a different
	 * corrector.
	 * @param pos
	 * @param sendResponse
	 */
	public void jumpTo(Vector3f pos, final boolean sendResponse) {
	//		_movement.setActive(false);
			_corrector.setActive(false);
			
			_jumpCorrector.setPosition(pos);
			_jumpCorrector.setActive(true);
			_jumpCorrector.registerInterest(this, sendResponse);
			
			TimerTask tt = new TimerTask() {
				public void run() {
					moveFinished(_jumpCorrector, sendResponse);
				}
			};
			_timeout = new Timer("jumpFinished", false);
			_timeout.schedule(tt, 2000);
		}

	/**
	 * the position that we can escape to will be returned
	 * based on escape paths using rays. (not normalized)
	 * @param size
	 * @return
	 */
	protected Vector3f findExitDirection(Vector3f size) {
		for (Vector3f dir : _directions) {
			float diameter = Math.abs(size.dot(dir)) + _diameter;
			
			Ray r = new Ray(getPosition(), dir);
			PickResults pickResults = new TrianglePickResults();
			pickResults.setCheckDistance( true );
			
			getNode().getParent().findPick(r, pickResults);
	
			if (pickResults.getNumber() == 0) {
				return dir.mult(diameter);
			} else {
				PickData data = pickResults.getPickData(0);
				if ( data.getTargetTris() != null && data.getTargetTris().size() > 0 ) {
					if (data.getDistance() > diameter) {
						return dir.mult(diameter);
					}
				} else {
					return dir.mult(diameter);
				}
			}
		}
		
		float diameter = Math.abs(size.dot(Vector3f.UNIT_X)) + _diameter;
		return Vector3f.UNIT_X.mult(diameter);
	}

	public void setInputEnabled(boolean enabled) {

	}

	/**
	 * finish the move and restore things to normal.
	 * @param corrector
	 * @param sendResponse
	 */
	public void moveFinished(Corrector corrector, boolean sendResponse) {
			corrector.removeInterest(this);
			corrector.setActive(false);
	
			_timeout.cancel();
		}
}