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

public class AutoStaticEntity extends StaticEntity {
	
	public static final float ERROR = 0.0001f;

	protected Vector3f _center;
	protected Vector3f _size;
	
	protected Quaternion _rotation;
	
	public AutoStaticEntity(EntityManager entityManager, String name, Spatial s, boolean makeUnique) {
		super(entityManager, name, makeUnique);
		
		prepareSpatial(s);
		
		_rotation = new Quaternion();
	}
	
	protected void prepareSpatial(Spatial s) {
		
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
		
//		if (_size.x < ERROR) { 
//			_size.x = ERROR;
//			_center.x -= 2*ERROR;
//		}
//		if (_size.y < ERROR) {
//			_size.y = ERROR;
//			_center.y -= 2*ERROR;
//		}
//		if (_size.z < ERROR) {
//			_size.z = ERROR;
//			_center.z -= 2*ERROR;
//		}
		
        CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
        cs.setCullMode(CullState.CS_BACK);

		s.setRenderState(cs);
		s.updateRenderState();

		StaticPhysicsNode node = _entityManager.getPhysicsSpace().createStaticNode();
		node.setName(getName());
		node.attachChild(s);
		node.generatePhysicsGeometry(true);
		node.setMaterial(Material.GRANITE);
		
		setNode(node);
	}
	
	protected void fillMeshes() {
		System.out.println("name: " + getName() + " center: " + _center + " size: " + _size);
		Box b = new Box(getName(), _center, _size.x*0.5f, _size.y*0.5f, _size.z*0.5f);
		_meshes = new ArrayList<TriMesh>();
		_meshes.add(b);

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

}
