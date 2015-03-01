package edu.isi.wubble.jgn.sheep.sidekick;

import java.util.HashMap;

import edu.isi.wubble.physics.entity.Entity;

public class Lexicon {
	private HashMap<Entity,String> _nameMap = new HashMap<Entity,String>();
	private HashMap<String,String> _classMap = new HashMap<String,String>();
	
	public void processObjectNaming(Entity e, String name) {
		_nameMap.put(e, name);
	}
	
	public String getObjectName(Entity e) {
		return _nameMap.get(e);
	}
	
	public void processTypeNaming(Entity e, String name) {
		_classMap.put(e.getClass().getName(), name);
	}
	
	public String getTypeName(Entity e) {
		return _classMap.get(e.getClass().getName());
	}
}
