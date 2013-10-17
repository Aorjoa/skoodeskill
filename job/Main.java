import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import sun.security.acl.WorldGroupImpl;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Semaphore sem_flag = new Semaphore(32);
		try {
			// Read file into StringBuilder
			BufferedReader readfile = new BufferedReader(new FileReader(
					"OSWI.txt"));
			StringBuilder file_success = new StringBuilder();
			String line = null;
			while ((line = readfile.readLine()) != null) {
				file_success.append(line + "\n");
			}
			System.out.println("Success! : Read file successful.");
			for (String item : file_success.toString().split("\n")) {
				String word = item.split(" ")[0];
				// Write file invoke thread
				word = word.toLowerCase();
				sem_flag.acquire();
				new Thread(new WriteThread(sem_flag, word)).start();
			}
			System.out.println("Success! : Write file succussful.");

		} catch (FileNotFoundException e) {
			// Display : file not found.
			System.out.println("Error! : File not found.");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Display : file cannot read.
			System.out.println("Error! : File cannot read.");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO: handle exception
			System.out.println("Error! : Thread interrupt error.");
		}

	}

}

class WriteThread implements Runnable {
	// Using class Semaphore limit thread at the same time.
	Semaphore sem_flag;
	private String word;

	public WriteThread(Semaphore sem_flag, String word) {
		this.sem_flag = sem_flag;
		this.word = word;
	}

	@Override
	public void run() {
		try {
			word = word.toUpperCase();
			new File("./" + word.charAt(0) + "/" + word.charAt(1)).mkdirs();
			BufferedWriter writefile = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(
							"./"
									+ word.charAt(0)
									+ "/"
									+ word.charAt(1)
									+ "/"
									+ (Character.toString(word.charAt(0))
											.toUpperCase() + word.toLowerCase()
											.substring(1)) + ".txt")));
			StringBuilder word_in_file = new StringBuilder();
			for (int i = 0; i < 100; i++) {
				word_in_file.append(word.toLowerCase() + "\n");
			}
			writefile.write(word_in_file.toString());
			writefile.close();
		} catch (FileNotFoundException e) {
			// Display : file not found.
			System.out.println("Error! : File not found.");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Display : file cannot read.
			System.out.println("Error! : File cannot read.");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			sem_flag.release();
		}
	}
}

class Semaphore {
	int maxThread;
	int runningThread = 0;

	public Semaphore(int maxThread) {
		this.maxThread = maxThread;
	}

	public synchronized void acquire() throws InterruptedException {
		if (runningThread == maxThread) {
			this.wait();
		}
		runningThread++;
	}

	public synchronized void release() {
		runningThread--;
		try {
			this.notify();
		} catch (IllegalMonitorStateException e) {
			System.out.println("Error! : Cannot monitor state.");
			e.printStackTrace();
		}
	}
}
