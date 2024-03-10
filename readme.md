# sdis1920-t1g07

## Java SE
The Java SE 11.0.6 was the version used on the development of this project.
The source code ('project' module) is located inside 'src' folder.

## Report
The report can be found on 'documents' folder.

## Filesystem Structure

In order to allow the test of several peers on a single computer, each peer uses its own filesystem subtree to keep the chunks it is backing up, the files it has recovered, its own metadata and files. The name of that subtree is the format [peer id]_directory.
Inside this directory there are three folders, one for the own files, named “files”, other of the restored files, name “restored” .
There is a third folder for the stored chunks, which are the other peer chunks, named “stored”.
When a chunk is stored, a folder with its file's id is created, and the chunks are inside that folder with name corresponding to each chunk's number.

## Compile
Compiling can be done running the 'compile.sh' script in the src folder (note this script can be found in the scripts folder):
> bash ../scripts/compile.sh

## Run
### RMI Registry
First, the 'registry.sh' script must be run in order to create a registry in port 1099:
> bash ../scripts/registry.sh

### Peer
You can run a peer using the following command:
> java project.peer.Peer <peer_id> <service_access_point> <port> [<neighbour_address> <neighbour_port>]

If only the first three parameters are given, the peer starts a new network, being the first one in it.
If the two last optional parameters are added, the peer will try to connect to the peer which address and port were provided in order to enter its network.

### TestApp
You can run the TestApp using the following script:
> bash ../scripts/test.sh <peer_access_point> BACKUP|RESTORE|DELETE|RECLAIM|STATE [<opnd_1> [<optnd_2]]

### Cleanup
To cleanup the peer directories created, simply run the following script:
> bash ../scripts/cleanup.sh


## Authors
João Paulo Monteiro Leite -- 201705312@fe.up.pt

Márcia Isabel Reis Teixeira -- 201706065@fe.up.pt

Maria Helena Ferreira -- up201704508@fe.up.pt

Sofia de Araújo Lajes -- up201704066@fe.up.pt
