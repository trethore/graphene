package io.github.trethore.graphene.internal.browser.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuAction;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuContext;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuItem;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class GrapheneContextMenuModelTest {
    @Test
    void cyclesOverEnabledCommandsAndCompletesSelection() {
        BrowserContextMenuItem.Command disabled = command(1, "Disabled", false);
        BrowserContextMenuItem.Command enabled = command(2, "Enabled", true);
        GrapheneContextMenuModel model = new GrapheneContextMenuModel(
                request(List.of(disabled, new BrowserContextMenuItem.Separator(), enabled)),
                String::length,
                10,
                10,
                200,
                200);

        model.keyPressed(GrapheneContextMenuModel.KeyAction.NEXT, Set.of());
        model.keyPressed(GrapheneContextMenuModel.KeyAction.SELECT, Set.of(BrowserModifier.SHIFT));

        BrowserContextMenuPresenter.Result result = model.completion().join();
        assertEquals(enabled.id(), result.selectedCommand().orElseThrow());
        assertEquals(Set.of(BrowserModifier.SHIFT), result.modifiers());
    }

    @Test
    void clampsPlacementAndCancelsClicksOutsideTheMenu() {
        GrapheneContextMenuModel model = new GrapheneContextMenuModel(
                request(List.of(command(1, "Command", true))), String::length, 500, 500, 100, 100);

        assertTrue(model.x() + model.width() <= 98);
        assertTrue(model.y() + model.height() <= 98);

        model.click(0, 0, true, Set.of());

        assertTrue(model.completion().join().selectedCommand().isEmpty());
    }

    private static BrowserContextMenuPresenter.Request request(List<BrowserContextMenuItem> items) {
        BrowserSession session = (BrowserSession) Proxy.newProxyInstance(
                BrowserSession.class.getClassLoader(),
                new Class<?>[] {BrowserSession.class},
                GrapheneContextMenuModelTest::invoke);
        BrowserContextMenuContext context = new BrowserContextMenuContext(
                session,
                new BrowserContextMenuContext.Position(0, 0),
                Set.of(),
                new BrowserContextMenuContext.Document("", "", ""),
                new BrowserContextMenuContext.Target("", "", false),
                new BrowserContextMenuContext.Media(BrowserContextMenuContext.MediaType.NONE, Set.of()),
                "",
                new BrowserContextMenuContext.Editing(false, Set.of(), "", List.of()));
        return new BrowserContextMenuPresenter.Request(context, items);
    }

    private static BrowserContextMenuItem.Command command(long id, String label, boolean enabled) {
        return new BrowserContextMenuItem.Command(
                new BrowserContextMenuItem.CommandId(id), BrowserContextMenuAction.OTHER, label, enabled, false);
    }

    private static Object invoke(Object proxy, Method method, Object[] arguments) {
        return switch (method.getName()) {
            case "equals" -> proxy == Objects.requireNonNull(arguments)[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "BrowserSessionProxy";
            default -> defaultValue(method.getReturnType());
        };
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == double.class) {
            return 0.0;
        }
        return null;
    }
}
