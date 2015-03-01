package edu.isi.wubble.jgn.message;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Logger;

import com.captiveimagination.jgn.clientserver.JGNClient;
import com.captiveimagination.jgn.clientserver.JGNServer;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.CertifiedMessage;
import com.captiveimagination.jgn.message.type.PlayerMessage;

public class InvokeMessage extends Message implements CertifiedMessage,PlayerMessage{

	// Since these messages never get created without the intention to send 
	// this abstraction makes sense.
	private static JGNServer _server;
	public static void SetServer(JGNServer s) { _server = s; }
	public JGNServer   getServer()            { return _server; }

	private static JGNClient _client;
	public static void SetClient(JGNClient s) { _client = s; }
	public JGNClient   getClient()            { return _client; }

	private String methodName; 
	private Object[] arguments;
	
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public Object[] getArguments() {
		return arguments;
	}
	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}
	
	// For debugging purposes.
	static int _curID = 0;
	int _msgID;
	
	/**
	 * convenience method for creating messages quickly.
	 * @param method
	 * @param args
	 * @return
	 */
	public static InvokeMessage createMsg(String method, Object[] args) {
		InvokeMessage message = new InvokeMessage();
		
		// Assign this message a unique, and bump the counter.
		message._msgID = _curID++;
		
		message.setMethodName(method);
		message.setArguments(args);
		return message;
	}
	
	public void callMethod(Object callback) {
		Method method;
		// System.out.println(this + " -- about to invoke. " + callback.getClass().getSimpleName());
		try {
			Class[] ids = null;
			if (arguments != null) { 
				ids = new Class[arguments.length];
				for (int i = 0; i < arguments.length; ++i) {
					ids[i] = arguments[i].getClass();
				}
			}
			method = callback.getClass().getMethod(methodName, ids);
			method.invoke(callback, arguments);
		} catch (Exception e) {
			String className = callback.getClass().getSimpleName();
			System.out.println("[InvokeError msgID " + _msgID +"] on obj --> \"" + className + "\" and method --> \"" + methodName + "\"");
			if (arguments != null) {
				for (int i = 0; i < arguments.length; ++i) {
					System.out.println("  arg[" + i + "] --> " + arguments[i] + " of class \"" + arguments[i].getClass().getSimpleName() + "\"");
				}
			} else {
				Method[] methods = callback.getClass().getMethods();
				System.out.println(className + " has the following methods:");
				for (int i = 0; i < methods.length; ++i) {
					System.out.print("  ..." + methods[i].getName() + " ");
					Class[] params = methods[i].getParameterTypes();
					if (params != null) {
						for (int j = 0; j < params.length; ++j) 
							System.out.print(params[j].getName() + " ");
						System.out.println("");
					} else {
						System.out.println("void");
					}
				}
			}
			e.printStackTrace();
		} 	
	}
	
	
	@Override
	public String toString() {
		return "InvokeMsg [" + this.hashCode() + "] w/ method \"" + getMethodName() + "\"";
	}
	
	protected void logMsg(String toWho) {
		String msg = this + " -- sent to " + toWho + " at " + getServer();
		Logger.getLogger("").info(msg);
		//System.out.println(msg);
	}
	
	/** 
	 * used from the *client* side game to send invoke messages
	 * to the server.
	 */
	public void sendToServer() {
		if (getClient() != null) {
			getClient().sendToServer(this);
		}
	}
	
	/**
	 * used from the *client* side game to send invoke messages
	 * to another player (yes we can do that)
	 * @param who
	 * 		the player we want to invoke this method on.
	 */
	public void sendToPlayer(short who) {
		if (getClient() != null) {
			getClient().sendToPlayer(this, who);
		}
	}

	public void sendTo(short who) { 
		if (getServer() != null) {
			String tgt = "" + who;
			logMsg(tgt);
			getServer().sendTo(this, who); 
		}
	}
	
	public void sendToAll() { 
		if (getServer() != null) {
			logMsg("all");
			getServer().sendToAll(this); 
			// System.out.println("Yes, I just sent to all.");
		} 
//		else {
//			System.out.println("What the fuck?  Where's the server?");
//		}
	}
	
	public void sendToAllExcept(short who) { 
		if (getServer() != null) { 
			logMsg(" all but " + who);
			getServer().sendToAllExcept(this, who); 
			// System.out.println("Yes, I just sent to all except.");
		}
//		else {
//			System.out.println("What the fuck?  Where's the server?");
//		}
	}
	
	public void sendToGroup(Collection<Short> allIds) {
		for (Short id : allIds) {
			getServer().sendTo(this, id);
		}
	}
}
