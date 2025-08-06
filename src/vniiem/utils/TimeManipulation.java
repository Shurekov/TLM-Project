package vniiem.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class TimeManipulation {
    
    private static final ZoneId UTC_ZONE = ZoneId.ofOffset("UTC", ZoneOffset.ofHours(0));

    public static double getUnixTimeUTC(LocalDateTime dt) {
        ZonedDateTime dtatz = dt.atZone(UTC_ZONE);
        return dtatz.toEpochSecond() + (dtatz.getNano() / 1000000000.0);
    }
    
    public static LocalDateTime fromUnixTimeUTC(double unix) {
        long secs = (long) unix;
        int nanos = (int) ((unix - secs) * 1_000_000_000);
        return LocalDateTime.ofEpochSecond(secs, nanos, ZoneOffset.UTC);
    }
}