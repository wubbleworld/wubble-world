package edu.isi.wubble.jgn.sheep.entity;

import java.util.Hashtable;

import com.jme.math.Vector3f;
import com.jmex.physics.contact.ContactInfo;

public class EndZone extends Zone {

	// The team to which this zone corresponds.
	int _team;
	public int getTeam() { return _team; }
	
	public EndZone(String name, int team, Vector3f pos, int dimX, int dimY, int dimZ) {
		super(name, pos, dimX, dimY, dimZ);
		_team = team;
	}

	
//	@Override
//	protected void checkCollisions() {
//		super.checkCollisions();
//		//System.out.println(getName() + ": checking for collisions.");
//	}
//
	@Override
	public void onCollision(Hashtable<String, ContactInfo> collisions) {
		 //System.out.println(getName() + ": something crashed into me!");
		 // printCollisions(collisions);
	}

	
//	@Override
//	public void behave() {
//		super.behave();
//		System.out.println("EndZone \"" + getName() + "\" behaving.");
//	}
}
