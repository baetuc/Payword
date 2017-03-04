package Servers;

import Messages.SignedMessage;
import Participants.User;
import Requests.Command;
import Requests.Items;
import com.google.common.base.Splitter;
import javazoom.jl.decoder.JavaLayerException;
import org.apache.commons.lang3.Validate;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

/**
 * Created by Cip on 28-Feb-17.
 */
public class UserClient {
    private static final String BROKER_IP = "127.0.0.1";
    private static final int BROKER_PORT = 2074;

    private static final String VENDOR_IP = "127.0.0.1";
    private static final int VENDOR_PORT = 2075;

    private Socket brockerSocket;
    private Socket vendorSocket;
    private User user;
    private long fileSize;
    private boolean exit;
    private SignedMessage certificate;
    private Items items;

    public UserClient() throws IOException, NoSuchAlgorithmException, JavaLayerException {
        this.brockerSocket = new Socket(BROKER_IP, BROKER_PORT);
        this.vendorSocket = new Socket(VENDOR_IP, VENDOR_PORT);
        this.user = new User();
        this.exit = false;
        this.fileSize = 0;
    }

    public static void main(String[] args) {
        try {
            UserClient client = new UserClient();
            client.start();
        } catch (Exception e) {
            System.out.println(e.getClass() + ": " + e.getMessage());
        }
    }

    public void start() throws ClassNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        String command;
        initialize();

        ObjectInputStream is = new ObjectInputStream(vendorSocket.getInputStream());
        ObjectOutputStream os = new ObjectOutputStream(vendorSocket.getOutputStream());

        while (!exit) {
            try {

                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("Enter command: ");
                command = br.readLine().trim();

                executeCommand(command.toLowerCase(), os, is);

            } catch (IllegalArgumentException | IOException | ClassNotFoundException | InterruptedException | LineUnavailableException | UnsupportedAudioFileException e) {
                System.err.println(e.getMessage());
            } catch (JavaLayerException e) {
                e.printStackTrace();
            }
        }
    }

    private void executeCommand(String command, ObjectOutputStream os, ObjectInputStream is)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedAudioFileException, InterruptedException, LineUnavailableException, JavaLayerException {

        Iterable<String> parts = Splitter.on(' ').trimResults().omitEmptyStrings().split(command);
        String head = null;
        String[] content = new String[2];

        for (String part : parts) {
            if (head == null) {
                head = part;
            } else {
                if (content[0] == null) {
                    content[0] = part;
                } else {
                    if (content[1] == null) {
                        content[1] = part;
                    } else {
                        System.out.println("Command invalid.");
                    }
                }
            }
        }

        if (head == null) {
            return;
        }

        switch (head) {
            case "ls":
                executeList(os, is);
                break;

            case "buy":
                executeBuy(false, content[0], null, os, is);
                break;

            case "custom_buy":
                executeBuy(true, content[0], content[1], os, is);
                break;

            case "exit":
                executeExit(os);
                exit = true;
                break;

            default:
                System.out.println("Unknown command: " + head);
        }
    }

    private void executeList(ObjectOutputStream os, ObjectInputStream is) throws IOException, ClassNotFoundException {
        Command command = new Command("ls", null);

        os.writeObject(command);
        Items items = (Items) is.readObject();
        System.out.println(items);
        this.items = items;

        os.flush();
    }

    private void executeBuy(boolean customBuy, String itemIndex, String hashIndex, ObjectOutputStream os, ObjectInputStream is)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, ClassNotFoundException, UnsupportedAudioFileException, InterruptedException, LineUnavailableException, JavaLayerException {

        checkNewHashGeneration(os, is);

        int index = Integer.parseInt(itemIndex) - 1;

        if (items == null) {
            executeList(os, is);
        }

        Validate.isTrue(0 <= index && index < items.getContent().size(), "Desired index of product is invalid: %d", index);
        Command command = new Command("buy", String.valueOf(index));
        os.writeObject(command);

        SignedMessage payment;
        if (customBuy) {
            int indexHash = Integer.parseInt(hashIndex) - 1;
            payment = user.createPaymentForIndex(indexHash);
        } else {
            payment = user.createPayment(items.getContent().get(index).getValue());
        }

        os.writeObject(payment);
        SignedMessage response = (SignedMessage) is.readObject();
        Validate.isTrue(!response.isException(), response.getMessage());

        playSong();
    }

    private void executeExit(ObjectOutputStream os) throws IOException {
        Command command = new Command("exit", null);
        os.writeObject(command);
        os.flush();
    }

    private void checkNewHashGeneration(ObjectOutputStream os, ObjectInputStream is) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, ClassNotFoundException {

        if (user.mustGenerateNewHashChain()) {
            System.out.println("Generating new hash chain.");

            String chainRoot = user.generateNewHashChain();
            user.updateLastGenerationDate();

            Command command = new Command("commitment", null);
            os.writeObject(command);

            SignedMessage commitment = user.createCommitment(certificate, chainRoot);
            os.writeObject(commitment);
            SignedMessage response = (SignedMessage) is.readObject();
            Validate.isTrue(!response.isException(), response.getMessage());
        }

        os.flush();
    }

    private void initialize() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, ClassNotFoundException {


        ObjectInputStream is = new ObjectInputStream(brockerSocket.getInputStream());
        ObjectOutputStream os = new ObjectOutputStream(brockerSocket.getOutputStream());

        SignedMessage registration = user.createRegistration();
        os.writeObject(registration);
        SignedMessage certificate = (SignedMessage) is.readObject();

        Validate.isTrue(!certificate.isException(), certificate.getMessage());

        // Verify that the certificate is valid
        Validate.isTrue(user.isValidPaywordCertificate(certificate), "Invalid payword cartificate!");
        this.certificate = certificate;
        os.flush();
    }

    private void playSong()
            throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException, JavaLayerException {

        File temp = File.createTempFile("temp_downloaded_song", ".mp3");
        temp.deleteOnExit();

        DataInputStream is = new DataInputStream(vendorSocket.getInputStream());
        long fileSize = is.readLong();

        try (OutputStream os = new FileOutputStream(temp)) {
            byte buffer[] = new byte[4096];
            int count;

            while ((fileSize > 0 && (count = is.read(buffer)) != -1)) {
                os.write(buffer, 0, count);
                fileSize -= count;
            }
            os.flush();
        }

        Desktop.getDesktop().open(temp);
    }

}
