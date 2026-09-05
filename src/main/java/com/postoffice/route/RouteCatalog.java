package com.postoffice.route;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hop names for User1 / User2 routes. Replace the lists below with the
 * five office names you want to use; the daily 06:00 job advances one hop.
 */
@Component
public class RouteCatalog {

    public static final List<String> DEFAULT_ROUTE = List.of(
            "Origin Hub",
            "Regional Sort",
            "Transit Hub",
            "Destination City",
            "Local Delivery"
    );

    // Nutan -> Debasish
    private static final List<String> NUTAN_TO_DEBASISH = List.of(
            "Angul",
            "Node 2",
            "Node 3",
            "Balasore",
            "Remuna"
    );

    // Debasish -> Nutan
    private static final List<String> DEBASISH_TO_NUTAN = List.of(
            "Remuna",
            "Balasore",
            "Node 3",
            "Node 2",
            "Angul"
    );

    private static final Map<String, List<String>> NAMED_ROUTES = Map.of(
            key("nutan", "debasish"), DEFAULT_ROUTE,
            key("debasish", "nutan"), DEFAULT_ROUTE
    );

    public List<String> nodesFor(String senderName, String receiverName) {
        List<String> named = NAMED_ROUTES.get(key(senderName, receiverName));
        if (named != null) {
            return new ArrayList<>(named);
        }
        return new ArrayList<>(DEFAULT_ROUTE);
    }

    public String firstNode(String senderName, String receiverName) {
        return nodesFor(senderName, receiverName).get(0);
    }

    public String lastNode(String senderName, String receiverName) {
        List<String> nodes = nodesFor(senderName, receiverName);
        return nodes.get(nodes.size() - 1);
    }

    public int indexOf(String senderName, String receiverName, String currentNode) {
        List<String> nodes = nodesFor(senderName, receiverName);
        int idx = nodes.indexOf(currentNode);
        return Math.max(idx, 0);
    }

    public String nextNode(String senderName, String receiverName, String currentNode) {
        List<String> nodes = nodesFor(senderName, receiverName);
        int idx = nodes.indexOf(currentNode);
        if (idx < 0) {
            return nodes.get(0);
        }
        if (idx >= nodes.size() - 1) {
            return nodes.get(nodes.size() - 1);
        }
        return nodes.get(idx + 1);
    }

    public boolean isLastNode(String senderName, String receiverName, String currentNode) {
        return lastNode(senderName, receiverName).equals(currentNode);
    }

    private static String key(String a, String b) {
        return normalize(a) + "->" + normalize(b);
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
