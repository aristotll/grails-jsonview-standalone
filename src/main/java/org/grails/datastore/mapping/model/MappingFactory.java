package org.grails.datastore.mapping.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

public class MappingFactory {
    public static final Set<String> SIMPLE_TYPES;

    static {
        SIMPLE_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                boolean.class.getName(),
                long.class.getName(),
                short.class.getName(),
                int.class.getName(),
                byte.class.getName(),
                float.class.getName(),
                double.class.getName(),
                char.class.getName(),
                Boolean.class.getName(),
                Long.class.getName(),
                Short.class.getName(),
                Integer.class.getName(),
                Byte.class.getName(),
                Float.class.getName(),
                Double.class.getName(),
                Character.class.getName(),
                String.class.getName(),
                java.util.Date.class.getName(),
                Time.class.getName(),
                Timestamp.class.getName(),
                java.sql.Date.class.getName(),
                BigDecimal.class.getName(),
                BigInteger.class.getName(),
                Locale.class.getName(),
                Calendar.class.getName(),
                GregorianCalendar.class.getName(),
                java.util.Currency.class.getName(),
                TimeZone.class.getName(),
                Object.class.getName(),
                Class.class.getName(),
                byte[].class.getName(),
                Byte[].class.getName(),
                char[].class.getName(),
                Character[].class.getName(),
                Blob.class.getName(),
                Clob.class.getName(),
                Serializable.class.getName(),
                URI.class.getName(),
                URL.class.getName(),
                UUID.class.getName(),
                "org.bson.types.ObjectId",
                "java.time.Instant",
                "java.time.LocalDateTime",
                "java.time.LocalDate",
                "java.time.LocalTime",
                "java.time.OffsetDateTime",
                "java.time.OffsetTime",
                "java.time.ZonedDateTime")));
    }

    public static boolean isSimpleType(final String typeName) {
        return SIMPLE_TYPES.contains(typeName);
    }
}
