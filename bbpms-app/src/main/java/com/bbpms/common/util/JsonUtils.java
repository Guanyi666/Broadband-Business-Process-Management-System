package com.bbpms.common.util;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Collections;
import java.util.List;
public final class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private JsonUtils() {}
    public static String toJson(Object o) { try { return MAPPER.writeValueAsString(o); } catch (Exception e) { throw new IllegalStateException(e); } }
    public static <T> T parse(String j, Class<T> t) { try { return MAPPER.readValue(j, t); } catch (Exception e) { throw new IllegalStateException(e); } }
    public static <T> List<T> parseList(String j, Class<T> t) { if (j == null || j.isBlank()) return Collections.emptyList(); try { return MAPPER.readValue(j, MAPPER.getTypeFactory().constructCollectionType(List.class, t)); } catch (Exception e) { throw new IllegalStateException(e); } }
}
