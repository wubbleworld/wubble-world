package edu.isi.wubble.physics.entity;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.input.InputHandler;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Box;
import com.jme.scene.shape.Cone;
import com.jme.scene.shape.Cylinder;
import com.jme.scene.shape.Sphere;
import com.jme.util.export.binary.BinaryImporter;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.StaticPhysicsNode;
import com.jmex.physics.geometry.PhysicsCylinder;

import edu.isi.wubble.JMEString;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.util.LispUtils;

public class EntityManager {

	protected HashMap<String,Entity> _entityMap;
	protected HashMap<Integer,Entity> _entityMapById;
	
	// dynamic information stored in this manager
	protected HashMap<String,DynamicEntity> _dynamicMap;
	protected HashMap<String,DynamicEntity> _updateMap;
	protected HashMap<String,DynamicEntity> _hiddenMap;
	protected TreeSet<String> _removedEntities;
	
	// static information stored in this manager
	protected HashMap<String,StaticEntity> _staticMap;
	
	// wubble information
	protected HashMap<String,WubbleEntity> _wubbleMap;
	
	// enemy information
	protected TreeSet<String> _enemySet;
	
	
	// Keep a pointer to the PhysicsSpace and InputHandler to make life
	// easier.
	protected Node         _rootNode;
	protected PhysicsSpace _physics;
	protected InputHandler _input;
	
	// World changing count.  This give us an indicator for how interesting
	// the current world is.
	protected double          _lastScore;
	protected HashSet<String> _worldChanges;
		
	public EntityManager(PhysicsSpace physics, InputHandler input, Node rootNode) {
		_physics  = physics;
		_input    = input;
		_rootNode = rootNode;
		
		_worldChanges = new HashSet<String>();
		
		_entityMap = new HashMap<String,Entity>();
		_entityMapById = new HashMap<Integer,Entity>();
		
		_dynamicMap = new HashMap<String,DynamicEntity>();
		_updateMap = new HashMap<String,DynamicEntity>();
		_hiddenMap = new HashMap<String,DynamicEntity>();
		
		_staticMap = new HashMap<String,StaticEntity>();
		
		_wubbleMap = new HashMap<String,WubbleEntity>();

		_removedEntities = new TreeSet<String>();
		_enemySet = new TreeSet<String>();
	}
	
	public PhysicsSpace getPhysicsSpace() {
		return _physics;
	}
	
	public InputHandler getInputHandler() {
		return _input;
	}
	
	public Node getRootNode() {
		return _rootNode;
	}
	
	public void addChange(String name) {
		_worldChanges.add(name);
	}
	
	public double howInteresting() {
		return _worldChanges.size();
	}
	
	public double getWorldDelta() {
		double d = Math.abs(howInteresting() - _lastScore);
		
		//getEntity("wubble").recordProperty("WorldInterestingLevel", new Double(d));
		return d;
	}
	
	public void addEntity(Entity e) {
		_entityMap.put(e.getName(), e);
		_entityMapById.put(e.getId(), e);
	}

	public void addDynamicEntity(DynamicEntity de) {
		if (_dynamicMap.containsKey(de.getName())) {
			System.out.println("**********DynamicEntity name collision! " + de.getName());
		}
		_dynamicMap.put(de.getName(), de);
	}
	
	public void addWubbleEntity(WubbleEntity we) {
		if (_wubbleMap.containsKey(we.getName())) {
			System.out.println("WubbleMap name collision!");
		}
		_wubbleMap.put(we.getName(), we);
	}

	public void addUpdateEntity(DynamicEntity de) {
		if (_updateMap.containsKey(de.getName())) {
			System.out.println("**********UpdateEntity name collision! " + de.getName());
		}
		_updateMap.put(de.getName(), de);
	}
	
	public void addHiddenEntity(DynamicEntity de) {
		if (_dynamicMap.containsKey(de.getName())) {
			_dynamicMap.remove(de.getName());
		}
		
		if (_hiddenMap.containsKey(de.getName())) {
			System.out.println("**********HiddenEntity name collision! " + de.getName());
		}
		_hiddenMap.put(de.getName(), de);
	}
	
	public void addStaticEntity(StaticEntity se) {
		if (_staticMap.containsKey(se.getName())) {
			System.out.println("******** StaticEntity name collision! " + se.getName());
		}
		_staticMap.put(se.getName(), se);
	}
	
	public void addEnemyEntity(EnemyEntity ee) {
		_enemySet.add(ee.getName());
	}

	public Collection<Entity> getAllEntities() {
		return _entityMap.values();
	}
	
	public Collection<DynamicEntity> getAllDynamicEntities() {
		return _dynamicMap.values();
	}
	
	public Collection<WubbleEntity> getWubbles() { 
		return _wubbleMap.values();
	}

	public Collection<DynamicEntity> getAllHidden() {
		return _hiddenMap.values();
	}

	public Collection<StaticEntity> getAllStatic() {
		return _staticMap.values();
	}

	public Entity getEntity(String name) {
		return _entityMap.get(name);
	}
	
	public Entity getEntity(int id) {
		return _entityMapById.get(id);
	}
	
	public DynamicEntity getDynamicEntity(String name) {
		return _dynamicMap.get(name);
	}
	
	public boolean hasEnemy(String name) {
		return _enemySet.contains(name);
	}
	
	public DynamicEntity makeVisible(String name) {
		DynamicEntity de = _hiddenMap.remove(name);
		_dynamicMap.put(name, de);
		return de;
	}
	
	/**
	 * loop over all of the alive dynamic entities and send
	 * them to the user.  Then send all of the deleted object
	 * names to the newly connected user.  Future updates
	 * will only be concerned with updating.
	 * @param id
	 */
	public void initialMessages(Short id, boolean hide) {
		for (DynamicEntity de : _dynamicMap.values()) {
			de.sendInitialMessages(id, false);
		}
		
		for (DynamicEntity de : _hiddenMap.values()) {
			de.sendInitialMessages(id, hide);
		}
		
		for (String name : _removedEntities) {
			InvokeMessage message = new InvokeMessage();
			message.setMethodName("removeEntity");
			message.setArguments(new Object[] { name });
			message.sendTo(id);
		}
	}
	
	public void preUpdate() {
		_lastScore = howInteresting();
		_worldChanges.clear();
		
		for (DynamicEntity de : getAllDynamicEntities())
			de.preUpdate();
	}
	
	/**
	 * iterate over the entities in the _updateMap and
	 * call their update functions.
	 * @param tpf
	 */
	public void updateEntities(float tpf) {
		for (DynamicEntity de : _updateMap.values()) {
			de.update(tpf);
		}
	}
	
	/**
	 * iterate and be sure to clear anything out that
	 * has the capability of being moved regardless of
	 * whether or not it actually has been moved.
	 */
	public void removeDynamicEntities() {
		for (PhysicsEntity de : _dynamicMap.values()) {
			DynamicPhysicsNode node =  (DynamicPhysicsNode) de.getNode();
			node.delete();
			node.removeFromParent();
		}
		
		_dynamicMap.clear();
		_hiddenMap.clear();
		_updateMap.clear();
		_removedEntities.clear();
	}

	public String removeEntity(String name) {
		if (!_dynamicMap.containsKey(name)) {
			System.out.println("*******Unknown remove entity " + name);
			return null;
		}
		PhysicsEntity pe = _dynamicMap.remove(name);
		
		// Have the DEs clean up after themselves.
		if (pe instanceof DynamicEntity) {
			DynamicEntity de = (DynamicEntity)pe;
			de.remove();
		}

		_removedEntities.add(pe.getName());
		return pe.getName();
	}

	public void removeEnemyEntity(String name) {
		if (!_dynamicMap.containsKey(name)) {
			System.out.println("******Unknown removeEnemyEntity " + name);
			return;
		}
		
		_enemySet.remove(name);
		_updateMap.remove(name);
		EnemyEntity ee = (EnemyEntity) _dynamicMap.remove(name);
		ee.getPhysicsNode().delete();
		ee.getNode().removeFromParent();
	}

	public void removeWubbles() {
		for (ActiveEntity de : _wubbleMap.values()) {
			de.remove();
//			de._physicsNode.delete();
//			de.getNode().removeFromParent();
			
			_dynamicMap.remove(de.getName());
		}
		_wubbleMap.clear();
	}

	public void removeEnemyEntities() {
		for (String enemy : _enemySet) {
			EnemyEntity ee = (EnemyEntity) _dynamicMap.remove(enemy);
			if (ee != null) {
				ee.remove();
//				ee.getPhysicsNode().delete();
//				ee.getNode().removeFromParent();
			}
		}
		_enemySet.clear();
	}
	
	/**
	 * create the base static entity.  Useable by other functions to extend
	 * and add their own components below and then generate geometry on them
	 * @param name
	 * @param ps
	 * @return
	 */
	public Entity createStaticEntity(String name) {
		Entity se = new Entity(this, name, false);
		StaticPhysicsNode node = _physics.createStaticNode();
		node.setName(name);
		se.setNode(node);
		
		return se;
	}
	
	public void createEntity(URL url, boolean accuracy) {
		try {
			Node node = (Node) BinaryImporter.getInstance().load(url);
			createEntity(node, accuracy);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void createEntity(Spatial s, boolean accuracy) {
		createEntity(s, accuracy, false);
	}
	
	public Entity createEntity(Spatial s, boolean accuracy, boolean clickable) {
		Entity se = new Entity(this, s.getName(), true);
		StaticPhysicsNode node = _physics.createStaticNode();
		
		// move the static node to the desired position and reset
		// the spatial's location.
		node.setLocalTranslation(s.getLocalTranslation());
		s.setLocalTranslation(new Vector3f());
		
		node.setName(s.getName());
		node.attachChild(s);
		node.generatePhysicsGeometry(accuracy);
		se.setNode(node);
		se.setPickable(clickable);
		
		addEntity(se);
		return se;
	}
	
	public Box createBox(String name, float xLen, float yLen, float zLen) {
		Box b = new Box(name, new Vector3f(0,0,0), xLen/2.0f, yLen/2.0f, zLen/2.0f);
		b.setModelBound(new BoundingBox());
		b.updateModelBound();
		
		return b;
	}
	
	public Sphere createSphere(String name, int zSamples, int radialSamples, float radius) {
		Sphere s = new Sphere(name, new Vector3f(0,0,0), zSamples, radialSamples, radius);
		s.setModelBound(new BoundingSphere());
		s.updateModelBound();
		
		return s;
	}
	
	public Cone createCone(String name, float width, float height) {
		Cone c = new Cone(name, 10, 20, width/2.0f, height);
		c.setModelBound(new BoundingBox());
		c.updateModelBound();
		
		return c;
	}
	
	public Cylinder createCylinder(String name, float width, float height) {
		Cylinder c = new Cylinder(name, 10, 20, width/2.0f, height, true);
		c.setModelBound(new BoundingBox());
		c.updateModelBound();

		return c;
	}
	
	
	/**
	 * create a box that cannot be moved.
	 * @param name
	 * @param ps
	 * @param length
	 * @return
	 */
	public Entity createStaticBox(String name, Vector3f length) {
		Entity se = createStaticEntity(name);
		se.getNode().attachChild(createBox(name, length.x, length.y, length.z));
		((StaticPhysicsNode) se.getNode()).generatePhysicsGeometry();
		
		se.getNode().setUserData("geometry-type", new JMEString("cube"));
		se.getNode().setUserData("size", new JMEString(LispUtils.toLisp(length)));
		
		se.setPickable(true);
		return se;
	}	
	
	/**
	 * initialize the important parts of the entity.
	 * @param name
	 * @param ps
	 * @return
	 */
	private DynamicEntity createDynamicEntity(String name, Vector3f size) {
		DynamicEntity de = new DynamicEntity(this, name, true);
		DynamicPhysicsNode dpn = _physics.createDynamicNode();
		dpn.setName(name);
		dpn.getLocalScale().set(size);

		de.setNode(dpn);
		de.setPhysicsNode(dpn);
		de.setPickable(true);
		return de;
	}
	
	public DynamicEntity createDynamicBox(String name, Vector3f length) {
		DynamicEntity de = createDynamicEntity(name, length);
		
		de.getPhysicsNode().getLocalScale().set(length);
		de.getPhysicsNode().attachChild(createBox(name, 1,1,1));
		de.getPhysicsNode().generatePhysicsGeometry();

		de.getNode().setUserData("GeometryType", new JMEString("cube"));
		de.getNode().setUserData("Size", new Vector3f(length));
		return de;
	}
	
	public PhysicsEntity createDynamicSphere(String name, float diameter) {
		PhysicsEntity de = createDynamicEntity(name, new Vector3f(1,1,1));
		
		de.getPhysicsNode().attachChild(createSphere(name, 20, 20, diameter/2.0f));
		de.getPhysicsNode().generatePhysicsGeometry();
		
		de.getNode().setUserData("GeometryType", new JMEString("sphere"));
		de.getNode().setUserData("Size", new Vector3f(diameter,diameter,diameter));
		return de;
	}
	
	public PhysicsEntity createDynamicCone(String name, float width, float height) {
		PhysicsEntity de = createDynamicEntity(name, new Vector3f(1,1,1));

		de.getPhysicsNode().attachChild(createCone(name, width, height));
		de.getPhysicsNode().generatePhysicsGeometry();
		de.getPhysicsNode().getLocalRotation().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);

		de.getNode().setUserData("GeometryType", new JMEString("cone"));
		de.getNode().setUserData("Size", new Vector3f(width,height,width));
		
		return de;
	}
	
	public PhysicsEntity createDynamicCylinder(String name, float width, float height) {
		PhysicsEntity de = createDynamicEntity(name, new Vector3f(1,1,1));

		de.getPhysicsNode().attachChild(createCylinder(name, width, height));
		de.getPhysicsNode().generatePhysicsGeometry();
		de.getPhysicsNode().getLocalRotation().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);

		de.getNode().setUserData("GeometryType", new JMEString("cylinder"));
		de.getNode().setUserData("Size", new Vector3f(width,height,width));

		return de;
	}	
	
	public void createArrow() {
		DynamicPhysicsNode dpn = _physics.createDynamicNode();
		dpn.setName("arrow");
		
		PhysicsCylinder pc = dpn.createCylinder("arrow");
		pc.setLocalScale(new Vector3f(0.1f,0.1f,0.45f));
		pc.updateWorldVectors();
		
		dpn.computeMass();
		
		PhysicsEntity de = new DynamicEntity(this, "arrow", true);
		de.setNode(dpn);
		de.setPhysicsNode(dpn);
	}	
}
