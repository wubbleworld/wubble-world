package edu.isi.wubble.physics.entity;

import java.util.ArrayList;

import com.jme.bounding.BoundingBox;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.shape.Box;
import com.jme.scene.state.CullState;
import com.jme.system.DisplaySystem;
import com.jmex.physics.StaticPhysicsNode;
import com.jmex.physics.material.Material;


public class StaticEntity extends PhysicsEntity {
	protected Vector3f _center;
	protected Vector3f _size;

	protected Quaternion _rotation;

	public StaticEntity(EntityManager entityManager, String name, boolean makeUnique) {
		super(entityManager, name, makeUnique);
		
		_rotation = new Quaternion();

		entityManager.addStaticEntity(this);
	}
	
	public StaticEntity(EntityManager entityManager, String name, Spatial s, boolean makeUnique) {
		super(entityManager, name, makeUnique);
		
		fromSpatial(s);
		
		_rotation = new Quaternion();

		entityManager.addStaticEntity(this);
	}
	
	
	public String getEntityType() {
		return "static";
	}
	
	public void fromSpatial(Spatial s) {
		s.setModelBound(new BoundingBox());
		s.updateModelBound();
		s.updateWorldBound();
		s.lockBounds();
		s.lockTransforms();
		s.lockBranch();

		BoundingBox bb = (BoundingBox) s.getWorldBound();
		_center = bb.getCenter();
		_size = bb.getExtent(null);
		_size.multLocal(2.0f);  // size is stored as full length
		
        CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
        cs.setCullMode(CullState.CS_BACK);

		s.setRenderState(cs);
		s.updateRenderState();

		StaticPhysicsNode node = _entityManager.getPhysicsSpace().createStaticNode();
		node.setName(getName());
		node.attachChild(s);
		node.generatePhysicsGeometry(true);
		node.setMaterial(Material.GRANITE);
		
		setPhysicsNode(node);
		setNode(node);
	}
	
	public Vector3f getPosition() {
		return _center;
	}
	
	public Quaternion getRotation() {
		return _rotation;
	}
	
	public Vector3f getSize() {
		return _size;
	}	
	
	protected void fillMeshes() {
		Box b = new Box(getName(), _center, _size.x*0.5f, _size.y*0.5f, _size.z*0.5f);
		_meshes = new ArrayList<TriMesh>();
		_meshes.add(b);

	}
}
