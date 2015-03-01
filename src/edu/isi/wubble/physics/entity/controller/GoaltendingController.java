package edu.isi.wubble.physics.entity.controller;

import com.jme.bounding.OrientedBoundingBox;
import com.jme.intersection.BoundingCollisionResults;
import com.jme.intersection.CollisionData;
import com.jme.intersection.CollisionResults;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.shape.Cylinder;

import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.entity.SDynamicEntity;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.Wrench;
import edu.isi.wubble.physics.entity.DynamicEntity;

public class GoaltendingController extends EntityController {
	
	private static final long serialVersionUID = 1L;

	CollisionResults _res;
	// SheepPhysicsState _w;

	Node _cyl1;
	Node _cyl2;
	Node _rootNode;
	Wrench _wrench;

	public GoaltendingController(Wrench w) {
		super();
		_wrench = w;
		_res = new BoundingCollisionResults();
		//_gtHash = new HashMap<String, String>();
		String namePref = w.getName();

		System.out.println("Created GT controller for \"" + w.getName() + "\"");
		
		_cyl1 = new Node(namePref + "_gtcyl1Node");
		_cyl2 = new Node(namePref + "_gtcyl2Node");
		_rootNode = _wrench.getManager().getRootNode();
		
		// Make goaltending regions.  Do it AFTER physics has been generated, 
		attachGTCyl(_cyl1, new Vector3f(5.5f, 0,0));
		attachGTCyl(_cyl2, new Vector3f(-5.5f, 0,0));
	}
	
	protected void attachGTCyl(Node cylNode, Vector3f tVector) {
		Node wrenchNode = _wrench.getNode();

		// since I don't want the GT regions to be physical entities.
		Cylinder c = new Cylinder(cylNode.getName() + "_cyl", 16, 16, .45f, 5, true);

		// Do I need to do this?
		cylNode.attachChild(c);
		cylNode.setModelBound(new OrientedBoundingBox());
		cylNode.updateModelBound();

		c.setLocalTranslation(tVector);
		c.getLocalRotation().fromAngleAxis(FastMath.PI / 2, Vector3f.UNIT_X );
		Utils.setColor(c, new ColorRGBA(0, .3f, .8f, .2f));
		
		cylNode.updateGeometricState(0, true);
		wrenchNode.attachChild(cylNode);
	}

	
	public void update(float time) {
		if (false) { return; }
		
		// _gtHash.clear();
		
		// Check collisions of the gt cylinders with the rest of the scene.
		Node gtNode = _cyl1;
		gtNode.calculateCollisions(_rootNode, _res);
		
		// Turned off collisions with this second cylinder.
		gtNode = _cyl2;
		gtNode.calculateCollisions(_rootNode, _res);

		SEntity e = _wrench; // getEntity();
		int numCol = _res.getNumber();
		if (numCol > 0) { 
			//System.out.println("Entity " + e.getName() + "'s GT: there have been " + numCol + " collisions!");
			
			for (int i=0; i < numCol; i++) {
				CollisionData cd = _res.getCollisionData(i);
				Geometry gSrc = cd.getSourceMesh();
				Geometry gTgt = cd.getTargetMesh();
				
				// I only want collisions between this zone and entities (other than itself.)
				String tgtName = gTgt.getName();
				String srcName = gSrc.getName();
				
				// Throw out a bunch of meaningless collisions, since there's too much shit on the screen.
				if (SEntity.checkCrapCollisions(srcName) == true || SEntity.checkCrapCollisions(tgtName) == true) { continue; }
				
				// Use the names of the parent to get the names of the entities, instead of the names of the meshes.  
				// This eliminates the need for that hackish workaround below.
				Node srcParent = gSrc.getParent();
				Node tgtParent = gTgt.getParent();

				String srcPName = srcParent.getName();
				String tgtPName = tgtParent.getName();
				
				// System.out.println("GT col btwn src " + srcName + " and tgt " + tgtName);
				
				// Don't count collisions between the goaltending cylinder and the wrench to which it's attached.
				if (tgtName.equals(e.getName())) { continue; }
								
				// I only want collisions between this decorator and entities (other than the entity to which 
				// this decorator is attached.)
				SEntity eTgt = SEntity.GetEntityForName(tgtName);
				if (eTgt != null && eTgt != _wrench && (eTgt instanceof SDynamicEntity)) {
					Node spp = srcParent.getParent();
					String sppName = spp.getName();
					
					// The position of the CoM of the GT cylinder being intersected.
//					Vector3f srcNodePos = gSrc.getWorldTranslation();
					
//					System.out.println("GT Collision " + i + " btwn src " + srcName + " srcParent " + srcPName + " spp " 
//							+ sppName + " tgt " + tgtName + ", tgtParent " + tgtPName + " spnpos " + srcNodePos);
						
					// Indicate that the entity is goaltending this thing. 
					addGT(tgtName, sppName, gSrc.getWorldTranslation());
				}
			}
		}
		
		_res.clear();
	}
	
	
	// HashMap<String, String> _gtHash;
	void addGT(String who, String what, Vector3f gtCylPos) {
		// _gtHash.put(who, what);
		DynamicEntity eWho = (DynamicEntity) SEntity.GetEntityForName(who);
		DynamicEntity eWhat = (DynamicEntity) SEntity.GetEntityForName(what);
		
		// If I can't find one of the entities, return.
		if (eWho == null || eWhat == null) { return; }
		
		// Jimbo is goaltending redWrench1
		//System.out.println(who + " is goaltending " + what);
		eWho.recordAuto(eWhat, "Goaltending", true);
		
		// The GT cylinder this wubble has encountered.
		Vector3f whoPos = eWho.getPosition();
		
		// The distance of the wubble from the center of the gt cylinder.
		float dist = gtCylPos.distance(whoPos);
		
		// We know the wubble is goaltending.  If it's CoM is close enough to the CoM of the GT 
		// cylinder, then we know that it's in the cylinder.
		//System.out.println("gtPos: " + Utils.vs(gtCylPos) + " gt: dist " + dist);
		final float IN_DIST = 0.9f;
		
		if (dist < IN_DIST) {
			// System.out.println(eWho.getName() + " is in " + what);
			eWho.recordAuto(eWhat, "In", true);
		}
		
	}
	
}
