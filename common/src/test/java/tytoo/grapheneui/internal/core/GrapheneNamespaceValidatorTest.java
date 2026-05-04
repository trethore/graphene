package tytoo.grapheneui.internal.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrapheneNamespaceValidatorTest {
    @Test
    void trimsValidNamespace() {
        assertEquals("my.mod-id_1", GrapheneNamespaceValidator.normalizeNamespace(" my.mod-id_1 ", "modId"));
    }

    @Test
    void rejectsBlankNamespace() {
        assertThrows(IllegalArgumentException.class, () -> GrapheneNamespaceValidator.normalizeNamespace(" ", "modId"));
    }

    @Test
    void rejectsUppercaseNamespace() {
        assertThrows(IllegalArgumentException.class, () -> GrapheneNamespaceValidator.normalizeNamespace("MyMod", "modId"));
    }
}
