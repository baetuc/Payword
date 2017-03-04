package Participants;

import Messages.BrokerRegistration;
import Messages.PaywordCertificate;
import Messages.SignedMessage;
import oracle.jdbc.driver.OracleDriver;
import org.apache.commons.lang3.Validate;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.*;

/**
 * Created by Cip on 25-Feb-17.
 */
public class Broker extends Participant {

    public Broker() throws NoSuchAlgorithmException {
    }

    public SignedMessage createPaywordCertificate(BrokerRegistration registration, String userIP)
            throws SQLException, IllegalArgumentException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        PaywordCertificate certificate = new PaywordCertificate();

        certificate.setBrokerID(this.id);
        certificate.setBrokerPublicKey(this.publicKey);
        certificate.setUserIP(userIP);
        certificate.setUserID(registration.getUserID());
        certificate.setUserPublicKey(registration.getUserPublicKey());
        fillCertificateFromDatabase(certificate, registration.getUserID(), registration.getUserPassword());

        return signMessage(certificate.toString());
    }


    private void fillCertificateFromDatabase(PaywordCertificate certificate, String userID, String userPassword)
            throws SQLException, IllegalArgumentException {

        Validate.notNull(certificate, "Certificate given must be not null.");
        Validate.notNull(userID, "User ID must be not null.");
        Validate.notNull(userPassword, "User password must be not null.");

        DriverManager.registerDriver(new OracleDriver());
        Connection databaseConnection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe",
                "student", "STUDENT");

        PreparedStatement statement = databaseConnection.prepareStatement(
                "SELECT credit_card_number, expire_date, credit_limit FROM user_info WHERE user_id = ? AND passwd = ?");
        // Fetching the results from the database
        statement.setString(1, userID);
        statement.setString(2, userPassword);
        ResultSet resultSet = statement.executeQuery();

        Validate.isTrue(resultSet.isBeforeFirst(), "Invalid credentials or unregistered user!");

        resultSet.next();
        certificate.setCreditCardNumber(resultSet.getString(1));
        certificate.setExpireDate(resultSet.getDate(2));
        certificate.setCreditLimit(resultSet.getLong(3));
    }

}

