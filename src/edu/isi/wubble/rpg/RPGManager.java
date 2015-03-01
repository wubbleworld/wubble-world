package edu.isi.wubble.rpg;

import java.util.TreeMap;
import java.util.concurrent.Callable;

import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.DefaultGuiGameState;
import edu.isi.wubble.gamestates.WubbleGameState;
import edu.isi.wubble.jgn.ConnectionManager;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.menu.MenuGameState;

public class RPGManager {
    protected static TreeMap<String,Object> _callbackMap;
    
    protected static int _role;
    
    protected static WubbleRPGState   _wrs;
    protected static RpgGuiState      _rgs;
    
    protected static RPGLoginState    _rls;
    
    static {
    	_callbackMap = new TreeMap<String,Object>();
    }
    
    public static void startLogin() {
    	if (Main.inst().inOpenGL()) {
    		_rls = (RPGLoginState) Main.inst().startState(RPGLoginState.class.getName());
    	} else {
    		try {
        		Callable<WubbleGameState> callable = createCallable(RPGLoginState.class.getName());
        		_rls = (RPGLoginState) GameTaskQueueManager.getManager().update(callable).get();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
		ConnectionManager.inst().setServer(Main.inst().getServer("rpg"));
		ConnectionManager.inst().connect(9100, 9200, _rls);
    }
    
    public static void startRpgGame() {
    	if (Main.inst().inOpenGL()) {
    		startWithoutCallable();
    	} else {
    		startWithCallable();
    	}
    	
		fillCallbackMap();
		ConnectionManager.inst().switchCallback(_rls, _wrs);
		_wrs.setCanUpdate(true);
		Main.inst().stopState(RPGLoginState.class.getName());
    }
    
	protected static void startWithCallable() {
		try {
			Callable<WubbleGameState> callable;

			callable = createCallable(RpgGuiState.class.getName());
			_rgs = (RpgGuiState) GameTaskQueueManager.getManager().update(callable).get();
			System.out.println("Main gui ready...");
			
			callable = new Callable<WubbleGameState>() {
				public WubbleGameState call() throws Exception {
					Main.inst().findOrCreateState(RPGWinningState.class.getName());
					Main.inst().findOrCreateState(RPGLosingState.class.getName());
					return null;
				}
			};
			GameTaskQueueManager.getManager().update(callable).get();
			System.out.println("Auxillary states ready...");

			callable = createCallable(WubbleRPGState.class.getName());
			_wrs = (WubbleRPGState) GameTaskQueueManager.getManager().update(callable).get();
			System.out.println("Rpg ready...");

		} catch (Exception e) {
			System.out.println("error inside start rpg");
			e.printStackTrace();
		}
	}
	
	protected static void startWithoutCallable() {
		_rgs = (RpgGuiState) Main.inst().startState(RpgGuiState.class.getName());
		System.out.println("Main gui ready...");
		
		Main.inst().findOrCreateState(RPGWinningState.class.getName());
		Main.inst().findOrCreateState(RPGLosingState.class.getName());
		System.out.println("Auxillary states ready...");
		
		_wrs = (WubbleRPGState) Main.inst().startState(WubbleRPGState.class.getName());
		System.out.println("Rpg ready...");
	}
	
    protected static Callable<WubbleGameState> createCallable(final String name) {
		Callable<WubbleGameState> callable = new Callable<WubbleGameState>() {
			public WubbleGameState call() throws Exception {
				return Main.inst().startState(name);
			}
		};
		return callable;
    }
	
	protected static void fillCallbackMap() {
		_callbackMap.clear();
		_callbackMap.put("default", _wrs);
		
		_callbackMap.put("chatMsg", _rgs);
		_callbackMap.put("updateHealth", _rgs);
		_callbackMap.put("updateCoinsLeft", _rgs);
		_callbackMap.put("translateResult", _rgs);
	}
	
	public static void stopRpgGame() {
		Main.inst().stopState(RpgGuiState.class.getName());
		Main.inst().stopState(WubbleRPGState.class.getName());
		
		ConnectionManager.inst().disconnect();

		Main.inst().startState(MenuGameState.class.getName());
	}
	
	public static DefaultGuiGameState getChat() {
		return _rgs;
	}
	
	public static void dispatchMessage(InvokeMessage msg) {
		Object callback = _callbackMap.get(msg.getMethodName());
		if (callback == null) {
			callback = _callbackMap.get("default");
		}
		msg.callMethod(callback);
	}
	
	public static void setRole(int role) {
		_role = role;
	}
	
	public static int getRole() {
		return _role;
	}
}