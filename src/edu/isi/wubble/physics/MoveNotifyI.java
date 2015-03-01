package edu.isi.wubble.physics;


public interface MoveNotifyI {
	public String getName();
	public void moveFinished(Corrector corrector, boolean sendUpdate);
}
