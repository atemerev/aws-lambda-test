package com.temerev.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

/**
 * An algorithm that takes an arbitrary array of arrays with numbers and flattens it into a single array.
 * Deployable to AWS Lambda.
 *
 * Takes the input as application/json, e.g.:
 * {"input":[1,[2,3,[4]],[5],6,[7,[8]]]}
 * And returns output as application/json, e.g.:
 * {"output": [1,2,3,4,5,6,7,8]}
 */
public class Flattener {

    public static final String INPUT_KEY = "input";
    public static final String OUTPUT_KEY = "output";
    public static final Charset ENCODING = StandardCharsets.UTF_8;

    /**
     * Handles AWS lambda request for flattening JSON streams.
     *
     * @param in  Input stream, containing JSON in {"input": [...]} format, with nested number arrays inside.
     * @param out Output stream,
     * @param ctx AWS Lambda context. Can be null (if null, no error logging is performed, but parsing
     *            exceptions are thrown instead — useful for unit testing).
     */
    public void handler(@NotNull InputStream in, @NotNull OutputStream out, @Nullable Context ctx) {
        JsonParser parser = Json.createParser(in);
        JsonGenerator generator = Json.createGenerator(out);
        try {
            parse(parser, generator, -1);
        } catch (ParseException e) {
            if (ctx != null) {
                ctx.getLogger().log(String.format("%d: %s", e.getErrorOffset(), e.getMessage()));
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * JSON nested input flattener accepting and emitting JSON objects as strings (useful for testing).
     *
     * @param jsonString Input JSON as string, in {"input": [...]} format.
     * @return Output JSON as String, in {"output": [...]} format.
     */
    public String flatten(String jsonString) throws ParseException {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            handler(new ByteArrayInputStream(jsonString.getBytes()), bs, null);
            return new String(bs.toByteArray(), ENCODING);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof ParseException) {
                throw (ParseException) cause;
            } else {
                throw e;
            }
        }
    }

    /**
     * Follows JsonParser's parsed tokens and descends to nested number arrays in the "input" property,
     * automatically constructing output JSON in JSON generator in progress.
     *
     * @param parser    JsonParser instance (can be in intermediate state).
     * @param generator JsonGenerator to use for constructing output.
     * @param level     Array nesting level in the input key. Starts at -1 (outside the property), goes
     *                  to 0 (input key reached), when inside the array it is >= 1.
     * @return JsonGenerator a JsonGenerator instance when parsing is finished
     * @throws ParseException If something else than arrays and numbers is contained in the "input" property,
     *                        or if JSON document is not well-formed.
     */
    private JsonGenerator parse(JsonParser parser, JsonGenerator generator, int level) throws ParseException {
        if (parser.hasNext()) {
            JsonParser.Event event;
            try {
                event = parser.next();
            } catch (JsonParsingException e) {
                throw new ParseException(e.getMessage(), (int) e.getLocation().getStreamOffset());
            }
            if (level < 0 && event == JsonParser.Event.KEY_NAME && INPUT_KEY.equals(parser.getString())) {
                generator.writeStartObject();
                generator.writeStartArray(OUTPUT_KEY);
                generator.flush();
                return parse(parser, generator, 0);
            } else if (level >= 0 && event == JsonParser.Event.START_ARRAY) {
                return parse(parser, generator, level + 1);
            } else if (level > 0 && event == JsonParser.Event.END_ARRAY) {
                if (level == 1) {
                    generator.writeEnd();
                    generator.writeEnd();
                    generator.flush();
                    generator.close();
                    return generator;
                } else {
                    return parse(parser, generator, level - 1);
                }
            } else if (level > 0 && event == JsonParser.Event.VALUE_NUMBER) {
                generator.write(parser.getBigDecimal());
                generator.flush();
                return parse(parser, generator, level);
            } else if (level > 0) {
                throw new ParseException(String.format("Only nested numbers array is allowed in input, got %s instead", event.toString()), (int) parser.getLocation().getStreamOffset());
            } else {
                return parse(parser, generator, level);
            }
        } else {
            throw new ParseException("JSON stream is not well-formed", (int) parser.getLocation().getStreamOffset());
        }
    }
}