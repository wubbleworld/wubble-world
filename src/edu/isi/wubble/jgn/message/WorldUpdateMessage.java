package edu.isi.wubble.jgn.message;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.PlayerMessage;

import edu.isi.wubble.jgn.db.DatabaseManager;
import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.entity.SDynamicEntity;
import edu.isi.wubble.jgn.sheep.entity.SEntity;

public class WorldUpdateMessage extends Message implements PlayerMessage {
	
	// Quick and dirty stuff to decide when to write to DB.
	public static int UPDATES_BEFORE_LOG = 30;
	public static int _updateCount = 0;
	public static int GetUpdateCount() { return _updateCount; }
	
	public WorldUpdateMessage() { 
		_updateCount++;
		
		// Create the hash of logged entities if it hasn't been created yet.
		if (_logHash == null) {
			_logHash = new HashMap<String, Boolean>();
		}
	}
	
	private ArrayList<String> _names;
	private ArrayList<String> _types;
	private ArrayList<Long>   _flags;

	private ArrayList<Float> _x;
	private ArrayList<Float> _y;
	private ArrayList<Float> _z;

	private ArrayList<Float> _rotX;
	private ArrayList<Float> _rotY;
	private ArrayList<Float> _rotZ;
	private ArrayList<Float> _rotW;
	
	public int getNumItems() {
		int numItems = 0;
		if (_names != null) {
			numItems = _names.size();
		}
		return numItems;
	}
	
	public ArrayList<String> getNames()     { return _names;        }
	public String            getName(int i) { return _names.get(i); }

	public void setNames(ArrayList<String> names) {
		this._names = names;
	}
	
	public ArrayList<String> getTypes()     { return _types;        }
	public String            getType(int i) { return _types.get(i); }
	
	public void setTypes(ArrayList<String> types) {
		this._types = types;
	}
	
	public ArrayList<Long> getFlags()       { return _flags;          }
	public Long            getFlag(int i)   { return _flags.get(i);   }
	
	public void setFlags(ArrayList<Long> flags) {
		this._flags = flags;
	}
	
	public ArrayList<Float> getX()       { return _x;        }
	public Float            getX(int i)  { return _x.get(i); }
	public void setX(ArrayList<Float> x) {
		this._x = x;
	}
	
	public ArrayList<Float> getY()       { return _y; }
	public Float            getY(int i)  { return _y.get(i); }
	
	public void setY(ArrayList<Float> y) {
		this._y = y;
	}
	
	public ArrayList<Float> getZ()       { return _z; }
	public Float            getZ(int i)  { return _z.get(i); }
	
	public void setZ(ArrayList<Float> z) {
		this._z = z;
	}
	
	public ArrayList<Float> getRotX()       { return _rotX; }
	public Float            getRotX(int i)  { return _rotX.get(i); }
	
	public void setRotX(ArrayList<Float> rotX) {
		this._rotX = rotX;
	}
	
	public ArrayList<Float> getRotY()       { return _rotY; }
	public Float            getRotY(int i)  { return _rotY.get(i); }
	
	public void setRotY(ArrayList<Float> rotY) {
		this._rotY = rotY;
	}
	
	public ArrayList<Float> getRotZ()      { return _rotZ; }
	public Float            getRotZ(int i) { return _rotZ.get(i); }
	
	public void setRotZ(ArrayList<Float> rotZ) {
		this._rotZ = rotZ;
	}
	
	public ArrayList<Float> getRotW()      { return _rotW; }
	public Float            getRotW(int i) { return _rotW.get(i); }
	
	public void setRotW(ArrayList<Float> rotW) {
		this._rotW = rotW;
	}	
	
	
	public static HashMap<String, Boolean> _logHash;
	
	public boolean isLogged(String name) {
		boolean retVal = _logHash.containsKey(name);
		return retVal;
	}
	

	// Indicate to the log that this entity has been removed from the world.
	public static long REMOVE = 1;
	public static void LogRemove(SEntity e) {
		long time = System.currentTimeMillis();
		String name = e.getName();
		long flags = REMOVE;
		
		SheepPhysicsState sps = Utils.GetSps();
//		DatabaseManager dbm = sps.getSheepDB();
//				
//		try {
//			String sql = "insert into sheep_log (update_index, name, flags, time) values (0, '" + name + "', " + flags + ", " + time + ")";
//			//System.out.println("logRemove: " + sql);
//			Statement s = dbm.getStatement();
//			s.executeUpdate(sql);
//			
//			// Clear this entity from the log hash.
//			_logHash.remove(name);
//		}
//		catch (Exception x) {
//			Logger.getLogger("").severe("Database error!");
//			x.printStackTrace();
//		}
	}
		
	
	// Read in the changeset of all entities active at the given update index.
	public void readFromDb(long updateTime) {
		//System.out.println("readFromDB: looking for index " + updateTime);
		String sql = "select name, type, flags, x_rot, y_rot, z_rot, w_rot, x_pos, y_pos, z_pos from sheep_log where time = " + updateTime;
		
		SheepPhysicsState sps = Utils.GetSps();
		DatabaseManager dbm = sps.getSheepDB();
		
		// Create the arraylists for the elements active in this update.
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> types = new ArrayList<String>();
		ArrayList<Long> flags = new ArrayList<Long>();
		ArrayList<Float>  rx = new ArrayList<Float>();
		ArrayList<Float>  ry = new ArrayList<Float>();
		ArrayList<Float>  rz = new ArrayList<Float>();
		ArrayList<Float>  rw = new ArrayList<Float>();
		ArrayList<Float>  px = new ArrayList<Float>();
		ArrayList<Float>  py = new ArrayList<Float>();
		ArrayList<Float>  pz = new ArrayList<Float>();
		
		try {
			Statement s = dbm.getStatement();
			s.execute(sql);
			ResultSet rs = s.getResultSet();
			
			// Go to the first result.
			 while(rs.next()) {
				String name = rs.getString("name");
				String type = rs.getString("type");
				Long  flag = rs.getLong("flags"); 
				float x_rot = rs.getFloat("x_rot");
				float y_rot = rs.getFloat("y_rot");
				float z_rot = rs.getFloat("z_rot");
				float w_rot = rs.getFloat("w_rot");
				
				float x_pos = rs.getFloat("x_pos");
				float y_pos = rs.getFloat("y_pos");
				float z_pos = rs.getFloat("z_pos");				
				
				// Add all this stuff to the appropriate fields of the update msg.
				names.add(name);
				types.add(type);
				flags.add(flag);
				rx.add(x_rot);
				ry.add(y_rot);
				rz.add(z_rot);
				rw.add(w_rot);
				
				px.add(x_pos);				
				py.add(y_pos);				
				pz.add(z_pos);				
			}
		}		
		catch (Exception x) {
			Logger.getLogger("").severe("Database error!");
			x.printStackTrace();			
		}
		
		// Set the instance variables of this update msg.
		_names = names;
		_types = types;
		_flags = flags;
		_x = px;
		_y = py;
		_z = pz;
		_rotX = rx;
		_rotY = ry;
		_rotZ = rz;
		_rotW = rw;
	}

	@Override
	// Right now toString just tells the names of the entities contained in the update.
	public String toString() {
		String retVal = "";
		
		int numItems = _names.size();
		for (int i=0; i<numItems; i++) {
			String name = _names.get(i);
			
			retVal = retVal + "\n" + name;
		}
		
		return retVal;
	}
}
