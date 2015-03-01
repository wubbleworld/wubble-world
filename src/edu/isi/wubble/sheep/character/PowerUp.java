package edu.isi.wubble.sheep.character;

import com.jme.bounding.BoundingSphere;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.shape.Sphere;

import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.sheep.PowerUpInfo;

public class PowerUp extends SheepGameCharacter {
	// This makes an error go away; dunno wtf it is.
	private static final long serialVersionUID = 1L;
	
	private Sphere _powerSphere;
	
	protected PowerUpInfo.PowerUpType _puType;
	public PowerUpInfo.PowerUpType getPUType() { return _puType; }
	
	public PowerUp(String name, String puType, Vector3f pos) { 
		super(name, pos); 
		
		// Can I convert this string to an enum?
		System.out.println("Creating new powerup with type " + puType);
		_puType = PowerUpInfo.PowerUpType.valueOf(puType);
		
		// NOTE: Set the powerup color.  (Should be done in initVisual, but 
		// _puType doesn't exist yet. Stupid Java super() rules.)
		ColorRGBA pColor = PowerUpInfo.GetPUColor(getPUType());

		Utils.makeTransparent(_powerSphere, pColor);
		
		setupSelection();
	}

	@Override
	protected void initVisual() {
		_visual = new Node(getName());
		_powerSphere = new Sphere(getName(), getPosition(), 16, 16, 0.4f);
		_visual.attachChild(_powerSphere);
		this.attachChild(_visual);
		
		this.setModelBound(new BoundingSphere());
		this.updateModelBound();
	}
	
	public void setPosition(Vector3f pos) {
		setLocalTranslation(pos);
		updateWorldVectors();
	}

	public float getSelectionWidth() {
		return 2;
	}
	
	public float getSelectionHeight() {
		return 2;
	}
	
	public float getSelectionOffset() {
		return -0.2f;
	}
}
