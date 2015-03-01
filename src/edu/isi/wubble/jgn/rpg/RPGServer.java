package edu.isi.wubble.jgn.rpg;

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

import edu.isi.wubble.jgn.db.DatabaseManager;
import edu.isi.wubble.jgn.message.GameStatusMessage;
import edu.isi.wubble.jgn.message.HiddenUpdateMessage;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;


public class RPGServer implements JGNConnectionListener,MessageListener  {

	public static StandardGame.GameType SERVER_TYPE;
	
	private static RPGServer _rpgServer;
	
	protected HashMap<Short,JGNConnection> _openConnections;
	protected HashMap<Short,Integer>       _playerToGame;

	protected long _gameStartTime;
	
	protected JGNServer  _server;
	private StandardGame _game;

	private RPGPhysics[]   _rpgPhysics;
	private Object         _lock;
	
	public static RPGServer inst() {
		if (_rpgServer == null) {
			_rpgServer = new RPGServer();
		}
		return _rpgServer;
	}
	
	private RPGServer() {
		_openConnections = new HashMap<Short,JGNConnection>();
		_playerToGame    = new HashMap<Short,Integer>();
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
		GameSettings gs = new PreferencesGameSettings(Preferences.userRoot().node("Wubble RPG Server"));
		gs.setWidth(600);
		gs.setHeight(400);
		gs.setFramerate(70);
		
		GameTaskQueueManager.getManager();
		_game = new StandardGame("Wubble RPG Server", SERVER_TYPE, gs);
		_game.start();
		
		_rpgPhysics = new RPGPhysics[numGames];
		for (int i = 0; i < numGames; ++i) {
			createPhysicsState(i);
		}
		
        // Initialize networking
        InetSocketAddress serverReliable = new InetSocketAddress(InetAddress.getLocalHost(), 9100);
		InetSocketAddress serverFast = new InetSocketAddress(InetAddress.getLocalHost(), 9200);
		_server = new JGNServer(serverReliable, serverFast);
		
		JGN.createThread(_server).start();
		
		_server.getFastServer().setConnectionTimeout(60000);
		_server.getReliableServer().setConnectionTimeout(60000);
		
		_server.addMessageListener(this);
		_server.addClientConnectionListener(this);
		
		InvokeMessage.SetServer(_server);
	}
	
	private void createPhysicsState(final int index) {
		Callable<RPGPhysics> callable = new Callable<RPGPhysics>() {
			public RPGPhysics call() throws Exception {
				RPGPhysics rpg = new RPGPhysics("RPGPhysics" + index, index);
				rpg.setup();
				return rpg;
			}
		};
		try {
			_rpgPhysics[index] = GameTaskQueueManager.getManager().update(callable).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private GameStatusMessage generateGameMessage() {
		GameStatusMessage msg = new GameStatusMessage();
		
		String[] shooterArray = new String[_rpgPhysics.length];
		String[] pickerArray = new String[_rpgPhysics.length];
		int[] doneArray = new int[_rpgPhysics.length];
		
		for (int i = 0; i < _rpgPhysics.length; ++i) {
			shooterArray[i] = _rpgPhysics[i].getPlayerName(SHOOTER);
			pickerArray[i] = _rpgPhysics[i].getPlayerName(PICKER);
			doneArray[i] = _rpgPhysics[i].getAmountDone();
		}
		msg.setShooterArray(shooterArray);
		msg.setPickerArray(pickerArray);
		msg.setAmountDoneArray(doneArray);
		
		return msg;
	}
	
	public void connected(JGNConnection connection) {
		if (_openConnections.size() == 0) {
			DatabaseManager.inst().connect();
		}

		short playerId = connection.getPlayerId();
		System.out.println("client: " + playerId + " connected");
		_openConnections.put(playerId, connection);
		
		// send the freshly connected player a list of available
		// games so that he can choose the one he would like to
		// connect to.
		GameStatusMessage msg = generateGameMessage();
		_server.sendTo(msg, playerId);
	}

	public void disconnected(JGNConnection connection) {
		short playerId = connection.getPlayerId();
		System.out.println("client: " + playerId + " disconnected");

		_openConnections.remove(connection.getPlayerId());
		Integer gameId = _playerToGame.remove(playerId);
		if (gameId == null) 
			return;
		
		RPGPhysics game = _rpgPhysics[gameId];
		game.removeUser(playerId);
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
		short playerId = message.getPlayerId();
		Integer gameIndex = _playerToGame.get(playerId);
		if (gameIndex == null) {
			unknownMsgReceived(message);
			return;
		}
		
		RPGPhysics state = _rpgPhysics[gameIndex];
		if ("InvokeMessage".equals(message.getClass().getSimpleName())) {
			InvokeMessage invoke = (InvokeMessage) message;
			invoke.callMethod(state);
		} else {
			System.out.println("recieved: " + message);
		}
	}
	
	private void unknownMsgReceived(Message message) {
		if ("InvokeMessage".equals(message.getClass().getSimpleName())) {
			InvokeMessage msg = (InvokeMessage) message;
			msg.callMethod(this);
		} else {
			System.out.println("uknown received: " + message);
		}
	}
	
	public void refresh(Short id) {
		_server.sendTo(generateGameMessage(), id);
	}
	
	/**
	 * called by the client when they decide to join a game.
	 * @param id
	 * @param gameId
	 * @param role
	 */
	public void joinGame(Short id, String userName, String password, Integer gameId, Integer role) {
		synchronized (_lock) {
			RPGPhysics rpg = _rpgPhysics[gameId];
			if (rpg == null) 
				return;
			
			if (rpg.getPlayerName(role) != null) {
				// send message to user telling them sorry.
				InvokeMessage msg = createMsg("failure", new Object[] { "Game slot is already taken." });
				msg.sendTo(id);
				return;
			}

			if (!rpg.isActive()) 
				rpg.startSession();
			
			if (rpg.login(id, userName, password, role)) {
				// send success (this will remap the MessageListener to the correct game state)
				_playerToGame.put(id, gameId);

				InvokeMessage msg = createMsg("success", null);
				msg.sendTo(id);
			} else {
				// send message to user telling them incorrect password
				InvokeMessage msg = createMsg("failure", new Object[] { "Incorrect user name or password." });
				msg.sendTo(id);
			}
		}
	}
	
	public void quit() {
		try {
			_server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendToAll(Collection<Short> allIds, Synchronize3DMessage m) {
		for (Short id : allIds) {
			_server.sendTo(m, id);
		}
	}
	
	public void sendTo(Short id, Synchronize3DMessage m) {
		_server.sendTo(m, id);
	}
	
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("usage: please indicate <graphical/headless>");
			return;
		}
		System.out.println("Received: " + args[0]);
		if (args[0].equals("graphical")) {
			RPGServer.SERVER_TYPE = StandardGame.GameType.GRAPHICAL;
		} else {
			RPGServer.SERVER_TYPE = StandardGame.GameType.HEADLESS;
		}
		Logger.getLogger("").setLevel(Level.WARNING);

		RPGServer.inst();
		try {
			RPGServer.inst().setup(5);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
