package Participants;

import Messages.*;
import com.google.common.hash.Hashing;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.*;

/**
 * Created by Cip on 22-Feb-17.
 */
public class User extends Participant {
    private static final int CHAIN_LENGTH = 200;
    private static final String VENDOR_ID = "VENDOR";

    private List<String> hashChain;
    private Date lastGeneration;
    private int currentHashIndex = -1;

    public User() throws NoSuchAlgorithmException {
        this.hashChain = null;
        System.out.print("Insert valid ID: ");
        this.id = readInformation();
    }

    public SignedMessage createRegistration()
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        System.out.print("Insert password: ");
        String password = readInformation();

        BrokerRegistration registration = new BrokerRegistration(id, password, publicKey);
        return signMessage(registration.toString());
    }

    public SignedMessage createCommitment(SignedMessage cert, String chainRoot)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        Commitment commitment = new Commitment(VENDOR_ID, cert, chainRoot, lastGeneration, CHAIN_LENGTH);
        return signMessage(commitment.toString());
    }

    public SignedMessage createPayment(int amount)
            throws IllegalArgumentException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        Payment payment = new Payment(id, payAmount(amount), currentHashIndex);
        return signMessage(payment.toString());
    }

    public SignedMessage createPaymentForIndex(int index)
            throws IllegalArgumentException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        Payment payment = new Payment(id, getHashForIndex(index), index);
        return signMessage(payment.toString());
    }

    public boolean isValidPaywordCertificate(SignedMessage message)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        PaywordCertificate certificate = PaywordCertificate.createPaywordCertificate(message.getMessage());
        return isValid(message, certificate.getBrokerPublicKey());
    }

    public boolean mustGenerateNewHashChain() {
        return hashChain == null || passedOneDay();
    }

    /**
     * Method that generates the hash chain, with the length {@value User#CHAIN_LENGTH}
     *
     * @return the root of the hash chain, namely c0
     */
    public String generateNewHashChain() {
        if (hashChain == null) {
            hashChain = new ArrayList<>();
        } else {
            hashChain.clear();
            currentHashIndex = -1;
        }

        String current = RandomStringUtils.random(64, "0123456789abcdef");

        for (int i = 0; i < CHAIN_LENGTH; ++i) {
            hashChain.add(current);
            current = Hashing.sha256().hashString(current, StandardCharsets.UTF_8).toString();
        }

        Collections.reverse(hashChain);
        return current;
    }

    public void updateLastGenerationDate() {
        this.lastGeneration = new Date();
    }

    private String payAmount(int amount) throws IllegalArgumentException {
        Validate.isTrue(currentHashIndex + amount < CHAIN_LENGTH, "Not enough money to pay amount: ", amount);

        currentHashIndex += amount;
        return hashChain.get(currentHashIndex);
    }

    private String getHashForIndex(int index) {
        Validate.isTrue(0 <= index && index < CHAIN_LENGTH, "Invalid chain index: ", index);

        return hashChain.get(index);
    }

    private boolean passedOneDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.lastGeneration);
        cal.add(Calendar.DATE, 1);
        Date nextDay = cal.getTime();
        Date currentDate = new Date();

        return currentDate.after(nextDay);
    }


}
