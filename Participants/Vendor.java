package Participants;

import Messages.*;
import Requests.Items;
import com.google.common.hash.Hashing;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.*;

/**
 * Created by Cip on 25-Feb-17.
 */
public class Vendor extends Participant {
    private static final String ROOT_FOLDER = "D:\\Filme\\RED HOT CHILLI PEPPERS - DISCOGRAPHY\\[1999] Californication";

    private final Map<String, UserInfo> userCommitments;
    private final Items items;

    public Vendor() throws NoSuchAlgorithmException {
        this.userCommitments = Collections.synchronizedMap(new HashMap<>());
        this.items = new Items();
        initializeItems();
    }

    public void addCommitment(SignedMessage commitmentMessage)
            throws IllegalArgumentException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        Commitment commitment = Commitment.createCommitment(commitmentMessage.getMessage());
        PaywordCertificate certificate = commitment.getPaywordCertificate();

        // Verify user's signature
        Validate.isTrue(isValid(commitmentMessage, certificate.getUserPublicKey()),
                "User signature invalid on commit message.");

        // Verify broker's signature
        Validate.isTrue(isValid(commitment.getSignedPaywordCertificate(), certificate.getBrokerPublicKey()),
                "Broker signature invalid on Payword certficate message.");

        userCommitments.put(certificate.getUserID(), new UserInfo(commitment));
        System.out.println("Received commitment for user: " + certificate.getUserID());
    }

    public Items listCommand() {
        return items;
    }

    public long validateBuy(int itemIndex, Payment payment, String userIP)
            throws IllegalArgumentException {

        Validate.isTrue(userCommitments.containsKey(payment.getUserID()), "No commitment found for user ID.");
        Validate.isTrue(0 <= itemIndex && itemIndex < items.getContent().size(), "Item index is invalid: %d", itemIndex);
        UserInfo info = userCommitments.get(payment.getUserID());

        Validate.isTrue(payment.getHashIndex() - info.getLastHashIndex() > 0, "Payment amount must be positive.");
        Validate.isTrue(payment.getHashIndex() == info.getLastHashIndex() + items.getItem(itemIndex).getValue(),
                "Payment size is incorrect: paid for %d$, but item price is %d$.",
                payment.getHashIndex() - info.getLastHashIndex(), items.getItem(itemIndex).getValue());
        Validate.isTrue(info.getCommitment().getChainLength() >= payment.getHashIndex(), "Hash index larger than chain size.");
        Validate.isTrue(userIP.equals(info.getCommitment().getPaywordCertificate().getUserIP()),
                "User must connect from the same IP as the one in the certificate.");

        long creditLimit = info.getCommitment().getPaywordCertificate().getCreditLimit();
        Date generationDate = info.getCommitment().getCurrentDate();

        Validate.isTrue(payment.getHashIndex() < creditLimit, "Credit limit exceeded for user " + payment.getUserID() +
                ". Left with " + (creditLimit - payment.getHashIndex() - 1));
        Validate.isTrue(!passedOneDay(generationDate), "Commitment certificate has expired for user " + payment.getUserID());

        validateChain(payment);
        info.incrementLastHashIndex(payment.getHashIndex() - info.getLastHashIndex());
        info.setLastHash(payment.getHash());
        System.out.println("Received payment for item " + (itemIndex + 1) + ", in value of " +
                items.getItem(itemIndex).getValue() + "$ from user " + payment.getUserID());

        userCommitments.put(payment.getUserID(), info);
        return creditLimit - info.getLastHashIndex() - 1;
    }

    private void validateChain(Payment payment) {
        UserInfo info = userCommitments.get(payment.getUserID());

        int amount = payment.getHashIndex() - info.getLastHashIndex();
        String current = payment.getHash();

        for (int i = 0; i < amount; ++i) {
            current = Hashing.sha256().hashString(current, StandardCharsets.UTF_8).toString();
        }

        Validate.isTrue(current.equals(info.getLastHash()), "Calculated hash is not equal to given one.");
    }

    private void initializeItems() {
        Random random = new Random();

        File[] listOfFiles = new File(ROOT_FOLDER).listFiles();
        if (listOfFiles == null) {
            return;
        }

        for (File file : listOfFiles) {
            if (!file.isFile()) {
                continue;
            }
            items.addItem(new Items.Item(file.getName(), random.nextInt(5) + 1));
        }
    }

    public String getItemPath(int itemIndex) {
        return ROOT_FOLDER + "\\" + items.getItem(itemIndex).getName();
    }

    public List<SignedMessage> createBrokerPayments() {
        List<SignedMessage> payments = new ArrayList<>();
        BrokerPayment payment;

        synchronized (userCommitments) {
            for (Map.Entry<String, UserInfo> userInfo : userCommitments.entrySet()) {
                UserInfo info = userInfo.getValue();
                if (info.getLastHashIndex() - info.getTransferred() == -1) {
                    continue; // no items buyed.
                }
                payment = new BrokerPayment(info.getCommitment(), info.getLastHash(), info.getLastHashIndex() + 1);
                payments.add(new SignedMessage(false, payment.toString(), null));
                info.incrementTransferred(info.getLastHashIndex() - info.getTransferred() + 1);
                userInfo.setValue(info);
            }
        }
        return payments;
    }


    private boolean passedOneDay(Date referenceDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(referenceDate);
        cal.add(Calendar.DATE, 1);
        Date nextDay = cal.getTime();
        Date currentDate = new Date();

        return currentDate.after(nextDay);
    }

    private final class UserInfo {
        private Commitment commitment;
        private int lastHashIndex;
        private String lastHash;
        private int transferred;

        public UserInfo(Commitment commitment) {
            this.commitment = commitment;
            this.lastHashIndex = -1;
            this.lastHash = commitment.getHashRoot();
            this.transferred = 0;
        }

        public Commitment getCommitment() {
            return commitment;
        }

        public int getLastHashIndex() {
            return lastHashIndex;
        }

        public void setLastHash(String lastHash) {
            this.lastHash = lastHash;
        }

        public String getLastHash() {
            return lastHash;
        }

        public int getTransferred() {
            return transferred;
        }

        public void incrementLastHashIndex(int amount) {
            lastHashIndex += amount;
        }

        public void incrementTransferred(int amount) {
            transferred += amount;
        }
    }

}
