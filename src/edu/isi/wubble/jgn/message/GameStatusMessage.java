package edu.isi.wubble.jgn.message;

import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.type.CertifiedMessage;
import com.captiveimagination.jgn.message.type.PlayerMessage;

public class GameStatusMessage extends Message implements PlayerMessage,CertifiedMessage {

	private String[] shooterArray;
	private String[] pickerArray;
	private int[] amountDoneArray;
	
	public String[] getShooterArray() {
		return shooterArray;
	}
	public void setShooterArray(String[] shooterArray) {
		this.shooterArray = shooterArray;
	}
	public String[] getPickerArray() {
		return pickerArray;
	}
	public void setPickerArray(String[] pickerArray) {
		this.pickerArray = pickerArray;
	}
	public int[] getAmountDoneArray() {
		return amountDoneArray;
	}
	public void setAmountDoneArray(int[] amountDoneArray) {
		this.amountDoneArray = amountDoneArray;
	}
	
	
}
