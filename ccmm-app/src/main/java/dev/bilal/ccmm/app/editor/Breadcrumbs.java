package dev.bilal.ccmm.app.editor;

import java.util.ArrayList;
import java.util.List;

/** Pure breadcrumb segments for a project-relative path and optional method. */
public final class Breadcrumbs {
    public static final String SEPARATOR = " > ";

    public record Segment(String label, String relativePath) {
        public Segment {
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("label is blank");
            }
        }

        /** True for path segments that map to the explorer; false for the method trailing segment. */
        public boolean pathSegment() {
            return relativePath != null && !relativePath.isBlank();
        }
    }

    private Breadcrumbs() {}

    /**
     * Path segments plus optional method name. Example:
     * {@code src/app/Foo.java} + {@code parse} → {@code src > app > Foo.java > parse}.
     */
    public static List<Segment> segments(String relativePath, String methodName) {
        List<Segment> out = new ArrayList<>();
        if (relativePath != null && !relativePath.isBlank()) {
            String normalized = relativePath.replace('\\', '/');
            String[] parts = normalized.split("/");
            StringBuilder prefix = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                if (prefix.length() > 0) {
                    prefix.append('/');
                }
                prefix.append(part);
                out.add(new Segment(part, prefix.toString()));
            }
        }
        if (methodName != null && !methodName.isBlank()) {
            out.add(new Segment(methodName, null));
        }
        return List.copyOf(out);
    }

    public static String join(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (Segment segment : segments) {
            if (text.length() > 0) {
                text.append(SEPARATOR);
            }
            text.append(segment.label());
        }
        return text.toString();
    }
}
