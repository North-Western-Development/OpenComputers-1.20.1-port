package li.cil.oc.util;

import com.google.common.net.InetAddresses;
import li.cil.oc.OpenComputers;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * One entry of the {@code internet.filteringRules} config list: {@code allow} or {@code deny},
 * followed by filters that all have to match (all, default, private, bogon, ipv4, ipv6,
 * ipv4-embedded-ipv6, ip:address[/prefix], domain:name). {@link #apply} returns the rule's
 * verdict if it matches, empty otherwise.
 */
public class InternetFilteringRule {
    public final String ruleString;
    private boolean invalid = false;
    private final Validator validator;

    @FunctionalInterface
    private interface Validator {
        Optional<Boolean> apply(InetAddress inetAddress, String host);
    }

    public InternetFilteringRule(String ruleString) {
        this.ruleString = ruleString;
        this.validator = createValidator();
    }

    private Validator createValidator() {
        try {
            final String[] ruleParts = ruleString.split(" ");
            switch (ruleParts[0]) {
                case "allow":
                case "deny": {
                    final boolean value = ruleParts[0].equals("allow");
                    final List<BiPredicate<InetAddress, String>> predicates = new ArrayList<>();
                    for (String f : Arrays.asList(ruleParts).subList(1, ruleParts.length)) {
                        final String[] filter = f.split(":", 2);
                        switch (filter[0]) {
                            case "default":
                                if (!value) {
                                    predicates.add((inetAddress, host) -> false);
                                } else {
                                    predicates.add((inetAddress, host) -> firstMatch(defaultRules(), inetAddress, host).orElse(false));
                                }
                                break;
                            case "private":
                                predicates.add((inetAddress, host) ->
                                        inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress() || inetAddress.isSiteLocalAddress());
                                break;
                            case "bogon":
                                predicates.add((inetAddress, host) -> {
                                    for (InetAddressRange range : bogonMatchingRules()) {
                                        if (range.matches(inetAddress)) return true;
                                    }
                                    return false;
                                });
                                break;
                            case "ipv4":
                                predicates.add((inetAddress, host) -> inetAddress instanceof Inet4Address);
                                break;
                            case "ipv6":
                                predicates.add((inetAddress, host) -> inetAddress instanceof Inet6Address);
                                break;
                            case "ipv4-embedded-ipv6":
                                predicates.add((inetAddress, host) -> inetAddress instanceof Inet6Address v6 && InetAddresses.hasEmbeddedIPv4ClientAddress(v6));
                                break;
                            case "domain": {
                                final String domain = filter[1];
                                final InetAddress[] addresses = InetAddress.getAllByName(domain);
                                predicates.add((inetAddress, host) -> {
                                    if (domain.equals(host)) return true;
                                    for (InetAddress a : addresses) {
                                        if (a.equals(inetAddress)) return true;
                                    }
                                    return false;
                                });
                                break;
                            }
                            case "ip": {
                                final String[] ipStringParts = filter[1].split("/", 2);
                                if (ipStringParts.length == 2) {
                                    final InetAddressRange ipRange = InetAddressRange.parse(ipStringParts[0], ipStringParts[1]);
                                    predicates.add((inetAddress, host) -> ipRange.matches(inetAddress));
                                } else {
                                    final InetAddress ipAddress = InetAddresses.forString(ipStringParts[0]);
                                    predicates.add((inetAddress, host) -> ipAddress.equals(inetAddress));
                                }
                                break;
                            }
                            case "all":
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown filter '" + filter[0] + "'.");
                        }
                    }
                    return (inetAddress, host) -> {
                        for (BiPredicate<InetAddress, String> p : predicates) {
                            if (!p.test(inetAddress, host)) return Optional.empty();
                        }
                        return Optional.of(value);
                    };
                }
                case "removeme":
                    // Ignore this rule.
                    return (inetAddress, host) -> Optional.empty();
                default:
                    throw new IllegalArgumentException("Unknown rule type '" + ruleParts[0] + "'.");
            }
        } catch (Throwable t) {
            OpenComputers.log.error("Invalid Internet filteringRules rule in configuration: \"" + ruleString + "\".", t);
            invalid = true;
            return (inetAddress, host) -> Optional.of(false);
        }
    }

    public boolean invalid() {
        return invalid;
    }

    public Optional<Boolean> apply(InetAddress inetAddress, String host) {
        return validator.apply(inetAddress, host);
    }

    /** The verdict of the first rule matching the address, if any. */
    public static Optional<Boolean> firstMatch(InternetFilteringRule[] rules, InetAddress inetAddress, String host) {
        for (InternetFilteringRule rule : rules) {
            final Optional<Boolean> result = rule.apply(inetAddress, host);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    // Lazily created: the default rules reference this class' constructor.
    private static InternetFilteringRule[] defaultRules;
    private static InetAddressRange[] bogonMatchingRules;

    private static synchronized InternetFilteringRule[] defaultRules() {
        if (defaultRules == null) {
            defaultRules = new InternetFilteringRule[]{
                    new InternetFilteringRule("deny private"),
                    new InternetFilteringRule("deny bogon"),
                    new InternetFilteringRule("allow all")
            };
        }
        return defaultRules;
    }

    private static synchronized InetAddressRange[] bogonMatchingRules() {
        if (bogonMatchingRules == null) {
            bogonMatchingRules = Arrays.stream(new String[]{
                    "0.0.0.0/8",
                    "10.0.0.0/8",
                    "100.64.0.0/10",
                    "127.0.0.0/8",
                    "169.254.0.0/16",
                    "172.16.0.0/12",
                    "192.0.0.0/24",
                    "192.0.2.0/24",
                    "192.168.0.0/16",
                    "198.18.0.0/15",
                    "198.51.100.0/24",
                    "203.0.113.0/24",
                    "224.0.0.0/3",
                    "::/128",
                    "::1/128",
                    "::ffff:0:0/96",
                    "::/96",
                    "64:ff9b::/96", // NAT64 well-known prefix (RFC 6052)
                    "100::/64",
                    "2001:10::/28",
                    "2001:db8::/32",
                    "fc00::/7",
                    "fe80::/10",
                    "fec0::/10",
                    "ff00::/8"
            }).map(s -> s.split("/", 2)).map(s -> InetAddressRange.parse(s[0], s[1])).toArray(InetAddressRange[]::new);
        }
        return bogonMatchingRules;
    }
}
