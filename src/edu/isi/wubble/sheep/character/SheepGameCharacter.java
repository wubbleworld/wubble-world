package edu.isi.wubble.sheep.character;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.character.GameCharacter;

public abstract class SheepGameCharacter extends GameCharacter implements Selectable {
	private static final long serialVersionUID = 1719128833267088984L;
	Quad _salienceQuad;
	
	public SheepGameCharacter() {}
	
	public SheepGameCharacter(String name, Vector3f pos) {
		super(name, pos);
	}

	// Selectable
	public void addAttention() {
		_salienceQuad.setCullMode(Spatial.CULL_INHERIT);
	}

	public void removeAttention() {
		_salienceQuad.setCullMode(Spatial.CULL_ALWAYS);
	}

	public void setupSelection() {
		_salienceQuad = new Quad(name + "SalienceQuad", getSelectionWidth(), getSelectionHeight());
		
		Quaternion q = new Quaternion();
		q.fromAngles(FastMath.PI / 2, 0, 0);
		_salienceQuad.setLocalRotation(q);
		_salienceQuad.setLocalTranslation(0, getSelectionOffset(), 0);
		_salienceQuad.setCullMode(Spatial.CULL_ALWAYS);
		
		CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
	    cs.setCullMode(CullState.CS_NONE);
	    _salienceQuad.setRenderState(cs);	
	    
	    TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
	    ts.setTexture(TextureManager.loadTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "circle.png")));
	    _salienceQuad.setRenderState(ts);
	    
		AlphaState ta = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
		ta.setBlendEnabled(true);
		ta.setTestEnabled(true);
		ta.setTestFunction(AlphaState.TF_GREATER);

		_salienceQuad.setRenderState(ta);
	    
		this.attachChild(_salienceQuad);
	}
	
	public abstract float getSelectionWidth();
	
	public abstract float getSelectionHeight();
	
	public float getSelectionOffset() {
		return 0;
	}
}
