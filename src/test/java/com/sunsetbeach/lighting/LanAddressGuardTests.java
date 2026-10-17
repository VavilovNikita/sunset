package com.sunsetbeach.lighting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.lighting.LanAddressGuard.RefusedHostException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * The SSRF guard: which addresses a lighting controller may have. Literals go through the real
 * resolver (no DNS is involved for an IP); names go through a stub so nothing here touches a network.
 */
class LanAddressGuardTests {

    private static final String PRIVATE_RANGES = "10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16";

    private final LanAddressGuard guard = new LanAddressGuard(PRIVATE_RANGES, InetAddress::getAllByName, a -> false);

    @Test
    void privateLanAddresses_areAccepted_andReturnedAsTheAddressToConnectTo() {
        assertThat(guard.resolve("10.0.0.5").getHostAddress()).isEqualTo("10.0.0.5");
        assertThat(guard.resolve("172.16.0.1").getHostAddress()).isEqualTo("172.16.0.1");
        assertThat(guard.resolve("172.31.255.254").getHostAddress()).isEqualTo("172.31.255.254");
        assertThat(guard.resolve(" 192.168.1.50 ").getHostAddress()).isEqualTo("192.168.1.50");
    }

    @Test
    void everythingElse_isRefused() {
        for (String host : new String[] {
                "127.0.0.1", "127.1", "localhost", "0.0.0.0",
                "169.254.169.254", "169.254.0.1", // link-local, incl. the cloud metadata address
                "8.8.8.8", "1.1.1.1", "172.15.255.255", "172.32.0.1", "192.167.1.1", "11.0.0.1", "100.64.0.1",
                "224.0.0.1", "255.255.255.255",
                "::1", "fe80::1", "fd00::1", "2001:db8::1", "[::1]",
                "::ffff:127.0.0.1", "::ffff:8.8.8.8", // IPv4 inside IPv6 is judged as the IPv4 it is
                "2130706433", // 127.0.0.1 as one integer
                "" }) {
            assertThatThrownBy(() -> guard.resolve(host)).as(host).isInstanceOf(RefusedHostException.class);
        }
        assertThatThrownBy(() -> guard.resolve(null)).isInstanceOf(RefusedHostException.class);
    }

    @Test
    void aNameIsJudgedByEveryAddressItResolvesTo() throws Exception {
        LanAddressGuard byName = new LanAddressGuard(PRIVATE_RANGES, host -> switch (host) {
            case "lan.example" -> new InetAddress[] {InetAddress.getByName("192.168.1.9")};
            case "public.example" -> new InetAddress[] {InetAddress.getByName("93.184.216.34")};
            case "mixed.example" -> new InetAddress[] {InetAddress.getByName("192.168.1.9"), InetAddress.getByName("93.184.216.34")};
            case "metadata.example" -> new InetAddress[] {InetAddress.getByName("169.254.169.254")};
            default -> throw new UnknownHostException(host);
        }, a -> false);

        assertThat(byName.resolve("lan.example").getHostAddress()).isEqualTo("192.168.1.9");
        assertThatThrownBy(() -> byName.resolve("public.example")).isInstanceOf(RefusedHostException.class);
        assertThatThrownBy(() -> byName.resolve("mixed.example")).isInstanceOf(RefusedHostException.class);
        assertThatThrownBy(() -> byName.resolve("metadata.example")).isInstanceOf(RefusedHostException.class);
        assertThatThrownBy(() -> byName.resolve("nowhere.example")).isInstanceOf(RefusedHostException.class).hasMessageContaining("does not resolve");
    }

    @Test
    void aNameWhoseAnswerChanges_isCheckedAgainOnTheNextResolve() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        LanAddressGuard rebinding = new LanAddressGuard(PRIVATE_RANGES, host -> new InetAddress[] {
                InetAddress.getByName(calls.getAndIncrement() == 0 ? "192.168.1.9" : "127.0.0.1")}, a -> false);

        assertThat(rebinding.resolve("rebind.example").getHostAddress()).isEqualTo("192.168.1.9");
        assertThatThrownBy(() -> rebinding.resolve("rebind.example")).isInstanceOf(RefusedHostException.class);
    }

    @Test
    void anAddressOfThisMachine_isRefusedEvenWhenPrivate() {
        LanAddressGuard withOwn = new LanAddressGuard(PRIVATE_RANGES, InetAddress::getAllByName, a -> a.getHostAddress().equals("172.18.0.2"));

        assertThatThrownBy(() -> withOwn.resolve("172.18.0.2")).isInstanceOf(RefusedHostException.class).hasMessageContaining("own");
        assertThat(withOwn.resolve("172.18.0.3")).isNotNull();
    }

    @Test
    void allowedCidrs_narrowThePrivateRanges() {
        LanAddressGuard narrow = new LanAddressGuard("192.168.1.0/24, 10.20.0.0/16", InetAddress::getAllByName, a -> false);

        assertThat(narrow.resolve("192.168.1.77")).isNotNull();
        assertThat(narrow.resolve("10.20.5.5")).isNotNull();
        assertThatThrownBy(() -> narrow.resolve("192.168.2.1")).isInstanceOf(RefusedHostException.class);
        assertThatThrownBy(() -> narrow.resolve("172.17.0.1")).isInstanceOf(RefusedHostException.class); // the docker bridge
        // a CIDR cannot widen it: a public range configured here still gets nothing through
        LanAddressGuard widened = new LanAddressGuard("8.8.8.0/24", InetAddress::getAllByName, a -> false);
        assertThatThrownBy(() -> widened.resolve("8.8.8.8")).isInstanceOf(RefusedHostException.class);
    }

    @Test
    void withoutAllowedCidrs_lightingIsOff_andNoAddressIsAccepted() {
        for (String unset : new String[] {"", "   ", ",", null}) {
            LanAddressGuard off = new LanAddressGuard(unset, InetAddress::getAllByName, a -> false);
            assertThat(off.isConfigured()).as("[" + unset + "]").isFalse();
            assertThatThrownBy(off::requireConfigured)
                    .isInstanceOf(com.sunsetbeach.error.ConflictException.class)
                    .hasMessage("Lighting is not configured on the server");
            // "any private address" would include the database container on the compose network
            for (String host : new String[] {"192.168.1.5", "10.0.0.5", "172.18.0.2"}) {
                assertThatThrownBy(() -> off.resolve(host)).as(host).isInstanceOf(RefusedHostException.class);
            }
        }
        assertThat(guard.isConfigured()).isTrue();
        guard.requireConfigured();
    }

    @Test
    void theComposeNetworkIsOutsideTheHotelLan() {
        LanAddressGuard hotel = new LanAddressGuard("192.168.1.0/24", InetAddress::getAllByName, a -> false);

        for (String dockerStyle : new String[] {"172.17.0.1", "172.18.0.2", "172.19.0.5", "172.20.0.3"}) {
            assertThatThrownBy(() -> hotel.resolve(dockerStyle)).as(dockerStyle).isInstanceOf(RefusedHostException.class);
        }
        assertThat(hotel.resolve("192.168.1.40")).isNotNull();
    }

    @Test
    void aMalformedCidr_stopsTheServerFromStarting() {
        assertThatThrownBy(() -> new LanAddressGuard("192.168.1.0/33", InetAddress::getAllByName, a -> false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new LanAddressGuard("not-a-cidr", InetAddress::getAllByName, a -> false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new LanAddressGuard("::1/128", InetAddress::getAllByName, a -> false)).isInstanceOf(IllegalStateException.class);
    }
}
