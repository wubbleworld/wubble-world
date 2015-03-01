package edu.isi.wubble.jgn.superroom;

import static edu.isi.wubble.jgn.message.InvokeMessage.createMsg;
import static edu.isi.wubble.jgn.rpg.RPGPhysics.PICKER;
import static edu.isi.wubble.jgn.rpg.RPGPhysics.SHOOTER;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNConnectionListener;
import com.captiveimagination.jgn.clientserver.JGNServer;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.synchronization.message.Synchronize3DMessage;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.system.GameSettings;
import com.jme.system.PreferencesGameSettings;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.resource.ResourceLocatorTool;
import com.jme.util.resource.SimpleResourceLocator;
import com.jmex.game.StandardGame;
import com.jmex.game.state.GameStateManager;

import edu.isi.wubble.jgn.db.DatabaseManager;
import edu.isi.wubble.jgn.message.GameStatusMessage;
import edu.isi.wubble.jgn.message.HiddenUpdateMessage;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;


public class Server implements JGNConnectionListener,MessageListener  {

	public static StandardGame.GameType SERVER_TYPE;
	
	private static Server _server;
	
	protected HashMap<Short,JGNConnection> _openConnections;
	protected HashMap<Short,Integer>       _playerToGame;

	protected long _gameStartTime;
	
	protected JGNServer  _jgnServer;
	private StandardGame _game;

	private SuperRoomState _room;
	private Object         _lock;
	
	public static Server inst() {
		if (_server == null) {
			_server = new Server();
		}
		return _server;
	}
	
	private Server() {
		_openConnections = new HashMap<Short,JGNConnection>();
		_lock            = new Object();
		
		try {
			URL modelURL = ClassLoader.getSystemResource("media/models/");
			URL textureURL = ClassLoader.getSystemResource("media/textures/");
			ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, 
					new SimpleResourceLocator(textureURL));
			ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, 
					new SimpleResourceLocator(modelURL));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
    	JGN.register(WorldUpdateMessage.class);
    	JGN.register(HiddenUpdateMessage.class);
		JGN.register(InvokeMessage.class);
		JGN.register(GameStatusMessage.class);
    	JGN.register(Vector3f.class);
    	JGN.register(Quaternion.class);
	}
	
	public void setup(int numGames) throws Exception {
		GameSettings gs = new PreferencesGameSettings(Preferences.userRoot().node("Wubble Server"));
		gs.setWidth(1024);
		gs.setHeight(768);
		gs.setFramerate(70);
		
		GameTaskQueueManager.getManager();
		_game = new StandardGame("Wubble RPG Server", SERVER_TYPE, gs);
		_game.start();
		
		_room = createState("Room0");
		_room.setActive(true);
		GameStateManager.getInstance().attachChild(_room);
		
//        // Initialize networking
//        InetSocketAddress serverReliable = new InetSocketAddress(InetAddress.getLocalHost(), 9100);
//		InetSocketAddress serverFast = new InetSocketAddress(InetAddress.getLocalHost(), 9200);
//		_jgnServer = new JGNServer(serverReliable, serverFast);
//		
//		JGN.createThread(_jgnServer).start();
//		
//		_jgnServer.getFastServer().setConnectionTimeout(60000);
//		_jgnServer.getReliableServer().setConnectionTimeout(60000);
//		
//		_jgnServer.addMessageListener(this);
//		_jgnServer.addClientConnectionListener(this);
//		
//		InvokeMessage.SetServer(_jgnServer);
	}
	
	private SuperRoomState createState(final String name) {
		Callable<SuperRoomState> callable = new Callable<SuperRoomState>() {
			public SuperRoomState call() throws Exception {
				SuperRoomState state = new SuperRoomState();
				return state;
			}
		};
		try {
			return GameTaskQueueManager.getManager().update(callable).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void connected(JGNConnection connection) {
//		if (_openConnections.size() == 0) {
//			DatabaseManager.inst().connect();
//		}
//
//		short playerId = connection.getPlayerId();
//		System.out.println("client: " + playerId + " connected");
//		_openConnections.put(playerId, connection);
//		
//		// send the freshly connected player a list of available
//		// games so that he can choose the one he would like to
//		// connect to.
//		GameStatusMessage msg = generateGameMessage();
//		_server.sendTo(msg, playerId);
	}

	public void disconnected(JGNConnection connection) {
//		short playerId = connection.getPlayerId();
//		System.out.println("client: " + playerId + " disconnected");
//
//		_openConnections.remove(connection.getPlayerId());
//		Integer gameId = _playerToGame.remove(playerId);
//		if (gameId == null) 
//			return;
//		
//		SuperPhysics game = _rpgPhysics[gameId];
//		game.removeUser(playerId);
	}
	
	public void messageCertified(Message message) {	}
	public void messageFailed(Message message) { }
	public void messageSent(Message message) { 	}

	/**
	 * dispatch the message based on the sending player's id
	 * this should automatically route players messages to the 
	 * correct instance of the game.
	 */
	public void messageReceived(Message message) {
//		short playerId = message.getPlayerId();
//		Integer gameIndex = _playerToGame.get(playerId);
//		if (gameIndex == null) {
//			unknownMsgReceived(message);
//			return;
//		}
//		
//		SuperPhysics state = _rpgPhysics[gameIndex];
//		if ("InvokeMessage".equals(message.getClass().getSimpleName())) {
//			InvokeMessage invoke = (InvokeMessage) message;
//			invoke.callMethod(state);
//		} else {
//			System.out.println("recieved: " + message);
//		}
	}
	
	private void unknownMsgReceived(Message message) {
//		if ("InvokeMessage".equals(message.getClass().getSimpleName())) {
//			InvokeMessage msg = (InvokeMessage) message;
//			msg.callMethod(this);
//		} else {
//			System.out.println("uknown received: " + message);
//		}
	}
	
//	public void refresh(Short id) {
//		_jgnServer.sendTo(generateGameMessage(), id);
//	}
	
	/**
	 * called by the client when they decide to join a game.
	 * @param id
	 * @param gameId
	 * @param role
	 */
	public void joinGame(Short id, String userName, String password, Integer gameId, Integer role) {
//		synchronized (_lock) {
//			SuperPhysics rpg = _rpgPhysics[gameId];
//			if (rpg == null) 
//				return;
//			
//			if (rpg.getPlayerName(role) != null) {
//				// send message to user telling them sorry.
//				InvokeMessage msg = createMsg("failure", new Object[] { "Game slot is already taken." });
//				msg.sendTo(id);
//				return;
//			}
//
//			if (!rpg.isActive()) 
//				rpg.startSession();
//			
//			if (rpg.login(id, userName, password, role)) {
//				// send success (this will remap the MessageListener to the correct game state)
//				_playerToGame.put(id, gameId);
//
//				InvokeMessage msg = createMsg("success", null);
//				msg.sendTo(id);
//			} else {
//				// send message to user telling them incorrect password
//				InvokeMessage msg = createMsg("failure", new Object[] { "Incorrect user name or password." });
//				msg.sendTo(id);
//			}
//		}
	}
	
	public void quit() {
		try {
			_jgnServer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public void sendToAll(Collection<Short> allIds, Synchronize3DMessage m) {
//		for (Short id : allIds) {
//			_server.sendTo(m, id);
//		}
//	}
//	
//	public void sendTo(Short id, Synchronize3DMessage m) {
//		_server.sendTo(m, id);
//	}
	
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("usage: please indicate <graphical/headless>");
			return;
		}
		System.out.println("Received: " + args[0]);
		if (args[0].equals("graphical")) {
			Server.SERVER_TYPE = StandardGame.GameType.GRAPHICAL;
		} else {
			Server.SERVER_TYPE = StandardGame.GameType.HEADLESS;
		}
		Logger.getLogger("").setLevel(Level.WARNING);

		Server.inst();
		try {
			Server.inst().setup(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
