package edu.isi.wubble.physics.entity.controller;

import com.jme.math.Quaternion;

import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.physics.entity.DynamicEntity;

public class ShareAttentionController extends EntityController {

	private static final long serialVersionUID = 1L;
	
	String _parentName;
	Quaternion _parentRot;
	DynamicEntity _parent;
	DynamicEntity _me;
	
	public ShareAttentionController(DynamicEntity me, String parentName) {
		_parentName = parentName;
		_me = me;
		_parentRot = new Quaternion();
		populateParentInfo();
		System.out.println(me.getName() + " will be sharing attention with " + _parent.getName());
	}
	
	
	@Override
	public void update(float time) {
		populateParentInfo();
		
		// If the parent disappeared, this controller doesn't make any sense.  So remove it.
		if (_parent == null) {
			_me.removeController(getClass().getName());
			return;
		}
		
		// Parent exists, so rotate me in the direction where it's looking.
		// System.out.println("Updating " + _me.getName() + " rotation to match " + _parent.getName() + "!");
		_me.getRotation().set(_parentRot);
	}
	
	void populateParentInfo() {
		_parent = (DynamicEntity)SEntity.GetEntityForName(_parentName);
		if (_parent != null) {
			_parentRot.set(_parent.getRotation());
		}
		
	}

}
