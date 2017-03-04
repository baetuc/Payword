package Messages;

import org.apache.commons.lang3.Validate;

import java.util.Date;

/**
 * Created by Cip on 25-Feb-17.
 */
public class Commitment {
    private static final String DELIMITER = "###C###";

    private String vendorID;
    private SignedMessage signedPaywordCertificate;
    private PaywordCertificate paywordCertificate;
    private String hashRoot;
    private Date currentDate;
    private int chainLength;

    public Commitment(String vendorID,
                      SignedMessage signedPaywordCertificate,
                      String hashRoot,
                      Date currentDate,
                      int chainLength) {

        this.vendorID = vendorID;
        this.signedPaywordCertificate = signedPaywordCertificate;
        this.hashRoot = hashRoot;
        this.currentDate = currentDate;
        this.chainLength = chainLength;
    }

    public Commitment() {
        this(null, null, null, null, 0);
    }

    public static Commitment createCommitment(String serializedCommitment) {
        Commitment commitment = new Commitment();
        String[] info = serializedCommitment.split(DELIMITER);
        Validate.isTrue(info.length == 5, "Serialized commitment does not have the correct format.");

        try {
            commitment.vendorID = info[0];
            commitment.signedPaywordCertificate = SignedMessage.createSignedMessage(info[1]);
            commitment.hashRoot = info[2];
            commitment.currentDate = new Date(Long.parseLong(info[3]));
            commitment.chainLength = Integer.parseInt(info[4]);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        }

        return commitment;
    }


    public String getVendorID() {
        return vendorID;
    }

    public SignedMessage getSignedPaywordCertificate() {
        return signedPaywordCertificate;
    }

    public String getHashRoot() {
        return hashRoot;
    }

    public Date getCurrentDate() {
        return currentDate;
    }

    public int getChainLength() {
        return chainLength;
    }

    public PaywordCertificate getPaywordCertificate() {
        if (paywordCertificate == null) {
            paywordCertificate = PaywordCertificate.createPaywordCertificate(signedPaywordCertificate.getMessage());
        }

        return paywordCertificate;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(vendorID).append(DELIMITER).append(signedPaywordCertificate.toString()).append(DELIMITER);
        sb.append(hashRoot).append(DELIMITER).append(currentDate.getTime()).append(DELIMITER);
        sb.append(chainLength);

        return sb.toString();
    }
}
