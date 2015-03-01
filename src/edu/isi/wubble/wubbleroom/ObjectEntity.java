package edu.isi.wubble.wubbleroom;

import java.util.HashMap;
import java.util.Random;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.shape.Cone;
import com.jme.scene.shape.Cylinder;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.MaterialState;
import com.jme.system.DisplaySystem;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.PhysicsUpdateCallback;

import edu.isi.wubble.JMEString;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.util.LispUtils;

public class ObjectEntity extends DynamicEntity implements PhysicsUpdateCallback {
	public static final float[] SIZES = new float[] { 
		0.5f, 1.0f, 1.5f, 2.0f, 2.5f 
	};
	public static String[] COLORS = new String[] {
		"red", "green", "yellow", "orange", "blue", "black", "white"
	};
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

	private static Random GENERATOR = new Random();
	
	protected Quaternion _uprightQuaternion;

	public ObjectEntity(EntityManager em, String name) {
		super(em, name, true);
		
		_uprightQuaternion = new Quaternion();
	}
	
	public void applyRandomSize(Vector3f axis, int min, int max) {
		Vector3f tmp = axis.mult(SIZES[GENERATOR.nextInt(max-min) + min]);
		getNode().getLocalScale().addLocal(tmp);
	}
	
	public void applyRandomColor() {
		MaterialState ms = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
		String color = COLORS[GENERATOR.nextInt(_colorMap.size())];
		ms.setDiffuse(_colorMap.get(color));
		ms.setAmbient(ColorRGBA.black);
		ms.setEmissive(ColorRGBA.black);
		ms.setShininess(0);	

		getNode().setUserData("color", new JMEString(color));
		getNode().setRenderState(ms);
		getNode().updateRenderState();
	}
	
	public void setUprightQuaternion(Quaternion q) {
		_uprightQuaternion = q;
	}
	
	public void beforeStep(PhysicsSpace ps, float time) {
		
	}
	
	public void afterStep(PhysicsSpace ps, float time) {
		getNode().setLocalRotation(new Quaternion(_uprightQuaternion));
	}
	
	/**
	 * initialize the important parts of the entity.
	 * @param name
	 * @param ps
	 * @return
	 */
	protected static ObjectEntity createEntity(EntityManager em, String name) {
		ObjectEntity de = new ObjectEntity(em, name);
		DynamicPhysicsNode dpn = em.getPhysicsSpace().createDynamicNode();
		dpn.setName(name);
		de.setNode(dpn);
		de.setPhysicsNode(dpn);
		
		em.getPhysicsSpace().addToUpdateCallbacks(de);
		de.setPickable(true);
		return de;
	}
	
	public static ObjectEntity createDynamicBox(EntityManager em, String name, Vector3f length) {
		ObjectEntity de = createEntity(em, name);
		de.getNode().attachChild(createBox(name, length.x, length.y, length.z));
		((DynamicPhysicsNode) de.getNode()).generatePhysicsGeometry();

		de.getNode().setUserData("geometry-type", new JMEString("cube"));
		de.getNode().setUserData("size", new JMEString(LispUtils.toLisp(length)));
		return de;
	}
	
	public static ObjectEntity createSphere(EntityManager em, String name, float diameter) {
		ObjectEntity de = createEntity(em, name);
		DynamicPhysicsNode dpn = (DynamicPhysicsNode) de.getNode();
		
		Sphere s = new Sphere(name, 20, 20, diameter/2.0f);
		s.setModelBound(new BoundingSphere());
		s.updateModelBound();
		dpn.attachChild(s);
		dpn.generatePhysicsGeometry();
		dpn.setUserData("geometry-type", new JMEString("sphere"));
		dpn.setUserData("size", new JMEString("(" + diameter + " " + diameter + " " + diameter +")"));
		return de;
	}
	
	public static ObjectEntity createCone(EntityManager em, String name, float width, float height) {
		ObjectEntity de = createEntity(em, name);
		DynamicPhysicsNode dpn = (DynamicPhysicsNode) de.getNode();

		Cone c = new Cone(name, 10, 20, width/2.0f, height);
		c.setModelBound(new BoundingBox());
		c.updateModelBound();
		dpn.attachChild(c);
		dpn.generatePhysicsGeometry();
		dpn.getLocalRotation().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
		dpn.setUserData("geometry-type", new JMEString("cone"));
		dpn.setUserData("size", new JMEString("(" + width + " " + height + " " + width +")"));
		
		de.setUprightQuaternion(new Quaternion(dpn.getLocalRotation()));
		return de;
	}
	
	public static ObjectEntity createCylinder(EntityManager em, String name, float width, float height) {
		ObjectEntity de = createEntity(em, name);
		DynamicPhysicsNode dpn = (DynamicPhysicsNode) de.getNode();

		Cylinder c = new Cylinder(name, 10, 20, width/2.0f, height, true);
		c.setModelBound(new BoundingBox());
		c.updateModelBound();
		dpn.attachChild(c);
		dpn.generatePhysicsGeometry();

		dpn.getLocalRotation().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
		dpn.setUserData("geometry-type", new JMEString("cylinder"));
		dpn.setUserData("size", new JMEString("(" + width + " " + height + " " + width +")"));

		de.setUprightQuaternion(new Quaternion(dpn.getLocalRotation()));
		return de;
	}
}
