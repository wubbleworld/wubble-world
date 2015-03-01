package edu.isi.wubble.jgn.sheep.entity;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Logger;

import com.jme.bounding.BoundingSphere;
import com.jme.math.Vector3f;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Sphere;
import com.jmex.physics.DynamicPhysicsNode;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.PhysicsUpdateCallback;
import com.jmex.physics.contact.ContactInfo;
import com.jmex.physics.material.Material;

import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.action.CollisionManager;
import edu.isi.wubble.physics.SpringCorrector;
import edu.isi.wubble.physics.entity.controller.JumpController;
import edu.isi.wubble.physics.entity.controller.SheepMovementController;
import edu.isi.wubble.sheep.PowerUpInfo;


public class PowerUp extends SDynamicEntity {
	
	// The controllers corresponding to the entity that runs into this powerup.
	SheepMovementController _mc = null;
	JumpController          _jc = null;
	
	protected static int _numOutstandingPUs;
	public static int GetOutstandingPUs() { return _numOutstandingPUs; }
	public static void ResetOutstandingPUs() {_numOutstandingPUs = 0; }
	
	protected int _ttl;
	protected Spatial _visSphere;
	
	boolean _isActive = false;
	public void setActive(boolean b) { _isActive = b; }
	public boolean isActive() { return _isActive; }
	
	protected float _eatScale = 1.0f;
	
	
	protected PowerUpInfo.PowerUpType _puType;
	public    PowerUpInfo.PowerUpType getPUType() { return _puType; }
	
	protected SDynamicEntity _hostDE;
	public    SDynamicEntity getDE() { return _hostDE; }
	
	// Once activated, this handler will take care of doing whatever this PU does.
	PhysicsUpdateCallback _cb;
	
	public String getFakeClass() { return "PowerUp" + getPUType(); }
	
	// Holds the list of correctors used for sticky powerups.
	protected HashMap<String, SpringCorrector> _theCorrections = new HashMap<String, SpringCorrector>();

	
	/////////////////////////////////////////////////////////////////	
	public PowerUp(String name, int ttl, PowerUpInfo.PowerUpType puType, Vector3f pos) { 
		super(name, Utils.IMPOSSIBLE_CLIENT_ID, pos);
		_puType = puType;
		_ttl    = ttl;
		
		
		Utils.setColor(_visSphere, 
				//ColorRGBA.green); 
				 PowerUpInfo.GetPUColor(getPUType()));
		
		// Bump the num outstanding.
		_numOutstandingPUs++;
	}
	
	/////////////////////////////////////////////////////////////////	
	@Override
	protected void makeBody() {
		DynamicPhysicsNode n = getNode();
		n.setMaterial(Material.GHOST);
		
		_visSphere =  new Sphere(getName(), new Vector3f(), 16, 16, .4f);
		
		// visSphere =  new Cylinder(n.getName(), 8, 8, 0.5f, 0.75f, true);
		n.attachChild(_visSphere);

		// visSphere.setModelBound(new OrientedBoundingBox());
		_visSphere.setModelBound(new BoundingSphere());
		_visSphere.updateModelBound();

	}

	/////////////////////////////////////////////////////////////////	
	public void findControllers(SDynamicEntity de) {
		
		String mcName = SheepMovementController.class.getName();
		String jcName = JumpController.class.getName();
		
		_mc = (SheepMovementController) de.getController(mcName);
		_jc = (JumpController) de.getController(jcName);

		// We better have found these controllers. 
		assert(_mc != null);
		assert(_jc != null);
	}
	
	
	// Do whatever it is that you do.
	/////////////////////////////////////////////////////////////////
	public void activate(SDynamicEntity de) {
		
		// Save the entity that activated me, and tell it that I'm its powerup.
		_hostDE = de;
		de.powerUp(this);
		
		// Find movement and jump controller.
		findControllers(de);

		// Assume this is a valid activation.
		setActive(true);
		
		// There's now one less PU available.
		_numOutstandingPUs--;

		String prettyName = "\"" + de.getName() + "\"";
		switch (getPUType()) {
		case SPEEDY:
			System.out.println("Making " + prettyName + " speedy!");
			float oldSpeed = _mc.getSpeedPowerUp();
			_mc.setSpeedPowerUp(oldSpeed * 2.0f);
		break;
		case JUMPER:
			System.out.println("Making " + prettyName + " Jordan!");
			float oldJumpSpeed = _jc.getModifier("jumper");
			_jc.updateModifier("jumper", oldJumpSpeed * 2.0f);
		break;	
		}
		
		// For monitoring the state of callbacks, register an update handler.
		_cb = new PhysicsUpdateCallback() {
			public void beforeStep( PhysicsSpace space, float time ) { update(); }
			public void afterStep( PhysicsSpace space, float time ) {}
		};
		Utils.GetSps().getPhysics().addToUpdateCallbacks(_cb); 
		
		// Power this guy up on the client end.
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("powerUp");
		im.setArguments(new Object[] {
				_hostDE.getName(),
				getPUType()});
		im.sendToAll(); // (de.getID());
	}

	
	// When the entity to whom this powerup is applied collides with another DEntity,
	// apply this corrector to that entity.
	public void addSticky(SDynamicEntity toWhat) {
		SDynamicEntity hostDE = getDE();
		Vector3f pos = new Vector3f(hostDE.getNode().getLocalTranslation());
		
		// Make a new corrector and add it to the list.
		String stickName = "sticky-"+hostDE.getName()+"-"+toWhat.getName();
		System.out.println("Adding sticky \"" + stickName + "\"");
		SpringCorrector sc = SpringCorrector.create(stickName, Utils.GetSps().getPhysicsSpace(), toWhat.getNode(), pos);
		_theCorrections.put(toWhat.getName(), sc);
	}
	
	
	
	public void applyStick() {
		// Go through the list of correctors I've applied and set their position to the position of the host.
		Vector3f hostPos = new Vector3f(getDE().getNode().getLocalTranslation());
		Set<String> names = _theCorrections.keySet();
		for (String n : names) {
			SpringCorrector sc = _theCorrections.get(n);
			System.out.println("Updating " + sc.getName());
			sc.setLocation(hostPos);
		}
	}

	
	/////////////////////////////////////////////////////////////////
	public void deactivate() {

		// Remove the update function from the callback.
		if (_cb != null) {
			Utils.GetSps().getPhysics().removeFromUpdateCallbacks(_cb);
			_cb = null;
		}
		
		// Tell client about it.
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("powerDown");
		im.setArguments(new Object[] {
				_hostDE.getName(),
				getPUType()});
		im.sendToAll();
		
		// If this thing has already been deactivated, return.  Otherwise, deactivate.
		if (!isActive()) { return; }
		setActive(false);
		
		// Do PU-specific deactivations on the host DE.
		switch (getPUType()) {
		case SPEEDY:
			float oldSpeed = _mc.getSpeedPowerUp();
			float newSpeed = Math.max(1, oldSpeed / 2.0f);
			
			_mc.setSpeedPowerUp(newSpeed);
		break;
		
		case JUMPER:
			float oldJumpSpeed = _jc.getModifier("jumper");
			float newJSpeed = Math.max(1, oldJumpSpeed / 2.0f);
			
			_jc.updateModifier("jumper", newJSpeed);
		break;
		
		case EATER:
//			_eatScale = 1.0f;
//			scaleHost();
		break;
		
		case POWERDOWN:
			_ttl = 1;
		break;
		
		case STICKY:
			// Remove the extant correctors.
			Set<String> names = _theCorrections.keySet();
			for (String n : names) {
				SpringCorrector sc = _theCorrections.get(n);
				sc.deactivate();
			}
		break;
		
		default:
			Logger.getLogger("").severe("Got deactivate for unsupported PU " + getPUType());
		break;
		}
		
		System.out.println("PowerUp " + getName() + " de-activated.");
	}


	// Look through the list of the stuff the host entity is colliding with.
	/////////////////////////////////////////////////////////////////
	HashMap<String, SDynamicEntity> getCollidedDEs() {
		
		SDynamicEntity host = getDE();
		HashMap<String, SDynamicEntity> retVal = new HashMap<String, SDynamicEntity>();
		
		// See if my host entity is colliding with any sheep.  If so, add 'em.
		Hashtable<String, ContactInfo> col = CollisionManager.Get().getCollisions(host.getName());
		
		if (col != null) {
			Set<String> withWhom = col.keySet();
			for (String name : withWhom) {
				// If the thing the host is colliding with is a DynamicEntity, and if I don't already 
				// have a corrector on the motherfucker, slap one on.
				SEntity e = SEntity.GetEntityForName(name);
				if (e instanceof SDynamicEntity) { retVal.put(name, (SDynamicEntity)e); }
			}
		}
		return retVal;
	}
	
	
	int _ticksSinceCorrection = 0;
	int _correctionThresh     = 10;
	/////////////////////////////////////////////////////////////////
	public void update() {
		super.update(0);
		
		// This will save time later.
		SheepPhysicsState sps = Utils.GetSps();
		
		// Don't do anything if I've already been deactivated.
		if (!isActive()) { return; }

		// See if it's time to deactivate.
		// FIXME: Parameterize this
		_ttl--;
		if (_ttl <= 0) {
			deactivate();
			return;
		}

		// Get the DEs the host collided with.
		HashMap<String, SDynamicEntity> col = getCollidedDEs();
		Set<String> withWhom               = col.keySet();
		SDynamicEntity host = getDE();
		
		for (String name : withWhom) {
			SDynamicEntity de = col.get(name);

			switch(getPUType()) {
				case STICKY:
					// If the thing the host is colliding with is a DynamicEntity, and if I don't already 
					// have a corrector on the motherfucker, slap one on.
					if (!_theCorrections.containsKey(name) && (de instanceof Sheep)) { addSticky(de); }
				break;
				
				case BOUNCE:
					// Vector3f curVelocity = host.getNode().getLinearVelocity(null);
					Vector3f direction = host.getRotation().getRotationColumn(2);
					direction.normalizeLocal();
					direction = direction.mult(10); // 3 * curVelocity.y);

					direction.y = 10; // currVelocity.y;

					de.getNode().setLinearVelocity(direction);
					de.getNode().setAngularVelocity(new Vector3f(0,5,0));
				break;
				
				case EATER:
					// If I ran into a sheep, eat it, grow, and score it for the eater's team.
					boolean deactivate = false;
					if (de instanceof Sheep) {
						Sheep s = (Sheep)de;
						// If the sheep has already been scored, don't do any of this stuff.
						if (s.isScored() == false) {
							s.setScored();
							int wubTeam = ((Wubble)_hostDE).getTeam();
							
							// FIXME: GameMechanics should be responsible for removing them, and for making 
							// sure they're not being scored twice.  See also Sheep::onCollision
							sps.getGameMechanics().scoreSheep(s, wubTeam); 
							_eatScale *= 1.1f; 
							scaleHost();
							sps.removeEntity(de);
							System.out.println("Eater: " + getName() + " -- wubble on team " + wubTeam + " ate sheep " + de.getName());
							deactivate = true;
						}
					}
					
					if (de instanceof Wubble) {
						Wubble w = (Wubble)de;
						sps.getGameMechanics().moveToHomeGoal(w);
						deactivate = true;
					}
					
					// Eater is too powerful to allow a wubble to have it for more than a moment.
					if (deactivate) { _ttl = 1; }
				break;
			}
		}
		// Non collision-based updating.
		switch(getPUType()) {
			case STICKY:
				// Update the sticky correctors.
				_ticksSinceCorrection++;
				if (_ticksSinceCorrection >= _correctionThresh) {
					_ticksSinceCorrection = 0;
					System.out.println("Applying stick from " + getName()) ;
					applyStick();
				}
			break;
		}
	}

	// Send the new scale to clients, and scale the host.
	/////////////////////////////////////////////////////////////////
	protected void scaleHost() {
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("setScale");
		im.setArguments(new Object[] {
				_hostDE.getName(),
				_eatScale});
		im.sendToAll();
		
		System.out.println("Scaling " + _hostDE.getName() + " to new scale " + _eatScale);
		DynamicPhysicsNode dpn = _hostDE.getNode();
		dpn.setLocalScale(_eatScale);

	}
	
	/////////////////////////////////////////////////////////////////
	protected void makePhysics() {
		super.makePhysics();
		getNode().setAffectedByGravity(false);
	}
	
	/////////////////////////////////////////////////////////////////
	@Override
	public void onCollision(Hashtable<String, ContactInfo> collisions) {
		super.onCollision(collisions);
		
		// If I've already activated this PU, don't do anything else.  (Sometimes
		// can get phantom collisions when the collision update cycle is long.)
		if (isActive()) { return; }
		
		// See if it's a wubble that crashed into me.		
		SheepPhysicsState w = SheepPhysicsState.getWorldState(); 
		Set<String> s = collisions.keySet();
		for (String eName : s) {
			SEntity e = SEntity.GetEntityForName(eName);
			if (e instanceof Wubble && !(e instanceof Sidekick)) {
				Wubble wub = (Wubble)e;
				// System.out.println("PUP \"" + getName() + "\" type " + getPUType() + " collided with: \"" + eName + "\"");
				
				// Give the Wubble some love.  (Not literally.)
				activate(wub);
				
				// Schedule myself for removal.
				w.removeEntity(getName());
			}			
		}
	}
}
