package io.github.sfaguiar.harvester.multiplayer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;

/**
 * Deterministic, DNS-resolution-free server identity for the client's
 * per-server opt-in store: the lowercased host string plus the explicit
 * port the player connected with — never a resolved IP, a server-visible
 * name (MOTD, etc.), or any other server-supplied value. The caller
 * (the glue that captures the raw connect-time address) is responsible
 * for supplying the exact string the player used to connect, before any
 * DNS resolution happens — see {@code
 * ClientNetworkHandlerConnectMixin}'s own documentation for where that
 * string is captured and why it must be captured there.
 */
public final class HarvesterServerIdentity {

    private final String normalizedHost;
    private final int port;

    private HarvesterServerIdentity(String normalizedHost, int port) {
        this.normalizedHost = normalizedHost;
        this.port = port;
    }

    public static HarvesterServerIdentity of(String host, int port) {
        Objects.requireNonNull(host, "host");
        return new HarvesterServerIdentity(host.trim().toLowerCase(Locale.ROOT), port);
    }

    /** {@code host:port}, host lowercased, port explicit — the value persisted verbatim in the opt-in file's {@code address} key. */
    public String normalizedAddress() {
        return normalizedHost + ":" + port;
    }

    /** First 8 hex characters of SHA-256({@link #normalizedAddress()}) — short, deterministic, collision-negligible at this scale. */
    public String shortHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedAddress().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(8);
            for (int i = 0; i < 4; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory JDK algorithm (JEP/JCA baseline on every JDK 17 distribution); unreachable in practice.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Filesystem-safe readable prefix: the host with any character outside {@code [a-z0-9.-]} replaced by {@code _}. */
    public String readableName() {
        StringBuilder sanitized = new StringBuilder(normalizedHost.length());
        for (int i = 0; i < normalizedHost.length(); i++) {
            char c = normalizedHost.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-') {
                sanitized.append(c);
            } else {
                sanitized.append('_');
            }
        }
        return sanitized.length() == 0 ? "server" : sanitized.toString();
    }

    /** {@code <readableName>-<shortHash>.properties} — the hash disambiguates different ports on the same host. */
    public String fileName() {
        return readableName() + "-" + shortHash() + ".properties";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HarvesterServerIdentity other)) {
            return false;
        }
        return port == other.port && normalizedHost.equals(other.normalizedHost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalizedHost, port);
    }

    @Override
    public String toString() {
        return normalizedAddress();
    }
}
