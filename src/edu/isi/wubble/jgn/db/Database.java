package edu.isi.wubble.jgn.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class Database {

	protected Connection _conn;

	protected HashMap<String,Integer> _maxMap;
	
	protected String _dbURL = "jdbc:mysql://www.wubble-world.com:3306/wubble_rpg";
	protected String _dbDriver = "com.mysql.jdbc.Driver";
	
	protected Statement _defaultStatement;
	
	public Database() {
		connect();
		
	}
  
	public void setDB(String s)          { _dbURL = s;    }
	public void setDriver(String s)      { _dbDriver = s; }
	
	public void setAutoCommit(boolean b) { 
		try { _conn.setAutoCommit(b); }
		catch (SQLException e) { e.printStackTrace(); }
	}
	
	public void commit() {
		try {
			_conn.commit(); 
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public boolean connect() {
		try {
			if (_conn != null) 
				disconnect();
			
			Class.forName(_dbDriver).newInstance();
			_conn = DriverManager.getConnection(_dbURL, "crue", "flat2#");

			_maxMap = new HashMap<String,Integer>();
			_defaultStatement = null;
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	public void disconnect() {
		try {
			_conn.close();
		} catch (Exception e) {
			System.out.println("ERROR closing");
			e.printStackTrace();
		}
	}
	
	public Connection getConnection() {
		return _conn;
	}
	
	public Statement createStatement() throws SQLException {
		return _conn.createStatement();
	}

	public Statement getStatement() throws SQLException {
		if (_defaultStatement == null)
			_defaultStatement = _conn.createStatement();
		return _defaultStatement;
	}

	public int getLastInsertId() throws SQLException {
		Statement s = getStatement();
		ResultSet rs = s.executeQuery("SELECT LAST_INSERT_ID()");
		int id = -1;
		if (rs.next()) {
			id = rs.getInt(1);
		}
		rs.close();
		return id;
	}

	public boolean authenticate(String userName, String password) {
		try {
			Statement s = getStatement();
			ResultSet rs = s.executeQuery("SELECT user_name FROM jeanie_baby.user " +
					"WHERE user_name = '" + userName + "' " +
					"AND user_password = sha1('" + password + "')");
			boolean success = false;
			if (rs.next())
				success = true;
			rs.close();
			return success;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public String create(String userName, String password) {
		String result = "INVALID";
		
		// NO SPACES
		if (userName.indexOf(" ") >= 0 || password.indexOf(" ") >= 0) {
			return result;
		}
		
		try {
			Statement s = getStatement();
			ResultSet rs = s.executeQuery("SELECT user_name FROM jeanie_baby.user " +
					"WHERE user_name = '" + userName + "'");
			boolean exists = false;
			if (rs.next())
				exists = true;
			rs.close();
			
			if (exists) {
				return "EXISTS";
			} else {
				Statement c = getStatement();
				c.executeUpdate("INSERT INTO jeanie_baby.user " +
						"(user_name,user_password) " +
						"VALUES ('" + userName + "',sha1('" + password + "'))");
				
				return "SUCCESS";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public void insertOrUpdate(String insertSQL, String updateSQL) {
		try {
			Statement s = DatabaseManager.inst().getStatement();
			int rows = s.executeUpdate(updateSQL);
			if (rows == 0)
				s.executeUpdate(insertSQL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int insert(String sql) {
		return insert(sql, false);
	}
	
	public int insert(String sql, boolean lastId) {
		try {
			Statement s = DatabaseManager.inst().getStatement();
			s.executeUpdate(sql);

			if (lastId)
				return DatabaseManager.inst().getLastInsertId();
		} catch (Exception e) {
			e.printStackTrace();
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
	
	protected int getMaxDB(String name) throws Exception {
		Statement s = getStatement();
		ResultSet rs = s.executeQuery("select max(id)+1 from " + name);
		if (rs.next()) {
			return rs.getInt(1);
		} 
		return 0;
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