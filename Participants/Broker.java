package Participants;

import Messages.*;
import com.google.common.hash.Hashing;
import oracle.jdbc.driver.OracleDriver;
import org.apache.commons.lang3.Validate;

import java.nio.charset.StandardCharsets;
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

    public SignedMessage transfer(BrokerPayment payment) throws SQLException {
        DriverManager.registerDriver(new OracleDriver());
        Connection databaseConnection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe",
                "student", "STUDENT");

        PreparedStatement statement = databaseConnection.prepareStatement(
                "SELECT credit_limit FROM user_info WHERE user_id = ?");

        String userID = payment.getCommitment().getPaywordCertificate().getUserID();
        statement.setString(1, userID);
        ResultSet resultSet = statement.executeQuery();

        Validate.isTrue(resultSet.isBeforeFirst(), "Inexistent user: " + userID);
        resultSet.next();

        long creditLimit = resultSet.getLong(1);
        creditLimit -= payment.getAmount();

        validateChain(payment);
        String responseMessage = "Transferred " + payment.getAmount() + "$ to account. User left with " + creditLimit + "$.";

        return new SignedMessage(false, responseMessage, null);
    }

    private void validateChain(BrokerPayment payment) {
        String initialHash = payment.getCommitment().getHashRoot();

        String current = payment.getFinalHash();
        for (int i = 0; i < payment.getAmount(); ++i) {
            current = Hashing.sha256().hashString(current, StandardCharsets.UTF_8).toString();
        }

        Validate.isTrue(current.equals(initialHash), "Hash chain is invalid! We will contact you for further instructions.");
    }


}

