package com.sunsetbeach.lighting;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The SSRF guard of the lighting module. An admin types a host and this server then sends HTTP
 * requests to it, so the host is accepted only when <em>every</em> address it resolves to is a
 * private IPv4 LAN address: 10/8, 172.16/12 or 192.168/16 <em>and</em> inside one of the CIDRs of
 * {@code app.lighting.allowed-cidrs}. That setting is required: unset or blank means lighting is off
 * ({@link #isConfigured()}), because "any private address" would include the database and the other
 * containers on the compose network. Loopback, link-local (169.254/16 - cloud metadata), "this host",
 * multicast, IPv6 and every public address are refused; so is any address that belongs to a network
 * interface of this machine (the docker bridge and the containers live in 172.16/12 too).
 *
 * <p>{@link #resolve} is called by {@link HttpWledClient} for each request, and the request then goes
 * to the resolved <em>IP</em>, not to the name - so a DNS answer that changes between "check" and
 * "connect" (rebinding) has nothing to change. It is also called when a controller is saved, to give
 * the admin a 400 early; that check alone would not be enough.
 */
@Component
public class LanAddressGuard {

    /** Why a host was refused, worded for the admin who typed it. */
    public static class RefusedHostException extends RuntimeException {
        public RefusedHostException(String message) {
            super(message);
        }
    }

    /** Test seam: turns a host into addresses (DNS). */
    @FunctionalInterface
    interface Resolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private record Cidr(int network, int mask) {
        boolean contains(int address) {
            return (address & mask) == network;
        }
    }

    public static final String NOT_CONFIGURED = "Lighting is not configured on the server";

    private static final String PRIVATE_ONLY = "The address must be a private LAN address (10.x.x.x, 172.16-31.x.x or 192.168.x.x)";

    private final List<Cidr> allowed;
    private final Resolver resolver;
    private final Predicate<InetAddress> isOwnAddress;

    @Autowired
    public LanAddressGuard(@Value("${app.lighting.allowed-cidrs:}") String allowedCidrs) {
        this(allowedCidrs, InetAddress::getAllByName, LanAddressGuard::belongsToThisMachine);
    }

    LanAddressGuard(String allowedCidrs, Resolver resolver, Predicate<InetAddress> isOwnAddress) {
        this.allowed = parseCidrs(allowedCidrs);
        this.resolver = resolver;
        this.isOwnAddress = isOwnAddress;
    }

    /**
     * Lighting is on only when {@code app.lighting.allowed-cidrs} names at least one network. Without it
     * "any private address" would include the other containers on the compose network (the database,
     * the frontend), which are in 172.16/12 too and are not this machine's own address.
     */
    public boolean isConfigured() {
        return !allowed.isEmpty();
    }

    /** 409 when lighting is off; called before anything that would make the server send a request. */
    public void requireConfigured() {
        if (!isConfigured()) {
            throw new com.sunsetbeach.error.ConflictException(NOT_CONFIGURED);
        }
    }

    /** The address to connect to (an IPv4 literal), or {@link RefusedHostException}. */
    public InetAddress resolve(String host) {
        if (!isConfigured()) {
            throw new RefusedHostException(NOT_CONFIGURED);
        }
        if (host == null || host.isBlank()) {
            throw new RefusedHostException("Specify the controller's address");
        }
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host.trim());
        } catch (UnknownHostException | RuntimeException e) {
            throw new RefusedHostException("The address does not resolve");
        }
        if (addresses == null || addresses.length == 0) {
            throw new RefusedHostException("The address does not resolve");
        }
        InetAddress chosen = null;
        for (InetAddress address : addresses) {
            requireAllowed(address);
            if (chosen == null) {
                chosen = address;
            }
        }
        return chosen;
    }

    private void requireAllowed(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            throw new RefusedHostException(PRIVATE_ONLY);
        }
        if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress() || address.isMulticastAddress()) {
            throw new RefusedHostException(PRIVATE_ONLY);
        }
        int value = toInt(address);
        if (!isPrivate(value)) {
            throw new RefusedHostException(PRIVATE_ONLY);
        }
        if (allowed.stream().noneMatch(c -> c.contains(value))) {
            throw new RefusedHostException("The address is outside the networks lighting controllers are allowed on");
        }
        if (isOwnAddress.test(address)) {
            throw new RefusedHostException("The address is one of this server's own");
        }
    }

    private static boolean isPrivate(int a) {
        return (a & 0xFF000000) == 0x0A000000 // 10.0.0.0/8
                || (a & 0xFFF00000) == 0xAC100000 // 172.16.0.0/12
                || (a & 0xFFFF0000) == 0xC0A80000; // 192.168.0.0/16
    }

    private static int toInt(InetAddress address) {
        byte[] b = address.getAddress();
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }

    private static boolean belongsToThisMachine(InetAddress address) {
        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (SocketException e) {
            return true; // cannot tell - refuse rather than allow
        }
    }

    private static List<Cidr> parseCidrs(String text) {
        List<Cidr> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        for (String part : text.split(",")) {
            String entry = part.trim();
            if (entry.isEmpty()) {
                continue;
            }
            try {
                String[] pieces = entry.split("/");
                int prefix = pieces.length == 2 ? Integer.parseInt(pieces[1].trim()) : 32;
                InetAddress base = InetAddress.getByName(pieces[0].trim());
                if (!(base instanceof Inet4Address) || prefix < 0 || prefix > 32 || pieces.length > 2) {
                    throw new IllegalArgumentException();
                }
                int mask = prefix == 0 ? 0 : -1 << (32 - prefix);
                result.add(new Cidr(toInt(base) & mask, mask));
            } catch (RuntimeException | UnknownHostException e) {
                // Fail at start-up: a typo here must not silently widen what the server may reach.
                throw new IllegalStateException("app.lighting.allowed-cidrs: \"" + entry + "\" is not an IPv4 CIDR");
            }
        }
        return result;
    }
}
