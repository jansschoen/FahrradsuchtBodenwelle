package de.fahrradsucht.bodenwelle;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class OsmTags {
    private OsmTags() {}
    public record Result(String traffic, String surface, boolean uncertain) {}
    private static final Pattern CYCLE_SIGN = Pattern.compile("(?i)(?:^|[;,])\\s*DE:(?:237|241)(?:[;,]|$)");
    private static final Pattern SHARED_SIGN = Pattern.compile("(?i)(?:^|[;,])\\s*DE:240(?:[;,]|$)");
    private static final Set<String> ROADS = set("primary", "primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link", "residential", "unclassified", "service", "living_street");
    private static Set<String> set(String... values) { return new java.util.HashSet<>(java.util.Arrays.asList(values)); }
    public static Result classify(Map<String, String> tags) {
        String highway = tags.getOrDefault("highway", "");
        String bicycle = tags.getOrDefault("bicycle", "");
        String foot = tags.getOrDefault("foot", "");
        String sign = tags.getOrDefault("traffic_sign", "");
        boolean designated = "designated".equals(bicycle), pedestrians = "designated".equals(foot);
        boolean cycleSign = CYCLE_SIGN.matcher(sign).find(), sharedSign = SHARED_SIGN.matcher(sign).find();
        String traffic = "unknown"; boolean uncertain = false;
        if ((cycleSign || sharedSign || "yes".equals(tags.get("bicycle_road"))) && "no".equals(bicycle)) uncertain = true;
        else if (cycleSign && sharedSign) uncertain = true;
        else if (sharedSign && "yes".equals(tags.get("segregated"))) uncertain = true;
        else if ((cycleSign || sharedSign) && ROADS.contains(highway)) uncertain = true; // Sign may refer to a parallel path.
        else if (sharedSign) traffic = "shared_path";
        else if (cycleSign && set("path", "cycleway", "footway").contains(highway)) traffic = "mandatory_cycleway";
        else if ("yes".equals(tags.get("bicycle_road")) && ROADS.contains(highway)) traffic = "bicycle_road";
        else if (set("path", "cycleway", "footway").contains(highway) && designated && pedestrians) {
            if ("no".equals(tags.get("segregated"))) traffic = "shared_path";
            else if ("yes".equals(tags.get("segregated"))) traffic = "cycleway";
            else uncertain = true;
        } else if ("cycleway".equals(highway) && !"no".equals(bicycle) && !"yes".equals(foot)) traffic = "cycleway";
        else if ("path".equals(highway) && designated && !"yes".equals(foot) && !pedestrians) traffic = "cycleway";
        else if (ROADS.contains(highway)) {
            boolean parallel = tags.entrySet().stream().anyMatch(e -> e.getKey().startsWith("cycleway")
                    && !set("no", "none").contains(e.getValue()));
            if (parallel) uncertain = true; else traffic = "carriageway";
        } else if (set("track", "pedestrian", "footway", "steps").contains(highway)) traffic = "other";
        String surface = switch (tags.getOrDefault("surface", "")) {
            case "asphalt" -> "asphalt";
            case "concrete", "concrete:plates", "concrete:lanes" -> "concrete";
            case "paving_stones" -> "concrete".equals(tags.get("material")) || "concrete".equals(tags.get("surface:material")) ? "concrete_paving" : "other";
            case "sett", "cobblestone", "unhewn_cobblestone", "cobblestone:flattened" -> "stone_paving";
            case "compacted", "fine_gravel" -> "compacted";
            case "gravel", "pebblestone" -> "gravel";
            case "ground", "dirt", "earth", "grass", "sand", "mud", "unpaved" -> "unpaved";
            case "", "paved" -> "unknown";
            default -> "other";
        };
        return new Result(traffic, surface, uncertain);
    }
}
