import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	 try {
		 //Read file into StringBuilder
		BufferedReader readfile = new BufferedReader(new FileReader("OSWI.txt"));
		StringBuilder file_success = new StringBuilder();
		String line = null;
		while((line = readfile.readLine()) != null){
			file_success.append(line+"\n");
		}
		System.out.println("Success! : file read successful.");
		
	} catch (FileNotFoundException e) {
		//Display : file not found.
		System.out.println("Error! : File not found.");
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		//Display : file cannot read.
		System.out.println("Error! : File cannot read.");
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}

}
