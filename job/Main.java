import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.hsqldb.Server;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Semaphore sem_flag = new Semaphore(32);
		Thread tc;
		try {
			
			// HSQLDB connect
			Server hsqlServer = null;
			Connection connection = null;
			ResultSet dataset = null;
			hsqlServer = new Server();
			hsqlServer.setLogWriter(null);
			hsqlServer.setSilent(true);
			hsqlServer.setDatabaseName(0, "skoodskill");
			hsqlServer.setDatabasePath(0, "file:skoodeskilldb");
			hsqlServer.start();
			// Create connection
			Class.forName("org.hsqldb.jdbcDriver");
			connection = DriverManager.getConnection("jdbc:hsqldb://localhost/skoodeskill", "sa", "");
			connection.prepareStatement("drop table dic if exists;").execute();
			connection.prepareStatement("create table dic(word varchar(20) not null);").execute();

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
				String insertsql = "insert into dic (word)" + "values ('"+word+"');";
				connection.prepareStatement(insertsql).execute();
				tc = new Thread(new WriteThread(sem_flag, word));
				tc.start();
				tc.join();
			}
			
			System.out.println("Success! : Write file succussful.");
			for (File file : new File("./").listFiles()) {
				sem_flag.acquire();
				tc = new Thread(new ZipFileFolk(sem_flag, file));
				tc.start();
				tc.join();
			}
			
			System.out.println("Success! : Zip and Compare folder succussful.");
			
			//Select all once (assume DB query more then cost)
			dataset = connection.prepareStatement("select word from dic;").executeQuery();
			connection.prepareStatement("update dic SET word=UPPER(LEFT(word,1))+SUBSTRING(word,2,LENGTH(word));").execute();
			hsqlServer.stop();
			hsqlServer = null;
			LinkedList<String> all_word = new LinkedList<String>();
			while(dataset.next()){
				all_word.add(dataset.getString(1));
			}
			int[] val = new int[3];
			for(String w : all_word){
				val[0] = (w.length()>5)?val[0]+1:val[0];
				Matcher	m = Pattern.compile("(.+)\\1+").matcher(w);
			       while (m.find()) {
			           val[1]++;
			           break;
			       }
			    if(w.charAt(0)==w.charAt(w.length()-1)){
			    	val[2]++;
			    }
			    
			}
			System.out.println("More than 5 word count : "+val[0]);
			System.out.println("Word collision more than 2 char count : "+val[1]);
			System.out.println("Word first letter same the last letter count : "+val[2]);
			while(!toPDF(all_word));
			System.out.println("PDF has been export (report.pdf).");
			
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

		} catch (ClassNotFoundException e) {
			System.out.println("Error! : Database connection error.");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			System.out.println("Error! : Database query error.");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
 public static boolean toPDF(LinkedList<String> data){
	  PDDocument doc = null;
      PDPage page = null;

     try{
         doc = new PDDocument();
         page = new PDPage();
         page.setMediaBox(PDPage.PAGE_SIZE_A4); 
         doc.addPage(page);
         PDFont font = PDType1Font.HELVETICA_BOLD;
         PDPageContentStream content = new PDPageContentStream(doc, page);
         int fit = 800;
         for(String s : data){
          if(fit < 50){
                 page = new PDPage();
                 page.setMediaBox(PDPage.PAGE_SIZE_A4);
                 content = new PDPageContentStream(doc, page);
                 doc.addPage(page);
          	   fit=800;
         }
         content.beginText();
         content.setFont( font, 10 );
         content.appendRawCommands(font + " TL\n");
         content.moveTextPositionByAmount( 10, fit );
         content.drawString(Character.toUpperCase(s.charAt(0)) + s.substring(1));
         content.appendRawCommands("T*\n");
         content.endText();
         content.close();
         fit-=15;
         }
        doc.save("report.pdf");
        doc.close();
  } catch (Exception e){
      System.out.println("Error! : Cannot export to pdf.");
      return false;
  }
	return true;
 }
}

class ZipFileFolk implements Runnable {
	Semaphore sem_flag;
	File file;

	public ZipFileFolk(Semaphore sem_flag, File file) {
		this.sem_flag = sem_flag;
		this.file = file;
	}

	@Override
	public void run() {
		try {
			if (file.isDirectory() && !file.toString().contains("/.") && !file.toString().endsWith(".tmp")) {
				ZipFile zipFile;
				zipFile = new ZipFile(file.toString() + ".zip");
				ZipParameters params = new ZipParameters();
				params.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
				params.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
				zipFile.addFolder(file.toString(), params);
				File new_zip_file = new File(file.toString() + ".zip");
				long foldersize = (long) (findSize(file.toString()) / 1024.0);
				long zip_size = (long) (new_zip_file.length() / 1024.0);
				long per = (long) ((zip_size * 100.0) / foldersize);
				System.out.println("Folder name : " + file.toString()
						+ "\tSIZE : " + foldersize + " KB\tZIP size: "
						+ zip_size + " KB\t%" + per);
			}
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			sem_flag.release();
		}
	}

	public static long findSize(String path) {
		long totalSize = 0;
		LinkedList<String> directory = new LinkedList<String>();
		File file = new File(path);
		if (file.isDirectory()) {
			directory.add(file.getAbsolutePath());
			while (directory.size() > 0) {
				String folderPath = directory.get(0);
				directory.remove(0);
				File folder = new File(folderPath);
				File[] filesInFolder = folder.listFiles();
				int noOfFiles = filesInFolder.length;
				for (int i = 0; i < noOfFiles; i++) {
					File f = filesInFolder[i];
					if (f.isDirectory()) {
						directory.add(f.getAbsolutePath());
					} else {
						totalSize += f.length();
					}
				}
			}
		} else {
			totalSize = file.length();
		}
		return totalSize;
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
