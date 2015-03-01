package edu.isi.wubble.sheep.character;


// This is kind of poorly named but it will do for now
public interface Selectable {

	public void setupSelection();
	
	public void addAttention();
	
	public void removeAttention();
	
	public float getSelectionWidth();
	
	public float getSelectionHeight();
	
	public float getSelectionOffset();
}
