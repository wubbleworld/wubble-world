package edu.isi.wubble.physics.entity.controller;

import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.sheep.entity.Sidekick;
import edu.isi.wubble.jgn.sheep.entity.Wubble;
import edu.isi.wubble.jgn.sheep.sidekick.Lexicon;
import edu.isi.wubble.physics.entity.Entity;

public class SidekickAIController extends EntityController {
	private static final long serialVersionUID = -1032658934570145129L;

	Sidekick _me;
	Wubble _parent;
	VisualSalienceController _mySalience, _parentSalience;
	Lexicon _lexicon = new Lexicon();
	
	// hack
	Entity _focus;
	
	public SidekickAIController(Sidekick me, Wubble parent) {
		_me = me;
		_parent = parent;
		
		_mySalience = (VisualSalienceController) _me.getController(VisualSalienceController.class.getName());
		_parentSalience = (VisualSalienceController) _parent.getController(VisualSalienceController.class.getName());
	}
	
	@Override
	public void update(float time) {
	}

	public void notifyAttention(Entity e) {
		String name = _lexicon.getTypeName(e);
		
		InvokeMessage im = new InvokeMessage();
		im.setArguments(new Object[]{e.getName()});
		im.setMethodName("addSidekickAttention");
		im.sendTo(_parent.getID());
		
		InvokeMessage im2 = new InvokeMessage();
		
		if (name == null) {
			im2.setArguments(new Object[]{_me.getName(), _me.getTeam(), "wha?"});
		} else {
			im2.setArguments(new Object[]{_me.getName(), _me.getTeam(), name + "!"});
		}
		im2.setMethodName("chatMessage");
		im2.sendTo(_parent.getID());
		
		_focus = e;
	}
	
	public void processUtterance(String text) {
		System.out.println("Big one said: " + text);
		
		Entity target = _parentSalience.getMostSalientEntity();
		if (_focus != null) {
			target = _focus;
		}
		
		if (target != null) {
//			InvokeMessage im = new InvokeMessage();
//			im.setArguments(new Object[]{target.getName()});
//			im.setMethodName("addSidekickAttention");
//			im.sendTo(_parent.getID());
			
			_lexicon.processTypeNaming(target, text);
			
			InvokeMessage im2 = new InvokeMessage();
			im2.setArguments(new Object[]{_me.getName(), _me.getTeam(), text + "!"});
			im2.setMethodName("chatMessage");
			im2.sendTo(_parent.getID());
		}
	}
}
