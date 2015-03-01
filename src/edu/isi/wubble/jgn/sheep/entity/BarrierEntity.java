package edu.isi.wubble.jgn.sheep.entity;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.math.Vector3f;
import com.jme.scene.shape.Box;
import com.jmex.physics.PhysicsNode;

public class BarrierEntity extends StaticEntity {
	
	float _sizeX;
	float _sizeY;
	float _sizeZ;
	
	public Box _vis;
	
	public BarrierEntity(String name, Vector3f pos, float sizeX, float sizeY, float sizeZ) {
		super(name);
		_sizeX = sizeX;
		_sizeY = sizeY;
		_sizeZ = sizeZ;

		doConstructorStuff(name, pos);
	}
	
	protected void makeBody() {
		// p("bsb: building " + name + " w/sizes " + sizeX + ", " + sizeY + ", " + sizeZ + "  at position " + vs(pos));
		PhysicsNode f = getNode(); 
		//DynamicPhysicsNode f = getNode(); 
		
		final Box vis = new Box( getName() + "__box", new Vector3f(), .5f, .5f, .5f);
		_vis = vis;
		f.attachChild(vis);
		
		// Scale everything.
		f.setLocalScale(new Vector3f(_sizeX, _sizeY, _sizeZ));

		// Put bounding boxes everywhere.
		vis.setModelBound(new OrientedBoundingBox());
		vis.updateModelBound();
		
		// We want to look through barrier entities.
		vis.setCullMode(Box.CULL_ALWAYS);
	}
}
