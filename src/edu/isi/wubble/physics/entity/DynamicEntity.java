package edu.isi.wubble.physics.entity;

import static edu.isi.wubble.jgn.message.InvokeMessage.createMsg;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.captiveimagination.jgn.synchronization.message.Synchronize3DMessage;
import com.jme.input.KeyInput;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsNode;

import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.physics.TimeManager;
import edu.isi.wubble.physics.entity.controller.EntityController;
import edu.isi.wubble.physics.entity.controller.JumpController;
import edu.isi.wubble.physics.entity.controller.LiftingController;
import edu.isi.wubble.physics.entity.controller.LookAtController;
import edu.isi.wubble.physics.entity.controller.MoveToController;
import edu.isi.wubble.physics.entity.controller.MovementController;
import edu.isi.wubble.physics.entity.controller.PairwiseUpdateController;
import edu.isi.wubble.physics.entity.controller.RotationController;
import edu.isi.wubble.physics.entity.controller.SoloUpdateController;
import edu.isi.wubble.physics.entity.controller.SpringController;
import edu.isi.wubble.physics.entity.controller.VelocityController;
import edu.isi.wubble.physics.state.AutoOffFluent;
import edu.isi.wubble.physics.state.Fluent;
import edu.isi.wubble.util.Globals;
import edu.isi.wubble.util.LispUtils;
import edu.isi.wubble.util.SWIFTContactInfo;

public class DynamicEntity extends PhysicsEntity {

	protected static short _serverCount = 0;
	protected short _serverId;
	
	protected boolean _isChanging;
	
	protected TreeMap<String, TreeMap<String,Object>> _derivedMap;

	protected PairwiseUpdateController _pairwiseController;

	protected ArrayList<String> _nearSet;
	
	TreeMap<Entity, Float> _distanceMap;
		
	public DynamicEntity(EntityManager em, String name, boolean makeUnique) {
		super(em, name, makeUnique);
		_distanceMap = new TreeMap<Entity, Float>();
		
		postConstruct();
	}
	
	// Auxiliary stuff for making a DynamicEntity, Wes-style.
	void postConstruct() {
		super.postConstruct();
		
		_liftable = true;
		_serverId = _serverCount++;
		
		_derivedMap = new TreeMap<String,TreeMap<String,Object>>();
		_nearSet = new ArrayList<String>();

		_entityManager.addDynamicEntity(this);

	}
	
	// This is to follow Shane's Entity constructor path.
	public DynamicEntity(String name, Vector3f pos) {
		super(name, pos);
		postConstruct();
	}
	
	public String getEntityType() {
		return "dynamic";
	}
	
	public void setPhysicsNode(PhysicsNode node) {
		super.setPhysicsNode(node);
		setupCollisions(false);
		addDefaultControllers();
	}
	
	public DynamicPhysicsNode getPhysicsNode() {
		return (DynamicPhysicsNode) super.getPhysicsNode();
	}
	
	public void setPosition(Vector3f pos) {
		getPhysicsNode().setLocalTranslation(pos.x, pos.y, pos.z);
	}
	
	public void think() {
		// override in lower classes to make decisions about this
		// agent and what it will do
		// this is called every update round
	}
	
	public void addLoggingControllers() {
		// we don't add this to the actual controller list
		// because it is a delayed update.
		_pairwiseController = new PairwiseUpdateController(this);

//		NearSphereController nsc = new NearSphereController(this);
//		nsc.setActive(true);
//		addController(nsc);
		
		SoloUpdateController suc = new SoloUpdateController(this);
		suc.setActive(true);
		addController(suc);
	}
	
	protected void addDefaultControllers() {
		if (Globals.LOG_DYNAMIC) 
			addLoggingControllers();
		
		VelocityController vc = new VelocityController(this);
		addController(vc);
		
		SpringController sc = new SpringController(this);
		addController(sc);
		
		LookAtController lc = new LookAtController(this);
		addController(lc);
		
		MoveToController mtc = new MoveToController(this);
		addController(mtc);
		
		LiftingController lift = new LiftingController(this);
		addController(lift);
	}
	
	public void addController(EntityController c) {
		getNode().addController(c);
		String controlName = c.getClass().getName();
		if (Globals.SHANE_PRINTING)
			System.out.println("Adding controller " + controlName + " to " + getName());
		_controlMap.put(controlName, c);
	}
	
	public Controller removeController(String name) {
		Controller tgt = getController(name);
		if (tgt != null) { getNode().removeController(tgt); }
		return tgt;
	}
	
	// This will save me soooo much typing...
	public PairwiseUpdateController getPUC() { return _pairwiseController; }
	
	public Controller getController(String name) {
		// Since the pairwiseUpdateController is not explicitly added to the node, 
		// check for the special case.
		if (name.equals(PairwiseUpdateController.class.getName())) {
			return _pairwiseController;
		}
		else { 
			if (_controlMap != null) { return _controlMap.get(name); }
			else { return null;} 
		}
	}
	
	/**
	 * call cleanup on the controllers so that we make sure to kill any errant 
	 * threads.
	 */
	protected void cleanup() {
		super.cleanup();
		
		// SUC isn't in the control map, so have to clean it up separately.
		if (_pairwiseController != null) { 
			_pairwiseController.cleanup();
		}
		
		for (EntityController c : _controlMap.values()) 
			c.cleanup();
	}
	
	
	@Override
	public void remove() {
		super.remove();
		cleanup();
	}
	

	public Vector3f getSize() {
		return _physicsNode.getLocalScale();
	}	
	
	/** 
	 * I assume that we will be wanting to actually consider distances
	 * between the physical objects and not their visual conterparts.
	 */
	protected void fillMeshes() {
		System.out.println("Filling Meshes [" + getName() + "]");
		_meshes = new ArrayList<TriMesh>();
		for (Spatial s : _physicsNode.getChildren()) {
			if (s instanceof TriMesh) {
				_meshes.add((TriMesh) s);
			}
		}
	}
	
	/**
	 * preUpdate sets up all of the recordable information.  The active fluents
	 * are prepared so that checks can be made and the different recordable features
	 * are reset for the next update cycle.  This method is called before
	 * any physics updates or anything else.
	 */
	public void preUpdate() {
		
		// Shane: Nothing lower in the class hierarchy ever REALLY had pre/post/delayed updates.  
		// I removed them to make this explicit, and to reduce code-confusion.  If you want 
		// PhysicsEntity to have pre/delayed/post feel free to add it back in.
		// super.preUpdate();
		_nearSet.clear();
		_collisionMap.clear();
		_derivedMap.clear();
		
		for (TreeMap<String,Fluent> map : _relations.values()) {
			for (Fluent er : map.values()) {
				er.preUpdate();
			}
		}
		
		for (Fluent ep : _properties.values()) {
			ep.preUpdate();
		}
		
		_isChanging = false;
	}
	
//	EntityController _mutualFOVController;
	
	/**
	 * delayed update is used to update once everyone has finished doing 
	 * their initial updates.  This allows you to do specialized updates
	 * that rely on other entities finishing their updating.
	 * 
	 * Every dynamic entity has delayedUpdate called on it before
	 * post update is called.  This allows *all* entities to process their
	 * updates before we condense information between them in the post update
	 * step.
	 * 
	 * A good example of this is collisions.  We don't want to process
	 * *any* entities collisions until all collisions have been logged.
	 * The logging of collisions occurs in the delayedUpdate step and
	 * the condensing of data occurs in the postUpdate step.
	 */	
	public void delayedUpdate() {
		// super.delayedUpdate();
		if (_pairwiseController != null) 
			_pairwiseController.update(0.0f);
		
		// Probably should add a list of these things - of post-update controllers - 
		// but for now I'll just lay them out explicitly.
//		if (_mutualFOVController != null) {
//			_mutualFOVController.update(0.0f);
//		}
	}
	
	/**
	 * post update finalizes all of the recordable information.  
	 */
	public void postUpdate() {
		// super.postUpdate();
		// post update for relations and properties only affect
		// auto record relations and properties.  Auto record says
		// that in order to stay active, you have to update the value
		// each time tick.  if not, it is assumed that it is turned off
		// and a false is recorded.
		for (TreeMap<String,Fluent> map : _relations.values()) {
			for (Fluent er : map.values()) {
				er.postUpdate();
			}
		}
		
		for (Fluent ep : _properties.values()) {
			ep.postUpdate();
		}
		
		// clear out the distances in preparation for the next round.
		// on second thought... don't clear out the distances in
		// case someone isn't moving.  They should just stay the
		// same or be overwritten.
//		_distanceMap.clear();
	}
	
	public void update(float tpf) {
		// nodes with controllers are automagically updated by something.
		// it must be internal in the engine when processing the
		// root node.  Or hidden from me in the code somewhere
	}
	
	public boolean isChanging() {
		return _isChanging;
	}
	
	public void setChanging(boolean changing) {
		_isChanging = changing;
	}

	public boolean isResting() {
		if (_physicsNode == null) {
			System.out.println("fuck me I'm null: " + getName());
			return false;
		}
		return _physicsNode.isResting();
	}
	
	public void fixOrientation() {
		getNode().setLocalRotation(new Quaternion());
	}
	
	public short getServerId() {
		return _serverId;
	}
	
	public Synchronize3DMessage generateSyncMessage() {
		Synchronize3DMessage sync = new Synchronize3DMessage();
		sync.setSyncObjectId(_serverId);
		
		Vector3f pos = getPosition();
		sync.setPositionX(pos.x);
		sync.setPositionY(pos.y);
		sync.setPositionZ(pos.z);
	
		Quaternion rot = getRotation();
		sync.setRotationX(rot.x);
		sync.setRotationY(rot.y);
		sync.setRotationZ(rot.z);
		sync.setRotationW(rot.w);
		
		return sync;
	}
	
	/**
	 * this is a smaller message that just contains the updated 
	 * position when we are not resting.
	 * @return  if we modified the message (added ourself) then we 
	 * 		return true
	 */
	public boolean lispUpdateMessage(StringBuffer msg) {
		if (isResting()) 
			return false;
		
		msg.append("(");
		msg.append("(name " + getName() + ")");
		msg.append("(position " + LispUtils.toLisp(getPosition()) + ")");
		msg.append(")");
		return true;
	}
	
	public StringBuffer xmlUpdateMessage() {
		StringBuffer buf = new StringBuffer();
		if (isResting())
			return buf;
		
		buf.append("<object name=\"" + getName() + "\">");
		buf.append("<position>" + toXML(getPosition()) + "</position>");
		buf.append("<rotation>" + toXML(getRotation()) + "</rotation>");
		buf.append("<derived>");
		for (Map.Entry<String,TreeMap<String,Object>> var : _derivedMap.entrySet()) {
			buf.append("<object name=\"" + var.getKey() + "\">");
			for (Map.Entry<String, Object> entry : var.getValue().entrySet()) {
				buf.append("<" + entry.getKey() + ">" + entry.getValue() + "</" + entry.getKey() + ">");
			}
			buf.append("</object>");
		}
		buf.append("</object>");
		return buf;
	}
	
	public void logProperties() {
		
	}
	
	public void sendServerId(Short id) {
		InvokeMessage msg = createMsg("mapServerId", new Object[] { getName(), getServerId() });
		msg.sendTo(id);
	}
	
	/**
	 * send inital information about this dynamic entity
	 * to the provided client.  There are three initial
	 * messages per dynamic entity.  One for the mapping which
	 * will be used later for updates and one for each of 
	 * position and rotation.
	 * @param id
	 */
	public void sendInitialMessages(Short id, boolean hide) {
		sendServerId(id);
		
		InvokeMessage msg;
		if (hide) {
			msg = createMsg("initialPosition", new Object[] { getName(), new Vector3f(0,-10,0) });
		} else {
			msg = createMsg("initialPosition", new Object[] { getName(), getPosition() });
		}
		msg.sendTo(id);

		msg = createMsg("initialRotation", new Object[] { getName(), getRotation() });
		msg.sendTo(id);
	}	

	public void setDistance(Entity otherEntity, float d) {
		// See note below.
		if (_distanceMap != null) {	_distanceMap.put(otherEntity, d); }
	} 

	public float getDistance(Entity a) {
		SWIFTContactInfo info = getSWIFT(a);
		if (info == null) {
			
			// There are race conditions between when the first collision is called is when the 
			// distance map gets created post-constructor.  For the millionth time, this is bc Java's
			// ordering constrains on construction are RETARDED.  The right way to solve this is to move
			// the distancemap up the class hierarchy, and construct it before physics is created, but
			// that's too much work.
			if (_distanceMap != null) {	return _distanceMap.get(a); }
			else { return 10000; }
		}
		else { return info.getDistance(); }
	}

	public void record(Entity b, String name, Object value) {
		TreeMap<String,Fluent> map = _relations.get(b.getName());
		if (map == null) {
			map = new TreeMap<String,Fluent>();
			_relations.put(b.getName(), map);
		}
		
		Fluent er = map.get(name);
		if (er != null) {
			er.update(value);
		} else {	
			map.put(name, new Fluent(name, this, b, value));
		}
	}
	
	public void recordAuto(Entity b, String name, Object value) {
		TreeMap<String,Fluent> map = _relations.get(b.getName());
		if (map == null) {
			map = new TreeMap<String,Fluent>();
			_relations.put(b.getName(), map);
		}
		
		Fluent er = map.get(name);
		if (er != null) {
			er.update(value);
		} else {	
			map.put(name, new AutoOffFluent(name, this, b, value));
		}
	}
	
	public void recordSample(Entity b, String name, Object value) {
		if (TimeManager.inst().getLogicalTime() % SAMPLE_RATE != 0)
			return;
		record(b, name, value);
	}
	
	public void addNearEntity(String name) {
		_nearSet.add(name);
	}
	
	public void addMovementControls(boolean networked) {
		MovementController mc = new MovementController(this);
		mc.setActive(true);
		
		RotationController rc = new RotationController(this);
		rc.setActive(true);
		
		JumpController jc = new JumpController(this, true);
		jc.setActive(true);

		if (networked) {
			mc.addAutoBinding();
			rc.addAutoBinding();
			jc.addAutoBinding();
		} else {
			mc.addKeyBinding(KeyInput.KEY_W, KeyInput.KEY_S);
			rc.addKeyBinding(KeyInput.KEY_A, KeyInput.KEY_D);
			jc.addKeyBinding();
		}
		addController(mc);
		addController(rc);
		addController(jc);
	}
	
	public void addMovementControls(String pad, boolean networked) {
		MovementController mc = new MovementController(this);
		mc.setActive(true);
		
		RotationController rc = new RotationController(this);
		rc.setActive(true);
		
		JumpController jc = new JumpController(this, true);
		jc.setActive(true);

		if (networked) {
			mc.addAutoBinding(pad);
			rc.addAutoBinding(pad);
			jc.addAutoBinding(pad);
		} else {
			mc.addKeyBinding(KeyInput.KEY_W, KeyInput.KEY_S);
			rc.addKeyBinding(KeyInput.KEY_A, KeyInput.KEY_D);
			jc.addKeyBinding();
		}
		addController(mc);
		addController(rc);
		addController(jc);		
	}
}

