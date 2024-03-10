package project;

import project.message.InvalidMessageException;
import project.peer.RemoteInterface;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

    static void usage(){
        System.err.println("usage: <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
    }

    public static void main(String[] args) {
        //minimum args is 2 to retrieve the internal state of the peer
        if (args.length < 2) {
            usage();
            System.exit(1);
        }

        String peer_ap = args[0];
        String sub_protocol = args[1];
        //opnd_1 is either a file_path or a maximum_disk_space
        String  opnd_1 = null;
        //opnd_2 is a replication_degree always!
        int opnd_2 = 0;
        if(args.length > 2)
            opnd_1 = args[2];
        if(args.length > 3)
            opnd_2 = Integer.parseInt(args[3]);

        try {
            Registry reg = LocateRegistry.getRegistry();
            RemoteInterface peer = (RemoteInterface) reg.lookup(peer_ap);

            switch (sub_protocol) {
                case "BACKUP":
                    if (args.length != 4) {
                        System.err.println("Expected 4 arguments, given" + args.length);
                        System.err.println("Usage: <peer_ap> BACKUP <file_path> <file_replication_degree>");
                        System.exit(-2);
                    }
                    if(opnd_2 <= 0) {
                        System.err.println("Usage: <peer_ap> BACKUP <file_path> <file_replication_degree>");
                        System.err.println("Expected file_replication_degree to be greater than 0 but is " + opnd_2);
                        System.exit(-2);
                    }
                    //triggers the backup of file in opnd_1 with a replication degree of opnd_2
                    peer.backup(opnd_1, opnd_2);
                    break;
                case "RESTORE":
                    if (args.length != 3) {
                        System.err.println("Expected 3 arguments, given" + args.length);
                        System.err.println("Usage: <peer_ap> RESTORE <file_path>");
                        System.exit(-3);
                    }
                    //triggers the restoration of the previously replicated file in opnd_1
                    peer.restore(opnd_1);
                    break;
                case "DELETE":
                    if (args.length != 3) {
                        System.err.println("Expected 3 arguments, given" + args.length);
                        System.err.println("Usage: java project.TestApp <peer_ap> DELETE <file_path>");
                        System.exit(-4);
                    }
                    //delete that file
                    peer.delete(opnd_1);
                    break;
                case "RECLAIM":
                    if (args.length != 3) {
                        System.err.println("Expected 3 arguments, given" + args.length);
                        System.err.println("Usage: java project.TestApp <peer_ap> RECLAIM <maximum_disk_space_in_KBytes>");
                        System.exit(-5);
                    }
                    // reclaim all the disk space being used by the service,
                    peer.reclaim(Integer.parseInt(opnd_1));
                    break;
                case "STATE":
                    if (args.length != 2) {
                        System.err.println("Expected 2 arguments, given" + args.length);
                        System.err.println("Usage: java project.TestApp <peer_ap> STATE");
                        System.exit(-6);
                    }
                    //retrieve the internal state of the peer
                    System.out.println(peer.state());
                    break;
                default:
                    usage();
                    System.err.println("Expected sub_protocol to be BACKUP, RESTORE, DELETE, RECLAIM, STATE.");
                    System.err.println("But sub_protocol given "+ sub_protocol);
                    System.exit(-7);
            }
        } catch (Exception e) {
        }
    }
}
