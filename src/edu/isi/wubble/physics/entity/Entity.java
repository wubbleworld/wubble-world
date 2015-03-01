package edu.isi.wubble.physics.entity;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import com.jme.bounding.BoundingBox;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.TriMesh;
import com.jme.scene.shape.Box;
import com.jme.scene.state.MaterialState;
import com.jme.system.DisplaySystem;
import com.jme.util.export.Savable;

import edu.isi.wubble.physics.CollisionInfo;
import edu.isi.wubble.physics.TimeManager;
import edu.isi.wubble.physics.state.AutoOffFluent;
import edu.isi.wubble.physics.state.Fluent;
import edu.isi.wubble.physics.state.StateDatabase;
import edu.isi.wubble.util.ColorHSB;
import edu.isi.wubble.util.Globals;
import edu.isi.wubble.util.LispUtils;
import edu.isi.wubble.util.SWIFTContactInfo;

public class Entity {
	public static final int SAMPLE_RATE = 15;
	
	public static HashMap<String,ColorRGBA> _colorMap;
	static {
		_colorMap = new HashMap<String,ColorRGBA>();
		_colorMap.put("red", ColorRGBA.red);
		_colorMap.put("green", ColorRGBA.green);
		_colorMap.put("yellow", ColorRGBA.yellow);
		_colorMap.put("orange", ColorRGBA.orange);
		_colorMap.put("blue", ColorRGBA.blue);
		_colorMap.put("black", ColorRGBA.black);
		_colorMap.put("white", ColorRGBA.white);
	}
	protected static Random _generator = new Random();
	
	private static short _count = 1;
	
	private String    _name;
	private int       _uniqueId;
	private Node      _node;
	private ColorRGBA _color;
	private ColorHSB  _hsbColor;
	
	protected boolean _liftable;
	protected boolean _pickable;
	
	protected EntityManager _entityManager;
	
	// you store things in here that you don't want to store
	// anywhere else (things that aren't changing, but you don't
	// want logged in properties.
	protected HashMap<String,String>                 _userData;
	
	protected TreeMap<String,Fluent>                 _properties;
	protected TreeMap<String,TreeMap<String,Fluent>> _relations;
	
	// stores the nearest points from one entity to another entity.  
	protected TreeMap<Integer, SWIFTContactInfo> _swiftMap;
	
	protected ArrayList<TriMesh> _meshes;

	public Entity(EntityManager entityManager, String name, boolean makeUnique) {
		_entityManager = entityManager;
		_name          = name;
		_liftable = false;
		_pickable = false;
		
		_userData = new HashMap<String,String>();

		_properties = new TreeMap<String,Fluent>();
		_relations = new TreeMap<String,TreeMap<String,Fluent>>();
		
		_swiftMap = new TreeMap<Integer,SWIFTContactInfo>();
		
		if (!Globals.USE_DATABASE) 
			_uniqueId = _count++;
		else
			useDatabase(makeUnique);
		
		_entityManager.addEntity(this);
	}
	
	public EntityManager getManager() {
		return _entityManager;
	}
	
	public void setEntityManager(EntityManager em) {
		_entityManager = em;
	}

	public int getId() {
		return _uniqueId;
	}
	
	public String getName() {
		return _name;
	}
	
	public ArrayList<TriMesh> getMeshes() {
		if (_meshes == null) {
			fillMeshes();
		}
		return _meshes;
	}
	
	/**
	 * to be overridden by lower classes 
	 * so that the meshes are generated
	 * properly
	 * @return
	 */
	protected void fillMeshes() { }
	
	/**
	 * EntityType will be used to distinguish
	 * static entities from dynamic entities.
	 * There are clever ways to discover them
	 * based on the data, but later you can do that
	 * if you want.
	 * @return
	 * 		static
	 *      dynamic
	 *      wubble
	 *      autonmous
	 */
	public String getEntityType() {
		return "unknown";
	}
	
	public Node getNode() {
		return _node;
	}
	
	public void setNode(Node n) {
		_node = n;
	}
	
	public ColorRGBA getColor() {
		return _color;
	}
	
	public ColorHSB getColorHSB() {
		return _hsbColor;
	}
	
	public void setColor(ColorRGBA color) {
		_color = color;
		_hsbColor = new ColorHSB(color);
		
        MaterialState ms = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
        ms.setDiffuse(color);
        ms.setAmbient(ColorRGBA.black);
        ms.setEmissive(ColorRGBA.black);
        getNode().setRenderState(ms);
        getNode().updateRenderState();
	}
	
	
	public void setPosition(Vector3f pos) {
		getNode().setLocalTranslation(pos);
	}
		
	public Vector3f getPosition() {
		return getNode().getLocalTranslation();
	}
	
	public Quaternion getRotation() {
		return getNode().getLocalRotation();
	}
	
	public boolean isChanging() {
		return false;
	}
	
	/**
	 * the wubble entity will be overloading this
	 * since its size is not determined by the 
	 * node but instead by the visual node.
	 * @return
	 */
	public Vector3f getSize() {
		//Object stored = getNode().getUserData("size");
		//if (stored == null) 
			return getNode().getLocalScale();
		//else
		// 	return (Vector3f) stored;
	}
	
	public void setSize(Vector3f size) {
		getNode().setLocalScale(size);
	}

	public boolean isPickable() {
		return _pickable;
	}
	
	public void setPickable(boolean pickable) {
		_pickable = pickable;
	}
	
	public void setUserData(String prop, String value) {
		_userData.put(prop, value);
	}
	
	public String getUserData(String key) {
		return _userData.get(key);
	}
	
	/**
	 * overridden by dynamic entities to store off the collision
	 * put here so that in case we someday care about collisions
	 * with static entities (right now we don't)
	 * @param collidingEntity
	 * @param contactInfo
	 */
	public void onCollision(Entity collidingEntity, CollisionInfo contactInfo) {
		
	}
	
	/**
	 * called by SWIFT to set up this contact info.  Contact
	 * info is shared between two entities, so you should never
	 * call this function outside of SWIFT.
	 * @param otherEntity
	 * @param info
	 */
	public void setSWIFT(Entity otherEntity, SWIFTContactInfo info) {
		_swiftMap.put(otherEntity.getId(), info);
	}
	
	public SWIFTContactInfo getSWIFT(Entity otherEntity) {
		return _swiftMap.get(otherEntity.getId());
	}
	
	/**
	 * attach point will return the correct node to attach
	 * children to this entity.  We do it this way in order
	 * to continue to use getNode() as it exists currently
	 * @return
	 */
	public Node getAttachPoint() {
		return getNode();
	}
	
	/**
	 * this is the inital lisp message that sets up the static
	 * properties of this object.  I use the _node's UserData 
	 * so if you plan on using this function make sure you 
	 * have properly configured your derived entities.
	 * @return
	 */
	public boolean lispInitMessage(StringBuffer msg) {
		msg.append("(");
		msg.append("(name " + getName() + ")");
		msg.append("(color " + getNode().getUserData("color") + ")");
		msg.append("(geometry-type " + getNode().getUserData("geometry-type") + ")");
		msg.append("(size " + getNode().getUserData("size") + ")");
		msg.append("(position " + LispUtils.toLisp(getPosition()) + ")");
		msg.append(")");
		return true;
	}
	
	public StringBuffer xmlInitMessage() {
		StringBuffer msg = new StringBuffer();
		msg.append("<object name=\"" + getName() + ">");
		msg.append("<color>" + getNode().getUserData("color") + "</color>");
		msg.append("<geometryType>" + getNode().getUserData("geometry-type") + "</geometryType>");
		msg.append("<size>" + getNode().getUserData("size") + "</size>");
		msg.append("<position>" + toXML(getPosition()) + "</position>");
		msg.append("<rotation>" + toXML(getRotation()) + "</rotation>");
		msg.append("</object>");
		return msg;
	}
	
	public void addInitialProperties() {
		// these are sampled, because they change so often
		recordSample("Position", new Vector3f(getPosition()));
		recordSample("Rotation", new Quaternion(getRotation()));

		record("EntityType", getEntityType());
		record("Size", getSize());
		record("Color", _color);
		record("HSBColor", _hsbColor);
		record("GeometryType", getNode().getUserData("geometry-type"));
	}
	
	/**
	 * record the property as a fluent.
	 * @param name
	 * @param value
	 */
	public void record(String name, Object value) {
		Fluent ep = _properties.get(name);
		if (ep == null) {
			_properties.put(name, new Fluent(name, this, value));
		} else {
			ep.update(value);
		}
	}
	
	public void closeFluent(String name) {
		Fluent ep = _properties.get(name);
		if (ep != null) {
			ep.closeDB();
		}
	}
	
	public void record(String name, Vector3f vec) {
		Fluent ep = _properties.get(name);
		if (ep == null) {
			_properties.put(name, new Fluent(name, this, new Vector3f(vec)));
		} else {
			ep.update(vec);
		}
	}
	
	public void record(String name, Quaternion q) {
		Fluent ep = _properties.get(name);
		if (ep == null) {
			_properties.put(name, new Fluent(name, this, new Quaternion(q)));
		} else {
			ep.update(q);
		}
	}
	
	public void recordAuto(String name, Object value) {
		Fluent ep = _properties.get(name);
		if (ep == null) {
			_properties.put(name, new AutoOffFluent(name, this, value));
		} else {
			ep.update(value);
		}
	}
	
	/**
	 * record sample will only record once per sample rate.
	 * this is a shitty hack that keeps me from creating a ton
	 * of position, rotation and distance updates.
	 * @param name
	 * @param value
	 */
	public void recordSample(String name, Object value) {
		if (TimeManager.inst().getLogicalTime() % SAMPLE_RATE != 0) 
			return;
		record(name, value);
	}
	
	public void recordSample(String name, Vector3f value) {
		if (TimeManager.inst().getLogicalTime() % SAMPLE_RATE != 0) 
			return;
		record(name, value);
	}
	
	public void recordSample(String name, Quaternion value) {
		if (TimeManager.inst().getLogicalTime() % SAMPLE_RATE != 0) 
			return;
		record(name, value);
	}
	/**
	 * useDatabase will rely on a backend database to assign unique
	 * numerical identifiers to the entities in the scene.
	 * @param makeUnique
	 */
	private void useDatabase(boolean makeUnique) {
		try {
			if (makeUnique) {
				_uniqueId = StateDatabase.inst().getNextId("entity_table");
				StateDatabase.inst().addEntity(getName(), _uniqueId);
			} else {
				Statement s = StateDatabase.inst().getStatement();
				String sql = "select id from entity_table where name = '" + getName() + "'";
				ResultSet rs = s.executeQuery(sql);
				if (rs.next()) {
					// from here out we use the same value
					// as this table gets big, this will be a 
					// potential concern for slowdown
					_uniqueId = rs.getInt(1);
				} else {
					// this is the first time we are seeing this
					// so we do have to record at least once.
					useDatabase(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void cleanup() {
		for (Fluent ep : _properties.values()) 
			ep.closeDB();

		for (TreeMap<String,Fluent> map : _relations.values())
			for (Fluent ep : map.values())
				ep.closeDB();
		
	}
	
	public void remove() {
		Node n = getNode();
		if (n != null) { 
			_entityManager.getRootNode().detachChild(n);
			n.removeFromParent();
			n.detachAllChildren();
		}
		
		cleanup();
	}
	
	public static Box createBox(String name, float xLen, float yLen, float zLen) {
		Box b = new Box(name, new Vector3f(0,0,0), xLen/2.0f, yLen/2.0f, zLen/2.0f);
		b.setModelBound(new BoundingBox());
		b.updateModelBound();
		
		return b;
	}
	
	public static String toXML(Vector3f vec) {
		return vec.x + " " + vec.y + " " + vec.z;
	}
	
	public static String toXML(Quaternion quat) {
		return quat.x + " " + quat.y + " " + quat.z + " " + quat.w;
	}
}
