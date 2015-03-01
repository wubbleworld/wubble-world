package edu.isi.wubble.wubbleroom;

import java.lang.reflect.Method;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.Main;
import edu.isi.wubble.gamestates.DefaultGuiGameState;
import edu.isi.wubble.gamestates.WubbleGameState;
import edu.isi.wubble.menu.MenuGameState;

public class RoomManager {
    protected static TreeMap<String,Object> _callbackMap;
    protected static String _activeRoom;
    
    protected static RoomGuiState     _rgs;
    protected static RoomState        _wrs;
    
    static {
    	_callbackMap = new TreeMap<String,Object>();
    }
    
    public static void startWubbleRoom(String roomName) {
    	_activeRoom = roomName;
    	if (Main.inst().inOpenGL()) {
    		startWithoutCallable();
    	} else {
    		startWithCallable();
    	}
    	SocketClient.inst().connect(Main.inst().getServer("room"));
    	SocketClient.inst().sendMessage("login " + Main.inst().getName());
    
    	_rgs.addSocketListeners();
    	_wrs.sendInitialSceneMsg();
    	_wrs.sendInitialMsg();
    	_wrs.sendUpdateMsg();
    	_wrs.beginSendingUpdates();
    }
    
	protected static void startWithCallable() {
		System.out.println("starting with callable");
		try {
			Callable<WubbleGameState> callable;
			GameTaskQueueManager manager = GameTaskQueueManager.getManager();
			
			callable = new Callable<WubbleGameState>() {
				public WubbleGameState call() throws Exception {
					_rgs = (RoomGuiState) Main.inst().startState(RoomGuiState.class.getName());
					_rgs.setRoom(_activeRoom);
					return _rgs;
				}
			};
			manager.update(callable).get();
			
			callable = createCallable(_activeRoom);
			_wrs = (RoomState) manager.update(callable).get();

			fillCallbackMap();
		} catch (Exception e) {
			System.out.println("error inside start rpg");
			e.printStackTrace();
		}
		System.out.println("finished starting with callable");
	}
	
	protected static void startWithoutCallable() {
		System.out.println("starting without callable");
		_rgs = (RoomGuiState) Main.inst().startState(RoomGuiState.class.getName());
		_rgs.setRoom(_activeRoom);

		_wrs = (RoomState) Main.inst().startState(_activeRoom);

		fillCallbackMap();
		System.out.println("finished starting without callable");
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
		_callbackMap.put("speak", _rgs);
	}
	
	public static void stopGame() {
		Main.inst().stopState(_activeRoom);
		Main.inst().stopState(_rgs.getClass().getName());
		
		Main.inst().startState(MenuGameState.class.getName());
	}
	
	public static RoomState getRoom() {
		return _wrs;
	}
	
	public static DefaultGuiGameState getChat() {
		return _rgs;
	}
	
	/**
	 * lisp messages will be dispatched through this mechanism
	 * This is where we maintain all of the different types 
	 * of states for the WubbleRoom.
	 * @param header
	 * @param message
	 */
	@SuppressWarnings("unchecked")
	public static void dispatchMessage(String header, String message) {
		Object callback = _callbackMap.get(header);
		if (callback == null) {
			callback = _callbackMap.get("default");
		}
		
		Method method;
		try {
			Class[] ids = new Class[] { String.class };
			method = callback.getClass().getMethod(header, ids);
			method.invoke(callback, message);
		} catch (Exception e) {
			System.out.println("[dispatchMessage] " + header);
			e.printStackTrace();
		} 	
	}
	
}
