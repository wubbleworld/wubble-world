package edu.isi.wubble.jgn;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.clientserver.JGNClient;
import com.captiveimagination.jgn.event.MessageListener;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import edu.isi.wubble.jgn.message.GameStatusMessage;
import edu.isi.wubble.jgn.message.HiddenUpdateMessage;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;

public class ConnectionManager {

	private static ConnectionManager _connManager;
	
	private JGNClient _client;
	private String _serverName;
	
	private ConnectionManager() {
	}
	
	public static ConnectionManager inst() {
		if (_connManager == null) {
			_connManager = new ConnectionManager();
		}
		return _connManager;
	}
	
	public void disconnect() {
		try {
			if (_client != null && _client.getServerConnection() != null &&
					_client.getServerConnection().isConnected()) {
				_client.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void connect(int reliablePort, int fastPort, MessageListener callback) {
		disconnect();
        try {
			InetSocketAddress serverReliable = new InetSocketAddress(_serverName, reliablePort);
			InetSocketAddress serverFast = new InetSocketAddress(_serverName, fastPort);
        
			// Initialize networking
			InetSocketAddress clientReliable = new InetSocketAddress(InetAddress.getLocalHost(), 0);
			InetSocketAddress clientFast = new InetSocketAddress(InetAddress.getLocalHost(), 0);
			_client = new JGNClient(clientReliable, clientFast);
			
			JGN.createThread(_client).start();

	    	JGN.register(WorldUpdateMessage.class);
	    	JGN.register(HiddenUpdateMessage.class);
	    	JGN.register(InvokeMessage.class);
	    	JGN.register(GameStatusMessage.class);
	    	JGN.register(Vector3f.class);
	    	JGN.register(Quaternion.class);

	    	// Connect to the server before we register anything
			_client.addMessageListener(callback);
			_client.connectAndWait(serverReliable, serverFast, 5000);
			
			_client.getFastServer().setConnectionTimeout(60000);
			_client.getReliableServer().setConnectionTimeout(60000);
			
			InvokeMessage.SetClient(_client);
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	public void switchCallback(MessageListener origCallback, MessageListener newCallback) {
		_client.removeMessageListener(origCallback);
		_client.addMessageListener(newCallback);
	}
	
	public void setServer(String serverName) {
		_serverName = serverName;
	}
	
	public JGNClient getClient() {
		return _client;
	}
	
	public short getClientId() {
		return _client.getPlayerId();
	}
}
