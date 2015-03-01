package edu.isi.wubble.jgn.rpg;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.sql.Statement;
import java.util.TreeSet;

import edu.isi.wubble.jgn.db.DatabaseManager;
import edu.isi.wubble.jgn.message.InvokeMessage;

public class RPGPhysicsDB {

	protected int _sessionId;
	protected long _startTime;
	
	public RPGPhysicsDB() {
		
	}
	
	public int getUserId(String userName) {
		try {
			String sql = "SELECT user_id FROM jeanie_baby.user WHERE user_name = '" + userName + "'";
			Statement s = DatabaseManager.inst().createStatement();
			ResultSet rs = s.executeQuery(sql);
			int id = -1;
			if (rs.next()) {
				id = rs.getInt("user_id");
			}
			rs.close();
			s.close();
			
			return id;
		} catch (Exception e) {
			System.out.println("[getUserId] " + e.getMessage());
		}
		return -1;
	}

	private long getTime() {
		return System.currentTimeMillis() - _startTime;
	}
	
	public String objectArrayToLispString(Object[] objects) {
		if (objects == null) 
			return "";
		
		StringBuffer buf = new StringBuffer("(");
		for (int i = 0; i < objects.length; ++i) {
			buf.append("\"" + objects[i].toString() + "\" ");
		}
		buf.append(")");
		return buf.toString();
	}
	
	public void createSession() {
		try {
			String sql = "INSERT INTO rpg_session (shooter_name, picker_name, start_time) " +
					"VALUES ('temp', 'temp', 0)";
			Statement s = DatabaseManager.inst().getStatement();
			s.executeUpdate(sql);
			
			_sessionId = DatabaseManager.inst().getLastInsertId();
			_startTime = System.currentTimeMillis();
		} catch (SQLException e) {
			System.out.println("[ERROR] - creating rpg session");
			e.printStackTrace();
		}
	}
	
	public void finishSession() {
		try {
			String sql = "UPDATE rpg_session SET end_time = " + getTime() + " WHERE session_id = " + _sessionId;
			Statement s = DatabaseManager.inst().getStatement();
			s.executeUpdate(sql);
		} catch (SQLException e) {
			System.out.println("[ERROR] - closing rpg session");
			e.printStackTrace();
		}
	}
	
	public void addUserAsShooter(String name) {
		try {
			String sql = "UPDATE rpg_session SET shooter_name = '" + name + "' WHERE session_id = " + _sessionId;
			Statement s = DatabaseManager.inst().getStatement();
			s.executeUpdate(sql);
		} catch (SQLException e) {
			System.out.println("[ERROR] - addUserAsShooter");
			e.printStackTrace();
		}
	}

	public void addUserAsPicker(String name) {
		try {
			String sql = "UPDATE rpg_session SET picker_name = '" + name + "' WHERE session_id = " + _sessionId;
			Statement s = DatabaseManager.inst().getStatement();
			s.executeUpdate(sql);
		} catch (SQLException e) {
			System.out.println("[ERROR] - addUserAsShooter");
			e.printStackTrace();
		}
	}
	
	public void receivedMsg(String msg, Object[] params) {
		try {
			String realParams = objectArrayToLispString(params);
			String sql = "INSERT INTO rpg_message (session_id, event_time, name, params)" +
					"VALUES (" + _sessionId + "," + getTime() + ",'" + msg + "','" + realParams + "')";
			Statement s = DatabaseManager.inst().getStatement();
			s.executeUpdate(sql);
		} catch (SQLException e) {
			System.out.println("[ERROR] - receivedMsg");
			e.printStackTrace();
		}
	}
	
	public void sendingMsg(InvokeMessage msg) {
		receivedMsg(msg.getMethodName(), msg.getArguments());
	}
	
	
	public void updateMsg(int type, String msg) {
		try {
			String sql = "INSERT INTO rpg_update (session_id, event_time, update_type, msg)" +
					"VALUES (" + _sessionId + "," + getTime() + "," + type + ",'" + msg + "')";
			Statement s = DatabaseManager.inst().getStatement();
			s.executeUpdate(sql);
		} catch (SQLException e) {
			System.out.println("[ERROR] - updateMsg");
			e.printStackTrace();
		}
	}
	
	public TreeSet<String> getKnownWords(String userName) {
		TreeSet<String> words = new TreeSet<String>();
		try {
			String sql = "SELECT word FROM known_words WHERE user_id = " + getUserId(userName);
			Statement s = DatabaseManager.inst().createStatement();
			ResultSet rs = s.executeQuery(sql);
			while (rs.next()) {
				words.add(rs.getString("word"));
			}
			rs.close();
			s.close();
		} catch (Exception e) {
			System.out.println("[getKnownWords] " + e.getMessage());
		}
		return words;
	}
	
	public void saveWord(String userName, String word) {
		try {
			String sql = "INSERT INTO known_words (user_id, word) " +
					"VALUES (" + getUserId(userName) + ",'" + word + "')"; 
			Statement s = DatabaseManager.inst().getStatement();
			s.executeUpdate(sql);
		} catch (Exception e) {
			System.out.println("[saveWord] " + e.getMessage());
		}
	}

}
