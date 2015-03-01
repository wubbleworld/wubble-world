package edu.isi.wubble.jgn.sheep;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNConnectionListener;
import com.captiveimagination.jgn.clientserver.JGNServer;
import com.jme.math.Quaternion;
import com.jme.system.GameSettings;
import com.jme.system.PreferencesGameSettings;
import com.jme.util.GameTaskQueueManager;
import com.jmex.game.StandardGame;

import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;

public class SheepServer implements JGNConnectionListener {
	protected static JGNServer _server;
	
	private StandardGame _game;
	private SheepPhysicsState _sheepPhysics;
	
	public SheepServer(boolean headless) throws Exception {		
		// Registration needs to be done before the first connectivity.
		
		//JGN.register(LoginMessage.class);
		JGN.register(WorldUpdateMessage.class);
		//JGN.register(MovementMessage.class);
		JGN.register(InvokeMessage.class);
		JGN.register(Quaternion.class);
		
		GameSettings gs = new PreferencesGameSettings(Preferences.userRoot().node("SheepServer"));
		gs.setWidth(800);
		gs.setHeight(600);
		gs.setFramerate(100);
		
		if (headless) {
			_game = new StandardGame("SheepServer", StandardGame.GameType.HEADLESS, gs);
		} else {
			_game = new StandardGame("SheepServer", StandardGame.GameType.GRAPHICAL, gs);
		}
		
		_game.start();

		// Solve the mysterious bug.
		GameTaskQueueManager.getManager();
		
		Callable<SheepPhysicsState> callable = new Callable<SheepPhysicsState>() {
			public SheepPhysicsState call() throws Exception {
				SheepPhysicsState sheep = new SheepPhysicsState();
				sheep.setup();
				return sheep;
			}
		};
		
		_sheepPhysics = null;
		try {
			_sheepPhysics = GameTaskQueueManager.getManager().update(callable).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        // Initialize networking
        InetSocketAddress serverReliable = new InetSocketAddress(InetAddress.getLocalHost(), 9300);
		InetSocketAddress serverFast = new InetSocketAddress(InetAddress.getLocalHost(), 9400);
		_server = new JGNServer(serverReliable, serverFast);
		
		JGN.createThread(_server).start();
		
		_server.addMessageListener(_sheepPhysics);
		_server.addClientConnectionListener(this);
		InvokeMessage.SetServer(_server);
	}
	
	public void connected(JGNConnection connection) {
		System.out.println("SS: client " + connection.getPlayerId() + ": connected");
	}

	public void disconnected(JGNConnection connection) {
		System.out.println("SS: client " + connection.getPlayerId() + ": disconnected");
		System.out.println("SS: client " + connection.getPlayerId() + ": removing entity");
		
		_sheepPhysics.removeEntity(connection.getPlayerId());
	}
	
	public static void Quit() {
		assert (_server != null);
		try {
			_server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public static JGNServer getServer() {
		return _server;
	}
	
	// MAIN
	public static void main(String[] args) {
		// Turn off most of the fucking messages.
		Logger.getLogger("").setLevel(Level.WARNING);
		
		try {
			if (args.length == 0) {
				SheepServer s = new SheepServer(false);
			} else {
				SheepServer s = new SheepServer(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
