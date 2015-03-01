package edu.isi.wubble.jgn.sheep.entity;

import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jmex.physics.StaticPhysicsNode;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;

public abstract class StaticEntity extends SPhysicsEntity {

	public StaticEntity(String name) { super(name); }
	
	public StaticEntity(String name, Vector3f pos) {
		super(name, pos);
	}

	
	protected Node makeNode() {
		SheepPhysicsState w = SheepPhysicsState.getWorldState();
		Node sn = w.getPhysicsSpace().createStaticNode(); 
		setNode(sn);
		
		return sn;
	}
	
	public StaticPhysicsNode getNode() { return (StaticPhysicsNode)super.getNode(); }
}
