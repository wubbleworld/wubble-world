package edu.isi.wubble.util;

import java.util.HashMap;

import com.jme.input.MouseInput;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.intersection.PickData;
import com.jme.intersection.PickResults;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.system.DisplaySystem;


public class PickerInputAction extends InputAction {

	protected char _mouseButton;
	protected boolean _onRelease;
	
	protected PickerI _picker;
	protected Ray _pickRay = new Ray();
	
	protected PickerInputAction() {
		
	}

	public PickerInputAction(PickerI picker) {
		this(picker, 'R', true);
	}
	
	public PickerInputAction(PickerI picker, char mouseButton, boolean onRelease) {
		_picker = picker;
		_mouseButton = mouseButton;
		_onRelease = onRelease;
	}
	
	public void performAction(InputActionEvent evt) {
		if (evt.getTriggerCharacter() == _mouseButton && !evt.getTriggerPressed()) {
			Vector2f mousePosition = new Vector2f(MouseInput.get().getXAbsolute(), MouseInput.get().getYAbsolute());
			DisplaySystem.getDisplaySystem().getPickRay(mousePosition, false, _pickRay);
			
			PickResults pickResults = new TrianglePickResults();
			pickResults.clear();
			pickResults.setCheckDistance( true );
			
			_picker.getPickNode().findPick(_pickRay, pickResults);
			for ( int i = 0; i < pickResults.getNumber(); i++ ) {
				PickData data = pickResults.getPickData( i );
				if ( data.getTargetTris() != null && data.getTargetTris().size() > 0 ) {
					Spatial target = data.getTargetMesh().getParentGeom();
					while ( target != null ) {
						if (_picker.isClickable(target.getName())) {
							System.out.println("picked: " + target);
							_picker.picked(target);
							return;
						}
						target = target.getParent();
					}
				}
			}
			_picker.picked(null);
		}	
	}
}
