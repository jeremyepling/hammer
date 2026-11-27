package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.SeismicScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record SeismicScanPayload(Identifier dimension, BlockPos origin, boolean started,
                                List<BlockPos> markers) implements CustomPacketPayload {
    public static final Type<SeismicScanPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "seismic_scan"));
    public static final StreamCodec<ByteBuf, SeismicScanPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SeismicScanPayload::dimension,
            BlockPos.STREAM_CODEC, SeismicScanPayload::origin,
            ByteBufCodecs.BOOL, SeismicScanPayload::started,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(SeismicScanner.MAX_DEPOSITS)), SeismicScanPayload::markers,
            SeismicScanPayload::new);

    public SeismicScanPayload {
        markers = List.copyOf(markers);
        if (markers.size() > SeismicScanner.MAX_DEPOSITS) throw new IllegalArgumentException("Too many ore markers");
    }

    @Override
    public Type<SeismicScanPayload> type() {
        return TYPE;
    }
}
