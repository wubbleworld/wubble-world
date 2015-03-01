package edu.isi.wubble.physics;

import java.util.*;

import com.jme.math.Vector3f;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.PhysicsUpdateCallback;

public class SpringCorrector implements PhysicsUpdateCallback {
	
	public static float THRESHOLD = 0.5f;
	public static float THRESHOLD_SQUARED = 1.25f;
	
	protected static HashMap<String,SpringCorrector> _correctorMap = 
		new HashMap<String,SpringCorrector>();
	
	public float STIFFNESS = 8.0f;
	public static float DAMPING = 3.0f; 

	private String name;
	public String getName() { return name; }
	
	private PhysicsSpace ps;
	protected DynamicPhysicsNode node;
	protected Vector3f pos;

	protected SpringCorrector(String name, PhysicsSpace ps, DynamicPhysicsNode node, Vector3f pos) {
		this.name = name;
		this.node = node;
		this.pos = pos;
		this.ps = ps;
	}
	
	public static SpringCorrector create(String name, PhysicsSpace ps, DynamicPhysicsNode node, Vector3f pos) {
		if (_correctorMap.containsKey(name)) {
			SpringCorrector sc = _correctorMap.get(name);
			sc.setLocation(pos);
			return sc;
		}
		
		SpringCorrector sc = new SpringCorrector(name, ps, node, pos);
		_correctorMap.put(name, sc);
		ps.addToUpdateCallbacks(sc);
		return sc;
	}
	
	public static void deactivate(String name) {
		SpringCorrector sc = _correctorMap.get(name);
		if (sc != null) 
			sc.deactivate();
		else {
			System.out.println("Tried to deactivate uknown: " + name);
			System.out.println("known values:");
			for (String s : _correctorMap.keySet()) {
				System.out.println("..." + s);
			}
		}
	}
	
	public void deactivate() {
		ps.removeFromUpdateCallbacks(this);
		_correctorMap.remove(name);
	}
	
	public boolean isClose() {
		Vector3f currPos = new Vector3f(node.getLocalTranslation());
		Vector3f diff = pos.subtract(currPos);
		return diff.lengthSquared() <= THRESHOLD_SQUARED;
	}
	
	public void setLocation(Vector3f pos) {
		this.pos = pos;
	}
	
	public void afterStep(PhysicsSpace space, float time) {
		if (node.isResting()) {
			node.unrest();
		}
	}

	public void beforeStep(PhysicsSpace space, float time) {
		
		Vector3f currVel = node.getLinearVelocity(null);
		Vector3f currPos = new Vector3f(node.getLocalTranslation());

		Vector3f forceSpring = pos.subtract(currPos).mult(STIFFNESS);
		Vector3f dampSpring = currVel.mult(DAMPING); 

		node.addForce(forceSpring.subtract(dampSpring));
		
	}

}