package io.github.trethore.graphene.internal.resource;

import java.util.Locale;

public final class GrapheneByteRange {
    private static final String BYTE_UNIT_PREFIX = "bytes=";

    private GrapheneByteRange() {}

    public static Resolution resolve(String rangeHeader, int resourceLength) {
        if (resourceLength < 0) {
            throw new IllegalArgumentException("resourceLength must not be negative");
        }
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return Resolution.full(resourceLength);
        }

        String normalizedHeader = rangeHeader.trim().toLowerCase(Locale.ROOT);
        if (!normalizedHeader.startsWith(BYTE_UNIT_PREFIX) || normalizedHeader.indexOf(',') >= 0) {
            return Resolution.full(resourceLength);
        }

        String rangeSpec = normalizedHeader.substring(BYTE_UNIT_PREFIX.length()).trim();
        int separatorIndex = rangeSpec.indexOf('-');
        if (separatorIndex < 0 || separatorIndex != rangeSpec.lastIndexOf('-')) {
            return Resolution.full(resourceLength);
        }

        String startText = rangeSpec.substring(0, separatorIndex).trim();
        String endText = rangeSpec.substring(separatorIndex + 1).trim();
        if (startText.isEmpty()) {
            return resolveSuffixRange(endText, resourceLength);
        }
        return resolvePositionRange(startText, endText, resourceLength);
    }

    private static Resolution resolveSuffixRange(String suffixText, int resourceLength) {
        Long suffixLength = parseNonNegativeLong(suffixText);
        if (suffixLength == null) {
            return Resolution.full(resourceLength);
        }
        if (suffixLength == 0 || resourceLength == 0) {
            return Resolution.unsatisfiable(resourceLength);
        }

        int selectedLength = (int) Math.min(suffixLength, resourceLength);
        return Resolution.partial(resourceLength - selectedLength, resourceLength, resourceLength);
    }

    private static Resolution resolvePositionRange(String startText, String endText, int resourceLength) {
        Long requestedStart = parseNonNegativeLong(startText);
        Long requestedEnd = endText.isEmpty() ? null : parseNonNegativeLong(endText);
        if (requestedStart == null || (!endText.isEmpty() && requestedEnd == null)) {
            return Resolution.full(resourceLength);
        }
        if (requestedEnd != null && requestedEnd < requestedStart) {
            return Resolution.full(resourceLength);
        }
        if (requestedStart >= resourceLength) {
            return Resolution.unsatisfiable(resourceLength);
        }

        long inclusiveEnd = requestedEnd == null ? resourceLength - 1L : Math.min(requestedEnd, resourceLength - 1L);
        return Resolution.partial((int) (long) requestedStart, (int) inclusiveEnd + 1, resourceLength);
    }

    private static Long parseNonNegativeLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            long parsedValue = Long.parseLong(value);
            return parsedValue < 0 ? null : parsedValue;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public enum Status {
        FULL,
        PARTIAL,
        UNSATISFIABLE
    }

    public record Resolution(Status status, int startInclusive, int endExclusive, int resourceLength) {
        public Resolution {
            if (status == null) {
                throw new NullPointerException("status");
            }
            if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > resourceLength) {
                throw new IllegalArgumentException("Invalid byte range resolution");
            }
        }

        private static Resolution full(int resourceLength) {
            return new Resolution(Status.FULL, 0, resourceLength, resourceLength);
        }

        private static Resolution partial(int startInclusive, int endExclusive, int resourceLength) {
            return new Resolution(Status.PARTIAL, startInclusive, endExclusive, resourceLength);
        }

        private static Resolution unsatisfiable(int resourceLength) {
            return new Resolution(Status.UNSATISFIABLE, 0, 0, resourceLength);
        }

        public int responseLength() {
            return endExclusive - startInclusive;
        }

        public String contentRange() {
            if (status == Status.PARTIAL) {
                return "bytes " + startInclusive + "-" + (endExclusive - 1) + "/" + resourceLength;
            }
            if (status == Status.UNSATISFIABLE) {
                return "bytes */" + resourceLength;
            }
            return "";
        }
    }
}
