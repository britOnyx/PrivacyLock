import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.SQLException;
import com.simonmittag.*;


import javax.crypto.*;

/*
 * This will create a folder which will be locked and hidden from the windows GUI
 */

public class PrivacyLock {

	private static Scanner scan;
	private static String strMasterPassword;
	private static boolean pswdAttempt;
	private static Singleton singleton;
	private static String dir;
	private static int holder;
	
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		
		try {
			
			//create singleton instance
			singleton = Singleton.getInstance();
			//this is used to flag whether correct password has been entered
			pswdAttempt = false;
			//gets password from class
			strMasterPassword = singleton.getPassword();
			dir = singleton.getDirectory();
			scan = new Scanner(System.in);
			
			System.out.println("Privacy Lock:");			
			
			//if master password hasnt been set
			if(strMasterPassword.equals(""))
			{
				//allow the user to set a password
				System.out.println("Enter Master Password:");
				String strPassword = scan.nextLine();
				
				//encrypt password
				String strSaltValue = singleton.setSaltvalue(50);
				String strEncryptedPass = singleton.generateSecurePassword(strPassword, strSaltValue);
				
				//save the encrypted data to 
				singleton.setPassword(strEncryptedPass);
				singleton.setSaltValueToDB(strSaltValue);
						
				CLS.main("CLS"); //this clears the CMD terminal (only when running JAR file in CMD)
			}
			
			//while the correct password hasn't been entered, continue loop
			while(!pswdAttempt)
			{
				
				//allow the user to enter the password
				System.out.println("Enter Password:");
				strMasterPassword = scan.nextLine();
				
				//this is used to decrypt the entered password using the stored encrypted password and salts value
				Boolean status = singleton.verifyUserPassword(strMasterPassword,singleton.getPassword(),singleton.getSaltValueFromDB());  
				
				//if the master password matches the password entered by the user, exit the loop
				if(status == true) { pswdAttempt = true; }
				else{
					//display feedback
					System.out.println("Password Incorrect!"); 
				}
			}
						
			//Get directory from DB and store in dir
			dir = singleton.getDirectory();
			File parentFolder = new File(dir); //parent folder where the encrypted folder will be placed
			scan.reset();
			
			//checks whether the parent folder exists so the encrypted folder can be placed
			//while the parent folder doesn't exist 
			while(!parentFolder.exists())
			{
				System.out.println("Folder Doesn't Exist");
				System.out.println("Enter Directory To Place Folder:");
				dir = scan.nextLine();
				
				//use new directory
				//keeps looping until valid parent directory has been entered
				parentFolder = new File(dir);
				parentFolder.mkdir();
				System.out.println("dir: " + dir);
				singleton.setDirectory(dir);
			}
						
			//go to menu
			menu(parentFolder);
			
		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	
	}
	
		
	//display functionality of the app
	private static void menu(File targetedFolder) throws SQLException {
		String attr = ""; //folder permissions variable
				
		System.out.println("---------------------------");
		System.out.println("Privacy Lock:");
		
		// if folder is NOT hidden
		if(!targetedFolder.isHidden())
		{
			//allow the user to hide the folder
			System.out.println("1.Hide Folder");
			attr = "attrib +H ";

		}else
		{
			//if the folder is hidden
			//allow the user to unhide the folder
			System.out.println("1.Unhide Folder");
			attr = "attrib -H ";
		}
		
		System.out.println("2.Get Folder Location");
		System.out.println("0.Exit"); //exit app
		
		holder = scan.nextInt(); //hold number the user entered from the menu
		
		switch(holder)
		{
		case 1: folderAttributeHide(targetedFolder, attr); //if 1 has been entered hide/show folder
			break;
		case 2: 
			System.out.println("---------------------------");
			System.out.println(singleton.getDirectory());
			break;
		case 0: System.exit(0); //exit application

		default:
			System.out.println("Must enter a number "); //if the number entered by the user is outside of the case, 
						break;
		}
		
		menu(targetedFolder);
	}
	
	private static void folderAttributeHide(File folder, String attr) 
	{
	    // execute attrib command to set hide attribute
	    try {
			Process p = Runtime.getRuntime().exec(attr + folder.getPath());
			p.waitFor();
			
			File listFiles[] = folder.listFiles(); //get the list of files in the directory
	         			
			HideFiles(listFiles, attr);
			
			
			//checks to see if folder is hidden
			if(folder.isHidden()) {
				//display output
		        System.out.println(folder.getName() + ": Folder hidden");
		        
		        //check if there are files and they're not hidden
		        if(listFiles.length > 0 && !listFiles[0].isHidden())
		        {
		        	HideFiles(listFiles, attr); // show files
		        	System.out.println("Files Hidden");
		        }

		        
		      }else {
		        System.out.println(folder.getName() + ": Folder Visible");
		        
		        //check if there are files and they're also hidden
		        if(listFiles.length > 0 && listFiles[0].isHidden())
		        {
		        	HideFiles(listFiles, attr); //hide files
		        	//LockFiles(listFiles);
		        }
		        
		      }
			
		} catch (IOException 	| InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

//	private static void LockFiles(File[] listFiles) {
//		// TODO Auto-generated method stub
//		for(File file : listFiles) {
//			
//		}
//	}

	private static void HideFiles(File[] listFiles, String attr) throws IOException, InterruptedException {
		
		System.out.println("---------------------------");
		
		if(attr.contains("-H")){
			System.out.println("UNHIDING FOLDER..."); 
		}
		else if(attr.contains("+H")){
			System.out.println("HIDING FOLDER..."); 
		}
		
		//go through each file
		for(File file : listFiles) {
        	 
			//add attribute to file to make it hidden
 			Process p_files = Runtime.getRuntime().exec(attr + file.getPath());
 			p_files.waitFor(); //wait for the process to execute
			
 			//display file name
            System.out.println("File name: "+file.getName());
            System.out.println(" ");
 			
          }
		
		System.out.println("--------------------------------");
	}
	
	public static class CLS {
	    public static void main(String... arg) throws IOException, InterruptedException {
	        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
	    }
	}
	
}
