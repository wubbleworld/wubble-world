package edu.isi.wubble.jgn.chatworld;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.prefs.Preferences;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNConnectionListener;
import com.captiveimagination.jgn.clientserver.JGNServer;
import com.jme.system.GameSettings;
import com.jme.system.PreferencesGameSettings;
import com.jme.util.GameTaskQueueManager;
import com.jme.util.resource.ResourceLocatorTool;
import com.jme.util.resource.SimpleResourceLocator;
import com.jmex.game.StandardGame;

import edu.isi.wubble.jgn.message.InvokeMessage;


public class ChatWorldServer implements JGNConnectionListener {

	public static StandardGame.GameType SERVER_TYPE;
	
	private static ChatWorldServer _chatServer;
	
	protected HashMap<Short,JGNConnection> _openConnections;
	
	protected long _gameStartTime;
	
	protected JGNServer      _server;
	private StandardGame     _game;
	private ChatWorldPhysics _chatPhysics;
	
	public static ChatWorldServer inst() {
		if (_chatServer == null) {
			_chatServer = new ChatWorldServer();
		}
		return _chatServer;
	}
	
	private ChatWorldServer() {
		_openConnections = new HashMap<Short,JGNConnection>();
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
		
		JGN.register(InvokeMessage.class);
	}
	
	public void setup() throws Exception {
		GameSettings gs = new PreferencesGameSettings(Preferences.userRoot().node("Wubble Chat Server"));
		gs.setWidth(600);
		gs.setHeight(400);
		gs.setFramerate(70);
		
		GameTaskQueueManager.getManager();
		_game = new StandardGame("Wubble Chat Server", SERVER_TYPE, gs);
		_game.start();
		
		Callable<ChatWorldPhysics> callable = new Callable<ChatWorldPhysics>() {
			public ChatWorldPhysics call() throws Exception {
				ChatWorldPhysics chat = new ChatWorldPhysics();
				return chat;
			}
		};
		try {
			_chatPhysics = GameTaskQueueManager.getManager().update(callable).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        // Initialize networking
        InetSocketAddress serverReliable = new InetSocketAddress(InetAddress.getLocalHost(), 9100);
		InetSocketAddress serverFast = new InetSocketAddress(InetAddress.getLocalHost(), 9200);
		_server = new JGNServer(serverReliable, serverFast);
		
		JGN.createThread(_server).start();
		
		_server.addMessageListener(_chatPhysics);
		_server.addClientConnectionListener(this);
		
		InvokeMessage.SetServer(_server);
	}

	public JGNServer getServer() {
		return _server;
	}
	
	public ChatWorldPhysics getPhysics() {
		return _chatPhysics;
	}
	
	public JGNConnection getConnection(short id) {
		return _server.getConnection(id);
	}
	
	public void connected(JGNConnection connection) {
		System.out.println("client: " + connection.getPlayerId() + " connected");
		if (_openConnections.size() == 0) {
			_gameStartTime = System.currentTimeMillis();
		}
		_openConnections.put(connection.getPlayerId(), connection);
	}

	public void disconnected(JGNConnection connection) {
		System.out.println("client: " + connection.getPlayerId() + " disconnected");
		_openConnections.remove(connection.getPlayerId());
		if (_openConnections.size() == 0) {
			_chatPhysics.setActive(false);
		}
	}
	
	public long getTime() {
		return System.currentTimeMillis() - _gameStartTime;
	}
	
	public void quit() {
		try {
			_server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("usage: please indicate <graphical/headless>");
			return;
		}
		System.out.println("Received: " + args[0]);
		if (args[0].equals("graphical")) {
			ChatWorldServer.SERVER_TYPE = StandardGame.GameType.GRAPHICAL;
		} else {
			ChatWorldServer.SERVER_TYPE = StandardGame.GameType.HEADLESS;
		}
		try {
			ChatWorldServer.inst().setup();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
