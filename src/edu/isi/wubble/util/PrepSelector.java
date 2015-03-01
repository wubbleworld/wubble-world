package edu.isi.wubble.util;

import com.jme.input.MouseInput;
import com.jme.input.MouseInputListener;
import com.jme.intersection.PickData;
import com.jme.intersection.PickResults;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.FastMath;
import com.jme.math.Ray;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Cone;
import com.jme.scene.shape.Cylinder;
import com.jme.scene.state.MaterialState;
import com.jme.system.DisplaySystem;

import edu.isi.wubble.wubbleroom.SocketClient;

public class PrepSelector extends Node {

	protected float _speed = 0.01f;
	protected boolean _enabled;
	protected MouseInputListener _listener;
	
	protected float _minX;
	protected float _maxX;
	
	protected float _minZ;
	protected float _maxZ;
	
	protected PickerI _picker;
	
	public PrepSelector(PickerI picker) {
		super();
		
		_picker = picker;
		init();
	}
	
	protected void init() {
		MaterialState ms = DisplaySystem.getDisplaySystem().getRenderer().createMaterialState();
		ms.setDiffuse(ColorRGBA.white);
		ms.setAmbient(ColorRGBA.black);
		ms.setEmissive(ColorRGBA.black);
		ms.setShininess(0);
		setRenderState(ms);

		Cone c = new Cone("prepSelectorCone", 10, 10, 0.2f, 0.3f);
		c.setLocalTranslation(new Vector3f(0,0.15f,0));
		c.getLocalRotation().fromAngleNormalAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
		attachChild(c);
		
		Cylinder cyl = new Cylinder("prepSelectorCyl", 10, 10, 0.1f, 0.6f, true);
		cyl.setLocalTranslation(new Vector3f(0,0.45f,0));
		cyl.getLocalRotation().fromAngleNormalAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
		attachChild(cyl);
		
		setLocalTranslation(new Vector3f(6,0,6));
		
		_listener = new MouseInputListener() {
			public void onButton(int button, boolean pressed, int x, int y)	{ 
				if (!pressed && button == 0) {
					Vector3f loc = getLocalTranslation().add(new Vector3f(0,0.25f,0));
            		SocketClient.inst().sendMessage("response (position " + LispUtils.toLisp(loc) + ")");
            		setEnabled(false);
				}
			}
			public void onWheel(int wheelDelta, int x, int y) { }

			public void onMove(int xDelta, int yDelta, int newX, int newY) {
				mouseMoved(xDelta, yDelta);
			}
		};
	}
	
	public void setBoundaries(float minx, float maxx, float minz, float maxz) {
		_minX = minx;
		_maxX = maxx;
		
		_minZ = minz;
		_maxZ = maxz;
	}
	
	public void setEnabled(boolean enabled) {
		_enabled = enabled;
		if (enabled) 
			enable();
		else
			disable();
	}
	
	public boolean getEnabled() {
		return _enabled;
	}

	protected void enable() {
		setCullMode(Spatial.CULL_NEVER);
		MouseInput.get().setCursorVisible(false);
		MouseInput.get().addListener(_listener);
	}
	
	protected void disable() {
		setCullMode(Spatial.CULL_ALWAYS);
		MouseInput.get().setCursorVisible(true);
		MouseInput.get().removeListener(_listener);
	}
	
	protected void mouseMoved(int dx, int dy) {
		Vector3f delta = new Vector3f(-dx*_speed, 0, dy*_speed);
		Vector3f newPos = getLocalTranslation().add(delta);
		
		newPos.setX(Math.max(_minX, newPos.x));
		newPos.setX(Math.min(_maxX, newPos.x));
		
		newPos.setZ(Math.max(_minZ, newPos.z));
		newPos.setZ(Math.min(_maxZ, newPos.z));
		
		newPos.setY(getHeight(newPos.x, newPos.z));
		
		setLocalTranslation(newPos);
		
	}
	
	protected float getHeight(float x, float z) {
		Vector3f origin = new Vector3f(x,11.0f,z);
		Ray r = new Ray(origin, new Vector3f(0,-1,0));
		
		PickResults pickResults = new TrianglePickResults();
		pickResults.setCheckDistance( true );
		_picker.getPickNode().findPick(r, pickResults);

		for ( int i = 0; i < pickResults.getNumber(); i++ ) {
			PickData data = pickResults.getPickData( i );
			if ( data.getTargetTris() != null && data.getTargetTris().size() > 0 ) {
				Spatial target = data.getTargetMesh().getParentGeom();
				while ( target != null ) {
					if (_picker.isClickable(target.getName())) {
						return origin.y - data.getDistance();
					}
					target = target.getParent();
				}
			}
		}
		return 0;
	}
}
