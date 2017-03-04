package Messages;

import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.Base64;

/**
 * Created by Cip on 23-Feb-17.
 */
public class SignedMessage implements Serializable {
    private static final String DELIMITER = "###SM###";

    private boolean exception;
    private String message;
    private byte[] signature;

    public SignedMessage(boolean exception, String message, byte[] signature) {
        this.exception = exception;
        this.message = message;
        this.signature = signature;
    }

    public SignedMessage(String message, byte[] signature) {
        this(false, message, signature);
    }

    public static SignedMessage createSignedMessage(String serializedMessage) {
        String[] info = serializedMessage.split(DELIMITER);
        Validate.isTrue(info.length == 3, "Serialized message format is invalid: %s.", serializedMessage);

        boolean exception = Boolean.parseBoolean(info[0]);
        byte[] sig = Base64.getDecoder().decode(info[2]);

        return new SignedMessage(exception, info[1], sig);
    }

    public String getMessage() {
        return message;
    }

    public byte[] getSignature() {
        return signature;
    }

    public boolean isException() {
        return exception;
    }

    @Override
    public String toString() {
        return String.valueOf(exception) + DELIMITER + message + DELIMITER + Base64.getEncoder().encodeToString(signature);
    }


}
