package edu.isi.wubble.character;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;

public class GameCharacter extends Node {
	private static final long serialVersionUID = 1L;

	protected Node               _visual;
	protected Node               _shadow;
	protected AnimationEngine    _animEngine;
	
	// To help with the transition to wrenches and stuff as characters.
	public GameCharacter() {}
	
	public GameCharacter(String name) {
		super(name);
		doConstructorStuff();
	}
	
	public GameCharacter(String name, Vector3f pos) {
		super(name);
		doConstructorStuff();
		setPosition(pos);
	}

	protected void doConstructorStuff() {
		initVisual();
		setupAnimations();
	}
	
	protected void initVisual()      {}
	protected void setupAnimations() {}
	
	
	/**
	 * Set the position of the objects.
	 * @param pos
	 */
	public void setPosition(Vector3f pos) {
		_visual.setLocalTranslation(pos);
		_visual.updateWorldVectors();
	}
	
	public void setRotation(Quaternion q) {
		_visual.setLocalRotation(q);
		_visual.updateWorldVectors();
	}

	public Node getVisualNode() {
		return _visual;
	}
	
	public void setVisualNode(Node n) {
		_visual = n;
	}
	
	public Vector3f getPosition() {
		return _visual.getLocalTranslation();
	}
	
	public void playAnimation(String animName) {
		if (_animEngine != null) {
			_animEngine.playAnimation(animName);
		}
	}

	protected Node loadFile(String file) {
		Node node = null;
		try {
			node = (Node) BinaryImporter.getInstance().load(
					ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, file));
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return node;
	}
	
	public void update(float tpf) {
		if (_shadow != null) {
			_shadow.setLocalTranslation(_visual.getLocalTranslation());
		}
		for (Controller c : getControllers()) {
			if (c.isActive()) {
				c.update(tpf);
			}
		}
	}

	public Node getShadowNode() {
		return _shadow;
	}
}
