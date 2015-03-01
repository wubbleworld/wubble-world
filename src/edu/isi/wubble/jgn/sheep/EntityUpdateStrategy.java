package edu.isi.wubble.jgn.sheep;

import edu.isi.wubble.jgn.sheep.entity.SEntity;


public class EntityUpdateStrategy {
	private SEntity _itsEntity = null;
	
	public EntityUpdateStrategy(SEntity e) { _itsEntity = e; }
	
	public boolean updateEntity() { return false; }
	
	public SEntity getEntity() { return _itsEntity; }
}
