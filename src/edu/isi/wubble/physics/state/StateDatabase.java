package edu.isi.wubble.physics.state;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;


import edu.isi.wubble.physics.TimeManager;

public class StateDatabase implements StateDatabaseI {
	private static StateDatabase _database;

	protected Connection _conn;
	protected int _sessionId;
	
	protected HashMap<String,Integer> _maxMap;
	
	protected Statement _defaultStatement;
	protected Statement _batchStatement;
	
	protected String _dbFile = "state.db";

	
	private StateDatabase() {
		_maxMap = new HashMap<String,Integer>();
		connect();
	}
	
	protected void connect() {
		try {
			Class.forName("org.sqlite.JDBC");
			_conn = DriverManager.getConnection("jdbc:sqlite:" + _dbFile);
			_conn.setAutoCommit(false);

			_defaultStatement = null;
			
			initializeDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		try {
			_conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void reconnect() {
		disconnect();
		connect();
	}
	
	public void setDB(String file) {
		_dbFile = file;
	}
	
	public static StateDatabase inst() {
		if (_database == null) 
			_database = new StateDatabase();
		return _database;
	}
	
	protected void initializeDatabase() throws Exception {
		Statement stat = getStatement();
		
		stat.executeUpdate(CREATE_SESSION);
		stat.executeUpdate(CREATE_ENTITY_TABLE);
		stat.executeUpdate(CREATE_FLUENT_LOOKUP_TABLE);
		stat.executeUpdate(CREATE_TAG_TABLE);
		stat.executeUpdate(CREATE_TAG_MAP_TABLE);
		
		findOrAdd("", -1);
	}
	
	public void createSession() {
		try {
			Statement s = getStatement();
			ResultSet rs = s.executeQuery("select max(session_id) from user_sessions");
			
			if (rs.next()) 
				_sessionId = rs.getInt(1) + 1;
			
			rs.close();
			s.execute("insert into user_sessions (session_id, start_time) " +
					"values (" + _sessionId + "," + System.currentTimeMillis() +")");
			
			System.out.println("[session=" + _sessionId + "]");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * used by the main program to save additional information
	 * about this session.
	 */
	public void closeSession() {
		try {
			TimeManager tm = TimeManager.inst();
			Statement s = getStatement();
			s.executeUpdate("update user_sessions set end_time = " + System.currentTimeMillis() + ", " +
					"end_offset_time = " + tm.getElapsedTime() + ", " +
					"end_logical_time = "+ tm.getLogicalTime() + 
					" where session_id = " + _sessionId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * used by other 'helper' applications and actually uses the 
	 * given logical time to make sure that value is correct.
	 * @return
	 * @throws Exception
	 */
	public void closeSession(int logicalTime) {
		try {
			Statement s = getStatement();
			s.executeUpdate("update user_sessions set end_time = " + System.currentTimeMillis() + ", " +
					"end_offset_time = 0, " +
					"end_logical_time = "+ logicalTime + 
					" where session_id = " + _sessionId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Statement getStatement() throws Exception {
		if (_defaultStatement == null)
			_defaultStatement = _conn.createStatement();
		return _defaultStatement;
	}
	
	public Statement createStatement() throws Exception {
		return _conn.createStatement();
	}
	
	public int getSessionId() {
		return _sessionId;
	}
	
	protected int getMaxDB(String name) throws Exception {
		Statement s = getStatement();
		ResultSet rs = s.executeQuery("select max(id)+1 from " + name);
		if (rs.next()) {
			return rs.getInt(1);
		} 
		return 0;
	}
	
	/**
	 * find the next id for the given database table.  Using mysql... this 
	 * can be done automatically for you...but with SQLite you are responsible
	 * for it yourself.
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public int getNextId(String tableName) throws Exception {
		Integer objValue = _maxMap.get(tableName);
		int value = 0;
		if (objValue == null)
			value = getMaxDB(tableName);
		else
			value = objValue.intValue() + 1;
		
		_maxMap.put(tableName, value);
		return value;
	}
	
	public PreparedStatement prepare(String sql) {
		PreparedStatement p = null;
		try {
			p = _conn.prepareStatement(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return p;
	}
	
	protected void unprepare(PreparedStatement prep) {
		try {
			prep.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void beginTransaction() {
//		try {
//			Statement s = getStatement();
//			s.execute("BEGIN TRANSACTION");
//
////			_batchStatement = getStatement();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
	
	public void endTransaction() {
		boolean notDone = true;
		while (notDone) {
			try {
//				_batchStatement.executeBatch();
				
//				Statement s = getStatement();
//				s.execute("END TRANSACTION");
				_conn.commit();
				notDone = false;
			} catch (Exception e) {
				Thread.yield();
			}
		}
	}
	
	/**
	 * dynamically allow the creation of new relation tables.
	 * this effectively lets us break up the tables into smaller
	 * workable chunks.
	 * 
	 * We may even allow the type of the table to become part
	 * of the table name.  For predicate based fluents this proves
	 * to be beneficial since predicates tend to last shorter amounts
	 * of time.
	 * @param tableName
	 */
	public void createFluentTable(String tableName) {
		String table = CREATE_FLUENT_TABLE.replaceFirst("STUB", tableName);
//		String index = CREATE_FLUENT_INDEX_1.replaceAll("STUB", tableName);
		
		try {
			Statement s = getStatement();
			s.executeUpdate(table);
//			s.executeUpdate(index);
		} catch (Exception e) {
			System.out.println("Table: " + table);
//			System.out.println("Index: " + index);
			e.printStackTrace();
		}
	}
	
	/**
	 * really for now only used to insert the missing entity
	 * for properties.  This allows us to do a join on tables
	 * and get unique rows.
	 * @param name
	 * @param id
	 * @return
	 */
	private int findOrAdd(String name, int id) {
		try {
			Statement s = getStatement();
			String sql = "select id from entity_table where name = '" + name + "'";
			ResultSet rs = s.executeQuery(sql);
			if (rs.next()) {
				id = rs.getInt(1);
			} else {
				addEntity(name, id);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}
	/**
	 * add an entity to the entity table.
	 * @param name
	 * @param id
	 */
	public void addEntity(String name, int id) {
		try {
			Statement s = getStatement();
			s.executeUpdate("insert into entity_table (id, session_id, name) " +
					"values (" + id + "," + _sessionId + ",'" + name + "')");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ResultSet getTables(String type) {
		try {
			return _conn.getMetaData().getTables(null, null, type, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}
