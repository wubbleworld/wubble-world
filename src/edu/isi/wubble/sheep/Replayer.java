package edu.isi.wubble.sheep;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.jme.math.Vector3f;
import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.jgn.db.DatabaseManager;
import edu.isi.wubble.jgn.message.WorldUpdateMessage;
import edu.isi.wubble.jgn.sheep.SheepPhysicsState;
import edu.isi.wubble.jgn.sheep.Utils;
import edu.isi.wubble.jgn.sheep.action.CollisionManager;
import edu.isi.wubble.jgn.sheep.entity.PowerUp;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.Sheep;
import edu.isi.wubble.jgn.sheep.entity.Wrench;
import edu.isi.wubble.jgn.sheep.entity.Wubble;
import edu.isi.wubble.sheep.PowerUpInfo.PowerUpType;

public class Replayer {
	
	// This class plays nicely with openGL's threading, and calls the functions 
	// necessary to initialize replay. 
	public class ReplayInitializer implements Callable<Integer> {
		public ReplayInitializer() { register(); }
		private void register()    { GameTaskQueueManager.getManager().update(this); }
		public Integer call() throws Exception {
			doInitReplay();
			return 10;
		}
	}
	
	
	SheepPhysicsState _sps;
	
	// Collision granularity.
	int _samplesInInterval = 5;	
	int _sampleNumber = 0;

	long _curTime   = -1;
	long _endTime   = 26277;

	boolean _isPaused = false;
	
	// Construct a replayer.
	public Replayer(SheepPhysicsState sps) {
		long first = 0;
		long last = -1;
		initReplayer(sps, first, last);
	}
	
	
	void initReplayer(SheepPhysicsState sps, long startTime, long stopTime) {
		_curTime = startTime;
		_endTime = stopTime;
		_sps = sps;
	}

		
	// Interface the buttons on the GUI.
	// Start the replay over from the beginning.
	public void replayButton() { new ReplayInitializer(); }
	
	// Toggle the pause button.
	public void pauseButton() {
		if (_isPaused == true) { _isPaused = false; }
		else { _isPaused = true; }
		System.out.println("Pause now " + _isPaused);
	}
	
	
	// For now, we're not replaying.
	public boolean isReplay() {
		return (_endTime >= _curTime); 
	}
	
	// Advance the current tick; this will either be the current time, if we're playing
	// live, or the next tick in the database, if not.
	void advanceTime() {
		long time = System.currentTimeMillis();
		if (!isReplay()) { _curTime = time; }
		else { 
			_curTime = getNextTime(_curTime + 1);
			
			// If there's no later entries in the db, go back to current time.
			if (_curTime < 0) { _curTime = time; }
		}
	}
	
	
	// Play one tick of the simulation.
	public void play(float tpf) {
		
		if (!_isPaused) {
			// If the time is right, reset the collisions.
			_sampleNumber++;
			if (_sampleNumber == _samplesInInterval) {
				_sampleNumber = 0;
				CollisionManager.Get().resetCollisionHash();
				// System.out.println("Collision hash is reset!");
			}
			
			// If I'm supposed to be replaying, do that.
			if (isReplay()) { replay(); }

			// Execute behaviors on the entities that have them.
			Hashtable<String, SEntity> entities = _sps.getEntities();
			for (SEntity e : entities.values()) {
				// Have the entity react to stuff.
				e.react();

				// Have the entity do whatever the entity does, assuming we're not replaying.
				if (isReplay() == false) { e.behave(tpf); }
			}

			// If I'm not replaying, then update the game (generate random powerups, etc.) 
			// and send world updates to all clients. 
			if (isReplay() == false) { 
				_sps.getGameMechanics().updateGame(); 
				_sps.sendWorldUpdate();
			}

			// Move to the next tick.
			advanceTime();
		} else {
			// Do whatever you do when you're paused.
			// System.out.println("Doing nothing, cause I'm paused.");
		}
	}
	
	// Given the startTime argument, find the earliest subsequent time mentioned
	// in the database.
	public long getNextTime(long startTime) {
		String findMinIdxSql = 
			"select time from sheep_log where time >= " + startTime + " limit 1";
		SheepPhysicsState sps = Utils.GetSps();
		DatabaseManager dbm = sps.getSheepDB();
		
		long nextTime = -1;
		try {
			Statement s = dbm.getStatement();
			//System.out.println("Executing: " + findMinIdxSql);
			s.execute(findMinIdxSql);
			
			// Should be just one element (the index) in the result set, so advance to it.
			ResultSet rs = s.getResultSet();
			rs.next();
			nextTime = rs.getLong("time");
		} catch(Exception x) {
			Logger.getLogger("").severe("Database error!");
			x.printStackTrace();			
		}
		
		return nextTime;
	}
	
	
	// Init the replay; with no arguments it defaults to replaying everything from the first recorded
	// tick to the last.
	public void doInitReplay() {
		String findMinTimeSql = "select min(time) from sheep_log s";
		String findMaxTimeSql = "select max(time) from sheep_log s";

		// Remove leftover dynamic entities from the simulation.
		_sps.removeDynamicEntities();
		
		SheepPhysicsState sps = Utils.GetSps();
		DatabaseManager dbm = sps.getSheepDB();

		try {
			Statement s = dbm.getStatement();
			//System.out.println("Executing: " + findMinIdxSql);
			s.execute(findMinTimeSql);

			// Should be just one element (the index) in the result set, so advance to it.
			ResultSet rs = s.getResultSet();
			rs.next();
			
			_curTime = rs.getLong(1);

			s.execute(findMaxTimeSql);
			rs = s.getResultSet();
			rs.next();
			_endTime = rs.getLong(1);
		}
		catch(Exception x) {
			Logger.getLogger("").severe("Database error!");
			x.printStackTrace();			
		}
		
		// When I initialize the replay, set this state active, in case it wasn't before.
		_sps.setActive(true);
		System.out.println("Replay initialized from " + _curTime + " to " + _endTime);		
	}
		
		
	public void replay() {
		// Get the state of the world at the current replay tick.
		WorldUpdateMessage replayMsg = new WorldUpdateMessage();
		
		System.out.println("Replaying: " + _curTime);
		
		// Get the next valid time.
		_curTime = getNextTime(_curTime);
		replayMsg.readFromDb(_curTime);
		
		// System.out.println("updateMsg contains: " + replayMsg);

		// Now the replay msg will contain the update for this timestamp.  Update all the entities to reflect 
		// what's going on in the update msg.
		updateReplayedEntities(replayMsg);

		// Advance to the next time step.  If I'm done, signal to stop.
		_curTime++;
		if (_curTime > _endTime) { 
			
			// Do other shit for when replay is finished.  For now, go inactive.
			System.out.println("Replay is finished -- going inactive.");
			_sps.setActive(false);
		}		
	}
	

	protected void updateReplayedEntities(WorldUpdateMessage replayMsg) {
		int numItems = replayMsg.getNumItems();
		
		// System.out.println("URE: replaying " + numItems + " in update msg.");
		
		for (int i=0; i<numItems; i++) {
			String name = replayMsg.getName(i);
			String type = replayMsg.getType(i);
			Long   flags = replayMsg.getFlag(i);
			
			float x = replayMsg.getX(i);
			float y = replayMsg.getY(i);
			float z = replayMsg.getZ(i);
			float rx = replayMsg.getRotX(i);
			float ry = replayMsg.getRotY(i);
			float rz = replayMsg.getRotZ(i);
			float rw = replayMsg.getRotW(i);
			
			// If I'm supposed to remove this entity, do that.
			if (flags == WorldUpdateMessage.REMOVE) {
				// I can call newRemove because I'm in the proper thread.  
				// Never call this directly at any other time.
				_sps.newRemove(name);
				continue;
			}
			
			SEntity e = SEntity.GetEntityForName(name);
			
			if (e == null) {
				System.out.println("item " + i + " -- entity " + name + " doesn't exist yet!");
				
				// Make a vector for this entity's position.
				Vector3f pos = new Vector3f(x,y,z);
				// Create an entity of the appropriate type.
				
				SEntity newEntity = null; // = new Entity(name, pos);
				if (type.equalsIgnoreCase("wubble")) { 
					System.out.println(name + " is a wubble!");
					newEntity = new Wubble(name, pos);
				}
				
				else if (type.equalsIgnoreCase("wrench")) { 
					System.out.println(name + " is a wrench!");
					newEntity = new Wrench(name, (int)x, (int)y, (int)z);
				}
				else if (type.equalsIgnoreCase("powerup")) {
					System.out.println(name + " is a powerup!");
					newEntity = new PowerUp(name, -1, PowerUpType.SPEEDY, pos);
				}
				
				else if (type.equalsIgnoreCase("sheep")) { 
					System.out.println(name + " is a sheep!");
					newEntity = new Sheep(name, (int)x,(int)y,(int)z);
				}
				e = newEntity;
			}
			
			//System.out.println("Updating item " + i + ": " + name);
			
			// Update this entity's position and rotation.
			e.getPosition().set(x,y,z);
			
			// ### Note: This might need to be local rotation instead of world rotation.
			e.getRotation().set(rx, ry, rz, rw);
		}
		
		// System.out.println("Leaving URE.");
	}
}
