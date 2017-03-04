package Messages;

import org.apache.commons.lang3.Validate;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Created by Cip on 25-Feb-17.
 */
public class BrokerRegistration {
    private static final String DELIMITER = "###R###";

    private String userID;
    private String userPassword;
    private PublicKey userPublicKey;

    public BrokerRegistration(String userID, String userPassword, PublicKey userPublicKey) {
        this.userID = userID;
        this.userPassword = userPassword;
        this.userPublicKey = userPublicKey;
    }

    public BrokerRegistration() {
        this(null, null, null);
    }

    public static BrokerRegistration deserializeRegistration(String serializedRegistration) throws GeneralSecurityException {
        BrokerRegistration registration = new BrokerRegistration();
        String[] info = serializedRegistration.split(DELIMITER);
        Validate.isTrue(
                info.length == 3,
                "Broker Registration object format is invalid: ",
                serializedRegistration);

        registration.userID = info[0];
        registration.userPassword = info[1];
        registration.userPublicKey = loadPublicKey(info[2]);

        return registration;
    }

    private static PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
        byte[] data = Base64.getDecoder().decode(stored);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        return fact.generatePublic(spec);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        try {
            sb.append(userID).append(DELIMITER).append(userPassword).append(DELIMITER);
            sb.append(savePublicKey(userPublicKey));
        } catch (GeneralSecurityException e) {
            System.out.println(e.getMessage());
        }

        return sb.toString();
    }

    public String getUserID() {
        return userID;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public PublicKey getUserPublicKey() {
        return userPublicKey;
    }

    private String savePublicKey(PublicKey publ) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec spec = fact.getKeySpec(publ, X509EncodedKeySpec.class);
        return Base64.getEncoder().encodeToString(spec.getEncoded());
    }
}
