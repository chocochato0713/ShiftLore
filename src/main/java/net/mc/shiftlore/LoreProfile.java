package net.mc.shiftlore;

import java.util.List;

public record LoreProfile(String id, List<String> shortLines, List<String> detailLines) {

    public List<String> linesFor(boolean expanded) {
        if (!expanded || detailLines.isEmpty()) {
            return shortLines;
        }
        return java.util.stream.Stream.concat(shortLines.stream(), detailLines.stream()).toList();
    }
}
