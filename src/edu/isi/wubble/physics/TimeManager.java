package edu.isi.wubble.physics;

public class TimeManager {

	private static TimeManager _mgr = null;
	
	private long _logicalTime;
	private long _systemStartTime;
	
	private TimeManager() {
		reset();
	}
	
	public static TimeManager inst() {
		if (_mgr == null) {
			_mgr = new TimeManager();
		}
		return _mgr;
	}
	
	public void reset() {
		_logicalTime = 0;
		_systemStartTime = System.currentTimeMillis();
	}
	
	public void update() {
		++_logicalTime;
	}
	
	public long getLogicalTime() {
		return _logicalTime;
	}
	
	public long getElapsedTime() {
		return System.currentTimeMillis() - _systemStartTime;
	}
}
