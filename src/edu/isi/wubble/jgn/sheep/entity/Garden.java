package edu.isi.wubble.jgn.sheep.entity;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jmex.physics.contact.ContactInfo;

import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.action.CollisionManager;
import edu.isi.wubble.physics.entity.controller.SheepMovementController;

public class Garden extends Zone {
	// Which team does this garden correspond to?
	int _team;
	
	// This is the list of stuff that's collided with the garden, as of the last onCollision update.
	Hashtable<String, ContactInfo> _gardenCollisions = null;
	
	// If there were no collisions on the last update cycle, this is true.  Otherwise, false.
	boolean _hadNoColllisions = true;
	
	public Garden(String name, int team, Vector3f pos, float dimX, float dimY, float dimZ) {
		super(name, pos, dimX, dimY, dimZ);
		_team = team;
	}
	
	
	public int numSheepInGarden() {
		// If the collisions haven't been set yet, or there aren't any, there are obviously no sheep.
		if (_gardenCollisions == null || _gardenCollisions.size() == 0) { return 0; }
		
		// Find how many of the collisions are sheep.
		Set<String> names = _gardenCollisions.keySet();

		int numSheep = 0;
		for (String n : names) {
			SEntity e = SEntity.GetEntityForName(n);
			// if (e instanceof DynamicEntity) { System.out.println(e.getName() + " is in garden " + getName()); }
			if (e instanceof Sheep) { numSheep++; }
		}
		return numSheep;
	}
	
	
	@Override
	public void behave(float tpf) {
		super.behave(tpf);

		// Get the number of things that are colliding with me right now.
		_gardenCollisions = CollisionManager.Get().getCollisions(getName());

		// How many sheep are in the zone?  If zero, and it was zero the last time, return.
		int colCount = numSheepInGarden();
		if (colCount == 0) {
			if (_hadNoColllisions) { return; }
			else { _hadNoColllisions = true; }
		} else { _hadNoColllisions = false; }

		// Find all the wubbles on this team.
		ArrayList<Wubble> wubTeam = new ArrayList<Wubble>();
		Hashtable<String, Wubble> wubbles = Wubble.GetWubbleHash(); 
		Set<String> eNames = wubbles.keySet();
		for (String n : eNames) {
			Wubble w = wubbles.get(n);
			// If the wubble is on the team corresponding to this garden, keep track of it.
			if (w.getTeam() == _team) { wubTeam.add(w); }
		}

		// Update the controllers of the appropriate wubbles wrt the number of sheep in the garden.
		SheepMovementController mc = null;
		for (Wubble w : wubTeam) {
			ArrayList<Controller> controllerList = w.getNode().getControllers();

			// Find this wubble's movement controller.
			mc = null;
			for (Controller c : controllerList) {
				if (c instanceof SheepMovementController)  { mc = (SheepMovementController)c; break; }
			}
			// There should always be a movement controller.
			// hmm... sidekicks seem to complicate things
			if (mc == null) {
//				System.out.println("Garden " + getName() + ": No SheepMovementController on entity " + w.getName());
				assert(false);
			} else {
				mc.setSheepInGarden(colCount);
			}


			// The slowdown factor (or the garden speed operand) varies according to the number 
			// of sheep in the garden.  Set the controller accordingly.
			//System.out.println("Garden " + getName() + " setting gardenSpeedMod to " + newGardenSpeed);
			
		}
	}
}
