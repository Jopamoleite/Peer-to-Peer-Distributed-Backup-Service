package project.peer;

import project.InvalidFileException;
import project.message.InvalidMessageException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote{
    int backup(String file_path, int replication_degree) throws RemoteException, InvalidMessageException, InvalidFileException;
    int restore(String file_path) throws RemoteException, InvalidFileException;
    int delete(String file_path) throws RemoteException, InvalidFileException;
    int reclaim(int  max_disk_space) throws RemoteException;
    String state() throws RemoteException;


}