package edu.isi.wubble.jgn.message;

import java.util.concurrent.Callable;
import com.jme.util.GameTaskQueueManager;

// Combines two great taste sensations in one!
// ////////////////////////////////////////////////////////////
public class InvokeCallable implements Callable<Integer> {
	InvokeMessage _im;
	Object _s;
	public InvokeCallable (InvokeMessage im, Object onObject) {
		_s  = onObject;
		_im = im;
		GameTaskQueueManager.getManager().update(this);
	}
	public Integer call() throws Exception {
		System.out.println("InvokeCallable: calling method on InvokeMsg[" + _im._msgID + "]");
		_im.callMethod(_s);
		return 10;
	}
}