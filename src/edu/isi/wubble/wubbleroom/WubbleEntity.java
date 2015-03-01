package edu.isi.wubble.wubbleroom;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.jme.intersection.PickData;
import com.jme.intersection.PickResults;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.Quaternion;
import com.jme.math.Ray;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.CullState;
import com.jme.system.DisplaySystem;
import com.jmex.physics.PhysicsSpace;

import edu.isi.wubble.JMEString;
import edu.isi.wubble.character.WubbleCharacter;
import edu.isi.wubble.physics.Corrector;
import edu.isi.wubble.physics.JumpCorrector;
import edu.isi.wubble.physics.MoveNotifyI;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.physics.entity.PhysicsEntity;
import edu.isi.wubble.physics.entity.controller.JumpController;
import edu.isi.wubble.physics.entity.controller.LiftingController;
import edu.isi.wubble.physics.entity.controller.MovementController;
import edu.isi.wubble.physics.entity.controller.RotationController;

public class WubbleEntity extends DynamicEntity implements MoveNotifyI {

	public static ArrayList<Vector3f> _directions;
	
	static {
		_directions = new ArrayList<Vector3f>();
		_directions.add(new Vector3f(-1,0,0));
		_directions.add(new Vector3f(1,0,0));
		_directions.add(new Vector3f(0,0,-1));
		_directions.add(new Vector3f(0,0,1));
	}
	
	protected WubbleCharacter    _visualCharacter;	
	protected Node               _visualNode;

	protected MovementController _movement;
	protected RotationController _rotation;
	protected JumpController     _jump;
	
	protected LiftingController _lifting;
	protected Corrector         _corrector;
	protected JumpCorrector     _jumpCorrector;
	
	protected Timer             _timeout;
	
	protected float             _diameter;
	
	public WubbleEntity(EntityManager em, String name) {
		super(em, name, false);
        CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
        cs.setCullMode(CullState.CS_BACK);
		
		_visualCharacter = new WubbleCharacter(name);
        _visualCharacter.setRenderState(cs);
        _visualCharacter.updateRenderState();
        _visualCharacter.setCullMode(Spatial.CULL_NEVER);
        _visualCharacter.setColor(ColorRGBA.yellow);
		_visualNode = _visualCharacter.getVisualNode();

		initProxy(em.getPhysicsSpace());
		
		_lifting = new LiftingController(this);
		_corrector = new Corrector(getPhysicsNode(), _visualNode);
		_jumpCorrector = new JumpCorrector(getPhysicsNode(), _visualNode);

		Node n = new Node(name);
		n.attachChild(_visualCharacter);
		n.attachChild(_physicsNode);

		n.addController(_lifting);
		n.addController(_corrector);
		n.addController(_jumpCorrector);
		
		n.setUserData("color", new JMEString("yellow"));
		n.setUserData("geometry-type", new JMEString("sphere"));
		n.setUserData("size", new JMEString("(0.25 0.25 0.25)"));

		setNode(n);
		initControls();
	}	
	
	protected void initProxy(PhysicsSpace ps) {
		_physicsNode = ps.createDynamicNode();
		_physicsNode.setName(getName());
		_physicsNode.createSphere(getName());
		_physicsNode.setLocalScale(new Vector3f(0.25f,0.25f,0.25f));
		_physicsNode.generatePhysicsGeometry();
		getPhysicsNode().computeMass();
		
		Vector3f scale = new Vector3f(_physicsNode.getLocalScale());
		_diameter = scale.x * 2.0f;
	}
	
	protected void initControls() {
//		GameControlManager manager = new GameControlManager();
//		GameControl forward = manager.addControl("forward");
//		//forward.addBinding(new KeyboardBinding(KeyInput.KEY_W));
//
//		GameControl backward = manager.addControl("backward");
//		//backward.addBinding(new KeyboardBinding(KeyInput.KEY_S));
//		
//		//_movement = new MovementController(_visualNode, _physicsNode, forward, backward);
		
		_movement = new MovementController(this);
		addController(_movement);
		
//		_movement.setActive(true);
//		getNode().addController(_movement);
		
//		GameControl left = manager.addControl("left");
//		//left.addBinding(new KeyboardBinding(KeyInput.KEY_A));
//		
//		GameControl right = manager.addControl("right");
//		//right.addBinding(new KeyboardBinding(KeyInput.KEY_D));

		//_rotation = new RotationController(_visualNode, left, right);
		
		_rotation = new RotationController(this);
		addController(_rotation);
//		_rotation.setActive(true);
//		getNode().addController(_rotation);
	}
	
	/**
	 * update the visual node to be the same position as the
	 * proxy node.
	 */
	public void update(float tpf) {
		_visualCharacter.update(tpf);
		_visualNode.getLocalTranslation().set(_physicsNode.getLocalTranslation());
		super.update(tpf);
	}
	
	public void setPosition(Vector3f pos) {
		_physicsNode.setLocalTranslation(pos);
		_visualNode.setLocalTranslation(pos);
	}
	
	public Vector3f getPosition() {
		return _physicsNode.getLocalTranslation();
	}
	
	public Quaternion getRotation() {
		return _physicsNode.getLocalRotation();
	}
	
	public Vector3f getSize() {
		return _physicsNode.getLocalScale();
	}
	
	/** 
	 * move the wubble to the desired position.  This is accomplished
	 * with correctors and there is a default timer that will stop
	 * the movement if it doesn't finish in 2 seconds.
	 * @param pos
	 * @param sendResponse
	 */
	public void moveTo(Vector3f pos, final boolean sendResponse) {
		_movement.setActive(false);
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
		_movement.setActive(false);
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
	
	public void pickUp(String name) {
		DynamicEntity de = _entityManager.getDynamicEntity(name);
		if (de == null) {
			System.out.println("Tried to pick up unknown: " + name);
			return;
		}
		_lifting.applyController(de);
		_visualCharacter.playAnimation("pickUp");
		
		SocketClient.inst().sendMessage("response t");
	}
	
	public void putDown() {
		_lifting.release();
		_visualCharacter.playAnimation("putDown");
		
		SocketClient.inst().sendMessage("response t");
		
		// need to determine an escape route... using ray casting
		PhysicsEntity de = _lifting.getCarriedEntity();
		Vector3f size = de.getSize();
		Vector3f exitDir = findExitDirection(size);
		
		moveTo(getPosition().add(exitDir), false);
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
		_movement.setActive(enabled);
		_rotation.setActive(enabled);
	}
	
	/**
	 * finish the move and restore things to normal.
	 * @param corrector
	 * @param sendResponse
	 */
	public void moveFinished(Corrector corrector, boolean sendResponse) {
		corrector.removeInterest(this);
		corrector.setActive(false);

		_movement.setActive(true);
		_timeout.cancel();
		
		if (sendResponse) 
			SocketClient.inst().sendMessage("response move T");
	}
	
	
}
