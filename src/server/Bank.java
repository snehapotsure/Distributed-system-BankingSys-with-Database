package server;

import exceptions.*;
import interfaces.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.sql.*;
import java.sql.Statement.*;

//Bank Class which implements all the methods defined in the BankInterface
public class Bank extends UnicastRemoteObject implements BankInterface {

    private List<Account> accounts; // users accounts
    private List<Session> sessions, deadSessions;
	static Connection con;
    public Bank() throws RemoteException
    {
        super();
        //set up ArrayLists and create test accounts
        accounts = new ArrayList<>();
        sessions = new ArrayList<>();
        deadSessions = new ArrayList<>();

      
    }

    public static void main(String args[]) throws Exception {
        try {
            //Set up securitymanager for server, and specify path to the policy file
			//Class.forName("com.mysql.jdbc.Driver");
			Class.forName("com.mysql.cj.jdbc.Driver");
			con=DriverManager.getConnection("jdbc:mysql://localhost:3306/test?autoReconnect=true&useSSL=false", "sneha" , "");
			if(System.getSecurityManager()== null)
				System.setSecurityManager(new SecurityManager());
			//Class.forName("com.mysql.cj.jdbc.Driver");
			//con=DriverManager.getConnection("jdbc:mysql://localhost:3306/test?autoReconnect=true&useSSL=false", "sneha" , "");
            
            System.out.println("\n--------------------\nSecurity Manager Set");
			
			
			
            //Add bank to the RMI registry so it can be located by the client
            String name = "Bank";
            BankInterface bank = new Bank();
			//BankInterface stub = (BankInterface)UnicastRemoteObject.exportObject(bank,0);
            Registry registry = LocateRegistry.getRegistry(Integer.parseInt(args[0]));
            registry.rebind(name, bank);
            System.out.println("Bank Server Bound");
            System.out.println("Server Stared\n--------------------\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public long login(String username, String password) throws RemoteException, InvalidLoginException {
        
            }
        }*/
		try
		{
			String usrnm,passwd;
			int acc=-1;
			System.out.println("connection established");
			
            
			
			java.sql.Statement smt=con.createStatement();
			ResultSet rs = smt.executeQuery("select * from user");
							
			while(rs.next())
			{
				usrnm = rs.getString("username");
				passwd = rs.getString("password");
				
				System.out.println(usrnm);
				
				if(usrnm.equals(username) && passwd.equals(password))
				{
					System.out.println("valid user");
					acc = rs.getInt("accountno");
					
					String str = "select amount from account where accountno=";
					str = str + acc;
					ResultSet rs1 = smt.executeQuery(str);
					double balance = 0.0;
					if(rs1.first())
						balance = rs1.getDouble("amount");
					
					Account validaccount=null;
					boolean accountfound = false;
					for(Account ac: accounts)
					{
						if(ac.getAccountNumber() == acc)
						{
							accountfound = true;
							validaccount = ac;
						}
					}
					if(!accountfound)
					{
						Account acct = new Account(username,password);
						acct.setAccountNumber(acc);
						validaccount = acct;
						accounts.add(acct);
						System.out.println("account added to account list");
					}
					
					validaccount.setBalance(balance);
					Session s = new Session(validaccount);
					sessions.add(s);
					
					smt.close();
					return s.sessionId;
				}
				
			}
			 throw new InvalidLoginException();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			 throw new InvalidLoginException();
		}
        //Throw exception if login details are not valid
       
    }

    @Override
    public double deposit(double amount, long sessionID) throws RemoteException, InvalidSessionException {
        //Check if user session is active, based on sessionID passed by client
        if(checkSessionActive(sessionID)) 
		{
           
			int accountn=-1; 
			double amountn=0;
			for(Session s: sessions)
			{
				if(s.sessionId == sessionID)
				{
					accountn = s.getAccount().getAccountNumber();
					break;
				}
			}
			
			if(accountn != -1)
			{
				try
				{
					java.sql.Statement smt=con.createStatement();
					ResultSet rs = smt.executeQuery("select amount from account where accountno = " + accountn);
					
					while(rs.next())
					{
						amountn = rs.getInt("amount");
						
					}
					
					String SQL_INSERT = "INSERT INTO transaction (accountno, datetim, transactiontype, transactionamt, amount) VALUES (?,?,?,?,?)";
						
					 
					PreparedStatement pstmt = con.prepareStatement(SQL_INSERT);
					 
					pstmt.setInt(1, accountn );
					java.util.Date date = new Date();
						
					java.sql.Timestamp timestamp = new java.sql.Timestamp(date.getTime());
					pstmt.setTimestamp(2, timestamp);
					pstmt.setString(3, "credit");
					pstmt.setDouble(4, amount);
					pstmt.setDouble(5, amountn+amount ); 
					
					int row = pstmt.executeUpdate();
					
					String SQL_UPDATE = "UPDATE account SET amount=? WHERE accountno=?";
					pstmt = con.prepareStatement(SQL_UPDATE);
					
					pstmt.setDouble(1, amount+amountn);
					pstmt.setInt(2, accountn );
					row = pstmt.executeUpdate();
					
					
					pstmt.close();
					
					
					Account acct = getAccount(accountn);
					acct.setBalance(amount + amountn);
					
					 //Create transaction object for this transaction and add to the account
					Transaction t = new Transaction(acct, "Deposit");
					t.setAmount(amount);
					acct.addTransaction(t);
					
					return amount + amountn;
					System.out.println("deposited rs"+amount);
						
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				//throw InvalidAccountException();
			}
			
			
        }
        return 0;
    }

    @Override
    public double withdraw( double amount, long sessionID) throws RemoteException,
            InsufficientFundsException, InvalidSessionException {
        //Check if user session is active, based on sessionID passed by client
        if(checkSessionActive(sessionID)) {
            
			
			int accountn=-1; 
			double amountn=0;
			for(Session s: sessions)
			{
				if(s.sessionId == sessionID)
				{
					accountn = s.getAccount().getAccountNumber();
					break;
				}
			}
			try{
			if(accountn != -1)
			{
				try
				{
					java.sql.Statement smt=con.createStatement();
					ResultSet rs = smt.executeQuery("select amount from account where accountno = " + accountn);
					
					if(rs.first())
					{
						amountn = rs.getDouble("amount");
						System.out.println(amountn);
					}
					
					if(Double.compare(amountn, amount) > 0)
					{
						System.out.println("can withdraw money");
						String SQL_INSERT = "INSERT INTO transaction (accountno, datetim, transactiontype, transactionamt, amount) VALUES (?,?,?,?,?)";
							
						 
						PreparedStatement pstmt = con.prepareStatement(SQL_INSERT);
						 
						pstmt.setInt(1, accountn );
						java.util.Date date = new Date();
						java.sql.Timestamp timestamp = new java.sql.Timestamp(date.getTime());
						pstmt.setTimestamp(2, timestamp);
						pstmt.setString(3, "debit");
						pstmt.setDouble(4, amount);
						pstmt.setDouble(5, amountn-amount ); 
						
						int row = pstmt.executeUpdate();
						
						String SQL_UPDATE = "UPDATE account SET amount=? WHERE accountno=?";
						pstmt = con.prepareStatement(SQL_UPDATE);
						
						pstmt.setInt(2, accountn );
						pstmt.setDouble(1, amountn-amount);
						
						row = pstmt.executeUpdate();
						
						
						pstmt.close();
						
						
					
					}
					else
					{
						//Throw exception if account doesn't have enough money to withdraw
						throw new InsufficientFundsException();
					}
						
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				//throw InvalidAccountException;
			}
			}
			catch(Exception iae)
			{
				iae.printStackTrace();
			}
			
			try{
				Account acct = getAccount(accountn);
				acct.setBalance(amountn - amount);
				
				//create new Transaction and add to account
				Transaction t = new Transaction(acct, "Withdrawal");
				t.setAmount(amount);
				acct.addTransaction(t);
			}
			catch(InvalidAccountException iae)
			{
				iae.printStackTrace();
			}		
			return  amountn - amount;
			
        }
       return 0; 
    }

    @Override
    public Account inquiry(long sessionID) throws RemoteException, InvalidSessionException {
        //Check if session is active based on sessionID that is passed in
        if(checkSessionActive(sessionID)) {
            try {
				
			int accountn=-1; 
			double amountn=0;
			for(Session s: sessions)
			{
				if(s.sessionId == sessionID)
				{
					accountn = s.getAccount().getAccountNumber();
					break;
				}
			}
			
                //Get account and return to the client
                Account account = getAccount(accountn);
                System.out.println(">> Balance requested for account " + accountn + "\n");
                return account;
            } catch (InvalidAccountException e) {
                e.printStackTrace();
            }
	
        }
        return null;
    }

    @Override
    public Statement getStatement(Date from, Date to, long sessionID) throws RemoteException,
           InvalidSessionException, StatementException {
        //Check if the session is active based on sessionID from client
        if(checkSessionActive(sessionID)) {
            try {
				int accountn=-1; 
				double amountn=0;
				for(Session s: sessions)
				{
					if(s.sessionId == sessionID)
					{
						accountn = s.getAccount().getAccountNumber();
						break;
					}
				}
                //Get correct user account
                Account account = getAccount(accountn);
				account.clearTransactions();

				try{
					
				
				java.sql.Statement smt=con.createStatement();
				ResultSet rs = smt.executeQuery("select * from transaction where accountno = " + accountn);
				
				
				while(rs.next())
				{
					int databaseacc = rs.getInt("accountno");
					
					if(databaseacc == accountn)
					{
						java.sql.Timestamp datats = rs.getTimestamp("datetim");
						String datatrtype = rs.getString("transactiontype");
						Double datatramt = rs.getDouble("transactionamt");
						Double dataamt = rs.getDouble("amount");
						
						Date date=new Date(datats.getTime());  
						
						Transaction t = new Transaction(account,datatrtype);
						t.setDate(date);
						t.setAmount(datatramt);
						t.setBalance(dataamt);
						
						account.addTransaction(t);
					}
					
					
				}
				}
				catch(SQLException e)
				{
					e.printStackTrace();
				}

                System.out.println(">> Statement requested for account " + accountn +
                        " between " + from.toString() + " " + to.toString() + "\n");
                //Create new statement using the account and the dates passed from the client
                Statement s = new Statement(account, from, to);
                return s;
            } catch (InvalidAccountException e) {
                e.printStackTrace();
            }
        }
        //throw exception if statement cannot be generated
        throw new StatementException("Could not generate statement for given account and dates");
    }

    @Override
    public Account accountDetails(long sessionID) throws InvalidSessionException {
        //Get account details based on the session ID
        //Each session has an associated account, so the accounts can be retrieved based on a session
        //Used on the client for looking up accounts
        for(Session s:sessions){
            if(s.getClientId() == sessionID){
                return s.getAccount();
				//return null;
				
            }
        }
        //Throw exception if session isn't valid
        throw new InvalidSessionException();
    }

    private Account getAccount(int acnum) throws InvalidAccountException{
        //Loop through the accounts to find one corresponding to account number passed from the client
        //and return it
        for(Account acc:accounts){
            if(acc.getAccountNumber() == acnum){
                return  acc;
            }
        }
        //Throw exception if account does not exist
        throw new InvalidAccountException(acnum);
    }

    private boolean checkSessionActive(long sessID) throws InvalidSessionException{
        //Loop through the sessions
        for(Session s : sessions){
            //Checks if the sessionID passed from client is in the sessions list and active
            if(s.getClientId() == sessID && s.isAlive()) {
                //Prints session details and returns true if session is alive
                System.out.println(">> Session " + s.getClientId() + " running for " + s.getTimeAlive() + "s");
                System.out.println(">> Time Remaining: " + (s.getMaxSessionLength() - s.getTimeAlive()) + "s");
                return true;
            }
            //If session is in list, but timed out, add it to deadSessions list
            //This flags timed out sessions for removeAll
            //They will be removed next time this method is called
            if(!s.isAlive()) {
                System.out.println("\n>> Cleaning up timed out sessions");
                System.out.println(">> SessionID: " + s.getClientId());
                deadSessions.add(s);
            }
        }
        System.out.println();

        // cleanup dead sessions by removing them from sessions list
        sessions.removeAll(deadSessions);

        //throw exception if sessions passed to client is not valid
        throw new InvalidSessionException();
    }
}
