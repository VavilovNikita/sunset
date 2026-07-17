package com.sunsetbeach.config;

import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * org.openapitools:jackson-databind-nullable only ships a Jackson 2 (com.fasterxml.jackson.*)
 * module. Spring Boot 4's Jackson integration is Jackson 3 (tools.jackson.*), a different,
 * source-incompatible artifact family, so that module is silently never invoked - without this,
 * every JsonNullable<T> field (Booking.paymentNote, AvailabilityDay.source, ...) serializes as
 * the wrapper object's own bean properties (`{"present":true}`) instead of unwrapping to the
 * value it holds. This re-implements just the (de)serialization behavior against the Jackson 3 API.
 */
public final class JsonNullableJackson3Module extends SimpleModule {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public JsonNullableJackson3Module() {
        addSerializer((Class) JsonNullable.class, new JsonNullableSerializer());
        addDeserializer((Class) JsonNullable.class, new JsonNullableDeserializer(null));
    }

    private static final class JsonNullableSerializer extends ValueSerializer<JsonNullable<?>> {
        @Override
        public void serialize(JsonNullable<?> value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            Object wrapped = value.isPresent() ? value.get() : null;
            if (wrapped == null) {
                gen.writeNull();
            } else {
                ctxt.writeValue(gen, wrapped);
            }
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, JsonNullable<?> value) {
            return value == null || !value.isPresent();
        }

        @Override
        public Class<?> handledType() {
            return JsonNullable.class;
        }
    }

    private static final class JsonNullableDeserializer extends ValueDeserializer<JsonNullable<?>> {
        private final JavaType wrappedType;

        JsonNullableDeserializer(JavaType wrappedType) {
            this.wrappedType = wrappedType;
        }

        @Override
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            JavaType type = property != null ? property.getType() : ctxt.getContextualType();
            JavaType contained = type != null && type.containedTypeCount() > 0 ? type.containedType(0) : null;
            return new JsonNullableDeserializer(contained);
        }

        @Override
        public JsonNullable<?> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            Object value = wrappedType != null ? ctxt.readValue(p, wrappedType) : ctxt.readValue(p, Object.class);
            return JsonNullable.of(value);
        }

        @Override
        public JsonNullable<?> getNullValue(DeserializationContext ctxt) {
            return JsonNullable.of(null);
        }
    }
}
