package edu.isi.wubble.physics.entity.controller;

import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.PhysicsEntity;
import edu.isi.wubble.util.Highlighter;

public class LookAtController extends EntityController {

	private static final long serialVersionUID = 1L;

	protected DynamicEntity _entity;
	
	protected PhysicsEntity _focusedEntity;
	
	public LookAtController(DynamicEntity entity) {
		_entity = entity;
		setActive(false);
	}
	
	public void applyController(PhysicsEntity entity) {
		stopController();

		_focusedEntity = entity;
		
		Highlighter.createHighlighter(_focusedEntity.getName(), _focusedEntity.getNode());
		_entity.record(_focusedEntity, "LookAt", true);
		setActive(true);
	}
	
	public void stopController() {
		if (_focusedEntity == null)
			return;
		
		Highlighter.deactivateHighlighter(_focusedEntity.getName());
		_entity.record(_focusedEntity, "LookAt", false);
		setActive(false);
	}
	
	@Override
	public void update(float time) {
		
	}
}
