package edu.isi.wubble;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import com.jme.renderer.Renderer;
import com.jme.system.GameSettings;
import com.jme.system.PreferencesGameSettings;
import com.jme.util.GameTaskQueueManager;
import com.jmex.game.StandardGame;
import com.jmex.game.state.GameState;
import com.jmex.game.state.GameStateManager;

import edu.isi.wubble.gamestates.WubbleGameState;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.login.LoginGameState;
import edu.isi.wubble.sheep.SheepGameState;
import edu.isi.wubble.sheep.gui.SheepGUIState;

public class Main {
	protected static Main _mainGame = null;
    private static final Logger logger = Logger
    		.getLogger(Main.class.getName());
    
    protected String _sheepServer = "tioga.isi.edu";
    protected String _rpgServer   = "tecolote.isi.edu";
    protected String _roomServer  = "tecolote.isi.edu";
    protected String _loginServer = "http://www.wubble-world.com";

    protected StandardGame _game;
    
    protected String _userName = "demon";
    protected String _password = "solid";
    protected String _uniqueId;
    protected Integer _team = 0;

    protected TreeMap<String,Object> _callbackMap;
    
    protected List<String> _activeList;
    
	protected Main() {
		GameSettings gs = new PreferencesGameSettings(Preferences.userRoot().node("Wubble World"));
		gs.setWidth(1024);
		gs.setHeight(768);
		gs.setStencilBits(4);
		
		GameTaskQueueManager.getManager();
		_game = new StandardGame("Wubble World", StandardGame.GameType.GRAPHICAL, gs);
		_game.start();
		
		_activeList = new LinkedList<String>();
		_callbackMap = new TreeMap<String, Object>();
	}

	public static Main inst() {
		if (_mainGame == null) {
			_mainGame = new Main();
		}
		return _mainGame;
	}
	
	public Renderer getRenderer() {
		return _game.getDisplay().getRenderer();
	}

	public void finish() {
		_game.shutdown();
		ConnectionManager.inst().disconnect();
		System.exit(0);
	}
	
	public boolean inOpenGL() {
		return _game.inGLThread();
	}
	
	/**
	 * loop over all the active states and disable their
	 * input, unless it is the state passed in.
	 * @param className
	 */
	public void enableInput(String className, boolean enabled) {
		System.out.println("className: " + className);
		for (GameState gs : GameStateManager.getInstance().getChildren()) {
			WubbleGameState wgs = (WubbleGameState) gs;
			if (wgs.isActive()) {
				if (className.equals(wgs.getName())) {
					wgs.setInputEnabled(enabled);
				} else {
					wgs.setInputEnabled(!enabled);
				}
			}
		}
	}
	
	/**
	 * pass the focus from one state to another.
	 * @param className
	 */
	public void giveFocus(String className) {
		WubbleGameState wgs = (WubbleGameState) GameStateManager.getInstance().getChild(className);
		if (wgs != null) {
			wgs.acquireFocus();
		}
	}
	
	/**
	 * This will start a state with the given className.  There is an assumption
	 * that there will only be one active state for a given state class...
	 * Don't know if I like that.
	 * @param className
	 * @param name
	 */
	public WubbleGameState startState(String className) {
		WubbleGameState wbs = findOrCreateState(className);
		wbs.setActive(true);
		
		return wbs;
		//WubbleGameState loading = findOrCreateState("edu.isi.wubble.gamestates.LoadingGameState");
		//loading.setActive(true);
	}
	
	public void pauseState(String className) {
		WubbleGameState wbs = findState(className);
		wbs.setActive(false);
	}
	
	public void unpauseState(String className) {
		WubbleGameState wbs = findState(className);
		if (wbs != null) 
			wbs.setActive(true);
	}
	
	/**
	 * This will stop the given state. 
	 * @param className
	 */
	public void stopState(String className) {
		WubbleGameState wbs = findState(className);
		if (wbs != null) {
			wbs.setActive(false);
			wbs.cleanup();
			
			GameStateManager.getInstance().detachChild(className);
		}
	}
	
	public WubbleGameState findState(String className) {
		return (WubbleGameState) GameStateManager.getInstance().getChild(className);
	}
	
	/**
	 * This method will find a state in the GameStateManager, or
	 * it will instantiate it and return the results.
	 * @param name
	 * @return
	 */
	public WubbleGameState findOrCreateState(final String className) {
		// This is an attempt to fix the webstart
		try {
			Class.forName(className);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		
		WubbleGameState wbs = (WubbleGameState) GameStateManager.getInstance().getChild(className);
		if (wbs == null) {
			try {
				wbs = (WubbleGameState) Class.forName(className).newInstance();
				wbs.setName(className);
				GameStateManager.getInstance().attachChild(wbs);
				return wbs;
			} catch (Exception e) {
				logger.severe("findOrCreateState: unable to create: " + className);
				e.printStackTrace();
			}
			
		}
		return wbs;
	}
	
	public void startSheepGame() {
		try {
			System.out.println("thread: " + Thread.currentThread().getName());
			Callable<WubbleGameState> callable = new Callable<WubbleGameState>() {
				public WubbleGameState call() throws Exception {
					return startState(SheepGameState.class.getName());
				}
			};
			GameTaskQueueManager.getManager().update(callable).get();
			
			System.out.println("thread: " + Thread.currentThread().getName());
			callable = new Callable<WubbleGameState>() {
				public WubbleGameState call() throws Exception {
					return startState(SheepGUIState.class.getName());
				}
			};
			GameTaskQueueManager.getManager().update(callable).get();
		} catch (Exception e) {
			System.out.println("error inside startSheepGame");
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param userName
	 */
	public void setName(String userName) {
		_userName = userName;
	}
	
	public String getName() {
		return _userName;
	}
	
	public void setPassword(String password) {
		_password = password;
	}
	
	public String getPassword() {
		return _password;
	}

	public void setId(String id) {
		_uniqueId = id;
	}
	
	public String getId() {
		return _uniqueId;
	}
	
	public Integer getTeam() {
		return _team;
	}
	
	public void setTeam(Integer team) {
		_team = team;
	}
	
	public void setServers(String sheep, String rpg, String room) {
		_sheepServer = sheep;
		_rpgServer   = rpg;
		_roomServer  = room;
	}
	
	public String getServer(String server) {
		if ("sheep".equals(server)) 
			return _sheepServer;
		else if ("rpg".equals(server))
			return _rpgServer;
		else if ("room".equals(server))
			return _roomServer;
		else if ("login".equals(server))
			return _loginServer;
		
		return "";
	}
	
	public static void main(String[] args) {
		// Turn off most of the fucking messages.
		Logger.getLogger("").setLevel(Level.WARNING);
		Main app = Main.inst();

		//RPGManager.startLogin();
		app.startState(LoginGameState.class.getName());
	}
}
