import com.temerev.lambda.Flattener;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.text.ParseException;

public class FlattenerTest {

    private final Flattener flattener = new Flattener();

    /**
     * A simplest test with an empty input array
     */
    @Test
    public void smokeTest() {
        try {
            String simplest = "{\"input\": []}";
            String result = flattener.flatten(simplest);
            JsonReader reader = Json.createReader(new StringReader(result));
            JsonObject obj = reader.readObject();
            assertTrue("No output key: " + result, obj.containsKey(Flattener.OUTPUT_KEY));
            JsonArray array = obj.getJsonArray("output");
            assertTrue("Non-empty output array for empty input array: " + result, array.isEmpty());
        } catch (ParseException e) {
            fail(e.getMessage() + " at offset " + e.getErrorOffset());
        }
    }

    /**
     * No input key
     */
    @Test
    public void wrongFormatTest() {
        try {
            String noinput = "{\"foo\": \"bar\"}";
            flattener.flatten(noinput);
            fail("Parsing succeeded, should give exception instead");
        } catch (ParseException e) {
            // ok
        }
    }

    /**
     * Unbalanced parentheses
     */
    @Test
    public void testUnbalanced() {
        try {
            String noinput = "{\"input\": \"[1,2,[3],[]]]\"}";
            flattener.flatten(noinput);
            fail("Parsing succeeded, should give exception instead");
        } catch (ParseException e) {
            // ok
        }
    }

    /**
     * Object instead of array
     */
    @Test
    public void testObjectInsteadOfArray() {
        try {
            String noinput = "{\"input\": {}}";
            flattener.flatten(noinput);
            fail("Parsing succeeded, should give exception instead");
        } catch (ParseException e) {
            // ok
        }
    }

    /**
     * Nested empty arrays
     */
    @Test
    public void nestedEmptyTest() {
        try {
            String nested = "{\"input\": [[[[[[ ]]]]]] }";
            String result = flattener.flatten(nested);
            JsonReader reader = Json.createReader(new StringReader(result));
            JsonObject obj = reader.readObject();
            assertTrue("No output key: " + result, obj.containsKey(Flattener.OUTPUT_KEY));
            JsonArray array = obj.getJsonArray("output");
            assertTrue("Non-empty output array for empty nested input array: " + result, array.isEmpty());
        } catch (ParseException e) {
            fail(e.getMessage() + " at offset " + e.getErrorOffset());
        }
    }

    /**
     * A test with already flattened array
     */
    @Test
    public void flatTest() {
        try {
            String flat = "{\"input\": [-1,2,3,4,5,6.0,7,8]}";
            String result = flattener.flatten(flat);
            JsonReader reader = Json.createReader(new StringReader(result));
            JsonObject obj = reader.readObject();
            assertTrue("No output key: " + result, obj.containsKey(Flattener.OUTPUT_KEY));
            JsonArray array = obj.getJsonArray("output");
            assertEquals("Wrong numbers count: " + array.size(), 8, array.size());
            assertEquals(-1, array.getInt(0));
            assertEquals(6, array.getInt(5));
            assertEquals(8, array.getInt(array.size() - 1));
        } catch (ParseException e) {
            fail(e.getMessage() + " at offset " + e.getErrorOffset());
        }
    }


    /**
     * A test with non-flat
     */
    @Test
    public void nonFlatTest() {
        try {
            String nonFlat = "{\"input\": [-1,2,[3,4],5.0,[6,[7,8,[],9]]]]}";
            String result = flattener.flatten(nonFlat);
            JsonReader reader = Json.createReader(new StringReader(result));
            JsonObject obj = reader.readObject();
            assertTrue("No output key: " + result, obj.containsKey(Flattener.OUTPUT_KEY));
            JsonArray array = obj.getJsonArray("output");
            assertEquals("Wrong numbers count: " + array.size(), 9, array.size());
            assertEquals(-1, array.getInt(0));
            assertEquals(6, array.getInt(5));
            assertEquals(9, array.getInt(array.size() - 1));
        } catch (ParseException e) {
            fail(e.getMessage() + " at offset " + e.getErrorOffset());
        }
    }

}
