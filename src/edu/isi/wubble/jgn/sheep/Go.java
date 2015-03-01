package edu.isi.wubble.jgn.sheep;


import java.lang.reflect.Method;


public class Go {

	protected static Go _go = null;
	
	public static Go Get() {
		if (_go == null) { _go = new Go(); }
		return _go; 
	}
	
	
	public static Process startAnotherClassInItsOwnThread(String classname, String[] args) {
	  Process process = Get().new Process(classname, args);
	  Thread thread = new Thread(process);
	  thread.start();
	  return process;
	}

	
	class Process implements Runnable {
		  String theClassName;
		  String[] theStartupArguments;

		  public Process(String classname, String[] args) {
		    theClassName = classname;
		    theStartupArguments = args;
		  }

		  public void run() {
			  try { startAnotherClass(theClassName, theStartupArguments); }
			  catch (Exception e) { System.out.println("Fuck!"); e.printStackTrace(); }
		  }
		}
	
	
	public static void startAnotherClass(String classname, String[] args) throws Exception {
		  //Get the class
		  Class classObject = Class.forName(classname);

		  //Find the main(String[]) method of that class.
		  //main has one parameter, a String array. Set
		  //that argument type
		  Class[] mainParamType = {args != null ? args.getClass() : null};
		  //Search for the main(String[]) method
		  
		  Method main = classObject.getMethod("main", mainParamType);
		  //Create an object array of the arguments to pass
		  //to the method
		  Object[] mainParams = {args};

		  //start the real class.
		  main.invoke(null, mainParams);
		}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] fakeArgs = new String[] { "blah", "blah" };
		try {
			System.out.println("Spawning sheep server.");
			startAnotherClassInItsOwnThread("edu.isi.wubble.jgn.sheep.SheepServer", fakeArgs);
			System.out.println("Sleeping for ten seconds.");
			Thread.sleep(10000);
			System.out.println("Spawning Main");
			startAnotherClassInItsOwnThread("edu.isi.wubble.GoMain", fakeArgs);
		} catch (Exception e) {
			System.out.println("Weird-ass exception: " + e);
			e.printStackTrace();
		}
	}
}
