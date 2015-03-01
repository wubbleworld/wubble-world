package edu.isi.wubble.physics.state;

import edu.isi.wubble.physics.entity.Entity;

public class AutoOffFluent extends Fluent {

	public AutoOffFluent(String name, Entity a, Entity b, Object value) {
		super(name, a, b, value);
		
		_closed = false;
		_updated = true;
	}
	
	public AutoOffFluent(String name, Entity a, Object value) {
		super(name, a, value);
		
		_closed = false;
		_updated = true;
	}

	/**
	 * preUpdate make sure that we have set updated to false
	 * in order to force the system to update this fluent
	 * to remain active.
	 */
	public void preUpdate() { 
		_updated = false;
	}

	/**
	 * check to see if the update happened.
	 */
	public void postUpdate() {
		if (_closed)
			return;
		
		// if we placed an autooff fluent on a non-boolean
		// fluent and forgot to update it one time tick
		// we simply turn off the fluent.
		if (!_booleanBased) {
			if (!_updated && !_closed) {
				closeDB();
				_value = null;
				_closed = true;
			}
		} else {
			// if we havent' updated and we were previously on
			// turn off.
			if (!_updated && ((Boolean) _value).booleanValue()) {
				closeDB();
				_value = new Boolean(false);
				openDB();
			}
		}
	}
	
	/**
	 * default update for objects.  Note that we have updated
	 * ourselves, but if that value is different than a previous
	 * value, we close this relation and open a new one.
	 * @param val
	 */
	public void update(Object val) {
		if (_closed) {
			_closed = false;
			setValue(val);
			openDB();
			return;
		}

		super.update(val);
	}
}
