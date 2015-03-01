package edu.isi.wubble.jgn.sheep.action;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jmex.physics.PhysicsCollisionGeometry;
import com.jmex.physics.PhysicsNode;
import com.jmex.physics.contact.ContactInfo;

import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.physics.CollisionInfo;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;

public class CollisionManager extends InputAction {

	// This class is for graphical collisions, which don't produce contact info, but which are
	// tracked in the same table.  Use this once instance for all of those occasions.
	public class FakeContactInfo implements ContactInfo {

		public Vector3f getContactNormal(Vector3f store) { return null; }
		public Vector3f getContactPosition(Vector3f store) { return null; }
		public Vector3f getContactVelocity(Vector3f store) { return null; }

		public void getDefaultFrictionDirections(Vector3f a, Vector3f b) {}
		public PhysicsCollisionGeometry getGeometry1() { return null; }
		public PhysicsCollisionGeometry getGeometry2() { return null; }
		public PhysicsNode getNode1() { return null; }
		public PhysicsNode getNode2() {	return null; }
		public float getPenetrationDepth() { return 0; }
		public float getTime() { return 0; }
	}
	
	public static FakeContactInfo FAKE_CONTACT;
	
	// These are the collisions for each tick. The "outer" hash is indexed by
	// the name of the collider, the "inner" hash is a hash of the entities it
	// collided with. This gets reset at some fixed sampling interval.
	Hashtable<String, Hashtable<String, ContactInfo>> _collisionHash          = null;
	Hashtable<String, Hashtable<String, ContactInfo>> _symmetricCollisionHash = null;
	HashMap<String, HashMap<String, ContactInfo>>     _neCollisionHash        = null; 
	
	// Moved all this shit from SPS; not sure why it was ever there.
	public Hashtable<String, Hashtable<String, ContactInfo>> getCollisions()          { return _collisionHash;          }
	public HashMap<String, HashMap<String, ContactInfo>>     getNECollisions()        { return _neCollisionHash;        }
	public Hashtable<String, Hashtable<String, ContactInfo>> getSymmetricCollisions() { return _symmetricCollisionHash; }
	public Hashtable<String, ContactInfo>          getCollisions(String name)         { return _symmetricCollisionHash.get(name); }
	public HashMap<String, ContactInfo>            getNECollisions(String name)       { return _neCollisionHash.get(name);        }
	
	// Reset (after creating, if necessary) the various collision structures.
	public void resetCollisionHash() {
		// I create both assymetric and symmetric at the same time, so this one test suffices.
		if (_collisionHash != null) { 
			_collisionHash.clear();
			_symmetricCollisionHash.clear();
			_neCollisionHash.clear();
		}
		else { 
			_collisionHash          = new Hashtable<String, Hashtable<String, ContactInfo>>(); 
			_symmetricCollisionHash = new Hashtable<String, Hashtable<String, ContactInfo>>();
			_neCollisionHash        = new HashMap<String, HashMap<String, ContactInfo>>();			
		}
	}


	public Hashtable<String, ContactInfo> isColliding(String name) {
		return _symmetricCollisionHash.get(name); 
	}
	
	// Returns a boolean if lowName is colliding with highName
	public ContactInfo getColliding(String lowName, String highName) {
		Hashtable<String, ContactInfo> colHash = getCollisions(lowName);
		ContactInfo retVal = null;
		
		if (colHash != null) { 
			retVal = colHash.get(highName);
//			for (Entry<String, ContactInfo> e : colHash.entrySet()) {
//				System.out.println("  -- " + lowName + " is colliding with " + e.getKey());
//			}
		}

		//System.out.println("^^ " + lowName + " colliding with " + highName + ": " + retVal);
		
		return retVal;
	}
	
	private static CollisionManager _inst;
	private CollisionManager() { 
		FAKE_CONTACT = new FakeContactInfo();
		resetCollisionHash();
	}

	public static CollisionManager Get() {
		if (_inst == null) { _inst = new CollisionManager(); }
		return _inst;
	}
	
	public void performAction(InputActionEvent evt) {
		final ContactInfo contactInfo = ((ContactInfo) evt.getTriggerData());
		
		// Take my collision pathway.
		processCollision((PhysicsNode)contactInfo.getNode1(), (PhysicsNode)contactInfo.getNode2(), contactInfo);
		
		// Take Wes's collision pathway.
		EntityManager em = Utils.GetEM();
		Entity entity1 = em.getEntity(contactInfo.getNode1().getName());
		Entity entity2 = em.getEntity(contactInfo.getNode2().getName());

		// Wes doesn't handle collisions with non-entity stuff (like postShape).
		if (entity1 == null || entity2 == null) { return; }
		
		CollisionInfo ci = new CollisionInfo(entity1, entity2);
		ci.setNormal(contactInfo.getContactNormal(null));
		ci.setPosition(contactInfo.getContactPosition(null));
		entity1.onCollision(entity2, ci);
		entity2.onCollision(entity1, ci);
	}
	
	
	Boolean _True = new Boolean(true); 
	protected void addToNECollisions(String collider, String colidee, ContactInfo info) {
		HashMap<String, HashMap<String, ContactInfo>> colHash = getNECollisions();
		HashMap<String, ContactInfo> cols = colHash.get(collider);
		
		//System.out.println("-- Processing col between " + collider + " and " + colidee);
		// If nothing has yet collided with this thing, make a new hashmap to hold the
		// collisions.
		if (cols == null) { cols = new HashMap<String, ContactInfo>(); }
		cols.put(colidee, info);
		colHash.put(collider, cols);
	}
	
	
	protected boolean handleNonEntityCollisions(String lowName, String highName, ContactInfo info) {
		boolean isNotEntity = false;
		SEntity eLow  = SEntity.GetEntityForName(lowName);
		SEntity eHigh = SEntity.GetEntityForName(highName);
		
		// Depending on whether the lowname or the highname (or both) aren't entities, add the
		// collisions to the non-entity collision hash.
		if (eLow == null) {
			addToNECollisions(lowName, highName, info);
			//System.out.println(lowName  + " IS NOT ENTITY in col between " + lowName + " and " + highName);
			isNotEntity = true;
		}
		
		if (eHigh == null) {
			addToNECollisions(highName, lowName, info);
			//System.out.println(highName  + " IS NOT ENTITY in col between " + lowName + " and " + highName);
			isNotEntity = true;
		}
		
		return isNotEntity;
	}
	
	
	public void processCollision(Node node1, Node node2, ContactInfo info) {
		String name1 = node1.getName();
		String name2 = node2.getName();
		
		//System.out.println("possibly meaningful collision between " + name1 + " and " + name2);

		// if (true) { return; }		
		Node lowNode = node1;
		Node highNode = node2;
		
		// Don't care about self collisions.
		if (node1 == node2) { assert(false); }

		// FIXME: This shouldn't be allowed to happen.  God damn near spheres.
		if (name1 == null || name2 == null) {
			System.out.println("wtf?  " + name1 + ", " + name2);
			assert(false);
			return;
		}
		
		// Put the nodes in lexographic order.
		if (name1.compareTo(name2) > 0) {
			lowNode = node2;
			highNode = node1;
		}

		String lowName = lowNode.getName();
		String highName = highNode.getName();
		
		Hashtable<String, Hashtable<String, ContactInfo>> collHash = getCollisions();
		
		// If both of these things aren't entities, put them in a different hash, and return.
		if (handleNonEntityCollisions(lowName, highName, info) == true) { return; }

		// If this hash doesn't exist -- if this entity hasn't collided 
		// with anything yet on this tick, create it.
		Hashtable<String, ContactInfo> entityColHash = collHash.get(lowName);
		if (entityColHash == null) {
			entityColHash = new Hashtable<String, ContactInfo>();
			collHash.put(lowName, entityColHash);
		}
		
		// Put this entity into the collision hash. If it's already there,
		// no harm done.
		if (!entityColHash.containsKey(highName)) {
			// Entity en = Entity.GetEntityForNode(highNode);
			entityColHash.put(highName, info);
			// System.out.println("First col (this tick) between " + lowName + " and " + highName);
		}

		/////////// Now the same thing, using the symmetric hash. //////////////
		Hashtable<String, Hashtable<String, ContactInfo>> symmetricHash = getSymmetricCollisions();			
		
		// Do the same for both sides of the symmetric hash.  First, on lowName.
		Hashtable<String, ContactInfo> symEntityColHashLow = symmetricHash.get(lowName);			
		if (symEntityColHashLow == null) {
			symEntityColHashLow = new Hashtable<String, ContactInfo>();
			symmetricHash.put(lowName, symEntityColHashLow);
		}
		
		// Now for highName.
		Hashtable<String, ContactInfo> symEntityColHashHigh = symmetricHash.get(highName);			
		if (symEntityColHashHigh == null) {
			symEntityColHashHigh = new Hashtable<String, ContactInfo>();
			symmetricHash.put(highName, symEntityColHashHigh);
		}			
		
		// Add these to the symmetric hash, low and high.
		if (! symEntityColHashLow.containsKey(highName)) { symEntityColHashLow.put(highName, info); }
		if (! symEntityColHashHigh.containsKey(lowName)) { symEntityColHashHigh.put(lowName, info);	}
	}
	
	// Print collision lists and other utilities. 
	// ////////////////////////////////////////////////////////////
	public void printCollisionList() {
		printColList(getCollisions());
	}
	
	public void printSymmetricCollisionList() {
		printColList(getSymmetricCollisions());
	}
	
	private void printColList(Hashtable<String, Hashtable <String, ContactInfo>> h) {
		Set<String> s = h.keySet();

		for (String lowName : s) {
			System.out.println("pcl: " + lowName + " collided with: ");
			Hashtable<String, ContactInfo> collidedHash = h.get(lowName);
			if (collidedHash == null) { continue; }
			Set<String> names = collidedHash.keySet();
			for (String highName : names) {
				System.out.println("    --> " + highName);
			}
		}
	}
}