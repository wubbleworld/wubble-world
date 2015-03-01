package edu.isi.wubble.physics.entity;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;
import com.jmex.physics.DynamicPhysicsNode;


public class WubbleEntity extends ActiveEntity {

	protected ColorRGBA          _color;
	
	public WubbleEntity(EntityManager em, String name, boolean makeUnique) {
		super(em, name, makeUnique);
		
		_color = ColorRGBA.blue;
		_entityManager.addWubbleEntity(this);
		_entityManager.addUpdateEntity(this);
	}
	
	public String getEntityType() {
		return "wubble";
	}

	public ColorRGBA getColor() {
		return _color;
	}

	public void initWubbleEntity(Vector3f startPos) {
		createVisualNode();
		createPhysicsNode(startPos);
		
		Node parent = new Node(getName());
		parent.attachChild(_physicsNode);
		parent.attachChild(_visualNode);
		
		setNode(parent);
		setPhysicsNode(_physicsNode);
	}

	public void createVisualNode() {
		Box b = new Box(getName(), new Vector3f(0,0,0), 0.5f, 0.5f, 0.5f);
		b.setModelBound(new BoundingBox());
		
		_visualNode = new Node(getName());
		_visualNode.attachChild(b);
		_visualNode.setLocalScale(new Vector3f(0.25f, 0.25f, 0.25f));
		_visualNode.lookAt(new Vector3f(0,0,-1), new Vector3f(0,1,0));
	}
	
	public void createPhysicsNode(Vector3f startPos) {
		_physicsNode = _entityManager.getPhysicsSpace().createDynamicNode();
		getPhysicsNode().setName(getName());
		getPhysicsNode().createSphere(getName());
		getPhysicsNode().setLocalTranslation(startPos);
		getPhysicsNode().setLocalScale(new Vector3f(0.25f,0.25f,0.25f));
		getPhysicsNode().generatePhysicsGeometry();
		getPhysicsNode().computeMass();		
	}
}