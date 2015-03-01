package edu.isi.wubble.jgn.db;



public class DatabaseManager extends Database {

	protected static DatabaseManager _manager = null;

	private DatabaseManager() {
		super();
	}
	
	public static DatabaseManager inst() {
		if (_manager == null) {
			_manager = new DatabaseManager();
		}
		return _manager;
	}
	
	public void reconnect() {
		disconnect();
		connect();
	}	
}
