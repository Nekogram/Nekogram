package tw.nekomimi.nekogram.passkey;

import org.telegram.messenger.Utilities;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class QrEncryption {

    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 12;      // Standard GCM 96-bit nonce
    private static final int GCM_TAG_LENGTH_BITS = 128; // 16 bytes

    public static byte[] encrypt(byte[] rawKeyBytes, byte[] password) throws GeneralSecurityException {
        var salt = new byte[SALT_LENGTH_BYTES];
        Utilities.random.nextBytes(salt);

        var iv = new byte[IV_LENGTH_BYTES];
        Utilities.random.nextBytes(iv);

        var secretKey = deriveKey(password, salt);

        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        var spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        var cipherTextWithTag = cipher.doFinal(rawKeyBytes);

        var out = ByteBuffer.allocate(salt.length + iv.length + cipherTextWithTag.length);
        out.put(salt);
        out.put(iv);
        out.put(cipherTextWithTag);

        return out.array();
    }

    public static byte[] decrypt(byte[] encryptedData, byte[] password) throws GeneralSecurityException, IllegalArgumentException {
        int minHeaderLen = SALT_LENGTH_BYTES + IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8);
        if (encryptedData == null || encryptedData.length < minHeaderLen) {
            throw new IllegalArgumentException("Invalid encrypted payload size");
        }

        var in = ByteBuffer.wrap(encryptedData);

        var salt = new byte[SALT_LENGTH_BYTES];
        in.get(salt);

        var iv = new byte[IV_LENGTH_BYTES];
        in.get(iv);

        var cipherTextWithTag = new byte[in.remaining()];
        in.get(cipherTextWithTag);

        var secretKey = deriveKey(password, salt);

        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        var spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        return cipher.doFinal(cipherTextWithTag);
    }

    private static SecretKey deriveKey(byte[] password, byte[] salt) {
        return new SecretKeySpec(Utilities.computePBKDF2(password, salt, 32), "AES");
    }
}
