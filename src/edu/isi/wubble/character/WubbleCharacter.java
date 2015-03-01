package edu.isi.wubble.character;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.jme.animation.AnimationController;
import com.jme.animation.Bone;
import com.jme.animation.SkinNode;
import com.jme.bounding.BoundingSphere;
import com.jme.image.Texture;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
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

import edu.isi.wubble.Main;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.sheep.PowerUpInfo;
import edu.isi.wubble.sheep.PowerUpInfo.PowerUpType;
import edu.isi.wubble.sheep.character.Sheep;

public class WubbleCharacter extends GameCharacter {

	private static final long serialVersionUID = 1L;

	protected Node     _wubNode;
	protected SkinNode _skin;
	private Sphere   _powerUpSphere;

	
	// Power-Ups
	private boolean _powersEnabled = false;
	
	// Sheep Power-Up
	private Sheep _fakeSheep = null;
	
	// Floating name
	private BillboardNode _nameBillboard;
	private Quad _nameQuad;
	private boolean _nameShowing = false;
	private String _nameText = "default";
	

	public WubbleCharacter(String name) {
		super(name);
		_nameText = name;
		_puList = new ArrayList<PowerUpType>();
	}
	
	
	ArrayList<PowerUpType> _puList;
	

	/**
	 * 
	 */
	protected void initVisual() {
		_wubNode = loadFile("wubble.jme");
		_wubNode.setCullMode(Node.CULL_NEVER);
		_wubNode.setLocalTranslation(new Vector3f(0, -0.2f, 0));

		_skin = (SkinNode) _wubNode.getChild("JeanGeometryShape-skin_node");

		_powerUpSphere = new Sphere(name + "WubbleBubble", 20, 20, 0.4f);
		_powerUpSphere.setLocalTranslation(0, 0.1f, 0);
		
		ColorRGBA off = new ColorRGBA(0, 0, 0, 0);
		
		Utils.makeTransparent(_powerUpSphere, off);
		
		_powerUpSphere.setCullMode(Spatial.CULL_ALWAYS);
		_powerUpSphere.updateRenderState();
		
		_visual = new Node(name);
		_nameBillboard = new BillboardNode("name");

		_nameQuad = new Quad("testQuad", 1.2f, 0.3f);
		_nameQuad.setLocalTranslation(0, 0.7f, 0);
		_nameQuad.setCullMode(Spatial.CULL_ALWAYS);
		
		_nameBillboard.attachChild(_nameQuad);
		
		_visual.attachChild(_wubNode);
		_visual.attachChild(_powerUpSphere);
		_visual.attachChild(_nameBillboard);
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
		_animEngine.addAnimation("idle", 10, 89, "idle");
		_animEngine.addAnimation("hmmm", 90, 125, "idle");
		_animEngine.addAnimation("pickNose", 150, 270, "idle");
		_animEngine.addAnimation("pickUp", 290, 335, "hold");
		_animEngine.addAnimation("hold", 320, 335, "hold");
		_animEngine.addAnimation("putDown", 336, 378, "idle"); 
		
		_animEngine.playAnimation("idle");
		_animEngine.setActive(true);

		addController(_animEngine);
	}

	// Request: Individual Customization in addition to this function
	public void setColor(ColorRGBA newColor) {
		Geometry g = _skin.getSkin();
		
		// 1 is the eyebrows and nose - not changed
		
		// This is the muzzle
		GeomBatch muzzleBatch = g.getBatch(2);
		MaterialState muzzleMaterial = (MaterialState) muzzleBatch.getRenderState(RenderState.RS_MATERIAL);
		muzzleMaterial.setDiffuse(newColor);
		
		// This is the ears
		GeomBatch earBatch = g.getBatch(3);
		MaterialState earMaterial = (MaterialState) earBatch.getRenderState(RenderState.RS_MATERIAL);
		earMaterial.setDiffuse(newColor);
		
		// This is the eyelids
		GeomBatch eyelidBatch = g.getBatch(4);
		MaterialState eyelidMaterial = (MaterialState) eyelidBatch.getRenderState(RenderState.RS_MATERIAL);
		eyelidMaterial.setDiffuse(newColor);
		
		// 5 is the whites of the eyes??
		
		// This is the body
		GeomBatch bodyBatch = g.getBatch(6);
		MaterialState bodyMaterial = (MaterialState) bodyBatch.getRenderState(RenderState.RS_MATERIAL);
		bodyMaterial.setDiffuse(newColor);
	}

	public void showName() {
		setNameText(_nameText);
		_nameShowing = true;
		_nameQuad.setCullMode(Spatial.CULL_INHERIT);
	}
	
	public void hideName() {
		_nameShowing = false;
		_nameQuad.setCullMode(Spatial.CULL_ALWAYS);
	}
	
	public void setNameText(String newName) {
		_nameText = newName;
		
		BufferedImage b = new BufferedImage(512, 128, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = b.createGraphics();
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setBackground(new Color(0, 0, 0, 0));
		
		g.setColor(new Color(0, 255, 0, 255));
		g.setFont(new Font("Arial", Font.PLAIN, 96));
		Rectangle2D rect = g.getFont().getStringBounds(_nameText, g.getFontRenderContext());
		
		g.drawString(_nameText, (int) ((b.getWidth() / 2) - (rect.getWidth() / 2)), (int) rect.getHeight());
		
		Texture t = TextureManager.loadTexture(b, Texture.MM_NONE, Texture.MM_NONE, 1.0f, BufferedImage.TYPE_INT_ARGB, true);
		t.setFilter(Texture.FM_LINEAR);
		t.setMipmapState(Texture.MM_LINEAR_LINEAR);
		
		TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
		ts.setTexture(t);
		
		_nameQuad.setRenderState(ts);
		
		AlphaState ta = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
		ta.setBlendEnabled(true);
		ta.setTestEnabled(true);
		ta.setTestFunction(AlphaState.TF_GREATER);

		_nameQuad.setRenderState(ta);
	}
	
	public void enablePowers() {
		_powersEnabled = true;		
		
		// FIXME: Deal with this differently later.
		_fakeSheep = new Sheep(this.name + "FakeSheep", new Vector3f(), new Quaternion());
		_fakeSheep.setCullMode(Spatial.CULL_ALWAYS);
		_fakeSheep.setLocalTranslation(0, -0.2f, 0);
		_visual.attachChild(_fakeSheep);
	}
	
	public boolean isPlayerWubble() {
		System.out.println("SYSTEM NAME: " + Main.inst().getName());
		System.out.println("WUBBLE NAME: " + this.getName());
		
		return this.getName().equals(Main.inst().getName());
	}
	
	public void powerDown(PowerUpType oldPower) {
		System.out.println("Wubble " + getName() + " powering down " + oldPower);
		if (!_powersEnabled) 
			return;
		
		if (_puList.remove(oldPower) == false) {
			System.out.println("Tried to remove powerup " + oldPower + " that " + getName() + " doesn't have!");
		}
		
		drawPowerUp();
	}
	
	public void powerUp(PowerUpType newPower) {
		if (!_powersEnabled) { return; }
		
		System.out.println("Wubble " + getName() + " got powerup " + newPower);
		// Add this type to the powerup queue.
		_puList.add(newPower);
		int count = _puList.size();
		System.out.println("    The wubble now has " + count + " powerups.");
		drawPowerUp();
	}
	
	// Invisible beats all power-ups, sheep is next "strongest"
	protected void drawPowerUp() {
		PowerUpType newPower = PowerUpType.NONE;
		
		if (_puList.isEmpty()) {
			// No power up to draw - NONE is correct
		} else if (_puList.size() == 1) {
			// Only 1 power-up
			newPower = _puList.get(0);
		} else {
			// Multi-Powers
			if (_puList.contains(PowerUpType.INVISIBLE)) {
				newPower = PowerUpType.INVISIBLE;
			} else if (_puList.contains(PowerUpType.SHEEP)){
				newPower = PowerUpType.SHEEP;
			} else {
				newPower = PowerUpType.MULTI;
			}
		} 
		
		ColorRGBA newColor = PowerUpInfo.GetPUColor(newPower);
		// If an invalid power, this is actually super weird.
		if (newColor == null) {
			Logger.getLogger("").severe("Invalid Power: " + newPower);
			assert(false);
		} 
		
		// Set the sphere's color, and save it.
		Utils.makeTransparent(_powerUpSphere, newColor);
		
		switch (newPower) {		
			case INVISIBLE:
				_wubNode.setCullMode(Spatial.CULL_ALWAYS);
				_nameQuad.setCullMode(Spatial.CULL_ALWAYS);
				if (isPlayerWubble()) {
					_powerUpSphere.setCullMode(Spatial.CULL_INHERIT);
				} else {
					_powerUpSphere.setCullMode(Spatial.CULL_ALWAYS); 
				}
			break;

			case SHEEP:
				_wubNode.setCullMode(Spatial.CULL_ALWAYS);
				_fakeSheep.setCullMode(Spatial.CULL_INHERIT);
				_powerUpSphere.setCullMode(Spatial.CULL_ALWAYS);
				_nameQuad.setCullMode(Spatial.CULL_ALWAYS);
			break;
			
			case NONE:
			case POWERDOWN:
				_powerUpSphere.setCullMode(Spatial.CULL_ALWAYS);
				_wubNode.setCullMode(Spatial.CULL_INHERIT);
				_fakeSheep.setCullMode(Spatial.CULL_ALWAYS);
				if (_nameShowing) {	showName(); }
			break;
			
			default:
				_powerUpSphere.setCullMode(Spatial.CULL_INHERIT);
				_wubNode.setCullMode(Spatial.CULL_INHERIT);
				_fakeSheep.setCullMode(Spatial.CULL_ALWAYS);
				if (_nameShowing) {	showName(); }
			break;
		}
	}

	
	public void flipVisual() {
		_visual.lookAt(new Vector3f(0, 0, 1), new Vector3f(0,1,0));
	}
}
