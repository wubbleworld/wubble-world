package edu.isi.wubble.physics.state;

import java.sql.Statement;

import edu.isi.wubble.physics.TimeManager;

public class Tag {
	
	protected boolean _isOpen;
	
	protected String _tag;
	protected int    _dbRecordId;
	
	public Tag(String tag) {
		_tag = tag;
	}
	
	public boolean isOpen() {
		return _isOpen;
	}
	
	public void openDB() {
		System.out.println("...opening tag " + _tag);
		_isOpen = true;
		try {
			Statement s = StateDatabase.inst().getStatement();
			_dbRecordId = StateDatabase.inst().getNextId("tag_table");
			int sessionId = StateDatabase.inst().getSessionId();
			long time = TimeManager.inst().getLogicalTime();
			
			String sql = START_I + _dbRecordId + "," + sessionId + ",'" + _tag + "'," + time + ")";
			s.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void closeDB() {
		System.out.println("...closing tag " + _tag);
		_isOpen = false;
		try {
			Statement s = StateDatabase.inst().getStatement();
			long time = TimeManager.inst().getLogicalTime();
			
			String sql = START_U + time + END_U + _dbRecordId;
			s.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static final String START_I = "insert into tag_table (id, session_id, tag, start_logical_time) VALUES (";
	protected static final String START_U = "update tag_table set end_logical_time = ";
	protected static final String END_U = " where id = ";
}
