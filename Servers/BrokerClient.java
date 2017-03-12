package Servers;

import Messages.BrokerPayment;
import Messages.BrokerRegistration;
import Messages.SignedMessage;
import Participants.Broker;
import Requests.Command;

import java.io.*;
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
                    Command command = (Command) is.readObject();
                    executeCommand(command, os, is, clientSocket);

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

    private void executeCommand(Command command, ObjectOutputStream os, ObjectInputStream is, Socket clientSocket)
            throws IllegalArgumentException, ClassNotFoundException, GeneralSecurityException, SQLException, IOException {
        switch (command.getType()) {
            case "registration":
                executeRegistration(os, is, clientSocket);
                break;

            case "transfer":
                executeTransfer(os, is);
                break;

            default:
                throw new IllegalArgumentException("Command unknown: " + command.getType());
        }
    }

    private void executeRegistration(ObjectOutputStream os, ObjectInputStream is, Socket clientSocket)
            throws IOException, ClassNotFoundException, GeneralSecurityException, SQLException {
        SignedMessage registration = (SignedMessage) is.readObject();
        BrokerRegistration brokerRegistration = BrokerRegistration.deserializeRegistration(registration.getMessage());
        String userIP = clientSocket.getInetAddress().toString();
        SignedMessage certificate = broker.createPaywordCertificate(brokerRegistration, userIP);

        os.writeObject(certificate);
    }

    private void executeTransfer(ObjectOutputStream os, ObjectInputStream is) throws IOException, ClassNotFoundException, SQLException {
        SignedMessage serializedPayment = (SignedMessage) is.readObject();
        BrokerPayment payment = BrokerPayment.createBrokerPayment(serializedPayment.getMessage());
        SignedMessage transferResponse = broker.transfer(payment);

        os.writeObject(transferResponse);
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
