package Participants;

import Messages.SignedMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.*;

/**
 * Created by Cip on 23-Feb-17.
 */
public abstract class Participant {
    protected String id;
    protected PrivateKey privateKey;
    protected PublicKey publicKey;

    public Participant() throws NoSuchAlgorithmException {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    protected SignedMessage signMessage(String message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature instance = Signature.getInstance("SHA1withRSA");

        instance.initSign(privateKey);
        instance.update((message).getBytes());
        return new SignedMessage(message, instance.sign());
    }

    protected boolean isValid(SignedMessage message, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA1withRSA");

        signature.initVerify(publicKey);
        signature.update(message.getMessage().getBytes());
        return signature.verify(message.getSignature());
    }

    protected String readInformation() {
        String result = null;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            result = reader.readLine();
        } catch (IOException e) {
            // Logger.log(e.getMessage(), ERROR);
            System.out.println(e.getMessage());
        }
        return result;
    }
}
