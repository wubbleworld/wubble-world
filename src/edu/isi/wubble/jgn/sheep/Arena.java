package edu.isi.wubble.jgn.sheep;

import java.io.IOException;
import java.util.List;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Box;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.Constants;
import edu.isi.wubble.jgn.sheep.entity.BarrierEntity;
import edu.isi.wubble.jgn.sheep.entity.EndZone;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.Garden;
import edu.isi.wubble.jgn.sheep.entity.ModelEntity;

public class Arena {
	int _xSize;
	int _ySize;
	int _zSize;
	SheepPhysicsState _sps;
	
	public Arena(SheepPhysicsState s, int x, int y, int z) {
		_xSize = x;
		_ySize = y;
		_zSize = z;
		_sps = s;
		System.out.println("Created arena, sizes (x y z) --> (" + _xSize + ", " + _ySize + "," + _zSize + ")");
		
		
		buildFloor();
		buildWalls();
		buildEndzones(8);
		buildGardens(10, 6);
		createArena();
	}
	
	
	public void createArena() {
		try {
			Node sheepArena = (Node) BinaryImporter.getInstance().load(
					ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, "sheep-physics.jme"));
			
			List<Spatial> children = sheepArena.getChildren();
			int index = 0;
			while (children.size() > 0) {
				Spatial s = children.remove(0);
				s.setModelBound(new BoundingBox());
				s.updateModelBound();
				ModelEntity e = new ModelEntity("modelEntity_" + index, s);
				index++;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		_sps.getRootNode().updateRenderState();
	}
	

	
	
	// Build the walls: sizes are the entire x,y,z.
	// ////////////////////////////////////////////////////////////
	protected void buildFloor() {
		//Spatial vis = buildWorldBox(new Vector3f(0, -0.5f, 0), _xSize, 1, _zSize, "floor");
		//Utils.setColor(vis, ColorRGBA.darkGray);
		
		BarrierEntity be = new BarrierEntity("floor", new Vector3f(0, -0.5f, 0), _xSize, 1, _zSize);
		be._vis.setCullMode(Box.CULL_NEVER);
		be.setColor(ColorRGBA.gray);
		
		//be.getNode().setMaterial(Material.GLASS);
		
		// There is a roof now.
		SEntity roof = new BarrierEntity("roof", new Vector3f(0, _ySize + 0.5f, 0), _xSize, 1, _zSize);
	}

	
	// ////////////////////////////////////////////////////////////
	protected void buildGardens(int length, int width) {
		int halfWidth = width / 2;
		
		Vector3f redCenter = new Vector3f(_xSize/2 - halfWidth, 0, 0);
		Vector3f blueCenter = new Vector3f(halfWidth - _xSize/2, 0, 0);
		
		// FIXME: These 0 and 1 need to be defined as constants somewhere.
		SEntity blueGarden= new Garden("blueGarden", 0, blueCenter, width, .5f, length);
		blueGarden.setColor(new ColorRGBA(0, 0, 1, 0.5f));
		
		SEntity redGarden = new Garden("redGarden", 1, redCenter, width, .5f, length);
		redGarden.setColor(new ColorRGBA(1, 0, 0, 0.5f));
	}
	
	// ////////////////////////////////////////////////////////////
	protected void buildEndzones(int endZSize) {
		int halfZ = _zSize / 2;
		// int endZoneSize = endZSize;
		
		System.out.println("Building endzones!");
		
		int zCenterRed = halfZ - (endZSize / 2);
		int zCenterBlue  = (endZSize / 2) - halfZ;
		
		Vector3f redCenter  = new Vector3f(0, 0.5f, zCenterRed);
		Vector3f blueCenter = new Vector3f(0, 0.5f, zCenterBlue);
		_sps.getGameMechanics().setCenters(redCenter, blueCenter);

		SEntity pRed = new EndZone("redZone", Constants.RED_TEAM, new Vector3f(redCenter), _xSize, 1, endZSize);
		pRed.setColor(new ColorRGBA(1, 0, 0, 0.3f));
		
		SEntity pBlue = new EndZone("blueZone", Constants.BLUE_TEAM, new Vector3f(blueCenter), _xSize, 1, endZSize);
		pBlue.setColor(new ColorRGBA(0, 0, 1, 0.3f));		
	}

	// Build the walls: sizes are the entire x,y,z.
	// ////////////////////////////////////////////////////////////
	protected void buildWalls() {
		int halfX = _xSize / 2;
		int halfZ = _zSize / 2;

		new BarrierEntity("farWall", new Vector3f(0, _ySize/2, halfZ), _xSize, _ySize, 1);
		//buildWorldBox(new Vector3f(0, _ySize/2, halfZ), _xSize, _ySize, 1, "farWall");
		
		//buildWorldBox(new Vector3f(0, _ySize/2, -halfZ), _xSize, _ySize, 1, "nearWall");
		new BarrierEntity("nearWall", new Vector3f(0, _ySize/2, -halfZ), _xSize, _ySize, 1);
		
		//buildWorldBox(new Vector3f(-halfX, _ySize/2, 0), 1, _ySize, _zSize, "leftWall");
		new BarrierEntity("leftWall", new Vector3f(-halfX, _ySize/2, 0), 1, _ySize, _zSize);
		
		
		//buildWorldBox(new Vector3f(halfX, _ySize/2, 0), 1, _ySize, _zSize, "rightWall");
		new BarrierEntity("rightWall", new Vector3f(halfX, _ySize/2, 0), 1, _ySize, _zSize);
	}
}
