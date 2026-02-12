package tytoo.grapheneui.internal.bridge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class GrapheneBridgeMessageCodecTest {
    @Test
    void parsePacketReturnsPacketForValidMessage() {
        GrapheneBridgeMessageCodec codec = new GrapheneBridgeMessageCodec();

        GrapheneBridgePacket packet = codec.parsePacket(
                """
                        {
                          "bridge":"graphene-ui",
                          "version":1,
                          "kind":"event",
                          "channel":"debug:event",
                          "payload":{"ok":true}
                        }
                        """
        );

        assertNotNull(packet);
        assertEquals(GrapheneBridgeProtocol.NAME, packet.bridge);
        assertEquals(GrapheneBridgeProtocol.KIND_EVENT, packet.kind);
        assertEquals("debug:event", packet.channel);
        assertNotNull(packet.payload);
        assertTrue(packet.payload.getAsJsonObject().get("ok").getAsBoolean());
    }

    @Test
    void parsePacketReturnsNullForBlankOrWrongBridge() {
        GrapheneBridgeMessageCodec codec = new GrapheneBridgeMessageCodec();

        assertNull(codec.parsePacket(""));
        assertNull(codec.parsePacket("   "));
        assertNull(codec.parsePacket("{\"bridge\":\"other\",\"kind\":\"event\"}"));
    }

    @Test
    void parsePayloadJsonThrowsForInvalidJson() {
        GrapheneBridgeMessageCodec codec = new GrapheneBridgeMessageCodec();

        assertThrows(IllegalArgumentException.class, () -> codec.parsePayloadJson("{"));
    }

    @Test
    void createSuccessResponseJsonContainsExpectedFields() {
        GrapheneBridgeMessageCodec codec = new GrapheneBridgeMessageCodec();
        JsonElement payload = codec.parsePayloadJson("{\"sum\":12}");

        String responseJson = codec.createSuccessResponseJson("id-1", "debug:sum", payload);
        JsonObject response = JsonParser.parseString(responseJson).getAsJsonObject();

        assertEquals(GrapheneBridgeProtocol.NAME, response.get("bridge").getAsString());
        assertEquals(GrapheneBridgeProtocol.KIND_RESPONSE, response.get("kind").getAsString());
        assertTrue(response.get("ok").getAsBoolean());
        assertEquals(12, response.getAsJsonObject("payload").get("sum").getAsInt());
    }
}
