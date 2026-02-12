package subtick.mixins;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.server.commands.TickCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import subtick.ITickHandler;
import subtick.Settings;
import subtick.TickPhase;

@Mixin(value = TickCommand.class, priority = 980)
public class TickCommandMixin
{
    @ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;register(Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;)Lcom/mojang/brigadier/tree/LiteralCommandNode;"), index = 0)
    private static LiteralArgumentBuilder<CommandSourceStack> registerSubtickCommands(LiteralArgumentBuilder<CommandSourceStack> builder) {
        if (!builder.getLiteral().equals("tick")) return builder;
        var step = Commands.literal("step")
                .then(Commands.argument("time", TimeArgument.time(1))
                        .then(Commands.argument("phase", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(TickPhase.commandSuggestions, b))
                                .executes((c) -> ITickHandler.get(c).step(
                                        c.getSource(),
                                        IntegerArgumentType.getInteger(c, "time"),
                                        TickPhase.byCommandKey(StringArgumentType.getString(c, "phase"))
                                ))
                        )
                );
        builder.then(step);
        return builder;
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
        if(freeze)
            cir.setReturnValue(ITickHandler.get(c).freeze(c, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
        else
            cir.setReturnValue(ITickHandler.get(c).unfreeze(c));
    }

    @Inject(method = "step", at = @At("HEAD"), cancellable = true)
    private static void step(CommandSourceStack c, int ticks, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException
    {
        cir.setReturnValue(ITickHandler.get(c).step(c, ticks, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
    }
}
