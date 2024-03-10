package project.peer;

import project.message.BaseMessage;
import project.message.MessageHandler;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Network {
    private static SSLServerSocket server_socket;
    private static SSLSocketFactory socket_factory;

    static void initiateServerSockets(int port) throws IOException {
        server_socket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(port);
        server_socket.setEnabledCipherSuites(server_socket.getSupportedCipherSuites());
        socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    public static void run() {
        while(true){
            try{
                SSLSocket socket = (SSLSocket) server_socket.accept();

                Peer.thread_executor.execute(() -> receiveRequest(socket));

            } catch (IOException ioException) {
                System.out.println("Failed to accept request");
            }
        }
    }

    public static void receiveRequest(SSLSocket socket) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            BaseMessage request = (BaseMessage) objectInputStream.readObject();

            BaseMessage response = MessageHandler.handleMessage(request);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(response);

            socket.close();
        } catch (IOException | ClassNotFoundException e) {
        }
    }

    public static BaseMessage makeRequest(BaseMessage request, String address, Integer port) throws IOException, ClassNotFoundException {
        SSLSocket socket = (SSLSocket) socket_factory.createSocket(address, port);
        socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(request);

        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        BaseMessage raw_response = (BaseMessage) objectInputStream.readObject();

        socket.close();

        return MessageHandler.handleMessage(raw_response);
    }
}
