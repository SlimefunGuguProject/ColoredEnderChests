package io.github.thebusybiscuit.coloredenderchests;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class Utils {

    private Utils() {
        // Utility class
    }

    @Nullable
    public static UUID generateFixedUUID(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.wrap(hash);
            long mostSigBits = buffer.getLong();
            long leastSigBits = buffer.getLong();

            return new UUID(mostSigBits, leastSigBits);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
