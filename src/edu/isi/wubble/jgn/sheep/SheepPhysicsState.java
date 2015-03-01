package edu.isi.wubble.jgn.sheep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.jme.input.FirstPersonHandler;
import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.MouseInput;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.controls.GameControlManager;
import com.jme.light.DirectionalLight;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.SceneElement;
import com.jme.scene.Text;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;
import com.jmex.game.state.GameStateManager;
import com.jmex.physics.PhysicsSpace;
import com.jmex.physics.contact.MutableContactInfo;
import com.jmex.physics.material.Material;

import edu.isi.wubble.AutoBinding;
import edu.isi.wubble.Constants;
import edu.isi.wubble.gamestates.PhysicsGameState;
import edu.isi.wubble.gamestates.WubbleGameState;
import edu.isi.wubble.jgn.db.DatabaseManager;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;
import edu.isi.wubble.jgn.sheep.action.AddForceInputAction;
import edu.isi.wubble.jgn.sheep.action.ChangeCurEntityAction;
import edu.isi.wubble.jgn.sheep.action.CollisionManager;
import edu.isi.wubble.jgn.sheep.action.ToggleEntitiesActiveInputAction;
import edu.isi.wubble.jgn.sheep.action.ToggleEntityActiveInputAction;
import edu.isi.wubble.jgn.sheep.audio.AudioServer;
import edu.isi.wubble.jgn.sheep.entity.PowerUp;
import edu.isi.wubble.jgn.sheep.entity.SDynamicEntity;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.Wubble;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.physics.entity.controller.LookingAtEachOtherController;
import edu.isi.wubble.physics.entity.controller.VisualSalienceController;
import edu.isi.wubble.physics.state.StateDatabase;
import edu.isi.wubble.sheep.Replayer;
import edu.isi.wubble.sheep.gui.SheepReplayGUI;
import edu.isi.wubble.util.Globals;
import edu.isi.wubble.util.Swift;

public class SheepPhysicsState extends PhysicsGameState implements MessageListener {
	// ////////////////////////////////////////////////////////////
	// Private classes
	// ////////////////////////////////////////////////////////////
	public static int ER_ID = 0;
	
	public static boolean PRODUCTION = true;
	
	// This is deployed to remove entities in a thread-safe manner.
	// ////////////////////////////////////////////////////////////
	public class EntityRemover implements Callable<Integer> {
		String _eName;
		int _myID;
		
		public EntityRemover(String eName) { _eName = eName; register(); }
		public EntityRemover(short id) {
			SEntity e = SDynamicEntity.GetDynEntityForID(id);
			if (e == null) {
				Logger.getLogger("").log(Level.SEVERE, 
						"EntityRemover: register removal for ABSENT dynEntity w/ id " + id);
				return;
			}
			_eName = e.getName();
			register();
		}
		
		private void register() {
			// Register the removal with the game task manager.  It will execute at the next update cycle.
			_myID = ER_ID++;
			
			//p("EntityRemover ["+_myID+"]: registered to remove " + _eName);
			GameTaskQueueManager.getManager().update(this);
		}
		
		public Integer call() throws Exception {
			//p("EntityRemover ["+_myID+"]: calling newRemove on \"" + _eName + "\"");
			newRemove(_eName);
			return 10;
		}
	}
	

	
	// ////////////////////////////////////////////////////////////
	// General world stuff
	// ////////////////////////////////////////////////////////////

	// Arena
	Arena _arena = null;
	
	// Entity player/replayer.
	Replayer _replayer;
	public Replayer getReplayer() { return _replayer; }

	// For controlling entities.
	protected GameControlManager _manager = new GameControlManager();
	public GameControlManager getGCM() { return _manager; }
	

	// Entities that are in the world
	private Hashtable<String, SEntity> _entities = null;
	
	public void                        setEntities(Hashtable<String, SEntity> _entities) {	this._entities = _entities; }
	public Hashtable<String, SEntity>    getEntities()     { return _entities; }
	protected Hashtable<String, SEntity> makeEntityHash()  { return new Hashtable<String, SEntity>(); }
	

	// A special one for wubbles, keyed off client IDs.
	public HashMap<Short, Wubble> _wubbleIDs = new HashMap<Short, Wubble>();
	public HashMap<Short, Wubble> getWubbleIDs() { return _wubbleIDs; }
	
	// There's a notion, that may go away, of a current entity, to which input
	// controllers apply.
	private SDynamicEntity _curEntity = null;
	public SDynamicEntity getCurEntity() { return _curEntity; }
	public void   setCurEntity(SDynamicEntity curEntity) { this._curEntity = curEntity; }
	
	
	AudioServer _audio;

	// Database manager for storing sheep logs.
	DatabaseManager _dbm;
	StateDatabase _sdb;
	public DatabaseManager getSheepDB() { return _dbm; }
	public StateDatabase getStateDB()   { return _sdb; }
	

	// Wraps the game-mechanic-specific code in its own class.
	protected GameMechanics _gm;
	public    GameMechanics getGameMechanics() { return _gm; }

	// Display stuff.
	Text _spsDisplay;
	Node _spsDisplayNode;
	protected StringBuffer _spsDisplayBuffer = new StringBuffer(40);

	
	// Construction
	// ////////////////////////////////////////////////////////////
	public SheepPhysicsState() {
		super();
		name = "SheepPhysics";
		
	    // Then our font Text object.
        _spsDisplay = Text.createDefaultTextLabel("SPS label");
        _spsDisplay.setCullMode(SceneElement.CULL_NEVER);
        _spsDisplay.setTextureCombineMode(TextureState.REPLACE);

        // Finally, a stand alone node (not attached to root on purpose)
        _spsDisplayNode = new Node( "SPS display node" );
        _spsDisplayNode.setRenderState(_spsDisplay.getRenderState( RenderState.RS_ALPHA ) );
        _spsDisplayNode.setRenderState(_spsDisplay.getRenderState( RenderState.RS_TEXTURE ) );
        _spsDisplayNode.attachChild(_spsDisplay);
        _spsDisplayNode.setCullMode(SceneElement.CULL_NEVER );
        
        _spsDisplayNode.updateGeometricState(0.0f, true);
        _spsDisplayNode.updateRenderState();
        
        // Make a player.
        _replayer = new Replayer(this);
	}
	
	
	public void setup() {
		setActive(true);
		GameStateManager.getInstance().attachChild(this);
		
		// System.out.println("SheepPhysicsState - setup");
		
		init();
	}

	private void setupDBLog() {
		// Run a db test.  (In real life I'll have to add my own connection, since this is the one that login uses
		// and I don't think I can switch it on the fly, can I?
		//_dbm = DatabaseManager.inst();
		
//		_dbm.setDB("jdbc:sqlite:sheep_log.db");
//		_dbm.setDriver("org.sqlite.JDBC");
		
		//_dbm.setDB("jdbc:mysql://www.wubble-world.com:3306/sheep");
		//_dbm.connect();

//		System.out.println("Creating sqlite sheep table.");
//		Statement stat = null;
//		try {
//			stat = _dbm.getStatement();
//		 		
//			String createStat = "CREATE TABLE if not exists sheep_log (" +
//			"update_index integer primary key autoincrement, " + 
//			"game_start bigint(20), " +
//			"name varchar(15), " +
//			"type varchar(15), " +
//			"flags int(11), " +
//			"x_rot float, " +
//			"y_rot float, " +
//			"z_rot float, " +
//			"w_rot float, " +
//			"x_pos float, " +
//			"y_pos float, " +
//			"z_pos float, " +
//			"time bigint(20)) ";
//		
//			stat.executeUpdate(createStat);
//			// _dbm.commit();
//		} catch (Exception e) { e.printStackTrace(); }
		
		// _dbm.setAutoCommit(true);
		
//		System.out.println("Created sqlite table.");
		
		// Setup the fluents and all that other shite.
		_sdb = StateDatabase.inst();
		
		System.out.println("Connecting to sheep db and state db");
	}

	
	// EntityManager migration stuff.
	WubbleGameState _srg;
	EntityManager _em;
	public EntityManager getEM() { return _em; }
	
	
	// Main init function; create a bunch of shit.
	// ////////////////////////////////////////////////////////////
	private void init() {
		// Turn on the database logging stuff.
		Globals.LOG_DYNAMIC  = true;
		Globals.USE_DATABASE = true;
		Globals.IN_SHEEP_GAME = true;
		
		this._showBounding = false;
		this._drawPhysics  = false;
		
		if (GetLogUpdates() == true) { 
			setupDBLog(); 
			StateDatabase.inst().createSession();
			StateDatabase.inst().beginTransaction();
		}
		
		// Create an EntityManager.
		assert(_input != null);
		_em = new EntityManager(getPhysicsSpace(), _input, _rootNode);

		
		_gm = new GameMechanics(this);
		_entities = makeEntityHash();
		
		//////////////////////		
		// Create a replay menu. - ONLY if we are not headless
		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null && false) {
			DisplaySystem.getDisplaySystem().getRenderer().getQueue().setTwoPassTransparency(false);
			
			String className = SheepReplayGUI.class.getName();
			
			WubbleGameState wbs = (WubbleGameState) GameStateManager.getInstance().getChild(className);
			if (wbs == null) {
				try {
					wbs = (WubbleGameState) Class.forName(className).newInstance();
					wbs.setName(className);
					GameStateManager.getInstance().attachChild(wbs);
				} catch (Exception e) { e.printStackTrace(); }
			}
			
			_srg = wbs;
			_srg.setActive(true);
		} else {
			_srg = null;
		}
		
		// Create an arena of appropriate dimensions.
		_arena = new Arena(this, 42, 20, 84);

		// Materials testing.
		Material wood = Material.WOOD;

		// Wood on wood contact is ten times less friction.
		MutableContactInfo deets = new MutableContactInfo();
		deets.setMu(100);

		// How to set contacthandling details?
		wood.putContactHandlingDetails(wood, deets);
		
		// Setup the assorted handlers, and the audio stream.
		setupHandlers();
		setupAudio();
		
		// Create the game mechs. and start the game.
		if (! _replayer.isReplay()) { _gm.setupGame(); }
		
		// This doesn't work yet; have to figure out how to fill the meshes appropriately
		// for the sorts of objects that are in the sheep game.
		
//		// Now that all of the objects are created we can be guaranteed
//		// that they all of have the correct meshes set up as well as
//		// all the other good stuff.  We now set up the SWIFT package
//		// used for tracking distances between objects.
//		Swift.inst();
//		
//		for (Entity e : getEM().getAllEntities()) {	e.addInitialProperties(); }
//		for (DynamicEntity e : getEM().getAllDynamicEntities()) { Swift.inst().addObject(e); }
//
//		// Write out the state db.
		if (GetLogUpdates()) { StateDatabase.inst().endTransaction(); }
		
		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null) {
			_rootNode.updateRenderState();
		}
	}


	
	// Audio
	// ////////////////////////////////////////////////////////////
	private void setupAudio() {
		_audio = new AudioServer();
		_audio.start();
	}
	
	public void startAudio(String name) {
		_audio.startAudio(name);
	}
	
	public void stopAudio(String name) {
		_audio.stopAudio(name);
	}
	

	// Seting up camera
	// ////////////////////////////////////////////////////////////
	public void initCamera() {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() == null)
			return;
		
		super.initCamera();
		
		Camera c = _camera;
		c.getLocation().x = 0;
		c.getLocation().y = 30;
		c.getLocation().z = 70;
		c.lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 5, -20));
		
		setupLighting();
	}
	
	
	// Seting up lighting
	// ////////////////////////////////////////////////////////////
	private void setupLighting() {
		DirectionalLight light = new DirectionalLight();
		light.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
		light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
		light.setDirection(new Vector3f(0, -10000, 0));
		light.setEnabled(true);

		LightState lightState = DisplaySystem.getDisplaySystem().getRenderer().createLightState();
		lightState.setEnabled(true);
		lightState.attach(light);
		getRootNode().setRenderState(lightState);
	}
	
	
	public void initInput() {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() == null) {
			_input = new InputHandler();
		} else {
			super.initInput();
			MouseInput.get().setCursorVisible(true);
			FirstPersonHandler fph = (FirstPersonHandler) _input;
			
			// Set this to true to get mouse control.
			fph.getKeyboardLookHandler().setEnabled(true);
			fph.getMouseLookHandler().setEnabled(false);
		}
	}
	
	// Hmmm.
//	public InputHandler getInput() { return _input; }
	

	// Working with entities
	// ////////////////////////////////////////////////////////////
	public void addEntity(SEntity e) {
		Node n = e.getNode();
		// rootNode.attachChild(n);
		getEntities().put(e.getName(), e);
		p("SPS::addEntity -- added entity \"" + e.getName() + "\" at position " + n.getLocalTranslation());
	}
	
	
	public void addCharToClient(SEntity e) {
		String className = e.getClass().getSimpleName();
		// This is so fucking negats.  Serious need of refactoring here.
		if (e instanceof PowerUp) {
			className = ((PowerUp)e).getFakeClass();
		}
		
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("addCharacter");
		im.setArguments(new Object[] {
				e.getName(),
				className,
				Utils.vectorToFloats(e.getPosition()),
				Utils.quaternionToFloats(e.getRotation())
		});

		im.sendToAll();
	}


	// Seting up keyboard handlers.  (Movement controllers for _curEntity are 
	// setup via the standard movement controllers described in DynamicEntity.)
	// ////////////////////////////////////////////////////////////
	protected void setupHandlers() {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() == null)
			return;
		
		_input.addAction(new AddForceInputAction(new Vector3f(0, 100, 0)), 
				InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_NUMPAD2,
				InputHandler.AXIS_NONE, true);
		
		// For stopping entities.
		_input.addAction(new ToggleEntityActiveInputAction(),
				InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_DIVIDE,
				InputHandler.AXIS_NONE, false);

		_input.addAction(new ToggleEntitiesActiveInputAction(),
				InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_MULTIPLY,
				InputHandler.AXIS_NONE, false);

		// For changing the current entity
		_input.addAction(new ChangeCurEntityAction(true),
				InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_ADD,
				InputHandler.AXIS_NONE, false);
		_input.addAction(new ChangeCurEntityAction(false),
				InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_SUBTRACT,
				InputHandler.AXIS_NONE, false);
				
		// Print stuff in the CE's fov.
		_input.addAction( new InputAction() {
			public void performAction(InputActionEvent evt) {
				if (!evt.getTriggerPressed()) { return; }
				SheepPhysicsState sps = Utils.GetSps();
				DynamicEntity ce = sps.getCurEntity();
				
				if (ce == null) { System.out.println("No current entity!"); return; }
				
				System.out.println("Entities salient to " + ce.getName() + ":");
				System.out.println("----------------------------------------------");
				
				VisualSalienceController vsc = (VisualSalienceController)ce.getController(VisualSalienceController.class.getName());
				if (vsc != null) { vsc.printSalienceMap(); }
				else { System.out.println("No salience controller!"); }
				
				System.out.println("\nEntities at whom " + ce.getName() + " is mutually looking:");
				System.out.println("--------------------------------------------------------");				
				// PairwiseUpdateController puc = ce.getPUC(); 
				LookingAtEachOtherController looking = (LookingAtEachOtherController) ce.getController(LookingAtEachOtherController.class.getName());
				if (looking != null) { looking.findLookAtEach(0.0f, true); } 
				else { System.out.println("No LookAtEach controller on " + ce.getName() + "!"); } 
			}
		},
			InputHandler.DEVICE_KEYBOARD, KeyInput.KEY_NUMPAD1,
			InputHandler.AXIS_NONE, false);

		
		///////////////////////////
		// Hacky handlers
		///////////////////////////
		
		KeyBindingManager.getKeyBindingManager().set("entityPos",    KeyInput.KEY_J);
		KeyBindingManager.getKeyBindingManager().set("pickUp",       KeyInput.KEY_U);
		KeyBindingManager.getKeyBindingManager().set("putDown",       KeyInput.KEY_D);
		KeyBindingManager.getKeyBindingManager().set("removeCurrent",   KeyInput.KEY_X);
		KeyBindingManager.getKeyBindingManager().set("toggleVolumes",   KeyInput.KEY_V);
		KeyBindingManager.getKeyBindingManager().set("togglePhysics",   KeyInput.KEY_P);
	}

	
	// ////////////////////////////////////////////////////////////
	public void printAllEntities() {
		Set<String> s = getEntities().keySet();
		
		for (String n : s) {
			System.out.println("  Entity is " + n);
		}
	}

	// Accessors and stuff
	// ////////////////////////////////////////////////////////////

	public PhysicsSpace getPhysicsSpace() {	return _physics; }
	public Node getRootNode() { return _rootNode; }
	
	public void p(String o) { System.out.println(o); }
	
	
	// In-process conversion to GameMechanics
	// ////////////////////////////////////////////////////////////		
	public void addWubble(String name, short id, Integer whichTeam) { 
		getGameMechanics().addWubble(name, id, whichTeam); 
	
		if (!SheepPhysicsState.PRODUCTION) {
			getGameMechanics().addSidekick(name);
		}
	}
	public void doResetGame() { getGameMechanics().doResetGame(); }
	public void finishReset() { getGameMechanics().finishReset(); }

	
	// ////////////////////////////////////////////////////////////
	// Removing things section.
	// ////////////////////////////////////////////////////////////

	// Use one of these methods to safely remove an entity
	// ////////////////////////////////////////////////////////////
	public void removeEntity(String eName) { new EntityRemover(eName);       }
	public void removeEntity(SEntity e)     { new EntityRemover(e.getName()); }
	public void removeEntity(short id)     { new EntityRemover(id);          }
	
	// ////////////////////////////////////////////////////////////
	// DON'T CALL THIS METHOD DIRECTLY!  THIS MEANS YOU!
	// Use removeEntity, which will schedule a Callable to call this at
	// a safe time.
	// ////////////////////////////////////////////////////////////
	public void newRemove(String eName) {
		SEntity e = SEntity.GetEntityForName(eName);
		// If the Entity can't be found, it must already have been removed.
		if (e == null) { 
			Logger.getLogger("").warning("call to newRemove when Entity \"" + eName + "\" is already removed.");
			return;
		}
		
		// If the user didn't call Entity::removeEntity, do it now.
		if (!e.isRemoved()) { e.remove(); }

		p("--- newRemove: telling clients to remove " + e.getName());

		// Send a message to all the clients that this entity was removed.
		InvokeMessage remMsg = new InvokeMessage();
		if (e instanceof Wubble) { 
			remMsg.setMethodName("removeWubble");
			_audio.removePlayer(e.getName());
			
			// Remove the Wubble's ID from the list of client IDs.
			Wubble w = (Wubble)e;
			Short id = w.getID();
			getWubbleIDs().remove(id);
			
		} 
		else { remMsg.setMethodName("removeCharacter"); } 


		remMsg.setArguments(new Object[] { e.getName() });			
		remMsg.sendToAll();
		
		// If I'm not replaying, log the fact that the entity was removed.
		if (_replayer.isReplay() == false && GetLogUpdates()) {
			WorldUpdateMessage.LogRemove(e);
		}
	}

	// Remove all the dynamic entities.
	public void removeDynamicEntities() {
		Hashtable<String, SEntity> entities = new Hashtable<String, SEntity>(getEntities());
		Set<Entry<String, SEntity>> es = entities.entrySet();
		for (Entry<String, SEntity> e : es) {
			SEntity entity = e.getValue();
			
			// If this is a dynamic entity, remove it.
			String name   = e.getKey();
			System.out.println("RDE: checking entity " + name);
			if (entity instanceof SDynamicEntity) { 
				//removeEntity(entity);
				entity.remove();
				System.out.println("RDE: Removed " + name);
			}
		}
	}
	
	// This gets called from both newRemove and Entity::remove(), so that entities can be removed from either place.
	public void removeMe(SEntity e) {
		// Remove it from the entity hash.
		getEntities().remove(e.getName());
		p("Entity \"" + e.getName() + "\" removed.");
	}

	



	// Overridden methods
	// ////////////////////////////////////////////////////////////
	
	/* WubbleGameState STUFF
	 * 
	 */
	
	@Override
	public float getPctDone() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public void cleanup() {
//		_audio.tearDown();

		for (Entity e : _em.getAllEntities()) {
			e.remove();
		}
		
		StateDatabase.inst().closeSession();
		StateDatabase.inst().endTransaction();
		Swift.inst().cleanup();
			
		Globals.USE_DATABASE = false;
		Globals.LOG_DYNAMIC = false;

		// Close the db connection.
		if (_dbm != null) {	_dbm.disconnect(); }
		
		SheepServer.Quit();		
	}
	
	
	// Render loop, called from JME.
	public void render(float tpf) {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() == null) {
			return; // do nothing, it is a headless game
		} else {
			// Do the standard visual game state render pass.
			super.render(tpf);
			
			// Also render the sps display.
			DisplaySystem.getDisplaySystem().getRenderer().draw(_spsDisplayNode);
		}
	}
	
	// static boolean _logUpdates = true;
	public static boolean GetLogUpdates() { return Globals.LOG_DYNAMIC; }
	
	public void sendWorldUpdate() {
		if (SheepServer.getServer() == null) { return; }
		
		WorldUpdateMessage upd = new WorldUpdateMessage();

		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> types = new ArrayList<String>();
		
		ArrayList<Float> x = new ArrayList<Float>();
		ArrayList<Float> y = new ArrayList<Float>();
		ArrayList<Float> z = new ArrayList<Float>();

		ArrayList<Float> rotX = new ArrayList<Float>();
		ArrayList<Float> rotY = new ArrayList<Float>();
		ArrayList<Float> rotZ = new ArrayList<Float>();
		ArrayList<Float> rotW = new ArrayList<Float>();
		
		for (SEntity e : getEntities().values()) {
			if (e instanceof SDynamicEntity) {
				SDynamicEntity de = (SDynamicEntity) e;
				names.add(de.getName());
				
				// Add this entity's type to the msg; this won't get sent to the client, 
				// but will be used in the logging.
				types.add(e.getClass().getSimpleName());
				
				Vector3f pos = de.getPosition();
				x.add(pos.x);
				y.add(pos.y);
				z.add(pos.z);
				
				Quaternion rot = de.getRotation();
				rotX.add(rot.x);
				rotY.add(rot.y);
				rotZ.add(rot.z);
				rotW.add(rot.w);
			}
		}
		
		upd.setNames(names);
		upd.setX(x);
		upd.setY(y);
		upd.setZ(z);
		upd.setRotX(rotX);
		upd.setRotY(rotY);
		upd.setRotZ(rotZ);
		upd.setRotW(rotW);
		
		// Send out the msg (WITHOUT type info.)
		SheepServer.getServer().getFastServer().broadcast(upd);
		
		// Set the types, and save this msg in the update logs.
		upd.setTypes(types);
	}


	
	// ////////////////////////////////////////////////////////////
	// Update
	// ////////////////////////////////////////////////////////////
	CollisionManager _cm = CollisionManager.Get();
	public void update(float tpf) {
		// Reset the collisions at the beginningof every update cycle.  (Later might want to do this 
		// more infrequently, to allow recording of super transient collisions.
		//_cm.resetCollisionHash();

		if (GetLogUpdates()) { _sdb.beginTransaction(); }
		//
		// Pre-physics updating
		//
		for (DynamicEntity de : getEM().getAllDynamicEntities()) { de.preUpdate(); }		
		
		// Update the state of the world for this tick.
		getReplayer().play(tpf);
		
		// Camera must be disabled for HEADLESS!!!
		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null) {
			_camera.update();
			handleUserInput();
		}

		// Update physics.
		super.update(tpf);

		// Delayed update
		for (DynamicEntity de : getEM().getAllDynamicEntities()) {
			if (de.getEntityType().equals("dynamic")) {	de.delayedUpdate(); } 
		}

		// Non-physics collisions get computed in this geometry update.
		_rootNode.updateGeometricState(tpf, true);
		
		// delayed update is used for recording purposes.  We need
		// to delay these calls until all objects have finished
		// local updates (movement and the like)
		for (DynamicEntity de : getEM().getAllDynamicEntities()) { de.delayedUpdate(); }
		
		// Do post-physics (and geometrical collision) processing on all entities.
		for (DynamicEntity de : getEM().getAllDynamicEntities()) { de.postUpdate(); }
		
		// If there are no wubbles in the game, go inactive.
		if (getWubbleIDs().size() == 0) {
			System.out.println("No wubbles in game; SPS going inactive.");
			setActive(false);
		}

		if (GetLogUpdates()) { _sdb.endTransaction(); }
	}	
	
	
	// Handle debug-class keypresses from the local console
	/////////////////////////////////////////////////////////////////	
	void handleUserInput() {
        if (KeyBindingManager.getKeyBindingManager().isValidCommand("camPos", false)) {
        	p("Camera pos: " + Utils.vs(_camera.getLocation()));
        }        
        
        if (KeyBindingManager.getKeyBindingManager().isValidCommand("pickUp", false)) {
        	// Get Jimbo, and have him pick up the active entity, assuming it's not himself.
        	
        	Wubble jimbo = (Wubble) DynamicEntity.GetEntityForName("Jimbo");
        	if (jimbo == null) { System.out.println("Can't find Jimbo! "); return; }
        	if (_curEntity == null) { System.out.println("No curEntity to pick up! "); return; }
        	// if (_curEntity == jimbo) { System.out.println("Jimbo can't pick himself up, dumb ass!"); return; }
        	
        	jimbo.pickup(_curEntity);
        }
        
        if (KeyBindingManager.getKeyBindingManager().isValidCommand("putDown", false)) {
        	// Get Jimbo, and have him pick up the active entity, assuming it's not himself.
        	
        	Wubble jimbo = (Wubble) DynamicEntity.GetEntityForName("Jimbo");
        	if (jimbo != null) { jimbo.putDown(); }
        }
        
        if (KeyBindingManager.getKeyBindingManager().isValidCommand("entityPos", false)) {
        	if (getCurEntity() != null) {
        		p("\"" + getCurEntity().getName() + "\"'s position: " + Utils.vs(getCurEntity().getPosition()));
        	}
        }
        
        if (KeyBindingManager.getKeyBindingManager().isValidCommand("removeCurrent", false)) {
        	SDynamicEntity ce = getCurEntity();
        	if (ce != null) {
        		p("Removing " + ce.getName());
        		ce.removeMovementControllers();
        		removeEntity(ce);
        		setCurEntity(null);
        	}
        }
        
        if (KeyBindingManager.getKeyBindingManager().isValidCommand("toggleVolumes", false)) {
        	if (_showBounding == true) { _showBounding = false; }
        	else { _showBounding = true; }
        }
        
        
        if (KeyBindingManager.getKeyBindingManager().isValidCommand("togglePhysics", false)) {
        	System.out.println("Toggling physics!");
        	if (_drawPhysics == true) { _drawPhysics = false; }
        	else { _drawPhysics = true; }
        }
	}
	
	
	/* JGN STUFF - implementing MessageListener
	 * 
	 */

	public void messageCertified(Message message) {
		// System.out.println("SPS: MESSAGE CERTIFIED: " + message);
	}

	public void messageFailed(Message message) {
		System.out.println("SPS: MESSAGE FAILED: " + message);
	}

	public void messageReceived(Message message) {
		//Class msgClass = message.getClass();
		//System.out.println("SPS: GOT A MESSAGE: " + message + " with -c- " + message.getClass());
		dispatchMessage(message);
		
	}

	public void messageSent(Message message) {
		//System.out.println("SPS: MESSAGE SENT: " + message);
		
	}
	
	// Methods to be invoked via InvokeMessage
	/////////////////////////////////////////////////////////////////
	public void login(Short id, String userName, String pass, Integer team) {
		System.out.println("SPS: Login message w/ cid " + id + " uname: " + userName + " pw " + pass);
		int wubbleTeam = team;
		addWubble(userName, id, wubbleTeam);
		
		// Send out the state of things.  Loop through all the dynamic entities.
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("setWorldState");
		im.setArguments(new Object[]{ constructDynamicEntityState(), _gm.getScore(Constants.BLUE_TEAM), _gm.getScore(Constants.RED_TEAM)});
		im.sendTo(id);
		
		// All the other clients need to be informed that there's a wubble, too.
		// Build the invokeMsg that will tell them so.
		im = new InvokeMessage();
		im.setMethodName("addWubble");
		im.setArguments(new Object[] {
				userName,
				ColorRGBA.green,
				wubbleTeam
		});

		im.sendToAllExcept(id);
	}
	
	
	// Assemble the list of entities
	/////////////////////////////////////////////////////////////////
	public Object[] constructDynamicEntityState() {
		//ArrayList<Object[]> crap = new ArrayList<Object[]>();
		ArrayList<Object> crap = new ArrayList<Object>();
		
		
		p("SPS: Building dyn entity state");
		// Iterate through all the dynamic entities in the system
		Hashtable<String, SEntity> h = getEntities();
		ArrayList<SEntity> entities = new ArrayList<SEntity>(h.values());

		for (SEntity e : entities) {
			// Get name, type, position, rotation, make an array out of it, and add to the master array.
			String className = e.getClass().getSimpleName();
			
			// Pass along the specific type of powerup.  Really, the update message should be 
			// modified for this sort of thing.
			if (className.equals("PowerUp")) { className = ((PowerUp)e).getFakeClass(); } 
//			"PowerUp" + ((PowerUp)e).getPUType(); }

			
			crap.add(new Object[] { e.getName(), className,
					Utils.vectorToFloats(e.getPosition()), Utils.quaternionToFloats(e.getRotation())});
		}
		
		return crap.toArray();
	}
	
	
	public void moveEntity(Integer entityID, Integer direction) {
		// p("Got moveEntity msg for direction " + direction + " for entity " + entityID);
	}
	
	
	// Moving the wubble triggers a number of movement-related predicates.
	public void moveWubble(String name, String action, Boolean status) {
		AutoBinding.bindingMsg(name, action, status);
		p("Got moveWubble msg - name " + name + " action " + action + " status " + status);
	}
	

	
	
	// TODO: Remember that these will need to be logged
	public void sendChatMessage(Short id, String text) {
		Wubble w = (Wubble) SDynamicEntity.GetDynEntityForID(id.shortValue());
		InvokeMessage im = new InvokeMessage();
		im.setArguments(new Object[]{w.getName(), w.getTeam(), text});
		im.setMethodName("chatMessage");
		im.sendToAllExcept(id);
		
		if (!SheepPhysicsState.PRODUCTION) {
			w.getSidekick().getAI().processUtterance(text);
		}
	}
	
	/////////////////////////////////////////////////////////////////	
	protected void dispatchMessage(Message m) {

		// Can't do this - mysterious shit going on in the back end.
		// if (m instanceof InvokeMessage) {
		if (Utils.FakeInstanceOf("InvokeMessage", m)) { 
			InvokeMessage invoke = (InvokeMessage)m;
//			System.out.println("SPS: dispatching invoke message w/ method " + invoke.getMethodName());			
			invoke.callMethod(this);
		} else {
//			System.err.println("SPS: What the hell kind of msg is: " + m + " class: " + m.getClass());		
		}
	}
	
	public void sayHi() { System.out.println("Hi!"); }
	
	
	public static SheepPhysicsState getWorldState() {
		String targetState = "SheepPhysics";
		
		SheepPhysicsState wbs = (SheepPhysicsState) GameStateManager.getInstance().getChild(targetState);
		if (wbs == null) {
			System.out.println("BAD BAD BAD NULLLLL!!!!");
		}
	
		return wbs;
	}
}


	
