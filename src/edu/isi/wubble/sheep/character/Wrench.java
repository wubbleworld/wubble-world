package edu.isi.wubble.sheep.character;

import java.io.IOException;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.TriMesh;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.jgn.sheep.Utils;

@SuppressWarnings("serial")
public class Wrench extends SheepGameCharacter {
	private static Node masterWrench = null;
	private static TriMesh masterMesh = null;
	@SuppressWarnings("unused")
	private static MaterialState masterMaterial = null;
	
	public MaterialState _wrenchMaterial;
	public SharedMesh _wrenchMesh;
	
	static {
		try {
			masterWrench = (Node) BinaryImporter.getInstance().load(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, "wrench.jme"));
			masterMesh = (TriMesh) masterWrench.getChild(0);
			masterMaterial = (MaterialState) masterMesh.getRenderState(RenderState.RS_MATERIAL);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Wrench(String name, Vector3f pos, Quaternion rot, int color) {
		this.setName(name);
		
		_wrenchMesh = new SharedMesh(name + "-SM", masterMesh);
			
		ColorRGBA c = new ColorRGBA(0, 0, 1, .7f);
		if (color != 0) { c = new ColorRGBA(1, 0, 0, .7f); }
		
		Utils.makeTransparent(_wrenchMesh, c);
		
//		_wrenchMesh.setModelBound(new OrientedBoundingBox());
		
		this.attachChild(_wrenchMesh);	
		
		setupSelection();
	}

	public float getSelectionWidth() {
		return 15;
	}
	
	public float getSelectionHeight() {
		return 6;
	}
	
	public float getSelectionOffset() {
		return -0.3f;
	}
}
