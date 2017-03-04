package Servers;

import Messages.BrokerRegistration;
import Messages.SignedMessage;
import Participants.Broker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Created by Cip on 28-Feb-17.
 */
public class BrokerClient {
    private static final int PORT = 2074;

    private ServerSocket socket;
    private Broker broker;

    public BrokerClient() throws IOException, NoSuchAlgorithmException {
        this.socket = new ServerSocket(PORT);
        this.broker = new Broker();
    }

    public void start() {
        while (true) {
            try (
                    Socket clientSocket = socket.accept();
                    ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream())) {

                try {
                    SignedMessage registration = (SignedMessage) is.readObject();
                    BrokerRegistration brokerRegistration = BrokerRegistration.deserializeRegistration(registration.getMessage());
                    String userIP = clientSocket.getInetAddress().toString();
                    SignedMessage certificate = broker.createPaywordCertificate(brokerRegistration, userIP);

                    os.writeObject(certificate);
                } catch (Exception e) {
                    SignedMessage certificate = new SignedMessage(true, e.getMessage(), null);
                    os.writeObject(certificate);
                    throw e;
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }


    public static void main(String[] args) {
        try {
            BrokerClient client = new BrokerClient();
            client.start();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

}
