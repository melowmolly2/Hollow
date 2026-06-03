package service.auction;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class LocalBidHistoryStore {
    private static final int MAX_POINTS_PER_ITEM = 100;
    private static final String KEY_PREFIX = "bidHistory.";
    private static final Gson GSON = new Gson();
    private static final Type POINT_LIST_TYPE = new TypeToken<List<BidPoint>>() {
    }.getType();

    private final Preferences preferences = Preferences.userNodeForPackage(LocalBidHistoryStore.class);

    public List<BidPoint> getPoints(Long itemId) {
        if (itemId == null) {
            return List.of();
        }

        String json = preferences.get(key(itemId), "[]");
        try {
            List<BidPoint> points = GSON.fromJson(json, POINT_LIST_TYPE);
            if (points == null) {
                return List.of();
            }

            return points.stream()
                    .filter(point -> point != null && point.amount != null && point.time != null)
                    .sorted(Comparator.comparingLong(point -> point.time))
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    public void addPoint(Long itemId, Double amount, Long time) {
        addPoint(itemId, amount, time, false, false);
    }

    public void addBidPoint(Long itemId, Double amount, Long time, boolean ownBid) {
        addPoint(itemId, amount, time, true, ownBid);
    }

    private void addPoint(Long itemId, Double amount, Long time, boolean bidRecord, boolean ownBid) {
        if (itemId == null || amount == null || time == null) {
            return;
        }

        List<BidPoint> points = new ArrayList<>(getPoints(itemId));
        points.add(new BidPoint(amount, time, bidRecord, ownBid));
        savePoints(itemId, points);
    }

    public void addPoints(Long itemId, List<BidPoint> newPoints) {
        if (itemId == null || newPoints == null || newPoints.isEmpty()) {
            return;
        }

        List<BidPoint> points = new ArrayList<>(getPoints(itemId));
        points.addAll(newPoints);
        savePoints(itemId, points);
    }

    private void savePoints(Long itemId, List<BidPoint> points) {
        Map<String, BidPoint> uniquePoints = new LinkedHashMap<>();
        points.stream()
                .filter(point -> point != null && point.amount != null && point.time != null)
                .sorted(Comparator.comparingLong(point -> point.time))
                .forEach(point -> mergePoint(uniquePoints, point));

        List<BidPoint> compacted = uniquePoints.values().stream()
                .skip(Math.max(0, uniquePoints.size() - MAX_POINTS_PER_ITEM))
                .toList();

        preferences.put(key(itemId), GSON.toJson(compacted));
    }

    private String key(Long itemId) {
        return KEY_PREFIX + itemId;
    }

    private void mergePoint(Map<String, BidPoint> uniquePoints, BidPoint point) {
        String key = point.time + ":" + point.amount;
        BidPoint existing = uniquePoints.get(key);
        if (existing == null) {
            uniquePoints.put(key, point);
            return;
        }

        existing.bidRecord = existing.bidRecord || point.bidRecord;
        existing.ownBid = existing.ownBid || point.ownBid;
    }

    public static class BidPoint {
        public Double amount;
        public Long time;
        public boolean bidRecord;
        public boolean ownBid;

        public BidPoint(Double amount, Long time) {
            this(amount, time, false, false);
        }

        public BidPoint(Double amount, Long time, boolean bidRecord, boolean ownBid) {
            this.amount = amount;
            this.time = time;
            this.bidRecord = bidRecord;
            this.ownBid = ownBid;
        }
    }
}
