package edu.isi.wubble.physics.entity.controller;

import java.util.Set;
import java.util.Map.Entry;

import edu.isi.wubble.jgn.sheep.entity.Sheep;
import edu.isi.wubble.jgn.sheep.entity.Wubble;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;

public class LookingAtEachOtherController extends EntityController {

	private static final long serialVersionUID = 1L;
	DynamicEntity _itsEntity;
	VisualSalienceController _vsc;
	
	public LookingAtEachOtherController(DynamicEntity de) {
		super();
		_itsEntity = de;
		
		// Save my VSC, which is assumed to exist at this point.
		_vsc = (VisualSalienceController)de.getController(VisualSalienceController.class.getName());
		if (_vsc == null) { System.out.println("Hey!  " + de.getName() + " has no VisualSalienceController!"); }
		assert(_vsc != null);
	}

	
	int _count = 0;
	public void findLookAtEach(float time, boolean printWho) {
		// For every entity in my FOV, see if I'm in their FOVs.
		Set<Entry<Entity, Float>> es = _vsc.getSalientEntities().entrySet();
		DynamicEntity de = null;
		Entity otherEntity = null;
		VisualSalienceController vsc = null;

		for (Entry<Entity, Float> entry : es) {
			otherEntity =  entry.getKey();
			// If the entity in my FOV is a bio-entity, then let's see if we're looking at each other.
			if (otherEntity instanceof Wubble || otherEntity instanceof Sheep) {
				de = (DynamicEntity)otherEntity;
				// System.out.println(_itsEntity.getName() + " considering: " + de.getName());
				// Get the other guy's vsc; if he doesn't have one, skip him.
				vsc = (VisualSalienceController) de.getController(VisualSalienceController.class.getName());
				// If the other thing has no FOV controller, skip it.
				if (vsc == null) { continue; }

				// See if _entity is in e's saliency map.  This is the "looking at each other" part.
				float sal = vsc.getSalience(_itsEntity);
				if (sal > 0.0f) {
					if (printWho == true) {
						System.out.println("    " + _count + ": " + _itsEntity.getName() + " and " + otherEntity.getName() + " see each other!");
						_count++;
						_itsEntity.recordAuto(otherEntity, "SeeEachOther", true);

						// Now, check the things that the entity with whom I'm mutual-looking is looking 
						// at.  These are the mutually attended objects.
						Set<Entry<Entity, Float>> e1Attended = _vsc.getSalientEntities().entrySet();
						for (Entry<Entity, Float> entry1 : e1Attended) {
							Entity attendedEntity = entry1.getKey();
							if (vsc.getSalience(attendedEntity) > 0.0f) {
								if (printWho == true) {
									System.out.println("    btwn " + _itsEntity.getName() + " and " + de.getName() + " -- " 
											+ attendedEntity.getName() + " is jointly attended!");
									
									// Add the fluent to the db.
									_itsEntity.recordAuto(attendedEntity, "MutuallyAttended", true);
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public void update(float time) { findLookAtEach(time, false); }
}
