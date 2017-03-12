package Messages;

import org.apache.commons.lang3.Validate;

/**
 * Created by Cip on 08-Mar-17.
 */
public class BrokerPayment {
    private static final String DELIMITER = "###BP###";

    private Commitment commitment;
    private String finalHash;
    private int amount;

    public BrokerPayment(Commitment commitment, String finalHash, int amount) {
        this.commitment = commitment;
        this.finalHash = finalHash;
        this.amount = amount;
    }

    public static BrokerPayment createBrokerPayment(String serializedBrokerPayment) {
        String[] info = serializedBrokerPayment.split(DELIMITER);
        Validate.isTrue(info.length == 3, "Broker Payment structure invalid!");

        Commitment commitment = Commitment.createCommitment(info[0]);
        int amount = Integer.parseInt(info[2]);
        return new BrokerPayment(commitment, info[1], amount);
    }

    public Commitment getCommitment() {
        return commitment;
    }

    public String getFinalHash() {
        return finalHash;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return commitment.toString() + DELIMITER + finalHash + DELIMITER + amount;
    }
}
