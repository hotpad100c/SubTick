package subtick.mixins;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.commands.TickCommand;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import subtick.ITickHandler;
import subtick.Settings;
import subtick.TickPhase;
import subtick.util.Translations;

@Debug(export = true)
@Mixin(value = TickCommand.class, priority = 990)
public class TickCommandMixin
{
    @ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;register(Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;)Lcom/mojang/brigadier/tree/LiteralCommandNode;"), index = 0)
    private static LiteralArgumentBuilder<CommandSourceStack> registerSubtickCommands(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.literal("step").then(Commands.argument("time", TimeArgument.time(1))
                .then(Commands.argument("phase", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(TickPhase.commandSuggestions, b))
                        .executes((c) -> ITickHandler.get(c).step(c.getSource(), IntegerArgumentType.getInteger(c, "time"), TickPhase.byCommandKey(StringArgumentType.getString(c, "phase"))))
                )));
        builder.then(Commands.literal("vanilla")
                .then(Commands.literal("freeze")
                        .executes((c) -> subtick$setVanillaFreeze(c.getSource(), true)))
                .then(Commands.literal("unfreeze")
                        .executes((c) -> subtick$setVanillaFreeze(c.getSource(), false))));
        return builder;
    }

    @Unique
    private static int subtick$setVanillaFreeze(CommandSourceStack source, boolean freeze) {
        ServerTickRateManager manager = source.getServer().tickRateManager();

        if (freeze) {
            if (ITickHandler.get(source).frozen()) {
                source.sendFailure(Component.literal(Translations.tr("subtick.error.subtick_frozen")));
                return 0;
            }
            if (manager.isFrozen()) {
                source.sendFailure(Component.translatable("commands.tick.status.frozen"));
                return 0;
            }
            manager.setFrozen(true);
            source.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), true);
            return 1;
        } else {
            int result = 0;
            if (manager.isFrozen()) {
                manager.setFrozen(false);
                source.sendSuccess(() -> Component.translatable("commands.tick.status.running"), true);
                result = 1;
            }
            if (ITickHandler.get(source).frozen()) {
                result = ITickHandler.get(source).unfreeze(source);
            }
            return result;
        }
    }

    @ModifyArg(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/arguments/TimeArgument;time(I)Lnet/minecraft/commands/arguments/TimeArgument;",
                    ordinal = 1
            ),
            index = 0
    )
    private static int time(int time)
    {
        return 0;
    }

    @Inject(method = "tickQuery", at = @At("HEAD"), cancellable = true)
    private static void freezeStatus(CommandSourceStack c, CallbackInfoReturnable<Integer> cir)
    {
        cir.setReturnValue(ITickHandler.get(c).when(c));
    }

    @Inject(method = "setFreeze", at = @At("HEAD"), cancellable = true)
    private static void setFreeze(CommandSourceStack c, boolean freeze, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException
    {
        ServerTickRateManager manager = c.getServer().tickRateManager();
        if(freeze) {
            if (manager.isFrozen()) {
                c.sendFailure(Component.literal(Translations.tr("subtick.error.vanilla_frozen")));
                cir.setReturnValue(0);
                return;
            }
            cir.setReturnValue(ITickHandler.get(c).freeze(c, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
        } else {
            int result = 0;
            if (manager.isFrozen()) {
                manager.setFrozen(false);
                c.sendSuccess(() -> Component.translatable("commands.tick.status.running"), true);
                result = 1;
            }
            if (ITickHandler.get(c).frozen()) {
                result = ITickHandler.get(c).unfreeze(c);
            }
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "step", at = @At("HEAD"), cancellable = true)
    private static void step(CommandSourceStack c, int ticks, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException
    {
        if (c.getServer().tickRateManager().isFrozen()) {
            return;
        }
        cir.setReturnValue(ITickHandler.get(c).step(c, ticks, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
    }
}