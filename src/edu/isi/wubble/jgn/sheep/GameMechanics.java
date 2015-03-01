package edu.isi.wubble.jgn.sheep;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.util.GameTaskQueueManager;

import edu.isi.wubble.Constants;
import edu.isi.wubble.jgn.message.InvokeCallable;
import edu.isi.wubble.jgn.message.InvokeMessage;
import edu.isi.wubble.jgn.sheep.action.CollisionManager;
import edu.isi.wubble.jgn.sheep.entity.SDynamicEntity;
import edu.isi.wubble.jgn.sheep.entity.SEntity;
import edu.isi.wubble.jgn.sheep.entity.PowerUp;
import edu.isi.wubble.jgn.sheep.entity.Sheep;
import edu.isi.wubble.jgn.sheep.entity.Sidekick;
import edu.isi.wubble.jgn.sheep.entity.Wrench;
import edu.isi.wubble.jgn.sheep.entity.Wubble;
import edu.isi.wubble.sheep.PowerUpInfo;
import edu.isi.wubble.sheep.PowerUpInfo.PowerUpType;

public class GameMechanics {
	// This precious little number gets us around some race conditions.
	// ////////////////////////////////////////////////////////////
	protected class WubbleAdder implements Callable<Integer> {
		String _name;
		short _id;
		Vector3f _pos;
		int      _whichTeam;
		boolean  _isSidekick;

		// This is the sidekick constructor.
		public WubbleAdder(String parentName) {
			// For sidekicks, the name is taken to be the parent's name.
			_name = parentName;
			_id = Utils.IMPOSSIBLE_CLIENT_ID;
			_isSidekick = true;
			GameTaskQueueManager.getManager().update(this);
		}
		
		public WubbleAdder(String name, short id, int whichTeam) {
			_name = name;
			_id   = id;
			_whichTeam = whichTeam;
			_isSidekick = false;

			GameTaskQueueManager.getManager().update(this);
		}
		
		public Integer call() throws Exception {
			Wubble w = null;
			if (_isSidekick == false) {
				// Create a new wubble, set its color, give movement controls.
				w = new Wubble(_name, _id, _whichTeam); //  getHomeGoal(_whichTeam));

				// Give the wubble its grand entrance, facing in the proper direction.
				moveToHomeGoal(w);
				w.addMovementControls(false);
			}
			else {
				// The wubble I'm adding is a sidekick.
				w = new Sidekick(_name);
				moveToHomeGoal(w);
				
				// This makes the sidekick immediately show up on the client
				getSps().addCharToClient(w);
				
				// Really I don't need to add movement controls, except that the sidekick's movement 
				// should be expressed in the form of controllers.  We'll thus need to make sure that 
				// computationally-directed movement can be sensibly affected.
				// w.addMovementControls(true);
			}
			
			
			if (w.getTeam() == Constants.RED_TEAM) { w.setColor(ColorRGBA.red); _numRed++; }
			else { w.setColor(ColorRGBA.blue); _numBlue++; } 

			p("Wubble " + _name + " has been added for team " + _whichTeam);
			p("There are " + _numRed + " red and " + _numBlue + " blue.");

			// This is junk.
			return 10;
		}
	}
	
	// Stuff relating to the number of outstanding PUs that can exist in the game,
	// and the threshhold for PU generation.
	static final int MAX_OUTSTANDING_PUS = 8;
	private static int _thresh = 500;
	private static long _gameStart = 0;
	
	public static int GetPUThreshold() { return _thresh; } 
	
	
	protected Vector3f getHomeGoal(int team) {
		Vector3f rc = new Vector3f(_redCenter);
		Vector3f bc = new Vector3f(_blueCenter);
		
		Vector3f pos = rc;
		if (team == Constants.BLUE_TEAM) { pos = bc; }
		
		return pos;
	}
	
	public void moveToHomeGoal(Wubble w) {
		if (false) { return; }
		System.out.println("Moving " + w.getName() + " to team " + w.getTeam() + " goal.");
		Vector3f newTran = new Vector3f (getHomeGoal(w.getTeam()));
		newTran.y += 5;
		w.getNode().setLocalTranslation(newTran);
		
		// Look to the center of the arena.
		// This change is very important, otherwise all rotations are slightly off
		// the wubble was looking down at the ground!!
		Vector3f lookVec = new Vector3f(0, newTran.y, 0);
		Vector3f upVec = Vector3f.UNIT_Y;
		w.getNode().updateGeometricState(0, true);
		w.getVisNode().lookAt(lookVec, upVec);
		w.getVisNode().updateGeometricState(0, true);
	}
	
	
	public void updateGame() { makeRandomPowerups(); }
	
	// These have been moved from various locations within the method block
	Vector3f _redCenter;
	Vector3f _blueCenter;
	int _endZoneSize;
	boolean _isJimboSpawned = false;
	
	// Assume this will get called before wubbles get added.
	public void setCenters(Vector3f redCenter, Vector3f blueCenter) {
		_redCenter = redCenter;
		_blueCenter = blueCenter;
	}
	
	protected SheepPhysicsState _sps;
	public    SheepPhysicsState getSps() { return _sps; }
	
	
	// Construction
	public GameMechanics(SheepPhysicsState sps) { _sps = sps; }

	int _numRed = 0;
	int _numBlue = 0;
	int _zStartOffset = 1;
	
	// Scoring
	int[] _score = new int[Constants.NUM_TEAMS];
	
	int _scoreToWin;
	public int getScore(int team)      { return _score[team]; }
	public int getScoreToWin()         { return _scoreToWin; }
	
	
	// For random purposes.  Or rather, for the purpose of randomness.
	Random _generator = new Random(System.currentTimeMillis());
	

	// ////////////////////////////////////////////////////////////
	// Fooled by randomness
	// ////////////////////////////////////////////////////////////

	// Wraps the random generator.
	public int randInt(int limit) {
		return (_generator.nextInt(limit));
	}
	
	// Return a random number between [0, +- limit)
	public int halfRand(int limit) {
		int realLimit = limit * 2 + 1;
		int r = _generator.nextInt(realLimit);

		int retVal = 0;
		if (r < limit) {
			retVal = -r;
		} else if (r > limit) {
			retVal = r - (limit + 1);
		}

		return retVal;
	}

	
	// ////////////////////////////////////////////////////////////		
	public void addWubble(String name, short id, Integer whichTeam) {
		// Add a callable to create a wubble during the next update cycle.
		// System.out.println("addWubble: " + name + " " + id + " " + whichTeam);
		new WubbleAdder(name, id, whichTeam);		
	}

	public void addSidekick(String parentName) { new WubbleAdder(parentName); }

	
	// Make powerups from time to time.
	// ////////////////////////////////////////////////////////////
	static int PID = 0;
	
	protected boolean makeRandomPowerups() {
		if (!SheepPhysicsState.PRODUCTION) { 
			return false; 
		}
		
		int numOutstanding = PowerUp.GetOutstandingPUs();
		if (numOutstanding >= MAX_OUTSTANDING_PUS) { return false; } 
		
		SheepPhysicsState sps = getSps();
		int roll = randInt(100000);
		int thresh = GetPUThreshold();
		boolean retVal = false; 
		// If I roll just right, make a new powerup.
		if (roll < thresh) { 
			retVal = true;
			Vector3f pos = getRandomPlacement();
			PowerUpInfo.PowerUpType [] vals = PowerUpInfo.PowerUpType.values();
			int length = vals.length;
			PowerUpInfo.PowerUpType randType = PowerUpType.NONE;
			while (randType == PowerUpType.NONE || randType == PowerUpType.POWERDOWN  || randType == PowerUpType.MULTI) {
				randType = vals[randInt(length)];
			}
			PowerUp pu = new PowerUp("PowerUp-"+PID, 500 + randInt(1000), randType, pos);
			sps.p("Made new PowerUp at pos " + Utils.vs(pos));
			PID++;
			
			// Send the invocation msg.
			getSps().addCharToClient(pu);
		}
		return retVal;
	}
	
	
	public Vector3f getRandomPlacement() {
		float z = halfRand(15);
		float y = .4f;
		float x = halfRand(21);
		Vector3f retVal = new Vector3f(x, y, z);
	
		// Should check that it isn't in the endzone, but worry about that later.
		return retVal;
	}
	
	/////////////////////////////////////////////////////////////////
	// Gamery
	/////////////////////////////////////////////////////////////////
	
	// Reset the game - meaning repopulate sheep, and whatever other shit is 
	// necessary to get it into a starting state.
	/////////////////////////////////////////////////////////////////
	public void resetGame() { 
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("doResetGame");
		SheepPhysicsState sps = getSps();
		new InvokeCallable(im, sps);
	}
	
	boolean _hasBeenWon = false;
	
	public void doResetGame() {
		SheepPhysicsState sps = getSps();
		sps.p("doResetGame!");
		if (_hasBeenWon == false) {
			sps.p("Resetting too early!");
			return;
		}
		
		// Reset team counts.  These get re-calculated when wubbles are added. 
		_numRed = 0;
		_numBlue = 0;
		
		// Yank all the sheep.
		Hashtable<String, SEntity> newEntities = new Hashtable<String, SEntity>(sps.getEntities());
		Set<String> names = new HashSet<String>(sps.getEntities().keySet());
		
		for (String name : names) {
			SEntity e = newEntities.get(name); 
			// p("Checking e: " + e.getName());
			
			// This is bollocks.  The Entities should tell you whether they should be removed.
			if ((e instanceof Sheep) || (e instanceof Wrench) || (e instanceof PowerUp)) {
				// p("    -- drg: removing old entity: " + e.getName());
				newEntities.remove(name);
				sps.removeEntity(e.getName());
			}

			// If it's an entity, power it down.
			if (e instanceof SDynamicEntity) {
				//p("Powering down \"" + e.getName() + "\" on reset.");
				((SDynamicEntity)e).powerDown();
			}
		}
		
		sps.setEntities(newEntities);
		
		// Don't do this for now, until I figure out wtf to do with these collisions so
		// as not to break "Jump."
		CollisionManager.Get().resetCollisionHash();
		
		// Move wubbles back to their endzones.
		for (Wubble w : sps.getWubbleIDs().values()) {
			moveToHomeGoal(w);
		}
		
		String message = "";
		if (_score[Constants.BLUE_TEAM] > _score[Constants.RED_TEAM]) {
			message = "BLUE team won! The score was " + _score[Constants.BLUE_TEAM] + "-" + _score[Constants.RED_TEAM];
		} else {
			message = "RED team won! The score was " + _score[Constants.RED_TEAM] + "-" + _score[Constants.BLUE_TEAM];
		}
		
		InvokeMessage victory = new InvokeMessage();
		victory.setArguments(new Object[]{message});
		victory.setMethodName("systemMessage");
		victory.sendToAll();
		
		// Reset the score, and send the new (zero) score to clients.
		for (int i=0; i<Constants.NUM_TEAMS; i++) { _score[i] = 0; }

		// Stick an IC in the pipeline to update all the clients of the score.
		updateScores();
		
		// reset the power ups
		PowerUp.ResetOutstandingPUs();
		
		// Since I might have had to delete stuff, I need to wait until the messages 
		// that finish off that deletion get processed.  Which means I need to delay 
		// finishing up the reset by keying it off the receipt of ANOTHER message.
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("finishReset");
		new InvokeCallable(im, sps);
	}

	public void updateScores() {
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("updateClientScores");
		new InvokeCallable(im, this);
	}
	
	public void updateClientScores() {
		InvokeMessage score = new InvokeMessage();
		score.setArguments(new Object[]{_score[0], _score[1]});
		score.setMethodName("updateScore");
		score.sendToAll();
	}
	
	public void finishReset() {
		// Geterdone.
		setupGame();
		
		// Tell all the clients about what's here.
		// Send out the state of things.  Loop through all the dynamic entities.
		InvokeMessage im = new InvokeMessage();
		im.setMethodName("setWorldState");
		im.setArguments(new Object[]{getSps().constructDynamicEntityState(), _score[Constants.BLUE_TEAM], _score[Constants.RED_TEAM]});
		im.sendToAll();
		
		InvokeMessage im2 = new InvokeMessage();
		im2.setArguments(new Object[]{"Starting new game!"});
		im2.setMethodName("systemMessage");
		im2.sendToAll();
	}
	
	
	
	static int _sheepID = 0;
	// Populate the world with the right amount of everything.
	/////////////////////////////////////////////////////////////////
	public void setupGame() {
		// Re-spawn the appropriate number of sheep
		int numSheep = 31;
		
		if (!SheepPhysicsState.PRODUCTION) {
			numSheep = 1;
		}
		
		// It takes half the sheep to win.
		_scoreToWin = numSheep / 2;
		p("It takes " + _scoreToWin + " sheep to win.");
		
		_hasBeenWon = false;
		
		// Non-infrastructural entities.
		//////////////////////////
		
		// Don't create this stuff if I'm replaying.
		int mod = 1;
		for (int i = 0; i < numSheep; i++) {
			//int x = _generator.nextInt(40) - 20;
			//int z = _generator.nextInt(80) - 40;
			mod *= -1;
			int x = i/2 * mod;
			int z = 0;

			Sheep s = new Sheep("sheep-" + i + "[" + _sheepID + "]", x, 2, z);
			
			if (!SheepPhysicsState.PRODUCTION) {
				s.setBehaviorActive(false);
			}
			
			_sheepID++;
		}
		
		// Add a wubble for testing.
		if (!_isJimboSpawned && !SheepPhysicsState.PRODUCTION) {
			System.out.println("Adding Jimbo.");
			_isJimboSpawned = true;
			addWubble("Jimbo", (short)100, (Integer)1);
			addSidekick("Jimbo");
			//addWubble("Bimbo", (short)101, (Integer)0);
		}
		
		//////////////////////////

		// Add some wrenches in the appropriate spots.
		new Wrench("blueWrench1", -8, 5, -20);
		new Wrench("blueWrench2", 8, 5, -20);
		new Wrench("redWrench1", -8, 5, 20);
		new Wrench("redWrench2", 8, 5, 20);
		
		// Save the start of the me.
		_gameStart = System.currentTimeMillis();
		System.out.println("Started game at " + _gameStart);
		
		// Make some predicates.
		//Predicate p = new Predicate("baseClassPred");
		//GoPred g = new GoPred("derivedGoClass", 1);
		
		//new PowerUp("PowerUpS", -1, PowerUpInfo.PowerUpType.SPEEDY, new Vector3f(0, 0.4f, 0));
		//new PowerUp("PowerUpE", -1, PowerUpInfo.PowerUpType.EATER, new Vector3f(2, 0.4f, 0));
		//new PowerUp("PowerUpS", -1, PowerUpInfo.PowerUpType.STICKY, new Vector3f(2, 0.4f, 2));
	}
	
	// A sheep just got scored.  Do whatever game-accounting is necessary.
	/////////////////////////////////////////////////////////////////
	public void scoreSheep(Sheep s, int team) {
		if (_hasBeenWon) { p("!!!! Can't score a sheep yet!"); return; }
		
		_score[team] += 1;
		p("Team " + team + " has scored " + _score[team] + " sheep!");
		
		updateScores();
//
//		InvokeMessage im = new InvokeMessage();
//		im.setArguments(new Object[]{_score[0], _score[1]});
//		im.setMethodName("updateScore");
//		im.sendToAll();
		
		if (_score[team] >= _scoreToWin) {
			p("Team " + team + " is the winner!");
			
			_hasBeenWon = true;
			
			// If I'm not replaying, then reset the game.
			if (!_sps.getReplayer().isReplay()) {
				resetGame();
			}
			else {
				System.out.println("Not resetting bc I'm in playback.");
			}
		}
	}

	public long getStartTime() { return _gameStart; }
	public void p(String s)    { getSps().p(s);     }
}
