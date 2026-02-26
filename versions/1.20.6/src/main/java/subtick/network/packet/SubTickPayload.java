package subtick.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import subtick.SubTick;

public record SubTickPayload(CompoundTag tag) implements CustomPacketPayload{
    public static final Type<SubTickPayload> TYPE = createId("subtick_payload");

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {return TYPE;}

    public static final StreamCodec<RegistryFriendlyByteBuf, SubTickPayload> CODEC =
            new StreamCodec<>() {
                @Override
                public @NotNull SubTickPayload decode(RegistryFriendlyByteBuf buf) {
                    CompoundTag tag = buf.readNbt();
                    return new SubTickPayload(tag);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SubTickPayload value) {
                    buf.writeNbt(value.tag);
                }
            };

    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createId(String path) {
        ResourceLocation identifier =
                //#if MC < 12100
                new ResourceLocation(SubTick.MOD_ID, path);
                //#else
                //$$ ResourceLocation.fromNamespaceAndPath(SubTick.MOD_ID, path);
        //#endif
        return new CustomPacketPayload.Type<>(identifier);
    }
}
