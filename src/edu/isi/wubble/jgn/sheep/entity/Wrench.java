package edu.isi.wubble.jgn.sheep.entity;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.intersection.TriangleCollisionResults;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.material.Material;

import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.physics.entity.controller.GoaltendingController;

public class Wrench extends SDynamicEntity {
	
	
	public Wrench(String name, int x, int y, int z) {
		// For now, no user controlling of wrenches.
		super(name, new Vector3f(x,y,z));
	}
	
	protected void makeBody() {
		String filename = "wrench.jme";
		Spatial body = Utils.loadModel(filename);
		
		// Set the name of this spatial to the name of the entity, to allow Entity::checkCollisions
		// to pull the right name for the mesh's parent.
		body.setName(getName());
		body.setModelBound(new OrientedBoundingBox());
		body.updateModelBound();
		// This is important for geometry-based collisions to work correctly
		for (Spatial child : ((Node) body).getChildren()) {
			child.setName(getName());
		}
		
		// Attach the body to the node, set the bounds and material.
		DynamicPhysicsNode p = getPhysicsNode();
		p.attachChild(body);
		p.setMaterial(Material.IRON);
		Utils.setColor(body, ColorRGBA.brown);
	}

	//
	// Wrenches need more accurate physics geometry, so this method is overridden.
	// ////////////////////////////////////////////////////////////
	protected void makePhysics() {
		// Is this where to put it?
		DynamicPhysicsNode p = getPhysicsNode();
		
		// Does this help?
		p.setModelBound(new OrientedBoundingBox());
		p.updateModelBound();
		
		p.generatePhysicsGeometry(true);
		p.computeMass();
	}
	
	
	@Override
	// Override this to also add the goaltending controller for wrenches.
	public void addLoggingControllers() {
		super.addLoggingControllers();
		
		GoaltendingController gtc = new GoaltendingController(this);
		gtc.setActive(true);
		addController(gtc);
	}

	@Override
	// ////////////////////////////////////////////////////////////
	protected void setCollisionResultsType() { _res = new TriangleCollisionResults(); }
}
