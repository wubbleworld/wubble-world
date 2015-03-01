package edu.isi.wubble.sheep.character;

import java.io.IOException;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.TriMesh;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.system.DisplaySystem;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;


@SuppressWarnings("serial")
public class Sheep extends SheepGameCharacter {
	private static Node masterSheep = null;
	
	static {
		try {
			masterSheep = (Node) BinaryImporter.getInstance().load(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, "sheep.jme"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public SharedMesh[] _meshes = new SharedMesh[3];
	
	public Sheep(String name, Vector3f pos, Quaternion rot) {
		this.setName(name);
		this.setLocalTranslation(pos);
		this.setLocalRotation(rot);

		this.setLocalScale(masterSheep.getLocalScale().divide(2.0f));
		
		for (int i = 0; i < 3; i++ ) {
			TriMesh original = (TriMesh) masterSheep.getChild(i);
			_meshes[i] = new SharedMesh(name + "-SM" + i, original);
			
			MaterialState ms = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
			MaterialState old = (MaterialState) original.getRenderState(RenderState.RS_MATERIAL);

			// TODO: refactor into Utils.duplicateRenderState();
			ms.setAmbient(old.getAmbient());
			ms.setDiffuse(old.getDiffuse());
			ms.setEmissive(old.getEmissive());
			ms.setShininess(old.getShininess());
			ms.setSpecular(old.getSpecular());
			
			_meshes[i].setRenderState(ms);
			this.attachChild(_meshes[i]);
		}
		
		setupSelection();
	}

	public float getSelectionWidth() {
		return 300;
	}
	
	public float getSelectionHeight() {
		return 300;
	}
}
