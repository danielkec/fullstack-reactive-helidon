package io.helidon.fs.reactive;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.adapter.JsonbAdapter;

public class ThrowableAdapter implements JsonbAdapter<Throwable, JsonObject> {


    @Override
    public JsonObject adaptToJson(final Throwable t) throws Exception {
        JsonObjectBuilder json = Json.createObjectBuilder();
        if (t == null) {
            return json.build();
        }
        json.add("message", t.getMessage());
        json.add("localizedMessage", t.getLocalizedMessage());
        json.add("cause", adaptToJson(t.getCause()));
        json.add("stackTrace", Arrays.stream(t.getStackTrace())
                .map(this::stackTraceElementToJson)
                .collect((Supplier<JsonArrayBuilder>) Json::createArrayBuilder, JsonArrayBuilder::add, (ab, ab2) -> {
                }));
        return json.build();
    }

    @Override
    public Throwable adaptFromJson(final JsonObject jsonObject) throws Exception {
        Throwable t = new Throwable(jsonObject.getString("message", null));
        Optional.ofNullable(jsonObject.getJsonArray("stackTrace"))
                .ifPresent(jo -> t.setStackTrace(
                        jo.stream()
                                .map(jv -> stackTraceElementFromJson((JsonObject) jv))
                                .toArray(StackTraceElement[]::new)));
        return t;
    }

    public JsonObject stackTraceElementToJson(final StackTraceElement traceElement) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        HashMap<String, String> nullableProps = new HashMap<>();
        nullableProps.put("classLoaderName", traceElement.getClassLoaderName());
        nullableProps.put("className", traceElement.getClassName());
        nullableProps.put("fileName", traceElement.getFileName());
        nullableProps.put("methodName", traceElement.getMethodName());
        nullableProps.put("moduleName", traceElement.getModuleName());
        nullableProps.put("moduleVersion", traceElement.getModuleVersion());

        nullableProps
                .entrySet()
                .stream()
                .filter(e -> e.getValue() != null)
                .forEach(e -> json.add(e.getKey(), e.getValue()));

        return json
                .add("lineNumber", traceElement.getLineNumber())
                .add("nativeMethod", traceElement.isNativeMethod())
                .build();
    }

    public StackTraceElement stackTraceElementFromJson(final JsonObject jsonObject) {
        return new StackTraceElement(
                jsonObject.getString("classLoaderName", null),
                jsonObject.getString("moduleName", null),
                jsonObject.getString("moduleVersion", null),
                jsonObject.getString("className", null),
                jsonObject.getString("methodName", null),
                jsonObject.getString("fileName"),
                jsonObject.getInt("lineNumber")
        );
    }

}
