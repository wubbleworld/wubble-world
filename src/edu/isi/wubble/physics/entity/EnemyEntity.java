package edu.isi.wubble.physics.entity;

import static com.jme.input.InputHandler.AXIS_NONE;

import com.jme.bounding.BoundingBox;
import com.jme.input.InputHandler;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.util.SyntheticButton;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Box;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.PhysicsUpdateCallback;
import com.jmex.physics.contact.ContactInfo;

import edu.isi.wubble.jgn.rpg.RPGPhysics;

public class EnemyEntity extends DynamicEntity {
	
	protected Node               _visualNode;
	
	protected int _state;
	protected boolean _onFloor;

	protected int _hitsLeft    = 4;
	protected int _timeSinceHit = 0;
	

	public EnemyEntity(EntityManager em, String name, boolean makeUnique) {
		super(em, name, makeUnique);

		_state = 0;
		_entityManager.addEnemyEntity(this);
		_entityManager.addUpdateEntity(this);
	}
	
	public boolean isResting() {
		return _physicsNode.isResting();
	}
	
	public Vector3f getPosition() {
		return _physicsNode.getLocalTranslation();
	}

	public Quaternion getRotation() {
		return _visualNode.getLocalRotation();
	}
	
	public void update(float tpf) {
		super.update(tpf);
		attack();
		getNode().updateGeometricState(0, true);
		_visualNode.setLocalTranslation(_physicsNode.getLocalTranslation());
		++_timeSinceHit;
	}
	
	private void attack() {
		if (_state == 0) {
			Vector3f ourPosition = _physicsNode.getLocalTranslation();
			for (ActiveEntity we : _entityManager.getWubbles()) {
				Vector3f pos = we.getPosition();
				if (pos.z <= ourPosition.z) {
					_state = 1;
					break;
				}
			}
		} else if (_state == 1) {
			moveTowardsWubble();
		}		
	}

	private void moveTowardsWubble() {
		if (!_onFloor) {
			return;
		}
		Vector3f ourPos = _physicsNode.getLocalTranslation();
		
		Vector3f dir = null;
		float distance = 0;
		for (ActiveEntity we : _entityManager.getWubbles()) {
			Vector3f pos = we.getPosition();
			Vector3f testDir = pos.subtract(ourPos);
			float d = testDir.lengthSquared();
			if (dir == null || d < distance) {
				dir = testDir;
				distance = d;
			}
		}

		dir.normalizeLocal();
		
		_visualNode.lookAt(ourPos.add(dir), new Vector3f(0,1,0));
		getPhysicsNode().addForce(new Vector3f(0,4,0));
		getPhysicsNode().addForce(dir.mult(4));
		
		if (getPhysicsNode().isResting()) {
			getPhysicsNode().unrest();
		}
	}
	
	/**
	 * turn on a monitor that checks when the object hits the floor.
	 * Really should be called touching something.
	 *
	 */
	public void monitorFloor(PhysicsSpace ps, InputHandler input) {
		SyntheticButton eventHandler = _physicsNode.getCollisionEventHandler();
		input.addAction( new InputAction() {
			public void performAction( InputActionEvent evt ) {
				_onFloor = true;
			}
		}, eventHandler.getDeviceName(), eventHandler.getIndex(),
		AXIS_NONE, false ); 
		
		// and a very simple callback to set the variable to false before each step
		ps.addToUpdateCallbacks( new PhysicsUpdateCallback() {
			public void beforeStep( PhysicsSpace space, float time ) {
				_onFloor = false;
			}

			public void afterStep( PhysicsSpace space, float time ) {

			}
		} ); 
	}    
	
	/**
	 * add the ability to die to our enemeis
	 * @param input
	 */
	public void addDeath(InputHandler input, final RPGPhysics callback) {
		final PhysicsEntity arrowEntity = _entityManager.getDynamicEntity("arrow");
		if (arrowEntity == null) {
			System.out.println("unable to add death because we don't " +
					"have an instrument of death");
			return;
		}
		final DynamicPhysicsNode arrowNode = (DynamicPhysicsNode) arrowEntity.getNode();
		
		SyntheticButton eventHandler = _physicsNode.getCollisionEventHandler();
		input.addAction( new InputAction() {
			public void performAction( InputActionEvent evt ) {
				final ContactInfo contactInfo = ( (ContactInfo) evt.getTriggerData() );

				try {
					DynamicPhysicsNode node1 = (DynamicPhysicsNode) contactInfo.getNode1();
					DynamicPhysicsNode node2 = (DynamicPhysicsNode) contactInfo.getNode2();

					if ((node1 == arrowNode || node2 == arrowNode) &&
							_timeSinceHit > 3) {
						System.out.println("we have a hit....");
						--_hitsLeft;
						_timeSinceHit = 0;
						if (_hitsLeft <= 0) {
							_entityManager.removeEnemyEntity(getName());
							callback.sendRemoveMessage(getName());
						}
					}
				} catch (Exception e) {
				}
				_onFloor = true;
			}
		}, eventHandler.getDeviceName(), eventHandler.getIndex(),
		AXIS_NONE, false ); 
	}	
	

	public static EnemyEntity createEnemy(EntityManager em, Spatial s, RPGPhysics callback) {
		DynamicPhysicsNode dpn = em.getPhysicsSpace().createDynamicNode();
		dpn.setName(s.getName());
		dpn.createBox("proxy-" + s.getName());
		dpn.setLocalTranslation(s.getLocalTranslation());
		dpn.setLocalScale(new Vector3f(0.35f, 0.35f,0.35f));
		dpn.generatePhysicsGeometry();
		dpn.computeMass();

		Box b = new Box(s.getName(), new Vector3f(0,0,0), 0.5f, 0.5f, 0.5f);
		b.setModelBound(new BoundingBox());

		Node visual = new Node("visual-node" + s.getName());
		visual.attachChild(b);
		visual.setLocalRotation(s.getLocalRotation());
		visual.setLocalScale(new Vector3f(0.35f, 0.35f, 0.35f));
		visual.lookAt(new Vector3f(0,0,-1), new Vector3f(0,1,0));
		
		Node parent = new Node(s.getName());
		parent.attachChild(dpn);
		parent.attachChild(visual);
		
		EnemyEntity we = new EnemyEntity(em, s.getName(), true);
		we._physicsNode = dpn;
		we._visualNode = visual;
		we.setNode(parent);
		we.monitorFloor(em.getPhysicsSpace(), em.getInputHandler());
		we.addDeath(em.getInputHandler(), callback);

		return we;
	}
}
