package tytoo.grapheneuidebug.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import tytoo.grapheneuidebug.test.GrapheneDebugTestRunner;

public final class GrapheneDebugCommands {
    private static boolean registered;

    private GrapheneDebugCommands() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        ClientCommandRegistrationCallback.EVENT.register(GrapheneDebugCommands::registerCommands);
        registered = true;
    }

    private static void registerCommands(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandBuildContext ignoredRegistryAccess
    ) {
        dispatcher.register(
                ClientCommandManager.literal("graphene")
                        .then(
                                ClientCommandManager.literal("test")
                                        .executes(context -> executeGrapheneTests(context.getSource()))
                        )
        );
    }

    private static int executeGrapheneTests(FabricClientCommandSource source) {
        source.sendFeedback(Component.translatable("command.graphene-ui-debug.test.started"));
        GrapheneDebugTestRunner.run(source);
        return 1;
    }
}
