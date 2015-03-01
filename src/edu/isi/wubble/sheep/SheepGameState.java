package edu.isi.wubble.sheep;

import static com.jme.input.InputHandler.AXIS_NONE;
import static com.jme.input.InputHandler.BUTTON_ALL;
import static com.jme.input.InputHandler.DEVICE_MOUSE;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.input.ChaseCamera;
import com.jme.input.InputHandler;
import com.jme.input.KeyInput;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;
import com.jme.light.PointLight;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Skybox;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.system.DisplaySystem;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.TextureManager;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;

import edu.isi.wubble.Constants;
import edu.isi.wubble.Main;
import edu.isi.wubble.character.GameCharacter;
import edu.isi.wubble.character.WubbleCharacter;
import edu.isi.wubble.gamestates.VisualGameState;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.jgn.message.InvokeCallable;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.sheep.audio.AudioClient;
import edu.isi.wubble.sheep.character.PowerUp;
import edu.isi.wubble.sheep.character.Selectable;
import edu.isi.wubble.sheep.character.Sheep;
import edu.isi.wubble.sheep.character.Wrench;
import edu.isi.wubble.sheep.gui.SheepGUIState;
import edu.isi.wubble.util.Highlighter;
import edu.isi.wubble.util.PickerI;
import edu.isi.wubble.util.PickerInputAction;

public class SheepGameState extends VisualGameState implements MessageListener, PickerI { 
	public static boolean needToExit = true;
	
	Quad _floor;
	Skybox _skyBox;

	SheepWubbleManager _wubbleManager;
	public GameCharacter getCharacter(String name) { return _wubbleManager.getCharacter(name); }
	
	ChaseCamera _chaseCamera;
	
	WorldUpdateMessage _lastUpdate = null;
	HashMap<String, Sheep> _sheepHash;
	
	AudioClient _audio;
	
	public SheepGameState() {
		super();
		init();
	}
	
	public void init() {	
		Logger.getLogger("").setLevel(Level.WARNING);
		_renderStatistics = false;
		
		_wubbleManager = new SheepWubbleManager(_rootNode);
		_sheepHash     = new HashMap<String, Sheep>();

		// We actually want to set it to false?
		DisplaySystem.getDisplaySystem().getRenderer().getQueue().setTwoPassTransparency(false);
		
		setupConnection();
		
		createPlayer();
		
		setupAudio();
		setupInput();
		setupCamera();
		
		// For some reason these need to be down here?
		createLights();
		createSkyBox();
		createArena();
		
		needToExit = true;
	}
	
	// We are putting the audio on a separate thread now
	public void setupAudio() {
		 _audio = new AudioClient();
		 _audio.start();
	}
	
	// Now we only need the Byte that identifies the player
	public void audioRegister(Byte b) {
		_audio.register(b);
	}
	
	public void audioSubscribe(Byte b) {
		_audio.subscribe(b);
	}
	
	public void audioUnsubscribe(Byte b) {
		_audio.unsubscribe(b);
	}
	
	public void createLights() {
		_rootNode.clearRenderState(RenderState.RS_LIGHT);
		
		LightState lightState = DisplaySystem.getDisplaySystem().getRenderer().createLightState();
		
		PointLight pl = new PointLight();
		pl.setLocation(new Vector3f(0,5,40));
		pl.setEnabled(true);
//		pl.setAmbient(Utils.off);
//		pl.setDiffuse(Utils.off);
//		pl.setSpecular(Utils.off);
		lightState.attach(pl);
//		
		pl = new PointLight();
		pl.setLocation(new Vector3f(0,5,-40));
		pl.setEnabled(true);
//		pl.setAmbient(ColorRGBA.white);
		lightState.attach(pl);
		
		_rootNode.setRenderState(lightState);
		_rootNode.updateRenderState();
	}
	
	public void createArena() {
//		Quaternion horizontal = new Quaternion();
//		horizontal.fromAngleNormalAxis(-FastMath.HALF_PI, new Vector3f(1,0,0));
//		
//		Quaternion vertical = new Quaternion();
//		vertical.fromAngleNormalAxis((float) (Math.PI / 2), new Vector3f(0,1,0));
//		
//		Quaternion verticalFlip = new Quaternion();
//		verticalFlip.fromAngleNormalAxis((float) (Math.PI), new Vector3f(0,1,0));

		try {
			Node sheepArena = (Node) BinaryImporter.getInstance().load(
					ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, "sheep-arena.jme"));
//			for ( Spatial child : sheepArena.getChildren()) {
//				System.out.println(child.getName());
//			}
			
			ColorRGBA blue = new ColorRGBA(0, 0, 1, 0.5f);
			ColorRGBA red = new ColorRGBA(1, 0, 0, 0.5f);
			
			Utils.makeTransparentZone(sheepArena.getChild("blueGoal"), blue);
			Utils.makeTransparentZone(sheepArena.getChild("redGoal"), red);
			Utils.makeTransparentZone(sheepArena.getChild("blueGarden"), red);
			Utils.makeTransparentZone(sheepArena.getChild("redGarden"), blue);
			
			_rootNode.attachChild(sheepArena);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// THIS IS CRITICAL ONLY IT CAN SAVE THE PLANET
		_rootNode.updateRenderState();
	}
	
	public void createSkyBox() {
		float size = 256;
		
		_skyBox = new Skybox("SkyBox", size, size, size);
		
		Quaternion horizontal = new Quaternion();
		horizontal.fromAngleNormalAxis((float) (Math.PI / 2), new Vector3f(1,0,0));
		
		Quaternion vertical = new Quaternion();
		vertical.fromAngleNormalAxis((float) (Math.PI / 2), new Vector3f(0,1,0));
		
		Quaternion verticalFlip = new Quaternion();
		verticalFlip.fromAngleNormalAxis((float) (3 * Math.PI / 2), new Vector3f(0,1,0));
	
		URL skyBoxBack = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "boxBack.jpg");
		URL skyBoxFront = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "boxFront.jpg");
		URL skyBoxDown = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "boxDown.gif");
		URL skyBoxUp = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "boxUp.jpg");
		URL skyBoxLeft = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "boxLeft.jpg");
		URL skyBoxRight = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "boxRight.jpg");
		
		// why did I make all texture units 0?
		_skyBox.setTexture(Skybox.DOWN, TextureManager.loadTexture(skyBoxDown,Texture.MM_LINEAR_LINEAR,Texture.FM_LINEAR), 0);
		Texture upTexture = TextureManager.loadTexture(skyBoxUp,Texture.MM_LINEAR_LINEAR,Texture.FM_LINEAR);
//		upTexture.setRotation(new Quaternion(new float[]{0, FastMath.PI / 2, 0}));
		_skyBox.setTexture(Skybox.UP, upTexture, 0);
		_skyBox.setTexture(Skybox.EAST, TextureManager.loadTexture(skyBoxRight,Texture.MM_LINEAR_LINEAR,Texture.FM_LINEAR), 0);
		_skyBox.setTexture(Skybox.WEST, TextureManager.loadTexture(skyBoxLeft,Texture.MM_LINEAR_LINEAR,Texture.FM_LINEAR), 0);
		_skyBox.setTexture(Skybox.SOUTH, TextureManager.loadTexture(skyBoxFront,Texture.MM_LINEAR_LINEAR,Texture.FM_LINEAR), 0);
		_skyBox.setTexture(Skybox.NORTH, TextureManager.loadTexture(skyBoxBack,Texture.MM_LINEAR_LINEAR,Texture.FM_LINEAR), 0);
		
		_skyBox.updateRenderState();
		
		_rootNode.attachChild(_skyBox);
		
		_rootNode.updateRenderState();
	}
	
	public void createPlayer() {
		_wubbleManager.setupPlayerWubble();
		_wubbleManager.getPlayerWubble().setLocalTranslation(0, 0.2f, 0);
		
		_wubbleManager.getPlayerWubble().setModelBound(new BoundingBox());
		_wubbleManager.getPlayerWubble().updateModelBound();
		_wubbleManager.getPlayerWubble().updateWorldBound();
		
		_rootNode.updateRenderState();
	}
	
	public void setupConnection() {
		String sheepServer = Main.inst().getServer("sheep");
		System.out.println("Sheep server is " + sheepServer);
		ConnectionManager.inst().setServer(sheepServer);
		ConnectionManager.inst().connect(9300, 9400, this);
		
		// Send the login msg to the server.
		InvokeMessage message = new InvokeMessage();		
		message.setMethodName("login");
		String user = Main.inst().getName();
		String pass = Main.inst().getPassword();
		Integer myTeam = Main.inst().getTeam();
		int myClientID = ConnectionManager.inst().getClientId();
		message.setArguments(new Object[] {	ConnectionManager.inst().getClientId(), user, pass, myTeam});
		
		System.out.println("SGS: Sending login msg to server: cid: " + myClientID + " user: " + user + " pw: " + pass + " team: " + myTeam);
		message.sendToServer();
	}


	// Message builders
	///////////////////////////////////////////////////////////
	protected InvokeMessage buildMoveMsg(String name, Integer direction) {
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("startMove");
		im.setArguments(new Object[] {
				name,
				direction
		});
		
		return im;
	}
	
	protected InvokeMessage buildStopMoveMsg(String name, Integer direction) {
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("stopMove");
		im.setArguments(new Object[] {
				name,
				direction
		});
		
		return im;
	}
	
	static int wubbleCount = 0;
	protected InvokeMessage buildAddWubbleMsg() {
		String wName = "pooky_" + wubbleCount;
		wubbleCount++;

		InvokeMessage im = new InvokeMessage();
		im.setMethodName("addWubble");
		im.setArguments(new Object[] {
				wName,
				1
		});
		
		return im;
	}

	// Invokeable stuff
	///////////////////////////////////////////////////////////
	public void addCharacter(String name, String type, Float[] pos, Float[] rot) {
		System.out.println("SGS: addCharacter w/ name " + name + " type " + type);
		
		if (type.equals("Wrench")) { 
			int wrenchTeam = Constants.BLUE_TEAM;
			if (name.indexOf("red") != -1) { wrenchTeam = Constants.RED_TEAM; }
			
			Wrench testWrench = new Wrench(name, Utils.floatsToVector(pos), Utils.floatsToQuaternion(rot), wrenchTeam);
			_wubbleManager.addCharacter(testWrench);
			
			// _wubbleManager.addWrench(name, Utils.floatsToVector(pos), Utils.floatsToQuaternion(rot));

			// TODO: we probably shouldn't treat these exactly the same in reality
		} else if (type.equals("Wubble") || type.equals("Sidekick")) {
			// If I don't know about this wubble yet, add it to the wubble manager.
			if (_wubbleManager.getWubble(name) == null) {
				System.out.println("  Adding new wubble: " + name);
				_wubbleManager.addWubble(name, ColorRGBA.red, 0);
			} else {
				System.out.println("  Wubble " + name + " already existed.");
			}
			
			// Update the wubble's position and rotation, based on the info I was passed.
			System.out.print("  Setting wubble " + name + " pos to " + Utils.vs(Utils.floatsToVector(pos)) + " ");
			for (int i = 0; i < rot.length; i++) {
				System.out.print("rot[" + i + "] " + rot[i] + " ");
			}
			System.out.println("");
			
			_wubbleManager.getWubble(name).setLocalTranslation(Utils.floatsToVector(pos));
			_wubbleManager.getWubble(name).setLocalRotation(Utils.floatsToQuaternion(rot));
			
		} else if (type.equals("Sheep")) {
			System.out.println("Adding sheep named " + name);
			
			// _wubbleManager.addSheep(name, Utils.floatsToVector(pos), Utils.floatsToQuaternion(rot));
			
			// FIXME: Make sure dynamic dispatch works sensibly.
			Sheep testSheep = new Sheep(name,  Utils.floatsToVector(pos), Utils.floatsToQuaternion(rot));
			_wubbleManager.addCharacter(testSheep);
		} else if (type.indexOf("PowerUp") != -1)  {
			int puLen = "PowerUp".length();
			int typeLen = type.length();
			//System.out.println("Looking for subst of " + type + " from " + puLen + " to " + typeLen);
			String puType = type.substring(puLen, typeLen);
			System.out.println("Adding powerup " + name + " of type " + puType);
			
			PowerUp pu = new PowerUp(name, puType, Utils.floatsToVector(pos));
			_wubbleManager.addCharacter(pu);
		}
		// We don't really care about this, do we?
		else { //Logger.getLogger("").severe("setWorldState: got config info for \"" + name + "\", but I don't know what that is." ); }
			System.out.println("setWorldState: got config info for \"" + name + "\", but I don't know what that is." );
		}

		
		// Draw everything.
		_rootNode.updateRenderState();
	}
	
	
	
	public void setWorldState(Object[] stuff, Integer blueScore, Integer redScore) {
		// Make an InvokeCallable to do this.
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("doSetWorldState");
		im.setArguments(new Object[] {stuff, blueScore, redScore});
		new InvokeCallable(im, this);
	}

	
	public void doSetWorldState(Object[] stuff, Integer blueScore, Integer redScore) {
		//System.out.println("SGS: doSetWorldState got invoked!");
		int count = 0;

		for (Object outer1 : stuff) {
			count++;
			
			Object[] outer = (Object[]) outer1;
			String name = (String) outer[0];
			String type = (String) outer[1];
			Float[] pos = (Float[]) outer[2];
			Float[] rot = (Float[]) outer[3];

			// System.out.println("Name is " + name);
			try { 
				addCharacter(name, type, pos, rot);
				System.out.println("Added character " + name);
			}
			catch(Exception e) {
				System.out.println("Fuck!");
				e.printStackTrace();
			}
		}
		
		updateScore(blueScore, redScore);
		
		// Power down all wubbles on world reset.
		_wubbleManager.powerDownWubbles();
	}

	
	// For eater.
	public void setScale(String name, Float newScale) {
		GameCharacter gc = getCharacter(name);
		if (gc != null) { gc.setLocalScale(newScale); }
	}
	
	public void updateScore(Integer blueScore, Integer redScore) {
		SheepGUIState gui = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
		if (gui != null) {
			gui.getMap().updateScore(blueScore, redScore);
		}
	}
	
	public void chatMessage(String name, Integer team, String text) {
		SheepGUIState gui = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
		if (gui != null) {
			gui.handleChat(name, team, text);
		}
	}
	
	public void systemMessage(String message) {
		SheepGUIState gui = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
		if (gui != null) {
			gui.systemMessage(message);
		}
	}
	
	// END invokables
	///////////////////////////////////////////////////////////
	
	public void setupInput() {
        _input.addAction(new InputAction() {
        	public void performAction(InputActionEvent evt) {
        		int keyCode = evt.getTriggerIndex();

    			String myName = Main.inst().getName();
    			
        		if (evt.getTriggerPressed()) {
        			
        			// These happen on the way down...
        			switch (keyCode) {
        				case KeyInput.KEY_W: 
        				case KeyInput.KEY_UP:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "forward", true}));
        					break;
        				
        				case KeyInput.KEY_S: 
        				case KeyInput.KEY_DOWN:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "backward", true}));
        					break;
        					
        				case KeyInput.KEY_A: 
        				case KeyInput.KEY_LEFT:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "left", true}));      					
        					break;
        					
        				case KeyInput.KEY_D: 
        				case KeyInput.KEY_RIGHT:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "right", true}));
        					break;
        					
        				case KeyInput.KEY_SPACE:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "jump", true}));
        					break;
        					
        				case KeyInput.KEY_LSHIFT:
        					_audio.startAudio();
        					SheepGUIState sheepState = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
        					sheepState.shiftDown();
        					break;

        				case KeyInput.KEY_SEMICOLON:
        					
        					break;        					
        					
        				default: break;
        			}
        		} else {
        			
        			// ... And these happen on the way up.
        			switch (keyCode) {	
        			
        				case KeyInput.KEY_W: 
        				case KeyInput.KEY_UP:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "forward", false}));
        					break;
        					
        				case KeyInput.KEY_S: 
        				case KeyInput.KEY_DOWN:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "backward", false}));      					        					
        					break;
        					
        				case KeyInput.KEY_A: 
        				case KeyInput.KEY_LEFT:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "left", false}));      					        					        					
        					break;
        					
        				case KeyInput.KEY_D: 
        				case KeyInput.KEY_RIGHT:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "right", false}));   					        					        					
        					break;
        					
        				case KeyInput.KEY_SPACE:
        					ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("moveWubble", new Object[]{myName, "jump", false}));
        					break;
        					
        				case KeyInput.KEY_SLASH:
        					setInputEnabled(false);
        					Main.inst().giveFocus(SheepGUIState.class.getName());
                			break;
                		
        				case KeyInput.KEY_LSHIFT:
        					_audio.stopAudio();
        					SheepGUIState sheepState = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
        					sheepState.shiftUp();
        					break;
                			
        				default: break;
        			}	
        		}
        	}
        }, InputHandler.DEVICE_KEYBOARD, InputHandler.BUTTON_ALL, InputHandler.AXIS_NONE, false);

        PickerInputAction pia = new PickerInputAction(this, 'L', true);
        _input.addAction(pia, DEVICE_MOUSE, BUTTON_ALL, AXIS_NONE, false);
	}

	public void setupCamera() {
		CullState cs = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
	    cs.setCullMode(CullState.CS_BACK);
	    _rootNode.setRenderState(cs);	
	    
	    initChaseCamera();
	    
	    _rootNode.updateRenderState();
	}
	
	@SuppressWarnings("unchecked")
	protected void initChaseCamera() {
		Vector3f targetOffset = new Vector3f(0,1.0f,0);
        HashMap props = new HashMap();
        props.put(ChaseCamera.PROP_TARGETOFFSET, targetOffset);
        props.put(ChaseCamera.PROP_STAYBEHINDTARGET, "true");
//        props.put(ChaseCamera.PROP_MINDISTANCE, "0.1");
//        props.put(ChaseCamera.PROP_MAXDISTANCE, "4");

        _chaseCamera = new ChaseCamera(_camera, _wubbleManager.getPlayerWubble(), props);
//        _chaseCamera.setLooking(false);
//        _chaseCamera.getMouseLook().setEnabled(false);
        _chaseCamera.setEnabledOfAttachedHandlers(false);
        _chaseCamera.setEnableSpring(false);   
        
        _chaseCamera.getMouseLook().setMinRollOut(3);
        _chaseCamera.getMouseLook().setMaxRollOut(10);	
        _chaseCamera.getMouseLook().setSpeed(0.5f);	
        _chaseCamera.getMouseLook().setLookMouseButton(1);
        _chaseCamera.getMouseLook().setMaxAscent(30 * FastMath.DEG_TO_RAD);
        _chaseCamera.getMouseLook().setMinAscent(10 * FastMath.DEG_TO_RAD);
        _chaseCamera.getMouseLook().setMouseRollMultiplier(0.2f);
	}
	
	/* 
	 * VisualGameState methods 
	 * 
	 * */
	
	public float getPctDone() {
		// TODO: How do we do this?
		return 1.0f;
	}

	public void cleanup() {
		try {
			ConnectionManager.inst().getClient().disconnect();
			ConnectionManager.inst().getClient().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (_audio != null) {
			_audio.cleanup();
		}
		
		System.out.println("SHEEP CLEANUP DONE");
		
		if (needToExit) 
			System.exit(0);
	}
	
	@Override
	public void update(float tpf) {
		super.update(tpf);
				
		processWorldUpdate();
		
		_wubbleManager.updateWubbles(tpf);
		
		_chaseCamera.getTarget().updateGeometricState(tpf,true);
		_chaseCamera.update(tpf);
		
		Highlighter.update(tpf);
		
		_rootNode.updateGeometricState(tpf, true);
	}

	@Override
	public void acquireFocus() {
		setInputEnabled(true);
	}
	
	public void messageCertified(Message message) {
		// TODO Auto-generated method stub
		
	}

	public void messageFailed(Message message) {
		// TODO Auto-generated method stub
		
	}

	public void messageReceived(Message m) {
		// Should move this dispatch code into Util, too, once I figure out how to pass in the context.
//		System.out.println("SGS: RECEIVED A MESSAGE " + m);
		
		if (Utils.FakeInstanceOf("InvokeMessage", m)) { 
			InvokeMessage invoke = (InvokeMessage)m;
			System.out.println("SGS: dispatching " + invoke);
			if (invoke.getMethodName().equals("addWubble") || invoke.getMethodName().equals("removeWubble")) {
				invoke.callMethod(_wubbleManager);
			} else {
				invoke.callMethod(this);
			}
		} else if (Utils.FakeInstanceOf("WorldUpdateMessage", m)) {
			//System.out.println("Got WorldUpdateMessage!  [Is this the problem?]");
			_lastUpdate = (WorldUpdateMessage) m;
		} else {
			// System.err.println("SGS: What the hell kind of msg is: " + m + " class: " + m.getClass());		
		}
	}

	public void messageSent(Message message) {
		if (Utils.FakeInstanceOf("InvokeMessage", message)) {
			InvokeMessage im = (InvokeMessage)message;
			if (im.getMethodName().compareTo("moveWubble") != 0) {
				System.out.println("SGS: sent " + message);
			}	 
		}
	}

	// Methods that will get invoked remotely (or some such thing)
	public class ObjectRemover implements Callable<Integer> {

		String _name;
		public ObjectRemover(String name) {
			_name = name;
			System.out.println("Created ObjectRemover to remove sheep " + name);
			GameTaskQueueManager.getManager().update(this);
		}

		public Integer call() throws Exception {
			doRemoveCharacter(_name);
			return 10;
		}
	}
	
	public void removeCharacter(String name)   { new ObjectRemover(name); }
	public void doRemoveCharacter(String name) { _wubbleManager.removeCharacter(name); }
	
	
	// Power up stuff.
	// ////////////////////////////////////////////////////////////
	public void powerUp(String wubName, PowerUpInfo.PowerUpType p) {
		_wubbleManager.powerUpWubble(wubName, p);
	}
	
	public void powerDown(String wubName, PowerUpInfo.PowerUpType p) {
		_wubbleManager.powerDownWubble(wubName, p);
	}
	
	
	// Attention/Sidekick related stuff
	public void addAttention(String objName) {
		GameCharacter gc = getCharacter(objName);
		
		if (gc != null) {
			if (gc instanceof Selectable) {
				((Selectable) gc).addAttention();
			}
		}
	}
	
	public void removeAttention(String objName) {
		GameCharacter gc = getCharacter(objName);
		if (gc != null) {
			if (gc instanceof Selectable) {
				((Selectable) gc).removeAttention();
			}
		}
	}
	
	public void addSidekickAttention(String objName) {
		Highlighter.deactivateAll();
		
		if (getCharacter(objName) != null) {
			Highlighter.createHighlighter(objName, getCharacter(objName));
		}
	} 
	
	public void removeSidekickAttention(String objName) {
		Highlighter.deactivateHighlighter(objName);
	}
	
	
	public void processWorldUpdate() {
		WorldUpdateMessage wum = _lastUpdate;

		if (wum == null)
			return;
		
		for (int i = 0; i < wum.getNames().size(); i++ ) {
			String name = wum.getNames().get(i);
			Vector3f pos = new Vector3f(wum.getX().get(i), wum.getY().get(i), wum.getZ().get(i));
			Quaternion rot = new Quaternion(wum.getRotX().get(i), wum.getRotY().get(i), wum.getRotZ().get(i), wum.getRotW().get(i));
			
			// System.out.println(name + " " + rot.toAngles(null)[0] * FastMath.RAD_TO_DEG);
			Spatial obj = getCharacter(name);
			
			if (obj != null) {
				obj.setLocalTranslation(pos);
				obj.setLocalRotation(rot);

				if (obj instanceof WubbleCharacter) {
					SheepGUIState gui = (SheepGUIState) Main.inst().findOrCreateState(SheepGUIState.class.getName());
					gui.getMap().updateWubble(name, pos, rot);
				}
			}
		}
		
		// Offer a clue to the GC.
		wum = null;
		_lastUpdate = null;
	}

	// PickerI stuff
	public Node getPickNode() {
		return _rootNode;
	}

	public boolean isClickable(String name) {
//		System.out.println("TEST: " + name);
		
		if (getCharacter(name) != null) {
			return true;
		}
		
		return false;
	}

	public void picked(Spatial s) {
		if (s != null) {
			System.out.println("PICKED: " + s.getName());
		
			ConnectionManager.inst().getClient().sendToServer(InvokeMessage.createMsg("pointForSidekick", new Object[]{Main.inst().getName(), s.getName()}));
		}
	}
}
