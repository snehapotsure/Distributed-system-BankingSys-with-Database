package interfaces;

import exceptions.*;
import server.Account;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

public interface BankInterface extends Remote {
    long login(String username, String password) throws RemoteException, InvalidLoginException;
    double deposit(double amount, long sessionID) throws RemoteException, InvalidSessionException;
    double withdraw(double amount, long sessionID) throws RemoteException, InvalidSessionException, InsufficientFundsException;
    Account inquiry(long sessionID) throws RemoteException, InvalidSessionException;
    StatementInterface getStatement(Date from, Date to, long sessionID) throws RemoteException, InvalidSessionException, StatementException;
    Account accountDetails(long sessionID) throws RemoteException, InvalidSessionException;
}
