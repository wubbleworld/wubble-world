package edu.isi.wubble.physics.entity;

import java.util.HashSet;
import java.util.TreeMap;

import com.jme.input.InputHandler;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.util.SyntheticButton;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jmex.physics.PhysicsNode;
import com.jmex.physics.contact.ContactInfo;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.action.CollisionManager;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.physics.CollisionInfo;
import edu.isi.wubble.physics.entity.controller.EntityController;

public abstract class PhysicsEntity extends SEntity {

	protected PhysicsNode _physicsNode;
	protected TreeMap<Integer, CollisionInfo>    _collisionMap;
	protected TreeMap<String,EntityController> _controlMap;
	
	// Sheep
	public PhysicsEntity(String name, Vector3f pos) {
		// Construct Wes's stuff
		super(Utils.GetEM(), name, true);

		// Construct my stuff.
		doConstructorStuff(name, pos);
		
		// Physics entities have an additional collision-setup step.
		// Static entities will be inheriting this... just a heads up
		setupCollisions(true);
	}

	// Wes
	public PhysicsEntity(EntityManager em, String name, boolean makeUnique) {
		super(em, name, makeUnique);
	}

	// Auxiliary stuff for making a DynamicEntity, Wes-style.
	void postConstruct() {
		_collisionMap = new TreeMap<Integer, CollisionInfo>();
		_controlMap = new TreeMap<String,EntityController>();
	}
	
	public PhysicsNode getPhysicsNode() {
		return _physicsNode;
	}

	public void setPhysicsNode(PhysicsNode node) {
		_physicsNode = node;
	}

	/**
	 * This is used by SDynamicEntity and the extra parameter avoids
	 * actually using the pipeline defined by DynamicEntity.
	 * If we ever decide to actually use the other pipeline we
	 * should axe this function as soon as possible.
	 * @param node
	 * @param blah
	 */
	public void setPhysicsNode(PhysicsNode node, boolean blah) {
		_physicsNode = node;
	}
	
	public boolean isCollisionWith(Entity b) {
		if (_collisionMap.containsKey(b.getId()))
			return true;
		return false;
	}
	
	public void onCollision(Entity collidingEntity, CollisionInfo contactInfo) {
		if (_collisionMap == null) // we are not a DynamicEntity
			return;
		
		// TODO: this is called multiple times for a single collision (substeps in the physics)
		// for now we just store the most recent of them.
		_collisionMap.put(collidingEntity.getId(), contactInfo);
	}

	protected void setupCollisions(boolean sheepOrNot) {
		if (sheepOrNot)
			sheepSetupCollisions();
		else
			wesSetupCollisions();
	}
	
	protected void wesSetupCollisions() {
		if (_physicsNode == null) {
			System.out.println("ERROR - setting up physics node.  Unknown physics node: " + getName());
			return;
		}
		
		SyntheticButton handler = _physicsNode.getCollisionEventHandler();
		
		_entityManager.getInputHandler().addAction(new InputAction() {
			private HashSet<String> _reportedMap = new HashSet<String>();
			
			public void performAction(InputActionEvent evt) {
				final ContactInfo contactInfo = ((ContactInfo) evt.getTriggerData());
				String name1 = contactInfo.getNode1().getName();
				String name2 = contactInfo.getNode2().getName();
	
				Entity entity1 = _entityManager.getEntity(name1);
				Entity entity2 = _entityManager.getEntity(name2);
	
				if (entity1 == null || entity2 == null) {
					// this occurs when we are colliding with a near sphere or
					// when we collide with something that isn't strictly an Entity
					if (!_reportedMap.contains(name1 + "," + name2)) {
						_reportedMap.add(name1 + "," + name2);
						System.out.println("ERROR - collision handler: " + name1 + " " + name2);
					}
					return;
				}
				
				CollisionInfo ci = new CollisionInfo(entity1, entity2);
				ci.setNormal(contactInfo.getContactNormal(null));
				ci.setPosition(contactInfo.getContactPosition(null));
				entity1.onCollision(entity2, ci);
				entity2.onCollision(entity1, ci);
			}
		}, handler.getDeviceName(), handler.getIndex(), InputHandler.AXIS_NONE, false);
	}

	@Override
	protected void makeBody() { }

	@Override
	protected Node makeNode() { return null; }

	
	// Get the contact info regarding the collision between this PE and otherEntity.
	public CollisionInfo getCollision(Entity otherEntity) { return _collisionMap.get(otherEntity.getId()); }

	
	// Register this entity for physics collision.
	// ////////////////////////////////////////////////////////////
	protected void sheepSetupCollisions() {
		// Setup the collision handler for this entity, so that it calls			
		// CollisionAction when collisions are detected.
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		SyntheticButton colHandler = getPhysicsNode().getCollisionEventHandler();

		//System.out.println("Registering \"" + getName() + "\" with collision manager.");
		w.getInput().addAction(CollisionManager.Get(), colHandler.getDeviceName(),
				colHandler.getIndex(), InputHandler.AXIS_NONE, false);
	}

	protected void makePhysics() {
		getNode().updateModelBound();
		getPhysicsNode().generatePhysicsGeometry();
	}
	
	
	@Override
	// Collisions get checked another way for physics entities, so nothing need be done here. 
	final protected void checkCollisions() {}

	public void remove() {
		super.remove();
		getPhysicsNode().delete();
		getPhysicsNode().removeFromParent();
	}	
	
}