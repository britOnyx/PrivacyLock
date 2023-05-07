import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.PBEKeySpec;

public class Singleton {

	 /* Declaration of variables */   
    // static variable single_instance of type Singleton
    private static Singleton single_instance = null;
	private String accountPass;
	private String foldDir;
	private int flag;
	private Connection connect;
	private DatabaseMetaData dbm;
	private Statement statement;
	private final Random random = new SecureRandom();  
    private final String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz{}!~#@[]-=+`¬¦";
    private String valSaltToString;
    
	private Singleton() throws SQLException, ClassNotFoundException
	{
		accountPass = "";
		flag =0;
		
		if(connect == null)
		{
			getConnect();
		}
	}
	
	public static Singleton getInstance() throws ClassNotFoundException, SQLException {
		if (single_instance == null) {
			single_instance = new Singleton();
		}
		return single_instance;
	}

	private void getConnect() throws SQLException, ClassNotFoundException {

		// connect to database file
		Class.forName("org.sqlite.JDBC");
		connect = DriverManager.getConnection("jdbc:sqlite:acc.db");
		initialise();	
	}
	
	private void initialise() throws SQLException, ClassNotFoundException {
		
		statement = connect.createStatement();
		dbm = connect.getMetaData();
		
		try {
			//get the table which stores the account
			ResultSet table = dbm.getTables(null, null, "accounts", null);
			
			//if the table doesn't exist
			if (!table.next()) {			

				//create the table which will store the data
				statement.execute("CREATE TABLE accounts(" + "accountID integer primary key, " // 
						+ "accountpass varchar(100) NOT NULL, accountSlt varchar(100) NOT NULL, folDir varchar(300) NOT NULL);");
				
			} else {
				
				//else check if data is stored and get password
				getPassword(); //gets encrypted Password
				getSaltValueFromDB(); //gets salt password
				getDirectory(); // get stored directory
				
			}
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//set the password from the database
	public void setPassword(String password) throws SQLException, ClassNotFoundException
	{
		String sqlStatement = null;
		accountPass = password;
		statement = connect.createStatement();
		dbm = connect.getMetaData();
		//else check if data is stored
		ResultSet res = statement.executeQuery("SELECT * FROM accounts");
				
		//if not data stored, enter data
		if(!res.next())
		{
			sqlStatement = "INSERT INTO accounts VALUES(1,'" + password + "','','');";
			
		}
		statement.execute(sqlStatement);
	}
	
	public String getPassword() throws SQLException, ClassNotFoundException
	{
		statement = connect.createStatement();
		dbm = connect.getMetaData();

		//else check if data is stored
		ResultSet res = statement.executeQuery("SELECT * FROM accounts WHERE accountID = 1;");
		
		if(res.next()) {
			accountPass = res.getString("accountpass");
		}
		return accountPass;
	}
	
	
	public void setDirectory(String dir) throws SQLException, ClassNotFoundException
	{
		String sqlStatement = null;
		foldDir = dir;
		statement = connect.createStatement();
		dbm = connect.getMetaData();
		
		//else check if data is stored
		ResultSet res = statement.executeQuery("SELECT * FROM accounts WHERE accountID = 1;");
				
		//if data stored, enter data
		if(res.next())
		{
			sqlStatement = "UPDATE accounts SET folDir = '" + foldDir + "' WHERE accountID = 1;";
		}
		//RUN THE SQL SCRIPT
		statement.execute(sqlStatement);
	}
	
	public String getDirectory() throws SQLException {
		
		statement = connect.createStatement();
		dbm = connect.getMetaData();

		//else check if data is stored
		ResultSet res = statement.executeQuery("SELECT * FROM accounts WHERE accountID = 1;");
		
		if(res.next()) {
			foldDir = res.getString("folDir");
		}
		return foldDir;
	}
	
	//set flag which states if the user has been created
	public void setFlag(int tempflag)
	{
		flag = tempflag;
	}
	
	public int getFlag()
	{
		return flag;
	}
	
	 /* Method to generate the salt value. */  
    public String setSaltvalue(int length) throws SQLException   
    {  
        StringBuilder finalval = new StringBuilder(length);  
  
        for (int i = 0; i < length; i++)   
        {  
        	//gets random characters from 'characters' variable and adds it to the finalval
            finalval.append(characters.charAt(random.nextInt(characters.length())));  
        }  
  
        valSaltToString = new String(finalval);  
               
        return valSaltToString;
    }
    
    
    public String getSaltValueFromDB() throws SQLException {
    	statement = connect.createStatement();
		dbm = connect.getMetaData();

		//else check if data is stored
		ResultSet res = statement.executeQuery("SELECT * FROM accounts WHERE accountID = 1;");
		
		if(res.next()) {
			valSaltToString = res.getString("accountSlt");
		}
		return valSaltToString;
    }
    
    public void setSaltValueToDB(String saltValue) throws SQLException {
    	statement = connect.createStatement();
		dbm = connect.getMetaData();
		String sqlStatement = null;
		//else check if data is stored
		ResultSet res = statement.executeQuery("SELECT * FROM accounts WHERE accountID = 1;");
		
		//if data stored, enter data
		if(res.next())
		{
			sqlStatement = "UPDATE accounts SET accountSlt = '" + saltValue + "' WHERE accountID = 1;";
		}
		
		//RUN THE SQL SCRIPT
		statement.execute(sqlStatement);
    }
	
    /* Method to encrypt the password using the original password and salt value. */  
    public String generateSecurePassword(String password, String salt)   
    {  
        String finalval = null;  
        //hashes each character of the password with an element of salt
        byte[] securePassword = hash(password.toCharArray(), salt.getBytes());  
        //the secure password is then encrypted with Base64
        finalval = Base64.getEncoder().encodeToString(securePassword);  
   
        return finalval;  
    }
    
    /* Method to generate the hash value */  
    public byte[] hash(char[] password, byte[] salt)   
    {  
    	//password, salt, iteration and key length
        PBEKeySpec spec = new PBEKeySpec(password, salt, 10000, 256);  
        
        Arrays.fill(password, Character.MIN_VALUE);  
        try   
        {  
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");  
            return skf.generateSecret(spec).getEncoded();  
        }   
        catch (NoSuchAlgorithmException | InvalidKeySpecException e)   
        {  
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);  
        }   
        finally   
        {  
            spec.clearPassword();  
        }  
    }
    
    /* Method to verify if both password matches or not */  
    public boolean verifyUserPassword(String providedPassword, String securedPassword, String salt)  
    {  
        boolean finalval = false;  
                
        /* Generate New secure password with the same salt form the DB*/  
        String newSecurePassword = generateSecurePassword(providedPassword, salt);  
          
        /* Check if two passwords are equal */  
        finalval = newSecurePassword.equals(securedPassword);  
          
        return finalval;  
    }
}
