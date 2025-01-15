package pt.ulisboa.tecnico.sec;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public final class Utils {

    private Utils() {
    }

    public static String getKeyPath(boolean isPrivateKey, int id) {

        switch (id) {
            case 1:
                if (isPrivateKey) {
                    return "keys/server1/private_key_server1.der";
                } else {
                    return "keys/server1/public_key_server1.der";
                }
            case 2:
                if (isPrivateKey) {
                    return "keys/server2/private_key_server2.der";
                } else {
                    return "keys/server2/public_key_server2.der";
                }
            case 3:
                if (isPrivateKey) {
                    return "keys/server3/private_key_server3.der";
                } else {
                    return "keys/server3/public_key_server3.der";
                }
            case 4:
                if (isPrivateKey) {
                    return "keys/server4/private_key_server4.der";
                } else {
                    return "keys/server4/public_key_server4.der";
                }
            case 5:
                if (isPrivateKey) {
                    return "keys/client5/private_key_client5.der";
                } else {
                    return "keys/client5/public_key_client5.der";
                }
            case 6:
                if (isPrivateKey) {
                    return "keys/client6/private_key_client6.der";
                } else {
                    return "keys/client6/public_key_client6.der";
                }
            case 7:
                if (isPrivateKey) {
                    return "keys/client7/private_key_client7.der";
                } else {
                    return "keys/client7/public_key_client7.der";
                }

            default:
                return "";
        }
    }

    private static PrivateKey getPrivateKey(String filename) throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(spec);
    }

    public static PublicKey getPublicKey(String filename) throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(spec);
    }

    public static byte[] generateSignature(Message m, int id) {
        byte[] signedData = null;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");

            String keyPath;

            if (m.getType() == MessageType.ACK) {
                AckMessage ack = (AckMessage) m;
                keyPath = getKeyPath(true, ack.getAckSenderId());
            } else {
                keyPath = getKeyPath(true, id);
            }

            PrivateKey key = getPrivateKey(keyPath);

            // buscar chave do ficheiro
            signature.initSign(key);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(m);
            oos.close();

            byte[] data = bStream.toByteArray();

            // Add data to be signed
            signature.update(data);

            // Generate the signature
            signedData = signature.sign();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return signedData;
    }

    public static byte[] generateSignatureBalance(PublicKey accountKey, float balance, int replicaId) {
        byte[] signedData = null;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");

            String keyPath = getKeyPath(true, replicaId);

            PrivateKey key = getPrivateKey(keyPath);

            // buscar chave do ficheiro
            signature.initSign(key);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(accountKey);
            oos.writeObject(balance);
            oos.close();

            byte[] data = bStream.toByteArray();

            // Add data to be signed
            signature.update(data);

            // Generate the signature
            signedData = signature.sign();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return signedData;
    }

    public static boolean verifySignature(Message m, byte[] signedData) {
        boolean isValid = false;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");

            int senderId;

            if (m.getType() == MessageType.ACK) {
                AckMessage ack = (AckMessage) m;
                senderId = ack.getAckSenderId();
            } else {
                senderId = m.getSenderId();
            }

            String keyPath = getKeyPath(false, senderId);

            PublicKey key = getPublicKey(keyPath);

            signature.initVerify(key);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(m);
            oos.close();

            byte[] data = bStream.toByteArray();

            signature.update(data);

            isValid = signature.verify(signedData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isValid;
    }

    public static boolean verifySignature(int senderId, PublicKey accountKey, float balance, byte[] signedData) {
        boolean isValid = false;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");

            String keyPath = getKeyPath(false, senderId);

            PublicKey key = getPublicKey(keyPath);

            signature.initVerify(key);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(accountKey);
            oos.writeObject(balance);
            oos.close();

            byte[] data = bStream.toByteArray();

            signature.update(data);

            isValid = signature.verify(signedData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isValid;
    }
}
