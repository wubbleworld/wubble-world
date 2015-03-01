package edu.isi.wubble.jgn.chatworld;

import static com.jme.util.resource.ResourceLocatorTool.TYPE_MODEL;

import java.net.URL;
import java.util.HashMap;

import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.jme.input.InputHandler;
import com.jme.input.MouseInput;
import com.jme.light.PointLight;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.state.LightState;
import com.jme.system.DisplaySystem;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.game.state.GameStateManager;

import edu.isi.wubble.AutoBinding;
import edu.isi.wubble.gamestates.PhysicsGameState;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.physics.entity.DynamicEntity;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.physics.entity.EntityManager;

public class ChatWorldPhysics extends PhysicsGameState implements MessageListener {

	protected EntityManager         _entityManager;
	protected HashMap<Short,String> _clientToName;
	
	protected long _lastUpdate;
	
	public ChatWorldPhysics() {
		super();
		name = "ChatWorldPhysics";
		
		_clientToName = new HashMap<Short,String>();
		
		init();
		setActive(false);
		GameStateManager.getInstance().attachChild(this);
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
		
		addEntitiesToRoot();
		System.out.println("Initialization time: " + (System.currentTimeMillis() - startTime) + "ms");
	}
	
	/** 
	 * overridden to keep from using the camera
	 */
	protected void initInput() {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null) {
			super.initInput();
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
	    light.setLocation( new Vector3f( 0f, 5, 0f ) );
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
			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "chatroom-physics.jme");
			_entityManager.createEntity(url, true);
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
//		try {
//			URL url = ResourceLocatorTool.locateResource(TYPE_MODEL, "rpg-dynamic.jme");
//			Node staticNode = (Node) BinaryImporter.getInstance().load(url);
//			Iterator<Spatial> iter = staticNode.getChildren().iterator();
//			while (iter.hasNext()) {
//				Node n = (Node) iter.next();
//				List<Spatial> children = n.getChildren();
//				while (children.size() > 0) {
//					Spatial s = children.remove(0);
//					ModelEntity.createEntity(_physics, s);
//				}
//			}
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}		    
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
	
	public void update(float tpf) {
		super.update(tpf);
		_rootNode.updateGeometricState(0, true);
		_entityManager.updateEntities(tpf);
		
		sendWorldUpdate();
	}
	
	public void render(float tpf) {
		if (DisplaySystem.getDisplaySystem().getCurrentContext() != null) 
			super.render(tpf);
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
		ChatWorldServer.inst().quit();
	}
	
	public void sendRemoveMessage(String name) {
		InvokeMessage msg = InvokeMessage.createMsg("removeWubble", new Object[] { name });
		msg.sendToAll();
	}
	
	//--------------------------------
	// below are methods that are invoked 
	// from clients as well as methods that
	// broadcast information to clients.
	//--------------------------------

	public void sendChatMessage(Short id, String msg) {
		String userName = _clientToName.get(id);

		InvokeMessage i = InvokeMessage.createMsg("chatMsg", new Object[] { userName, msg });
		i.sendToAllExcept(id);
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
		String name = _clientToName.get(id);
		if (name == null) {
			System.err.println("ERROR - unknown client: " + id);
			return;
		}
		AutoBinding.bindingMsg(name, action, active);
	}
	
	public void login(Short id, String userName, String password) {
		// validate against the database maybe?
		_clientToName.put(id, userName);
		
		ChatWubbleEntity.createWubble(_entityManager, userName, false);
		// first we need to send out initial object mappings....
		_entityManager.initialMessages(id, false);
		
		setActive(true);
	}
	
	/**
	 * send out the world update as long as everything looks good
	 * we are sending it on the fast server, so that means we are
	 * sending it UDP, which is good because I don't care if we lose 1
	 * @param wum
	 */
	private void sendWorldUpdate() {
		if (ChatWorldServer.inst().getServer() == null) 
			return;
		
		long timeSinceUpdate = System.currentTimeMillis() - _lastUpdate;
		if (timeSinceUpdate < 35) 
			return;

		for (DynamicEntity de : _entityManager.getAllDynamicEntities()) {
			if (!de.isResting()) {
				ChatWorldServer.inst().getServer().sendToAll(de.generateSyncMessage());
			}
		}
		
		_lastUpdate = System.currentTimeMillis();
	}
			
	public void messageCertified(Message message) { }
	public void messageFailed(Message message) { }
	public void messageSent(Message message) { }

	public void messageReceived(Message message) {
		if ("InvokeMessage".equals(message.getClass().getSimpleName())) {
			InvokeMessage invoke = (InvokeMessage) message;
			invoke.callMethod(this);
		} else {
			System.out.println("recieved: " + message);
		}
	}

}
