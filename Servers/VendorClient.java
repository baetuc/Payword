package Servers;

import Messages.Payment;
import Messages.SignedMessage;
import Participants.Vendor;
import Requests.Command;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

/**
 * Created by Cip on 01-Mar-17.
 */
public class VendorClient {
    private static final int PORT = 2075;

    private ServerSocket socket;
    private Vendor vendor;

    public VendorClient() throws IOException, NoSuchAlgorithmException {
        this.socket = new ServerSocket(PORT);
        this.vendor = new Vendor();
    }

    public void start() {
        while (true) {
            try {

                Socket clientSocket = socket.accept();
                ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());

                boolean exit = false;
                while (!exit) {
                    try {
                        String userIP = clientSocket.getInetAddress().toString();
                        Command command = (Command) is.readObject();

                        if (command.getType().equals("exit")) {
                            exit = true;
                            continue;
                        }

                        executeCommand(command, userIP, clientSocket, os, is);
                    } catch (Exception e) {
                        SignedMessage response = new SignedMessage(true, e.getMessage(), null);
                        os.writeObject(response);
                        os.flush();
                        throw e;
                    }
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void executeCommand(Command command, String userIP, Socket socket, ObjectOutputStream os, ObjectInputStream is)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        switch (command.getType()) {
            case "commitment":
                addCommitment(os, is);
                break;

            case "ls":
                executeList(os);
                break;

            case "buy":
                executeBuy(command.getParameter(), userIP, socket, os, is);
                break;

            default:
                throw new IllegalArgumentException("Invalid command: " + command.getType());
        }
    }

    private void addCommitment(ObjectOutputStream os, ObjectInputStream is)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        SignedMessage commitment = (SignedMessage) is.readObject();
        vendor.addCommitment(commitment);

        SignedMessage response = new SignedMessage(false, null, null);
        os.writeObject(response);
    }

    private void executeList(ObjectOutputStream os) throws IOException {
        os.writeObject(vendor.listCommand());
    }

    private void executeBuy(String itemIndex, String userIP, Socket socket, ObjectOutputStream os, ObjectInputStream is)
            throws IOException, ClassNotFoundException {

        SignedMessage signedPayment = (SignedMessage) is.readObject();
        try {
            int index = Integer.parseInt(itemIndex);
            Payment payment = Payment.createPayment(signedPayment.getMessage());
            vendor.validateBuy(index, payment, userIP);

            SignedMessage response = new SignedMessage(false, null, null);
            os.writeObject(response);

            streamSong(index, socket);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Item index is an invalid number: " + itemIndex);
        }

    }

    public void streamSong(int itemIndex, Socket socket)
            throws IOException {
        String filename = vendor.getItemPath(itemIndex);
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        os.writeLong(new File(filename).length());

        try (InputStream is = new FileInputStream(filename)) {
            byte buffer[] = new byte[4096];
            int count;
            while ((count = is.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            os.flush();
        }
    }

    public static void main(String[] args) {
        try {
            VendorClient client = new VendorClient();
            client.start();
        } catch (NoSuchAlgorithmException | IOException e) {
            System.out.println(e.getMessage());
        }
    }


}
