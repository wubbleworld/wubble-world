package edu.isi.wubble.jgn.rpg;

import static com.jme.input.InputHandler.AXIS_NONE;
import static com.jme.input.InputHandler.BUTTON_ALL;
import static com.jme.input.InputHandler.DEVICE_MOUSE;
import static com.jme.util.resource.ResourceLocatorTool.TYPE_MODEL;
import static edu.isi.wubble.jgn.message.InvokeMessage.createMsg;
import static java.lang.Math.floor;
import static java.lang.Math.random;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.captiveimagination.jgn.synchronization.message.Synchronize3DMessage;
import com.jme.bounding.BoundingBox;
import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.MouseInput;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.KeyInputAction;
import com.jme.light.PointLight;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.LightState;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.game.state.GameStateManager;

import edu.isi.wubble.AutoBinding;
import edu.isi.wubble.gamestates.PhysicsGameState;
import edu.isi.wubble.jgn.db.DatabaseManager;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.EnemyEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;
import edu.isi.wubble.util.Globals;

public class RPGPhysics extends PhysicsGameState {

	public static final int SHOOTER = 0;
	public static final int PICKER = 1;
	
	protected WordScrambler _scrambler;
	
	protected EntityManager _entityManager;
	protected RPGPhysicsDB  _db;
	
	protected TreeMap<Short,Integer>        _clientToRole;
	protected TreeMap<Short,String>         _clientToName;
	protected TreeMap<Integer,Short>        _roleToClient;
	protected TreeMap<Integer,String>       _selectedMap;
	protected TreeMap<Integer,RPGWubbleEntity> _roleMap;
	protected TreeMap<Short,TreeSet<String>> _clientToKnownWords;

	protected int _coinsLeft = 5;
	protected long _lastUpdate = 0;
	
	protected int  _id;
	
	/**
	 * 
	 * @param parent
	 * @param mediaURL
	 */
	public RPGPhysics(String name, int identifier) {
		super();
		
		_id = identifier;
		this.name = name;
		
		_clientToRole = new TreeMap<Short,Integer>();
		_clientToName = new TreeMap<Short,String>();
		_roleToClient = new TreeMap<Integer,Short>();
		
		_selectedMap = new TreeMap<Integer,String>();
		_roleMap = new TreeMap<Integer,RPGWubbleEntity>();
		
		_scrambler = new WordScrambler();
		_clientToKnownWords = new TreeMap<Short,TreeSet<String>>();
	}
	
	public void setup() {
		Globals.SHANE_PRINTING = false;
		
		init();
		setActive(false);
		GameStateManager.getInstance().attachChild(this);
	}
	
	public String getPlayerName(int role) {
		Short client = _roleToClient.get(role);
		if (client == null)
			return null;
		
		return _clientToName.get(client);
	}
	
	public int getId() {
		return _id;
	}
	
	public int getAmountDone() {
		return (int) (100.0f * ((5 - _coinsLeft) / 5.0f));
	}
	
	public Collection<Short> getClientIds() {
		return _roleToClient.values();
	}
	
	public void startSession() {
		_db = new RPGPhysicsDB();
		_db.createSession();
	}
	
	/** 
	 * called from the server to add a user to this game with this
	 * role.  Trouble would be that in fact rather than just simply
	 * storing the name of the current person playing this role, we
	 * really need to store the name of all the people who played
	 * this role for this session.
	 * @param id
	 * @param userName
	 * @param password
	 * @param role
	 */
	public boolean login(Short id, String userName, String password, Integer role) {
		// validate against the database maybe?
		if (!DatabaseManager.inst().authenticate(userName, password))
			return false;
		
		if (_clientToRole.containsKey(id)) {
			System.out.println("ERROR - we already have this client");
		}
		if (_roleToClient.containsKey(role)) {
			System.out.println("ERROR - we already have this role");
		}
		
		System.out.println("adding: " + userName + " " + id + " " + role);
		_clientToRole.put(id, role);
		_clientToName.put(id, userName);
		_roleToClient.put(role, id);
		_clientToKnownWords.put(id, _db.getKnownWords(userName));
		
		_db.receivedMsg("login", new Object[] { userName, getRoleName(role) });
		
		return true;
	}
	
	public void removeUser(final Short id) {
		if (_clientToRole.size() == 1) { 
			_db.finishSession();
		}
		
		Callable<?> callable = new Callable<Object>() {
			public Object call() throws Exception {
				clearActiveChoice(id);
				Integer role = _clientToRole.remove(id);
				_roleToClient.remove(role);
				_clientToName.remove(id);
				
				if (_clientToRole.size() == 0) { 
					setActive(false);
				}
				return null;
			}
		};
		GameTaskQueueManager.getManager().update(callable);
	}
	
	/**
	 * 
	 *
	 */
	protected void init() {
		_physics.setAutoRestThreshold(0.05f);
		_entityManager = new EntityManager(_physics, _input, _rootNode);

		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null) 
			MouseInput.get().setCursorVisible(true);
		
		long startTime = System.currentTimeMillis();
		
		initLights();
		initStatic();
		initDynamic();
		initWubbles();
		initEnemies();
		initCoins();
		
		addEntitiesToRoot();
		System.out.println("Initialization time: " + (System.currentTimeMillis() - startTime) + "ms");
		_timer.reset();
	}
	
	protected void initInput() {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null) {
			super.initInput();
			
	        KeyBindingManager keyboard = KeyBindingManager.getKeyBindingManager();
	        keyboard.set("pickUp", KeyInput.KEY_NUMPAD7);
	        keyboard.set("putDown", KeyInput.KEY_NUMPAD8);
	        keyboard.set("delete", KeyInput.KEY_DELETE);
	        keyboard.set("fixOrientation", KeyInput.KEY_NUMPAD9);
			
	    	_input.addAction(new InputAction() {
	            public void performAction( InputActionEvent evt ) {
	            	if (evt.getTriggerCharacter() == 'L' && !evt.getTriggerPressed()) {
	            		fireArrow();
	            	}
	            }
	        }, DEVICE_MOUSE, BUTTON_ALL, AXIS_NONE, false );

	    	_input.addAction(new KeyInputAction() {
	        	public void performAction(InputActionEvent evt) {
	        		if (_selectedMap.get(PICKER) == null) 
	        			_selectedMap.put(PICKER,"crate1");
	        		
	        		_roleMap.get(PICKER).pickUp(_selectedMap.get(PICKER));
	        	}
	        }, "pickUp", false);
	        
	        _input.addAction(new KeyInputAction() {
	        	public void performAction(InputActionEvent evt) {
	        		_roleMap.get(PICKER).putDown();
	        	}
	        }, "putDown", false);

	        _input.addAction(new KeyInputAction() {
	        	public void performAction(InputActionEvent evt) {
	        		//fixOrientation();
	        	}
	        }, "fixOrientation", false);
		} else {
			_input = new InputHandler();
		}
	}
	
	/**
	 * overridden to keep from creating a camera
	 */
	protected void initCamera() {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null) {
			super.initCamera();
		}
	}
	
	protected void initLights() {
	    LightState lightState = DisplaySystem.getDisplaySystem().getRenderer().createLightState();
	    lightState.setEnabled( true );

	    /** Set up a basic, default light. */
	    PointLight light = new PointLight();
	    light.setDiffuse( new ColorRGBA( 0.75f, 0.75f, 0.75f, 0.75f ) );
	    light.setAmbient( new ColorRGBA( 0.5f, 0.5f, 0.5f, 1.0f ) );
	    light.setLocation( new Vector3f( 7.5f, 5, 7.5f ) );
	    light.setShadowCaster(true);
	    light.setEnabled( true );
	    lightState.attach(light);

        lightState.setGlobalAmbient(new ColorRGBA(0.6f, 0.6f, 0.6f, 1.0f));
        
	    /** Attach the light to a lightState and the lightState to rootNode. */
	    _rootNode.setRenderState(lightState);
		_rootNode.updateRenderState();
	}
	
	
	/**
	 * initialize the static components of the scene
	 * and generate their physics.
	 *
	 */
	protected void initStatic() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-cavern.jme");
			_entityManager.createEntity(url, true);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-static-tmp.jme");
			Node staticNode = (Node) BinaryImporter.getInstance().load(url);
			List<Spatial> children = staticNode.getChildren();
			while (children.size() > 0) {
				Spatial s = children.remove(0);
				s.setModelBound(new BoundingBox());
				s.updateModelBound();
				_entityManager.createEntity(s, true, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
	}

	/**
	 * initialize the dynamic elements of the scenes
	 * those without controllers.
	 *
	 */
	protected void initDynamic() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-dynamic.jme");
			Node staticNode = (Node) BinaryImporter.getInstance().load(url);
			Iterator<Spatial> iter = staticNode.getChildren().iterator();
			while (iter.hasNext()) {
				Node n = (Node) iter.next();
				List<Spatial> children = n.getChildren();
				while (children.size() > 0) {
					Spatial s = children.remove(0);
					ModelEntity.createEntity(_entityManager, s);
				}
			}
			
			_entityManager.createArrow();
			
		} catch (Exception e) {
			e.printStackTrace();
		}		    
	}
	
	protected void initWubbles() {
		RPGWubbleEntity we = new RPGWubbleEntity(this, _entityManager, "shooter");
		we.setPosition(new Vector3f(-5,5,-5));
		we.addMovementControls(getId() + "", true);
		we.addHealthCallbacks(_input, this);
		we.addShootingAbility(_physics);
		_roleMap.put(SHOOTER, we);

		we = new RPGWubbleEntity(this, _entityManager, "picker");
		we.setPosition(new Vector3f(-3,5,-5));
		we.addMovementControls(getId() + "", true);
		we.addHealthCallbacks(_input, this);
		we.addCarryingAbility();
		_roleMap.put(PICKER,we);
	}
		
	/**
	 * create all the stitches and put them in their correct 
	 * places.
	 *
	 */
	protected void initEnemies() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-stitches.jme");
			Node stitchesNode = (Node) BinaryImporter.getInstance().load(url);
			for (Spatial s : stitchesNode.getChildren()) {
				EnemyEntity.createEnemy(_entityManager, s, this);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}			
	}
	
	/**
	 * initialize the coins by selecting 5 total to 
	 * leave alive.  Those will be renamed coin1...coin5
	 */
	protected void initCoins() {
		try {
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-coins.jme");
			Node coinsNode = (Node) BinaryImporter.getInstance().load(url);
			List<Spatial> children = coinsNode.getChildren();
			for (int i = 1; i < 6; ++i) {
				Spatial s = children.remove((int) floor(random() * children.size())); 
				s.setName("coin" + i);
				ModelEntity.createCoin(this, _entityManager, s);
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}			
	}
	
	public void update(float tpf) {
		super.update(tpf);
		_rootNode.updateGeometricState(0, true);
		_entityManager.updateEntities(tpf);
		
		sendWorldUpdate(null);
	}
	
	public void render(float tpf) {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null) 
			super.render(tpf);
	}
	
	public void reset() {
		_clientToName.clear();
		_roleToClient.clear();
		_clientToRole.clear();
		_selectedMap.clear();
		_clientToKnownWords.clear();
		
		// we go in reverse order.... first removing all of the objects
		// coins, enemies, wubbles, dynamic...
		_entityManager.removeWubbles();
		_entityManager.removeEnemyEntities();
		_entityManager.removeDynamicEntities();

		_input.clearActions();
		_input.removeAllActions();
		_input.removeAllFromAttachedHandlers();
		
		_physics.removeAllFromUpdateCallbacks();
		
		_coinsLeft = 5;
		// we go in forward order and add all of the dynamic,wubbles,...
		//initInput();
		initDynamic();
		initWubbles();
		initEnemies();
		initCoins();
		
		addEntitiesToRoot();
	}
	
	public String getRoleName(int role) {
		if (role == SHOOTER) 
			return "shooter";
		else
			return "lifter";
	}
	
	/**
	 * if we have a visual component loop over all of the dynamic entities
	 * and add them to the visual component.
	 */
	protected void addEntitiesToRoot() {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null) {
			for (Entity e : _entityManager.getAllEntities()) {
				_rootNode.attachChild(e.getNode());
			}
		}
		
		_rootNode.updateRenderState();
		_rootNode.updateGeometricState(0, true);
	}
	
	/**
	 * 
	 * @param s
	 */
	public void setActiveChoice(Short id, String spatialName) {
		int role = _clientToRole.get(id);
		String originalSpatial = _selectedMap.get(role);
		if (originalSpatial != null) {
			clearActiveChoice(id);
		}
		
		Entity e = _entityManager.getEntity(spatialName);
		if (e == null) {
			System.out.println("[setActiveChoice] unknown entity " + spatialName);
			return;
		}
		
		if (!e.isPickable()) {
			System.out.println("[setActiveChoice] unknown pickable: " + spatialName);
			return;
		}

		_selectedMap.put(role, spatialName);
		InvokeMessage msg = createMsg("addHighlight", new Object[] { spatialName+role, spatialName });
		msg.sendToGroup(getClientIds());
		_db.sendingMsg(msg);
	}
	
	/**
	 * 
	 * @param id
	 */
	public void clearActiveChoice(Short id) {
		Integer role = _clientToRole.get(id);
		if (role == null) 
			return;
		
		String originalSpatial = _selectedMap.remove(role);
		InvokeMessage msg = createMsg("clearHighlight", new Object[] { originalSpatial+role });
		msg.sendToGroup(getClientIds());
		_db.sendingMsg(msg);
	}
	
	public void tryTranslation(Short id, String jumbled, String english) {
		String actualTranslation = _scrambler.translateWord(english);
		String userName = _clientToName.get(id);
		InvokeMessage msg = null;
		if (actualTranslation.equals(jumbled)) {
			msg = createMsg("translateResult", new Object[] { true });
			_db.saveWord(userName, english);
			_clientToKnownWords.get(id).add(english);
		} else {
			msg = createMsg("translateResult", new Object[] { false });
		}
		msg.sendTo(id);
		_db.receivedMsg("translate", new Object[] { id, jumbled, english });
		_db.sendingMsg(msg);
	}

	@Override
	public float getPctDone() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * cleanup is called by the state manager when the game
	 * is quitting.
	 */
	public void cleanup() {
		RPGServer.inst().quit();
	}
	
	protected void pickingUpCoin() {
		final Callable<?> callable = new Callable<Object>() {
			public Object call() throws Exception {
				putDown(_roleToClient.get(PICKER));

				String name = _entityManager.removeEntity(_selectedMap.get(PICKER));
				if (name != null) {
					sendRemoveMessage(name);
				}
				--_coinsLeft;
				
				sendCoinUpdate();
				if (_coinsLeft == 0) {
					InvokeMessage msg = InvokeMessage.createMsg("playWinningState", null);
					msg.sendToGroup(getClientIds());
					_db.sendingMsg(msg);
					reset();
				}
				return null;
			}
		};
		
		TimerTask coinTask = new TimerTask() {
			public void run() {
				GameTaskQueueManager.getManager().update(callable);
			}
		};
		
		Timer t = new Timer();
		t.schedule(coinTask, 5000);
	}
	
	public void sendCoinUpdate() {
		InvokeMessage msg = createMsg("updateCoinsLeft", new Object[] { 5-_coinsLeft });
		msg.sendToGroup(getClientIds());
		_db.sendingMsg(msg);
	}
	
	public void sendRemoveMessage(String name) {
		InvokeMessage msg = createMsg("removeEntity", new Object[] { name });
		msg.sendToGroup(getClientIds());
		_db.sendingMsg(msg);
	}
	
	//--------------------------------
	// below are methods that are invoked 
	// from clients as well as methods that
	// broadcast information to clients.
	//--------------------------------

	public void sendChatMessage(Short id, String sentence) {
		Integer role = _clientToRole.get(id);
		String userName = _clientToName.get(id);
		String roleName = getRoleName(role);
		TreeSet<String> knownSet = _clientToKnownWords.get(id);
		if (knownSet == null) {
			System.out.println("SHOULD NOT HAPPEN");
			knownSet = new TreeSet<String>();
		}
		
		String noPunctuation = sentence.replaceAll("\\p{Punct}", " ");
		StringBuffer buf = new StringBuffer();
		StringTokenizer str = new StringTokenizer(noPunctuation, " ");
		while (str.hasMoreTokens()) {
			String word = str.nextToken().toLowerCase();
			if (knownSet.contains(word))
				buf.append(word);
			else
				buf.append(_scrambler.translateWord(word));
			buf.append(" ");
		}

		if (_clientToRole.size() == 2) {
			InvokeMessage msg = createMsg("chatMsg", new Object[] { userName, buf.toString() });
			if (role == SHOOTER) {
				msg.sendTo(_roleToClient.get(PICKER));
			} else {
				msg.sendTo(_roleToClient.get(SHOOTER));
			}
		}
		_db.receivedMsg("chat", new Object[] { userName, roleName, sentence });
	}
	
	/**
	 * Movement messages come from the client when they wish
	 * to do something different with their movements
	 * @param userName
	 * 		the client initiating the movement message
	 * @param action
	 * 		the movement action to be executed
	 * @param active
	 * 		the state the action should be in
	 */
	public void movementMsg(Short id, String action, Boolean active) {
		System.out.println("id: " + id + " action: " + action + " active: " + active);
		int role = _clientToRole.get(id);
		if (role == SHOOTER) 
			AutoBinding.bindingMsg("shooter" + getId(), action, active);
		else if (role == PICKER) {
			AutoBinding.bindingMsg("picker" + getId(), action, active);
		}
		
		String name = getRoleName(role);
		_db.receivedMsg("move", new Object[] { name, action, active });
	}
	
	public void pickUp(Short id) {
		int role = _clientToRole.get(id);
		String object = _selectedMap.get(PICKER);
		if (role != PICKER || object == null) 
			return;
		
		if (object.startsWith("coin")) {
			pickingUpCoin();
		}
		_roleMap.get(PICKER).pickUp(object);

		InvokeMessage msg = createMsg("play", new Object[] { PICKER, "pickUp" });
		msg.sendToGroup(getClientIds());
		_db.receivedMsg("pickup", new Object[] { getRoleName(role), object });
	}
	
	public void putDown(Short id) {
		int role = _clientToRole.get(id);
		if (role != PICKER) 
			return;
		
		_roleMap.get(PICKER).putDown();
		
		InvokeMessage msg = createMsg("play", new Object[] { PICKER, "putDown" });
		msg.sendToGroup(getClientIds());
		_db.receivedMsg("putdown", new Object[] { getRoleName(role) });
	}
	
	/**
	 * stand the object back up as long as it is the picker
	 * issuing the command.  If it is the shooter, then they
	 * aren't allowed to stand things up.
	 * @param id
	 */
	public void fixOrientation(Short id) {
		int role = _clientToRole.get(id);
		String object = _selectedMap.get(PICKER);
		if (role != PICKER || object == null) 
			return;
		
		DynamicEntity de = _entityManager.getDynamicEntity(object);
		if (de != null) 
			de.fixOrientation();

		_db.receivedMsg("fix", new Object[] { getRoleName(role), object });
	}
	
	/**
	 * send out the world update as long as everything looks good
	 * we are sending it on the fast server, so that means we are
	 * sending it UDP, which is good because I don't care if we lose 1
	 * @param wum
	 */
	private void sendWorldUpdate(WorldUpdateMessage wum) {
		long timeSinceUpdate = System.currentTimeMillis() - _lastUpdate;
		if (timeSinceUpdate < 35) 
			return;

		for (DynamicEntity de : _entityManager.getAllDynamicEntities()) {
			if (!de.isResting()) {
				RPGServer.inst().sendToAll(getClientIds(), de.generateSyncMessage());
			}
		}
		
		for (DynamicEntity de : _entityManager.getAllHidden()) {
			if (!de.isResting()) {
				if (_roleToClient.get(SHOOTER) != null) {
					Synchronize3DMessage msg = de.generateSyncMessage();
					RPGServer.inst().sendTo(_roleToClient.get(SHOOTER), msg);
				}
				
				if (_roleToClient.get(PICKER) != null) {
					Synchronize3DMessage msg = de.generateSyncMessage();
					msg.setPositionX(0); msg.setPositionY(-10); msg.setPositionZ(0);
					RPGServer.inst().sendTo(_roleToClient.get(PICKER), msg);
				}
			}
		}
		_lastUpdate = System.currentTimeMillis();
	}
			
	/**
	 * 
	 */
	public void fireArrow() {
        Callable<?> call = new Callable<Object>() {
            public Object call() throws Exception {
        		_roleMap.get(SHOOTER).fireArrow();
				return null;
            }
        };
        GameTaskQueueManager.getManager().update(call);
        
        _db.receivedMsg("fireArrow", new Object[] { getRoleName(SHOOTER) });
	}
	
	public void ready(Short id) {
		System.out.println("ready " + id);
		String user = _clientToName.get(id);
		int role = _clientToRole.get(id);
		
		if (role == SHOOTER) {
			_db.addUserAsShooter(user);
			_entityManager.initialMessages(id, false);
		} else {
			_db.addUserAsPicker(user);
			_entityManager.initialMessages(id, true);
		}

		RPGWubbleEntity.sendWubblesUpdate(_entityManager);
		sendCoinUpdate();
		setActive(true);
	}
	
	
}
