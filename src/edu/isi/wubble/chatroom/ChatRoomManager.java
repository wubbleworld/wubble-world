package edu.isi.wubble.chatroom;

import java.util.TreeMap;
import java.util.concurrent.Callable;

import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.Main;
import edu.isi.wubble.WubbleManager;
import edu.isi.wubble.gamestates.DefaultGuiGameState;
import edu.isi.wubble.gamestates.WubbleGameState;
import edu.isi.wubble.jgn.message.InvokeMessage;

public class ChatRoomManager {
    protected static TreeMap<String,Object> _callbackMap;
    
    protected static ChatRoomState    _crs;
    protected static DefaultGuiGameState _cbs;
    
    protected static WubbleManager    _mgr;
    static {
    	_callbackMap = new TreeMap<String,Object>();
    }

    public static void startChatRoom() {
    	_mgr = new WubbleManager();
    	if (Main.inst().inOpenGL()) {
    		startWithoutCallable();
    	} else {
    		startWithCallable();
    	}
 
    	_crs.setup(_mgr);
    	_crs.connectToServer();
    	_crs.setActive(true);
    }
    
	protected static void startWithCallable() {
		try {
			Callable<WubbleGameState> callable;
			GameTaskQueueManager manager = GameTaskQueueManager.getManager();
			
			callable = createCallable(DefaultGuiGameState.class.getName());
			_cbs = (DefaultGuiGameState) manager.update(callable).get();

			callable = createCallable(ChatRoomState.class.getName());
			_crs = (ChatRoomState) manager.update(callable).get();

			fillCallbackMap();
		} catch (Exception e) {
			System.out.println("error inside start rpg");
			e.printStackTrace();
		}
	}
	
	protected static void startWithoutCallable() {
		_cbs = (DefaultGuiGameState) Main.inst().startState(DefaultGuiGameState.class.getName());
		_crs = (ChatRoomState) Main.inst().startState(ChatRoomState.class.getName());

		fillCallbackMap();
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
		
		_callbackMap.put("default", _crs);
		_callbackMap.put("addWubble", _mgr);
		_callbackMap.put("removeWubble", _mgr);
		_callbackMap.put("chatMsg", _cbs);
	}
	
	public static void stopGame() {
		Main.inst().stopState(_crs.getClass().getName());
		Main.inst().stopState(_cbs.getClass().getName());
	}
	
	public static DefaultGuiGameState getChat() {
		return _cbs;
	}
	
	public static void dispatchMessage(InvokeMessage msg) {
		Object callback = _callbackMap.get(msg.getMethodName());
		if (callback == null) {
			callback = _callbackMap.get("default");
		}
		msg.callMethod(callback);
	}
}
