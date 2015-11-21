package levin.printout;


public class ErrorLog {

	
	
	public static void log(String message){
		System.err.println(message);
		System.exit(0);
	}

}
