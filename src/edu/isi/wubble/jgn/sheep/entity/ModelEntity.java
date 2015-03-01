package edu.isi.wubble.jgn.sheep.entity;

import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import com.jmex.physics.PhysicsNode;

public class ModelEntity extends StaticEntity {
	Spatial _me;
	
	public ModelEntity(String name, Spatial s) {
		super(name);
		_me = s;
		
		doConstructorStuff(s.getName(), s.getLocalTranslation());
	}
	
	@Override
	protected void makeBody() {
		PhysicsNode node = getNode(); 
		
		_me.setLocalTranslation(new Vector3f());
		
		// Do we need this?
		node.setName(_me.getName());
		
		node.attachChild(_me);
		
//		node.setMaterial(Material.WOOD);
	}

	@Override
	protected void makePhysics() {
		getNode().generatePhysicsGeometry(true);
	}
}