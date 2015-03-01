package edu.isi.wubble.jgn.rpg;

import static com.jme.input.InputHandler.AXIS_NONE;

import com.jme.bounding.BoundingBox;
import com.jme.input.InputHandler;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.util.SyntheticButton;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Box;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.contact.ContactInfo;

import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.physics.entity.ActiveEntity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.physics.entity.PhysicsEntity;
import edu.isi.wubble.physics.entity.WubbleEntity;

public class RPGWubbleEntity extends WubbleEntity {

	protected int                _wubbleHealth      = 20;
	protected int                _timeSinceLastHit  = 0;
	
	protected RPGPhysics        _parent;

	public RPGWubbleEntity(RPGPhysics parent, EntityManager em, String name) {
		super(em, name, false);		
		
		initWubbleEntity(new Vector3f(0,0,0));
		_parent = parent;
	}
	
	public void update(float tpf) {
		super.update(tpf);
		++_timeSinceLastHit;
	}
	
	public void sendHealthUpdate() {
		int role = RPGPhysics.SHOOTER;
		if (getName().equals("picker"))
			role = RPGPhysics.PICKER;
		
		InvokeMessage msg = InvokeMessage.createMsg("updateHealth", new Object[] { role, _wubbleHealth });
		msg.sendToGroup(_parent.getClientIds());
	}
	
	protected void addHealthCallbacks(InputHandler input, RPGPhysics callback) {
		SyntheticButton eventHandler = _physicsNode.getCollisionEventHandler();
		input.addAction( new InputAction() {
			public void performAction( InputActionEvent evt ) {
				final ContactInfo contactInfo = ( (ContactInfo) evt.getTriggerData() );

				if (!(contactInfo.getNode1() instanceof DynamicPhysicsNode) ||
					!(contactInfo.getNode2() instanceof DynamicPhysicsNode))
					return;
				
				DynamicPhysicsNode node1 = (DynamicPhysicsNode) contactInfo.getNode1();
				DynamicPhysicsNode node2 = (DynamicPhysicsNode) contactInfo.getNode2();

				boolean is1 = _entityManager.hasEnemy(node1.getName());
				boolean is2 = _entityManager.hasEnemy(node2.getName());
				if ((is1 || is2) && (_timeSinceLastHit > 35)) {
					System.out.println("ouch wubble hit, wubble hit...." + node1.getName() + " " + node2.getName());
					--_wubbleHealth;
					_timeSinceLastHit = 0;
					if (_wubbleHealth <= 0) {
						InvokeMessage msg = InvokeMessage.createMsg("playLosingState", null);
						msg.sendToGroup(_parent.getClientIds());
						_parent.reset();
					}
					
					sendHealthUpdate();
				}
			}
		}, eventHandler.getDeviceName(), eventHandler.getIndex(), AXIS_NONE, false ); 
		
	}
	
	/**
	 * adds the proper nodes so that when fire is called
	 * everything is ready to fire.
	 */
	public void addShootingAbility(PhysicsSpace ps) {
		// attach a firing node to the visual in order to get the proper
		// inherited rotation and translation values when we try
		// to fire our weapon (typically done with mount points, but this is our
		// hack at that)		
		Quaternion quat = new Quaternion();
		quat.fromAngles(FastMath.DEG_TO_RAD*-5,0,0);

		Node firingNode = new Node("arrowFire");
		firingNode.setLocalTranslation(new Vector3f(0,0.12f,2.3f));
		firingNode.setLocalRotation(quat);
		firingNode.setCullMode(Node.CULL_NEVER);
		
		Box b = new Box("arrowFire", new Vector3f(0,0,0), 0.5f, 0.5f, 0.5f);
		b.setModelBound(new BoundingBox());
		firingNode.attachChild(b);
		firingNode.setLocalScale(new Vector3f(0.25f,0.25f,0.25f));

		_visualNode.attachChild(firingNode);
		_visualNode.updateWorldVectors();
	}
	
	public void fireArrow() {
		PhysicsEntity arrow = _entityManager.getDynamicEntity("arrow");
		DynamicPhysicsNode node = (DynamicPhysicsNode) arrow.getNode();
		
		Spatial s = _visualNode.getChild("arrowFire");
		node.getLocalTranslation().set(new Vector3f(s.getWorldTranslation()));
		node.getLocalRotation().set(new Quaternion(s.getWorldRotation()));
		node.updateWorldVectors();
		
		Vector3f dir = s.getWorldRotation().getRotationColumn(2);
		dir.normalizeLocal();
		dir.multLocal(20.0f);
		node.setLinearVelocity(dir);
		if (node.isResting()) {
			node.unrest();
		}
		
		System.out.println("...arrow: " + node.getLocalTranslation());
	}
	
	public static void sendWubblesUpdate(EntityManager em) {
		for (ActiveEntity we : em.getWubbles()) {
			((RPGWubbleEntity) we).sendHealthUpdate();
		}
	}
}
