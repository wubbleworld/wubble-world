package edu.isi.wubble.physics.state;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import edu.isi.wubble.physics.TimeManager;
import edu.isi.wubble.physics.entity.Entity;
import edu.isi.wubble.util.Formatter;
import edu.isi.wubble.util.Globals;

public class Fluent {
	protected static HashMap<String,Integer> _fluentIdMap;
	
	static {
		_fluentIdMap = new HashMap<String,Integer>();
	}

	protected String _name;
	protected int    _nameId;
	
	protected Entity _entity1;
	protected Entity _entity2;
	
	protected Object _value;
	
	protected boolean _updated;
	protected boolean _booleanBased;
	protected boolean _closed;
	
	protected int    _dbRecordId;
	
	protected Fluent(String name, Object value) {
		_name = name;
		
		Integer nameKey = _fluentIdMap.get(name);
		if (nameKey == null) {
			if (!Globals.USE_DATABASE)
				nameKey = _fluentIdMap.size();
			else
				nameKey = findOrAdd();
			_fluentIdMap.put(_name, nameKey);
		}
		_nameId = nameKey;
		_value = value;
		
		if (_value instanceof Boolean)
			_booleanBased = true;
	}
	
	public Fluent(String name, Entity a, Object value) {
		this(name, value);
		_entity1 = a;
		_entity2 = null;
		
		openDB();
	}
	
	public Fluent(String name, Entity a, Entity b, Object value) {
		this(name, value);
		_entity1 = a;
		_entity2 = b;
		
		openDB();
	}

	public String getName() {
		return _name;
	}
	
	/**
	 * a uniqe string representing the whole fluent and the parameters
	 * that are part of it.
	 * @return
	 */
	public String getKey() {
		if (_entity2 == null)
			return _name + " " + _entity1.getName();
		else
			return _name + " " + _entity1.getName() + " " + _entity2.getName();
	}

	/**
	 * findOrAdd will locate this fluent in the database.  If it doesn't
	 * exist it will create it.  This keeps the numbering consistent across
	 * trials...
	 * @return
	 */
	protected int findOrAdd() {
		int results = -1;
		try {
			Statement s = StateDatabase.inst().getStatement();
			String sql = "select id from lookup_fluent_table where name = '" + _name + "'";
			ResultSet rs = s.executeQuery(sql);
			if (rs.next()) {
				results = rs.getInt(1);
			} else {
				results = add();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}

	/**
	 * add this fluent to the database and get it a unique identifier.
	 * @return
	 */
	protected int add() {
		int results = -1;
		try {
			results = StateDatabase.inst().getNextId("lookup_fluent_table");
			Statement s = StateDatabase.inst().getStatement();
	
			String sql = "insert into lookup_fluent_table (id, name) values (" + results + ",'" + _name + "')";
			s.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}

	public void preUpdate() {}
	public void postUpdate() {}
	
	/**
	 * allow users to open a database record for this fluent
	 * and if it fails it must mean we are missing the 
	 * correct database table, so we will create that
	 * table.
	 */
	public void openDB() {
		openDB(true);
	}
	
	private void openDB(boolean fixOnError) {
		if (!weCare())
			return;

		try {
			_dbRecordId = StateDatabase.inst().getNextId("fluent_" + _name);
			_entity1.getManager().addChange(getKey());

			int sessionId = StateDatabase.inst().getSessionId();
			long time = TimeManager.inst().getElapsedTime();
			long logicalTime = TimeManager.inst().getLogicalTime();
			
			int entity1Id = _entity1.getId();
			int entity2Id = (_entity2 == null) ? -1 : _entity2.getId();
			
			String sql = "insert into fluent_" + _name + " (session_id, id, fluent_id, entity_1, entity_2, " +
					"value, start_time, start_logical_time) values (" + sessionId + "," + _dbRecordId + "," + _nameId + 
					"," + entity1Id + "," + entity2Id + ",'" + Formatter.format(_value) + "'," + 
					time + "," + logicalTime + ")";

			Statement s = StateDatabase.inst().getStatement();
			s.executeUpdate(sql);
			
			_updated = true;
			_closed = false;
		} catch (Exception e) {
			if (fixOnError) {
				StateDatabase.inst().createFluentTable(_name);
				openDB(false);
			} else {
				System.out.println(_name + " " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public void closeDB() {
		if (!weCare() || _closed)
			return;

		try {
			int sessionId = StateDatabase.inst().getSessionId();
			long time = TimeManager.inst().getElapsedTime();
			long logicalTime = TimeManager.inst().getLogicalTime();

			String sql = "update fluent_" + _name + " set end_time = " + time + 
				", end_logical_time = " + logicalTime + 
				" where id = " + _dbRecordId + " and session_id = " + sessionId;

			Statement s = StateDatabase.inst().getStatement();
			s.executeUpdate(sql);

			_closed = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * default update for objects.  Note that we have updated
	 * ourselves, but if that value is different than a previous
	 * value, we close this fluent and open a new one.
	 * @param val
	 */
	public void update(Object val) {
		if (_value == null)
			return;
		
		_updated = true;
		// if we are closed, it doesn't matter what
		// object was recorded, we need to open a 
		// record for it.
		if (_closed) {
			setValue(val);
			openDB();
			return;
		}
		
		// if the values changes then we need to 
		// close the DB record for this fluent's old
		// value and open a new DB record for the 
		// new value.
		if (!_value.equals(val)) {
			closeDB();
			setValue(val);
			openDB();
		}
	}
	
	protected boolean weCare() {
		if (!_booleanBased)
			return true;
		
		if (((Boolean) _value).booleanValue())
			return true;
		
		return false;
	}
	
	protected void setValue(Object val) {
		if (val instanceof Quaternion)
			setValue((Quaternion) val);
		else if (val instanceof Vector3f)
			setValue((Vector3f) val);
		else
			_value = val;
	}

	protected void setValue(Quaternion q) {
		((Quaternion) _value).set(q);
	}

	protected void setValue(Vector3f v) {
		((Vector3f) _value).set(v);
	}

}