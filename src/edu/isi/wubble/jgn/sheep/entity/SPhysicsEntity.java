package edu.isi.wubble.jgn.sheep.entity;

import com.jme.input.InputHandler;
import com.jme.input.util.SyntheticButton;
import com.jme.math.Vector3f;
import com.jmex.physics.PhysicsNode;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.action.CollisionManager;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.EntityManager;

//public abstract class PhysicsEntity extends DynamicEntity {
public abstract class SPhysicsEntity extends SEntity {

	// This is the same trickery used in the other stuff - calling this constructor
	// WON'T do the rest of the construction.
	public SPhysicsEntity(String name) {	super(Utils.GetEM(), name, true); } 

	// This is the bridge from/to Wes's constructor.  Also won't do the other 
	// constructor stuff.
	public SPhysicsEntity(EntityManager em, String name, boolean makeUnique) {
		super(em, name, makeUnique);
	}

	public SPhysicsEntity(String n, Vector3f pos) {
		// Construct Wes's stuff
		super(Utils.GetEM(), n, true);

		// Construct my stuff.
		doConstructorStuff(n, pos);
		
		// Physics entities have an additional collision-setup step.
		setupCollisions();
	}
	
	
	
	// Subclasses will give themselves the bodies of their dreams.
	protected abstract void makeBody();

	
	@Override
	public PhysicsNode getNode() { return (PhysicsNode)super.getNode(); }

	
	// Register this entity for physics collision.
	// ////////////////////////////////////////////////////////////
	protected void setupCollisions() {
		// Setup the collision handler for this entity, so that it calls			
		// CollisionAction when collisions are detected.
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		SyntheticButton colHandler = getNode().getCollisionEventHandler();

		//System.out.println("Registering \"" + getName() + "\" with collision manager.");
		w.getInput().addAction(CollisionManager.Get(), colHandler.getDeviceName(),
				colHandler.getIndex(), InputHandler.AXIS_NONE, false);
	}

	
	protected void makePhysics() {
		getNode().updateModelBound();
		((PhysicsNode)getNode()).generatePhysicsGeometry();
	}
	
	
	@Override
	// Collisions get checked another way for physics entities, so nothing need be done here. 
	final protected void checkCollisions() {}


	public void remove() {
		super.remove();
		getNode().delete();
	}
}