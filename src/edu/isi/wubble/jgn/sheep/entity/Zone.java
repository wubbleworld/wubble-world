package edu.isi.wubble.jgn.sheep.entity;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;
import com.jme.scene.state.AlphaState;
import com.jme.system.DisplaySystem;

import edu.isi.wubble.jgn.sheep.Utils;

public class Zone extends SEntity {

	float _sizeX = 0;
	float _sizeY = 0;
	float _sizeZ = 0;
	
	public Zone(String name, Vector3f pos, float dimX, float dimY, float dimZ) {
		super(name);
		_sizeX = dimX;
		_sizeY = dimY;
		_sizeZ = dimZ;
		System.out.println("Making zone " + name + ": pos: " + Utils.vs(pos) + "  dim: (" + dimX + ", " + dimY + ", " + dimZ + ")");
		doConstructorStuff(name, pos);
	}
	

	protected void makeBody() {
		Node  f = getNode();
		String name = getName();
		
		// final PhysicsBox pBox = f.createBox("physBox__" + name);
		final Box vis = new Box(name + "__" + "staticbox", new Vector3f(), .5f, .5f, .5f);
		f.attachChild(vis);
		
		AlphaState as = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
		as.setEnabled(true);
		as.setBlendEnabled(true);
		vis.setRenderState(as);
		
		// Scale everything.
		f.setLocalScale(new Vector3f(_sizeX, _sizeY, _sizeZ));
		
		// Translate the static node, not just the visual part of it!
		f.getLocalTranslation().set(getPosition());
		System.out.println("Zone " + getName() + ": setting xlation to " + Utils.vs(getPosition()));
		
		// Put bounding boxes everywhere.
		vis.setModelBound(new OrientedBoundingBox());
		vis.updateModelBound();
		f.updateModelBound();
		
		//f.updateGeometricState(0, true);
		//f.updateWorldBound();
		
		// All patches will be red for now.
		Utils.setColor(vis, ColorRGBA.red);
		// System.out.println("Setting zone " + getName() + " color to red!  Loc: " + Utils.vs(f.getLocalTranslation()));
	}

	
//	public void testCheckCollisions(String prefix) {
//		
//		// ### Look for collisions between some node and the scene. Will need to
//		// move this stuff into its own class. Also need to keep track of the
//		// things that I have to explicitly check for collisions; or maybe each
//		// entity can do this, if it has auxiliary components or something. Or
//		// maybe I need to define an aux. component.
//		
//		CollisionResults res = new BoundingCollisionResults();
//		// s1Node.findCollisions(rootNode, res);
//		Node testNode = getCurEntity().getNode();
//		testNode.calculateCollisions(_rootNode, res);
//		int numCol = res.getNumber();
//		if (numCol > 0) { 
//			//p("There have been " + numCol + " collisions!");
//			for (int i=0; i < numCol; i++) {
//				CollisionData cd = res.getCollisionData(i);
//				Geometry gSrc = cd.getSourceMesh();
//				Geometry gTgt = cd.getTargetMesh();
//				
//				p(prefix + " collision " + i + " btwn src " + gSrc.getName() + " and tgt " + gTgt.getName());
//			}
//		}
//		
//		// This also works, and doesn't call the collision callbacks.
//		//if (s1Node.hasCollision(rootNode, false) == true) { p("Collision with sphere!"); }
//	}
	
	
	protected Node makeNode() { Node n = new Node(); setNode(n); return n; }
}
