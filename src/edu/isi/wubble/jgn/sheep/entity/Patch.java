package edu.isi.wubble.jgn.sheep.entity;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.shape.Box;
import com.jmex.physics.PhysicsNode;
import com.jmex.physics.material.Material;

import edu.isi.wubble.jgn.sheep.Utils;

public class Patch extends SDynamicEntity {

	int _sizeX = 0;
	int _sizeY = 0;
	int _sizeZ = 0;
	
	public Patch(String name, Vector3f pos, int dimX, int dimY, int dimZ) {
		super(); 
		_sizeX = dimX;
		_sizeY = dimY;
		_sizeZ = dimZ;
//		SheepPhysicsState w = SheepPhysicsState.GetWorld();		
		System.out.println("Making patch " + name + ": pos: " + Utils.vs(pos) + "  dim: (" + dimX + ", " + dimY + ", " + dimZ + ")");
		doConstructorStuff(name, pos);
	}
	
	
	protected void makeBody() {
		PhysicsNode f = getNode();
		// f.setAffectedByGravity(false);
		
		String name = getName();
		
		// final PhysicsBox pBox = f.createBox("physBox__" + name);
		final Box vis = new Box("staticbox_" + "__" + name, new Vector3f(), .5f, .5f, .5f);
		
//		f.attachChild(pBox);
		f.attachChild(vis);
		
		// Scale everything.
		f.setLocalScale(new Vector3f(_sizeX, _sizeY, _sizeZ));
		
		// Translate the static node, not just the visual part of it!
		f.getLocalTranslation().set(getPosition());
		
		// Put bounding boxes everywhere.
		vis.setModelBound(new OrientedBoundingBox());
		vis.updateModelBound();
		
//		pBox.setModelBound(new OrientedBoundingBox());
//		pBox.updateModelBound();
		
		f.setModelBound(new OrientedBoundingBox());
		f.updateModelBound();
		
		f.setMaterial(Material.GRANITE);
		f.generatePhysicsGeometry();
		f.updateGeometricState(0, true);
		
		//f.updateModelBound();
		
		// All patches will be red for now.
		Utils.setColor(vis, ColorRGBA.red);
		System.out.println("!!! Setting " + getName() + " color to red!  Loc: " + Utils.vs(f.getLocalTranslation()));
	}
}
