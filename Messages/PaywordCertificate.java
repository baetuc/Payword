package Messages;

import org.apache.commons.lang3.Validate;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * Created by Cip on 23-Feb-17.
 */
public class PaywordCertificate {
    private static final String DELIMITER = "##DEL##";
    private String brokerID;
    private String userID;
    private String userIP;
    private PublicKey brokerPublicKey;
    private PublicKey userPublicKey;
    private String creditCardNumber;
    private Date expireDate;
    private long creditLimit;

    public PaywordCertificate(
            String brokerID,
            String userID,
            String userIP,
            PublicKey brokerPublicKey,
            PublicKey userPublicKey,
            String creditCardNumber,
            Date expireDate,
            long creditLimit) {

        this.brokerID = brokerID;
        this.userID = userID;
        this.userIP = userIP;
        this.brokerPublicKey = brokerPublicKey;
        this.userPublicKey = userPublicKey;
        this.creditCardNumber = creditCardNumber;
        this.expireDate = expireDate;
        this.creditLimit = creditLimit;
    }

    public PaywordCertificate() {
        this(null, null, null, null, null, null, null, 0);
    }

    public static PaywordCertificate createPaywordCertificate(String serializedCertificate) {
        PaywordCertificate certificate = new PaywordCertificate();
        String[] info = serializedCertificate.split(DELIMITER);
        Validate.isTrue(info.length == 8, "Serialized payword certificate does not have correct format.");

        try {
            certificate.brokerID = info[0];
            certificate.userID = info[1];
            certificate.userIP = info[2];
            certificate.brokerPublicKey = loadPublicKey(info[3]);
            certificate.userPublicKey = loadPublicKey(info[4]);
            certificate.creditCardNumber = info[5];
            certificate.expireDate = new Date(Long.parseLong(info[6]));
            certificate.creditLimit = Long.parseLong(info[7]);

        } catch (GeneralSecurityException | NumberFormatException e) {
            System.out.println(e.getMessage());
        }

        return certificate;
    }

    private static PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
        byte[] data = Base64.getDecoder().decode(stored);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        return fact.generatePublic(spec);
    }

    public String getBrokerID() {
        return brokerID;
    }

    public String getUserID() {
        return userID;
    }

    public String getUserIP() {
        return userIP;
    }

    public PublicKey getBrokerPublicKey() {
        return brokerPublicKey;
    }

    public PublicKey getUserPublicKey() {
        return userPublicKey;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public long getCreditLimit() {
        return creditLimit;
    }

    public void setBrokerID(String brokerID) {
        this.brokerID = brokerID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setUserIP(String userIP) {
        this.userIP = userIP;
    }

    public void setBrokerPublicKey(PublicKey brokerPublicKey) {
        this.brokerPublicKey = brokerPublicKey;
    }

    public void setUserPublicKey(PublicKey userPublicKey) {
        this.userPublicKey = userPublicKey;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public void setCreditLimit(long creditLimit) {
        this.creditLimit = creditLimit;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(brokerID).append(DELIMITER).append(userID).append(DELIMITER).append(userIP);
            sb.append(DELIMITER).append(savePublicKey(brokerPublicKey)).append(DELIMITER);
            sb.append(savePublicKey(userPublicKey)).append(DELIMITER).append(creditCardNumber);
            sb.append(DELIMITER).append(expireDate.getTime()).append(DELIMITER).append(creditLimit);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String savePublicKey(PublicKey publ) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec spec = fact.getKeySpec(publ, X509EncodedKeySpec.class);
        return Base64.getEncoder().encodeToString(spec.getEncoded());
    }

}
