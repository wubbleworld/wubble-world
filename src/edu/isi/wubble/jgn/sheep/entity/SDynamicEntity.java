package edu.isi.wubble.jgn.sheep.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

import com.jme.input.KeyInput;
import com.jme.input.controls.GameControlManager;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.contact.ContactInfo;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.controller.JumpController;
import edu.isi.wubble.physics.entity.controller.LookAtController;
import edu.isi.wubble.physics.entity.controller.MovementController;
import edu.isi.wubble.physics.entity.controller.NearSphereController;
import edu.isi.wubble.physics.entity.controller.RotationController;
import edu.isi.wubble.physics.entity.controller.SheepMovementController;

public abstract class SDynamicEntity extends DynamicEntity {
	
	// Dynamic entities are indexed by ID.
	// Shane's Theorem: The key set for this table is IDENTICAL to the set of players in the system
	static private HashMap<Short, SDynamicEntity> _idDynEntityHash = new HashMap<Short, SDynamicEntity>();
	public static SDynamicEntity             GetDynEntityForID(short id)    {	return _idDynEntityHash.get(id); }
	public static Collection<SDynamicEntity> GetPlayers()                   { return _idDynEntityHash.values(); }
	private   boolean _isInteresting;
	public    boolean isInteresting() { return _isInteresting;  }
	public    void    setInteresting()    { _isInteresting = true; }
	public    void    setNotInteresting() { _isInteresting = false; }
	
	private boolean _isColliding;
	public  boolean isColliding()           { return _isColliding; }
	public  void    setColliding(boolean b) { _isColliding = b; }
	
	private Vector3f   _oldPos;
	private Quaternion _oldRot;
	
	// Override this method for entities that have seperate visual and physics nodes.
	public Node getVisNode()     { return getNode();   }
	
	
	// The powerups this entity has.  Deactivated (and removed) on game reset.
	protected HashMap<String, PowerUp> _powerUps;

	// Any dynamic entity might be controlled by a client.
	short _clientID;
	public short getID() { return _clientID; }

	// For making non-client controlled entities.
	static int _unnamedIndex = 0;

	// Stuff that uses the argument-less constructor is not (and cannot be) 
	// controlled by a client.
	public SDynamicEntity() {
		super("Englebert_" + _unnamedIndex, new Vector3f());
		_unnamedIndex++;
		
		// Set the id to the default, non-controllable index.
		_clientID = Utils.IMPOSSIBLE_CLIENT_ID;
		finishConstruction();
	}
	
	
	// This is an entity that will never be controlled by a client.
	public SDynamicEntity(String name, Vector3f pos) {
		super(name, pos);
		_clientID = Utils.IMPOSSIBLE_CLIENT_ID;
		finishConstruction();
	}
	
	// Client-controlled entity.
	public SDynamicEntity(String name, short id, Vector3f pos) { 
		super(name, pos);
		_clientID = id;
		
		// Add this entity to the hash.
		_idDynEntityHash.put(id, this);
		finishConstruction();
	}
	
	
	// Post-constructor constructor; designed to get around Java's construction inadequacies.
	private void finishConstruction() {
		// Each entity has a hash of its powerups, which might not be active. 
		_powerUps = new HashMap<String, PowerUp>();
	
		// Everyone is born a little dull.
		setNotInteresting();
		
		// Setup the rest of DynamicEntity stuff - controllers, collisions.
		addDefaultControllers();
		
		// Draw the near spheres so they actually look ok.
		NearSphereController nsc = (NearSphereController) getController(NearSphereController.class.getName());
		if (nsc != null) {	nsc.drawNearSphere(true); } 
		
		// Note that collisions are redundant with my CM.  Shouldn't matter, though.
		setupCollisions(true);
		
		_oldPos = new Vector3f();
		_oldRot = new Quaternion();
	}

	
	protected Node makeNode() {
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		DynamicPhysicsNode pn = w.getPhysicsSpace().createDynamicNode();
		setNode(pn);
		
		// Controllers will have to be added seperately, since I've decoupled them from the
		// two-arg form of SPN.
		System.out.println("Makenode: setting physics node for " + getName());
		setPhysicsNode(pn, false);
		
		return pn;
	}
	
	public DynamicPhysicsNode getNode() {
		return (DynamicPhysicsNode)super.getNode(); 
	}
	

	public void powerDown() {
		// Deactivate all the PUs I have, and then clear them out.
		Set<String> names = _powerUps.keySet();
		for (String n : names) {
			PowerUp p = _powerUps.get(n);
			p.deactivate();
		}
		_powerUps.clear();
	}

	public void powerUp(PowerUp p) {
		// Add this powerup to my list.
		_powerUps.put(p.getName(), p);
	}
	
	
	static float POS_INTERESTING_THRESH = .02f;
	static float ROT_INTERESTING_THRESH = 20.0f;
	boolean _clearInteresting = false;
	public void checkIfInteresting() {
		Vector3f p   = getPosition();
		Quaternion r = getRotation();
		
		// By default, people are not interesting.  You have to do, or have something done to you, to be interesting.
		// All this rigamarole is to make sure an interesting person stays interesting for a whole tick, regardless of 
		// when (in the update cycle) he becomes interesting. 
		if (_isInteresting == true) {
			if (_clearInteresting == true) { _clearInteresting = false; _isInteresting = false; }
			else { _clearInteresting = true; return; }
		}
		
		float dist = _oldPos.distance(p);
		if (this instanceof Wubble) {
			//System.out.println("for " + getName() + " dist between " + Utils.vs(p) + " and " + Utils.vs(_oldPos) + " is " + dist);
		}

		// Compare this position and quaternion with the last one; if either has changed 
		// enough, than I am interesting.  If I'm colliding with anything, I'm also interesting.
		if (isColliding()) { 
			//System.out.println(getName() + " is interesting bc colliding."); 
			setInteresting(); }
		else if (dist > POS_INTERESTING_THRESH) { 
			//System.out.println(getName() + " is interesting bc moving."); 			
			setInteresting(); }
		else {
			// Figure out how big a difference this rotation is from the last one.
			Quaternion nowInv = r.inverse();
			Quaternion qPrime = nowInv.mult(_oldRot);
			float[] angles = qPrime.toAngles(null);
			float tot = 0;
			for (float f:  angles) { tot += java.lang.Math.abs(f); }
			if (tot > .02) {
				//System.out.println(getName() + " is interesting bc rotating."); 
				setInteresting(); 
			}
		}
		
		//if (isInteresting()) { System.out.println("'" + getName() + "' is interesting."); }

		// Update the pos, rot.
		_oldPos.set(p);
		_oldRot.set(r);
	}
	
	public String qs(Quaternion q) {
		return "{x: " + q.x + " y: " + q.y + " z: " + q.z + " w: " + q.w + "}";
	}
	
	public void stopMoving() {	
//		DynamicPhysicsNode p = getNode();
//
//		p.setAngularVelocity(new Vector3f(0, 0, 0));
//		p.setLinearVelocity(new Vector3f(0, 0, 0));
//
//		p.updateWorldData(0);
//		p.updateGeometricState(0, true);
//
//		// World.p("Stopping " + ce.getName());
	}
	
	
	public MovementController _movementController;
	public RotationController _rotationController;
	public JumpController     _jumpController;

	// Remove the controllers, if they exist, and reset them.
	public void removeMovementControllers() {
		
		// Remove the movement controllers from the node, if it has any.
		if (_movementController != null) { removeController(_movementController.getClass().getName()); }
		if (_jumpController != null)     { removeController(_jumpController.getClass().getName()); }
		if (_rotationController != null) { removeController(_rotationController.getClass().getName()); }
				
		// Remove these controls from the mgr, too.
		GameControlManager m = Utils.GetSps().getGCM();
		
		// I'm almost positive we don't want these anymore, but I'm leaving 
		// them here to remind me in case I'm wrong.
		m.removeControl("forward");
		m.removeControl("backward");
		m.removeControl("left");
		m.removeControl("right");
		m.removeControl("jump");
		
		_movementController = null;
		_rotationController = null;
		_jumpController = null;
	}
	
	

	
	@Override
	public void delayedUpdate() {
		// Multi-tier construction (splitting between the actual constructor and doConstructorStuff) 
		// can allow a window in which the physics node isn't set.  Therefore, wait before doing this.
		if (_physicsNode != null) {
			//System.out.println("_physicsNode for " + getName() + " isn't null!");
			super.delayedUpdate(); 
		}
	}
	
	// Add bindings to make this entity controllable.
	public void addMovementControls(boolean local) {
		// It's possible that SDEs will already have these controllers; if they do, yank them.
		removeMovementControllers();
		
		_movementController = new SheepMovementController(this, getDefaultSpeed()); 
		_rotationController = new RotationController(this, getDefaultTurn());
		_jumpController     = new JumpController(this, true);
		
//		String mcName = _movementController.getClass().getName();
//		String rcName = _rotationController.getClass().getName();
//		String jcName  = _jumpController.getClass().getName();
//		removeController(mcName);
//		removeController(rcName);
//		removeController(jcName);
		
		if (local) {
			_movementController.addKeyBinding(KeyInput.KEY_NUMPAD8, KeyInput.KEY_NUMPAD5);
			_rotationController.addKeyBinding(KeyInput.KEY_NUMPAD4, KeyInput.KEY_NUMPAD6);
			_jumpController.addKeyBinding();
		} else {
			_movementController.addAutoBinding();
			_rotationController.addAutoBinding();
			_jumpController.addAutoBinding();
		}
		
		System.out.println("-- Adding movement, rotation, jump controllers to " + getName() + " --");
		
		addController(_movementController);
		addController(_rotationController);
		addController(_jumpController);
	}
	
		
	private static final int DEFAULT_SPEED = 6;
	private static final int DEFAULT_TURN  = 2;

	private int   getDefaultTurn()  { return DEFAULT_TURN;  }
	private int   getDefaultSpeed() { return DEFAULT_SPEED; }
	
	@Override
	// Default behavior is to print this Entity's "meaningful" collisions when they occur.
	public void onCollision(Hashtable<String, ContactInfo> collisions) {
		// If I collided with something, I am interesting.
		boolean iAmInteresting = false;
		Set<String> s = collisions.keySet();
		for (String eName : s) {
			SEntity e = SEntity.GetEntityForName(eName);
			if (e instanceof SDynamicEntity) {
				iAmInteresting = true;
				// Whatever I collide with is also interesting.
				SDynamicEntity de = ((SDynamicEntity)e);
				de.setColliding(true);
				de.setInteresting();
				// System.out.println("\"" + getName() + "\" collided with: \"" + eName + "\""); 
			}			
		}

		if (iAmInteresting) {
			setColliding(true);
			setInteresting();
			// System.out.println(getName() + " setting interesting in onCollision");
		}
	}

		
	
	public void remove() {
		super.remove();
		removeMovementControllers();
		_idDynEntityHash.remove(getID());
		//_indicator.removeFromParent();
	}
	
	public void applyImpulse(int forceMultiplier) {
		DynamicPhysicsNode p = (DynamicPhysicsNode)getNode();
		
		Vector3f oldForce = new Vector3f();
		p.getForce(oldForce);
		
		Vector3f vel = new Vector3f();
		p.getLinearVelocity(vel);
		
		// Add a burst in the appropriate direction.
		Vector3f l1 = p.getLocalTranslation();
		//Vector3f l2 = _indicator.getWorldTranslation();
		Vector3f l2 = Vector3f.UNIT_Z;
		Vector3f impulse = l2.subtract(l1);
		//w.p("l1 --> " + w.vs(l1) + " l2 --> " + w.vs(l2) + " impulse --> " + w.vs(impulse));
//		impulse.y = 1;
		
		//impulse = direction;
		
		impulse.normalizeLocal().multLocal(forceMultiplier);
		impulse.setY(0);
		
		p.addForce(impulse);
		//p.addForce(direction);

		//World.p("Applying force to " + getName() + " --> " + World.vs(impulse));
		
//				+ " [Old force: " + w.vs(oldForce) + "  oldVel:" + w.vs(vel) + "]");			
	}
	

	

	@Override
	public void behave(float tpf) {
		super.behave(tpf);
		update(tpf);
	}
	
	public void react() {
		super.react();
		
		// See if whatever has happened to me during this tick has made me interesting.
		checkIfInteresting();
		
		// Reset this each time through the loop.  If the collision is enduring it will get set to true again.
		setColliding(false);
	}

	
	public void update(float tpf) {
		super.update(tpf);
		// Controllers attached to a node get updated automatically, so this update actually 
		// updates them TWICE per cycle.  Who knows what bugs were lying in wait?
//		for (Controller c : getNode().getControllers()) {
//			c.update(tpf);
//		}
	}
}
