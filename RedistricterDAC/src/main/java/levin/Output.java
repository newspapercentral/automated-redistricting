package levin;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class Output {
	
	public static void write(String file, String output){
		PrintWriter writer;
		try {
			writer = new PrintWriter(file, "UTF-8");
			writer.print(output);
			writer.close();
		} catch (FileNotFoundException e) {
			System.err.println("Output error: trying to write to file:" + file + " with output: " + output);
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}

}
