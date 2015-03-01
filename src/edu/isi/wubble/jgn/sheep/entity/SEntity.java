package edu.isi.wubble.jgn.sheep.entity;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.intersection.BoundingCollisionResults;
import com.jme.intersection.CollisionData;
import com.jme.intersection.CollisionResults;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jmex.physics.contact.ContactInfo;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.action.CollisionManager;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.util.Globals;

public abstract class SEntity extends Entity {

	// For determining (visual) collision results.
	protected CollisionResults _res = null;
	
	// Governs how (and whether) the entity will update itself after each tick.  
	// private EntityUpdateStrategy _updateStrategy = null;
	private boolean              _isBehaviorActive = true;
	public void setBehaviorActive(boolean b) { _isBehaviorActive = b; }
	public boolean isBehaviorActive() { return _isBehaviorActive; }
	
	// Get the position of the entity, represented by the translation of its node.
	// NOTE: If you do things to this vector, it could affect stuff in
	// strange ways.
	public Vector3f getPosition() { return getNode().getLocalTranslation();     }
	public void setPosition(Vector3f pos) { getNode().setLocalTranslation(pos); }

	
	// Entity decorators; these are (presumably) connected to the entity, and
	// get updated in the physics update cycle.
//	List<EntityDecorator> _ed = null;
//	public void addDecorator(EntityDecorator ed) { _ed.add(ed); }

	
	// Static hash tables, for looking up an entity in the system.
	static private Hashtable<Node, SEntity>   _nodeEntityHash_x     = new Hashtable<Node, SEntity>();
	static private Hashtable<String, SEntity> _nameEntityHash_x     = new Hashtable<String, SEntity>();
	// static private HashMap<String, SEntity>   _decoratorEntityHash  = new HashMap<String, SEntity>();
	
	
	public static SEntity GetEntityForNode(Node n)     {	return _nodeEntityHash_x.get(n);    }
	public static SEntity GetEntityForName(String n)   { return _nameEntityHash_x.get(n);    }
	
	// public static SEntity GetDecoratedEntity(String n) { return _decoratorEntityHash.get(n); }
	// public static HashMap<String, SEntity> GetDecoratorHash() { return _decoratorEntityHash; }
	
	// Use this for mapping stuff to entities - either nodes or names.
	protected void indexEntity(Node n)   { _nodeEntityHash_x.put(n, this); }
	protected void indexEntity(String s) { _nameEntityHash_x.put(s, this); }

	// This idiotic decomposition of constructor behavior is to get around
	// Java's posits decree that calls to superclass constructors have to be the
	// first line of code in subclass constructors.
	public SEntity(String name, Vector3f pos) {
		super(Utils.GetEM(), name, true);
		doConstructorStuff(name, pos); 
	}
	
	// This one-item constructor does NOT call doConstructorStuff - so subclasses that need 
	// to do stuff before proper construction are allowed to - but it does need a name, to 
	// keep EntityManager happy.  Although I don't think this actually matters for the moment,
	// since I'm using my own EM-like features.
	public SEntity(String name) { 
		super(Utils.GetEM(), name, true);
	}
	
	// This follows the originan Wes-entity pathway; doesn't construct any SEntity-specific stuff.
	public SEntity(EntityManager em, String name, boolean makeUnique) {
		super(em, name, makeUnique);
	}
	

	// Subclasses will override this method to create the appropriate kind of node.
	// ////////////////////////////////////////////////////////////
	abstract protected Node makeNode();
	
	protected void doConstructorStuff(String name, Vector3f pos) {
		
		System.out.println("-- doConstructorStuff for " + getName());
		
		makeNode();
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		
		// Make the decorator list.
//		_ed = new ArrayList<EntityDecorator>();
		
		// Naming conventions in the service of collisions dictate that things will 
		// go apeshit if there are underscores in the name.
		if (name.indexOf("_") != -1) {
			System.out.println("Hey!  Don't create entities with _ in the name!");
			assert(false);
		}
		
		// How can this ever be null?
		assert(getNode() != null);

		getNode().setName(name);
		getNode().setLocalTranslation(pos);
		
		// System.out.println("Entity: translating " + getName() + " to " + Utils.vs(pos));
		// Allocate results object; normally this will be bounding collision results, but 
		// subclasses can override for better accuracy.
		setCollisionResultsType();

		// Add this node to the class hash.
		indexEntity(getNode());
		indexEntity(name);

		// Make a visual body to go with this physics node. Subclassed entities
		// will want to change this to make the appropriate geometry.
		makeBody();
		makePhysics();

		getNode().setModelBound(new OrientedBoundingBox());
		getNode().updateModelBound();
		getNode().updateGeometricState(0, true);

		// Reflect the appropriate translations for the node and its children.
		getNode().updateWorldVectors();

		// Add myself to the world.
		w.addEntity(this);
		
		// Attach myself to the scene.
		w.getRootNode().attachChild(getNode());
		w.getRootNode().updateModelBound();
		w.getRootNode().updateGeometricState(0, true);
	}

	// ////////////////////////////////////////////////////////////
	protected void makePhysics() {
		// Is this where to put it?
		getNode().updateModelBound();
	}
		
	
	// ////////////////////////////////////////////////////////////	
	protected void setCollisionResultsType() {
		_res   = new BoundingCollisionResults();
	}
	
	
	// Why the fuck didn't I write this function two weeks ago?
	// Because I'm an idiot.
	// ////////////////////////////////////////////////////////////
	public static ArrayList<SEntity> GetCollisionsWith(Class c, Hashtable<String, ContactInfo> collisions) {
		ArrayList<SEntity> collisionsWith = new ArrayList<SEntity>();
		
		for (String eName : collisions.keySet()) {
			SEntity e = SEntity.GetEntityForName(eName);
			if (c.isInstance(e)) { collisionsWith.add(e); }
		}
		return collisionsWith;
	}

	
	//
	// This is for adding non-physical graphics details to an entity. (This is
	// called after physics geometry is created.)
	// ////////////////////////////////////////////////////////////
	protected void makeAdornments() {}

	
	// ////////////////////////////////////////////////////////////
	protected abstract void makeBody();

	
	// Makes this entity do whatever this entity does.
	// ////////////////////////////////////////////////////////////	
	public void behave(float tpf) {}

	
	// Entity reacts to the conditions in which it finds itself.
	// ////////////////////////////////////////////////////////////		
	public void react() {
		checkCollisions();
		
		// If I'm colliding with something, do whatever I do in those cirumstances.
		Hashtable<String, ContactInfo> h = CollisionManager.Get().isColliding(getName());
		if (h != null) { onCollision(h); }
	}
	
	
	// Returns true if srcName is not one of the collisions we don't care about.
	// Daniel: This is now static, since it uses no data specific to the instance
	// ////////////////////////////////////////////////////////////			
	public static boolean checkCrapCollisions(String srcName) {
		boolean retVal = false;
		if (srcName.indexOf("floor") != -1 || srcName.indexOf("left") != -1 || srcName.indexOf("right") != -1 ||
				srcName.indexOf("near") != -1 || srcName.indexOf("far") != -1) { retVal = true; }
		return retVal;
	}
	
	// Daniel again: I don't like your function's name (or its semantics) so I made a new one.
	public static boolean isInterestingCollision(String name) {
		return !(name.contains("floor") || 
				 name.contains("left") || 
				 name.contains("right") ||
				 name.contains("near") || 
				 name.contains("far"));
	}
	
	
	// This checks to see if there are collisions between jME components.  These 
	// are GRAPHICAL (jme-based) collisions.  Physical entities will override this
	// function, since they do their collisions differently.
	// ////////////////////////////////////////////////////////////
	protected void checkCollisions() {
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		Node n = getNode();
		n.calculateCollisions(w.getRootNode(), _res);
		int numCol = _res.getNumber();
		if (numCol > 0) { 
			// System.out.println("Zone::afterPhysics -- there have been " + numCol + " collisions on " + getName());
			for (int i=0; i < numCol; i++) {
				CollisionData cd = _res.getCollisionData(i);
				Geometry gSrc = cd.getSourceMesh();
				Geometry gTgt = cd.getTargetMesh();

				// I only want collisions between this zone and entities (other than itself.)
				String tgtName = gTgt.getName();
				String srcName = gSrc.getName();

				// Throw out a bunch of meaningless collisions, since there's too much shit on the screen.
				if (checkCrapCollisions(srcName) == true || checkCrapCollisions(tgtName) == true) { continue; }
				
				// Use the names of the parent to get the names of the entities, instead of the names of the meshes.  
				// This eliminates the need for that hackish workaround below.
				Node srcParent = gSrc.getParent();
				Node tgtParent = gTgt.getParent();

//				System.out.println("[jme] collision " + i + " btwn src \"" + srcName + "\" and tgt \"" + tgtName + "\"");
//				System.out.println("    srcParent is " + srcParent.getName() + " tgtParent is " + tgtParent.getName());
				
				srcName = srcParent.getName();
				tgtName = tgtParent.getName();

				// These jME collisions aren't reported with the top-level node, but rather 
				// the visual nodes, which have derivative names of the form name__static_box.
				// Strip that crap off for entity lookup.
//				int startJunk = tgtName.indexOf("__");
//				if (startJunk >= 0) { tgtName = tgtName.substring(0, startJunk); }
//				
//				startJunk = srcName.indexOf("__");
//				if (startJunk >= 0) { srcName = srcName.substring(0, startJunk); }
				
				// Don't report anything if these are the same entity.  I dunno why it would 
				// happen, but we don't want it.
				if (srcName.compareTo(tgtName) == 0) { System.out.println("Weird self collision!"); continue; }
				
				// System.out.println("tgtName " + tgtName + "  srcName " + srcName);
				SEntity eTgt = SEntity.GetEntityForName(tgtName);
				SEntity eSrc = SEntity.GetEntityForName(srcName);
				
				if (eSrc != null && eTgt != null && eTgt != this) {
					//System.out.println("[jme] collision " + i + " btwn src \"" + gSrc.getName() + "\" and tgt \"" + gTgt.getName() + "\"");
					CollisionManager.Get().processCollision(eTgt.getNode(), eSrc.getNode(), CollisionManager.FAKE_CONTACT);
				}
			}
		}
		
		// Reset these after each tick.
		_res.clear();
	}
	
	
	// Event handler for collisions.
	// ////////////////////////////////////////////////////////////	
	public void onCollision(Hashtable<String, ContactInfo> collisions) {
		// System.out.println("Entity " + getName() + " collided with something!");
	}
	
	public void printCollisions(Hashtable<String, SEntity> collisions) {
		Set<String> names = collisions.keySet();
		for (String n : names) {
			System.out.println("\"" + getName() + "\"" + " collided with \"" + n + "\"");
		}
	}


	// Stop moving, if this is the sort of entity that moves.
	// ////////////////////////////////////////////////////////////
	public void stopMoving() {}

	
	// Remove this entity from the system; note: after this function returns,
	// the entity doesn't exist anymore.
	// ////////////////////////////////////////////////////////////
	private boolean _isRemoved = false;
	public boolean isRemoved() { return _isRemoved; }
	private void setRemoved() { _isRemoved = true; }
	public void remove() {
		super.remove();
		setRemoved();
		setBehaviorActive(false);
		
		// Cleanup the decorators, then remove the references to them.
		// wk: only if we have actually created _ed... not in Wes's pipeline
//		if (_ed != null) {
//			for (EntityDecorator ed : _ed) { ed.cleanup(); }
//			_ed.clear();
//		}
		
		// Remove the entity from the class-level hash tables.
		_nodeEntityHash_x.remove(this);
		_nameEntityHash_x.remove(getName());
		
		if (Globals.IN_SHEEP_GAME) {
			// Remove the entity from the world's reckoning, w/ the possible
			// (temporary) exception of the collisionHash. See notes in
			// World::removeEntity.
			SheepPhysicsState w = SheepPhysicsState.getWorldState();
			w.removeMe(this);
		}
	}

//	// Physics stuff - if this entity is supposed to do special stuff before or
//	// after the physics calculations. Note that you could use this as a sort of
//	// controller...
//	// ////////////////////////////////////////////////////////////7
//	public void preUpdate() {}
//
//	
//	public void delayedUpdate() {}
////		System.out.println("DelayedUpdate called on " + getName());
//
//
//  	// NOTE: Make sure you call the super on subclasses...
//	public void postUpdate() {
////		if (_ed != null) {
////			for (EntityDecorator ed : _ed) { ed.doStuff(); }
////		}
//	}
//	

	// Bollocks
	// ////////////////////////////////////////////////////////////
	public void setColor(ColorRGBA color) {
		ArrayList<Spatial> c = getNode().getChildren();

		// Set the color of all the children of the main node.  This won't work when the children are nodes.  Really need some way to recurse infinitely.
		for (Spatial child : c) { if (child != null) {Utils.setColor(child, color); }	}
	}
}
