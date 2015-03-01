package edu.isi.wubble.jgn.sheep.entity;

import java.util.ArrayList;
import java.util.Hashtable;

import com.jme.bounding.BoundingSphere;
import com.jme.intersection.TriangleCollisionResults;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.shape.Sphere;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.contact.ContactInfo;
import com.jmex.physics.material.Material;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.physics.entity.controller.LiftingController;
import edu.isi.wubble.physics.entity.controller.LookingAtEachOtherController;
import edu.isi.wubble.physics.entity.controller.VisualSalienceController;

public class Wubble extends SDynamicEntity {
	
	private static Hashtable<String, Wubble> _wubbleHash = new Hashtable<String, Wubble>();
	public  static Hashtable<String, Wubble> GetWubbleHash() { return _wubbleHash; }
	public static Wubble GetWubbleForName(String n) { return _wubbleHash.get(n); }
	
	private Node   _visNode;
	private Sphere _visSphere;
	
	private Sidekick _sidekick;
	
	public Node getVisNode()     { return _visNode;   }
	public Sphere getVisSphere() { return _visSphere; }
	
	protected int _team;
	public int  getTeam()      { return _team; }
	public void setTeam(int t) { _team = t; }
	
	// If a wubble's created without an id, use the default of 0.
	public Wubble(String name, Vector3f pos) { 
		super(name, (short)0, pos);
		initWubble();
	}
	
	// This is a wubble controlled by a client.
	public Wubble(String name, short id, int team) { // Vector3f pos) {
		super(name, id, new Vector3f());
		System.out.println("Making wubble " + getName() + " on team " + team);

		setTeam(team);
		initWubble();
	}
	
	// Have the wubble pickup a dEntity
	public void pickup(SDynamicEntity de) {
		
		String lcName = LiftingController.class.getName();
		LiftingController liftController = (LiftingController)getController(lcName);
		if (liftController == null) { assert(false); }
		
		// Lift the sheep.
		liftController.applyController(de);
		System.out.println(getName()  + " is lifting " + de.getName());
	}
	
	// Have the wubble release whatever it's holding, if anything.
	public void putDown() {
		String lcName = LiftingController.class.getName();
		
		LiftingController liftController = (LiftingController)getController(lcName);
		if (liftController == null) { assert(false); }
		liftController.release();
	}
	
	
	// Common constructor code.
	private void initWubble() {
		// Add myself to the hash.
		_wubbleHash.put(getName(), this);
		
		SheepPhysicsState sps = Utils.GetSps();

		// Keep track of this Wubble's ID
		System.out.println("--- Adding wubble " + getName() + " w/id " + getID() + " ---");
		sps.getWubbleIDs().put(getID(), this);
		
		// Make sure the SPS is active, since now there's [at least] one player in there.
		if (sps.isActive() == false) {
			sps.setActive(true);
			System.out.println("Setting SPS active on the arrival of \"" + getName() + "\"");
		}
		
		// Make a NS complex as decorator.  (Do this here, after physics 
		// geometry has been generated.  If it continues to be a problem, make it 
		// a physics ghost.)
		
		// Actually, don't.  Wes's NSs supersede this.
		// new NearSphereComplex(this);
		
		// instead, make an fov thingy
		addController(new VisualSalienceController(this));
		addController(new LookingAtEachOtherController(this));
	}
	
	@Override
	// ////////////////////////////////////////////////////////////
	protected void setCollisionResultsType() { _res = new TriangleCollisionResults(); }
	
	
	protected void makeBody() {
		DynamicPhysicsNode n = getNode();
		
		// A generic entity is a red sphere, made of wood. (Huh huh, I said
		// 'wood.')
		_visSphere = new Sphere(getName(), 16, 16, 0.25f);
		
		// new
		_visSphere.setModelBound(new BoundingSphere());
		_visSphere.updateModelBound();
		_visSphere.updateGeometricState(0, true);
		
		_visNode   = new Node(getName());
		_visNode.attachChild(_visSphere);
		
		// new
		_visNode.setModelBound(new BoundingSphere());
		
		// We need this to match the client's rotation
		Quaternion q = new Quaternion();
		q.fromAngles(0, 180 * FastMath.DEG_TO_RAD, 0);
		_visNode.setLocalRotation(q);
		_visNode.updateModelBound();
		_visNode.updateGeometricState(0, true);
		
		Utils.setColor(_visSphere, ColorRGBA.red);
		n.attachChild(_visNode);
		n.setMaterial(Material.ICE);
	}
	
	// will just return null if there is no sidekick
	public Sidekick getSidekick() {
		return _sidekick;
	}
	
	public void setSidekick(Sidekick s) {
		_sidekick = s;
	}
	
	@Override
	public void onCollision(Hashtable<String, ContactInfo> collisions) {
		super.onCollision(collisions);
		
		// See if I'm colliding with any sheep.
		ArrayList<SEntity> c = GetCollisionsWith(Sheep.class, collisions);

		// Find out characteristics of the wubble's speed.
		Vector3f curVelocity = getNode().getLinearVelocity(null);
		Vector3f wubDirection = getRotation().getRotationColumn(2);
		wubDirection.normalizeLocal();
		
		//wubDirection = wubDirection.mult(curVelocity.length() * 1.2f);
//		direction.y = curVelocity.y;

		// Apply a bump to this sheep.  (Check bounce to see how it's done.)
		for (SEntity e : c) {
			Sheep s = (Sheep) e;
			//System.out.println("Wubble " + getName() + " collided with sheep " + s.getName());
			
			Vector3f sheepVelocity = s.getNode().getLinearVelocity(null);
			Vector3f sheepDirection = s.getRotation().getRotationColumn(2);
			sheepDirection.normalizeLocal();
			sheepDirection = wubDirection;

			//sheepDirection = sheepDirection.mult(curVelocity.length() * 1.4f);
			//s.getNode().setLinearVelocity(curVelocity);
			sheepDirection = sheepDirection.mult(250);
			s.getNode().addForce(sheepDirection);
			//s.getNode().addForce(new Vector3f(0, 500, 0));
			// s.getNode().setAngularVelocity(new Vector3f(0,5,0));
		}
	}
	
	@Override
	public Quaternion getRotation() {
		return _visNode.getLocalRotation();
	}
	
	public void remove() {
		super.remove();
		_wubbleHash.remove(getName());
	}
}
