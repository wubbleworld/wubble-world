package edu.isi.wubble.jgn.sheep.entity;

import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.physics.FollowCorrector;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.controller.ShareAttentionController;
import edu.isi.wubble.physics.entity.controller.SidekickAIController;

public class Sidekick extends Wubble {
	FollowCorrector _fc;
	String _parentName;
	Wubble _parent;
	
	SidekickAIController _ai;
	
	public Sidekick(String parentName) {
		super("little " + parentName, Utils.IMPOSSIBLE_CLIENT_ID, 1);
		_parentName = parentName;

		// Make sure the parent is still around before trying to finish construction.
		Wubble parent = (Wubble)SEntity.GetEntityForName(parentName);
		if (parent == null) {
			System.out.println(getName() + "'s parent is gone!  Removing...");
			Utils.GetSps().removeEntity(getName());
		}
		else { 
			setTeam(parent.getTeam());
			
			parent.setSidekick(this);
			_ai = new SidekickAIController(this, parent);
			addController(_ai);
		}
		
		// Stick the little guy to his parent wubble.
		// Make a new corrector and add it to the list.
		String stickName = "sticky-"+getName()+"-"+parent.getName();
		System.out.println("Adding sticky \"" + stickName + "\"");
		_fc = FollowCorrector.Create(stickName, Utils.GetSps().getPhysicsSpace(), this, parentName);
		
		// Add the SA controller.
		addController(new ShareAttentionController(this, parentName));
	}
	
	public SidekickAIController getAI() {
		return _ai;
	}
	
	@Override
	public void update(float tpf) {
		super.update(tpf);
		// If the thing I'm following has disappeared, remove myself.
		_parent = (Wubble)SEntity.GetEntityForName(_parentName);
		if (_parent == null) { 
			System.out.println(getName() + "'s parent is gone!  Removing...");
			Utils.GetSps().removeEntity(getName());
		}
	}
}
