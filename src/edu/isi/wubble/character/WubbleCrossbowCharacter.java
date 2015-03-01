package edu.isi.wubble.character;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import com.jme.animation.AnimationController;
import com.jme.animation.Bone;
import com.jme.animation.SkinNode;
import com.jme.bounding.BoundingSphere;
import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.BillboardNode;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.batch.GeomBatch;
import com.jme.scene.shape.Quad;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;

import edu.isi.wubble.sheep.character.Sheep;

public class WubbleCrossbowCharacter extends WubbleCharacter {

	private static final long serialVersionUID = 1L;

	public WubbleCrossbowCharacter(String name) {
		super(name);

	}

	/**
	 * 
	 */
	protected void initVisual() {
		_wubNode = loadFile("wubble-crossbow.jme");
		_wubNode.setCullMode(Node.CULL_NEVER);
		_wubNode.setLocalTranslation(new Vector3f(0, -0.2f, 0));

		_skin = (SkinNode) _wubNode.getChild("JeanGeometryShape-skin_node");

		_visual = new Node("visual-node-" + name);
				
		_visual.attachChild(_wubNode);
		_visual.lookAt(new Vector3f(0, 0, -1), new Vector3f(0,1,0));

		Sphere s = new Sphere("shadowSphere", 20, 20, 0.25f);
		s.setModelBound(new BoundingSphere());
		s.updateModelBound();
		s.setCullMode(Node.CULL_ALWAYS);
		
		_shadow = new Node("shadowNode");
		_shadow.attachChild(s);	
		
		attachChild(_visual);
		attachChild(_shadow);		
	}

	/**
	 * set up and name the animations for this character
	 * remember to add the _animEngine to the controllers
	 * for this node, otherwise update won't be called
	 * on it.
	 */
	public void setupAnimations() {
		Bone pelvisBone = (Bone) _wubNode.getChild(1);
		
		AnimationController ac = pelvisBone.getAnimationController();
		_animEngine = new AnimationEngine(ac);
		_animEngine.addAnimation("idle", 10, 60, "idle");
		
		_animEngine.playAnimation("idle");
		_animEngine.setActive(true);
		
		addController(_animEngine);
	}
}
