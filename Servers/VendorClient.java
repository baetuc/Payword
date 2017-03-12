package Servers;

import Messages.Payment;
import Messages.SignedMessage;
import Participants.Vendor;
import Requests.Command;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.List;

/**
 * Created by Cip on 01-Mar-17.
 */
public class VendorClient {
    private static final String BROKER_IP = "127.0.0.1";
    private static final int BROKER_PORT = 2074;

    private static final int PORT = 2075;

    private ServerSocket socket;
    private Vendor vendor;


    public VendorClient() throws IOException, NoSuchAlgorithmException {
        this.socket = new ServerSocket(PORT);
        this.vendor = new Vendor();

        System.out.println("Started Vendor process.");
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = socket.accept();
                new Thread(new ServerThread(vendor, clientSocket)).start();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }


    /**
     * Class that deals with a single client in a concurrent mode.
     */
    private static final class ServerThread implements Runnable {
        private Vendor vendor;
        private Socket clientSocket;

        public ServerThread(Vendor vendor, Socket socket) {
            this.vendor = vendor;
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {

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
                        System.out.println(e.getMessage());
                    }
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
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
            System.out.println("Executed list command.");
        }

        private void executeBuy(String itemIndex, String userIP, Socket socket, ObjectOutputStream os, ObjectInputStream is)
                throws IOException, ClassNotFoundException {

            SignedMessage signedPayment = (SignedMessage) is.readObject();
            try {
                int index = Integer.parseInt(itemIndex);
                Payment payment = Payment.createPayment(signedPayment.getMessage());
                long left = vendor.validateBuy(index, payment, userIP);

                SignedMessage response = new SignedMessage(false, "Left with " + left + "$.", null);
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
    }

    /**
     * Thread that deals with the transfer commands given at the command line.
     */
    private static final class TransferThread implements Runnable {
        private Vendor vendor;

        public TransferThread(Vendor vendor) {
            this.vendor = vendor;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    if (!br.readLine().trim().toLowerCase().equals("transfer")) {
                        System.out.println("Only transfer command is valid!");
                        continue;
                    }

                    try (Socket socket = new Socket(BROKER_IP, BROKER_PORT);
                         ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
                         ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream())) {

                        os.writeObject(new Command("transfer", null));
                        List<SignedMessage> payments = vendor.createBrokerPayments();
                        os.writeInt(payments.size());
                        for (SignedMessage payment : payments) {
                            os.writeObject(payment);
                            SignedMessage response = (SignedMessage) is.readObject();
                            System.out.println(response.getMessage());
                        }

                        if (payments.size() == 0) {
                            System.out.println("No new payments performed.");
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            VendorClient client = new VendorClient();
            new Thread(new TransferThread(client.vendor)).start();
            client.start();
        } catch (NoSuchAlgorithmException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
