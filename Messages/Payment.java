package Messages;

import org.apache.commons.lang3.Validate;

/**
 * Created by Cip on 25-Feb-17.
 */
public class Payment {
    private static final String DELIMITER = "###P###";

    private String userID;
    private String hash;
    private int hashIndex;

    public Payment(String userID, String hash, int hashIndex) {
        this.userID = userID;
        this.hash = hash;
        this.hashIndex = hashIndex;
    }

    public Payment() {
        this(null, null, 0);
    }

    public static Payment createPayment(String serializedPayment) {
        Payment payment = new Payment();
        String[] info = serializedPayment.split(DELIMITER);
        Validate.isTrue(info.length == 3, "Payment object has %d fields, instead of 3.", info.length);

        try {
            payment.userID = info[0];
            payment.hash = info[1];
            payment.hashIndex = Integer.parseInt(info[2]);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        }

        return payment;
    }

    public String getHash() {
        return hash;
    }

    public int getHashIndex() {
        return hashIndex;
    }

    public String getUserID() {
        return userID;
    }

    @Override
    public String toString() {
        return userID + DELIMITER + hash + DELIMITER + hashIndex;
    }
}
