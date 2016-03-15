package us.myles.ViaVersion.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import us.myles.ViaVersion.CancelException;
import us.myles.ViaVersion.packets.Direction;
import us.myles.ViaVersion.util.PacketUtil;
import us.myles.ViaVersion2.api.PacketWrapper;
import us.myles.ViaVersion2.api.data.UserConnection;
import us.myles.ViaVersion2.api.protocol.base.ProtocolInfo;

import java.util.List;

public class ViaDecodeHandler extends ByteToMessageDecoder {

    private final ByteToMessageDecoder minecraftDecoder;
    private final UserConnection info;

    public ViaDecodeHandler(UserConnection info, ByteToMessageDecoder minecraftDecoder) {
        this.info = info;
        this.minecraftDecoder = minecraftDecoder;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> list) throws Exception {
        // use transformers
        if (bytebuf.readableBytes() > 0) {
            if (info.isActive()) {
                int id = PacketUtil.readVarInt(bytebuf);
                // Transform
                try {

                    PacketWrapper wrapper = new PacketWrapper(id, bytebuf, info);
                    ProtocolInfo protInfo = info.get(ProtocolInfo.class);
                    protInfo.getPipeline().transform(Direction.INCOMING, protInfo.getState(), wrapper);
                    ByteBuf newPacket = ctx.alloc().buffer();
                    wrapper.writeToBuffer(newPacket);

                    bytebuf.clear();
                    bytebuf = newPacket;
                } catch (Exception e) {
                    bytebuf.clear();
                    throw e;
                }
            }
            // call minecraft decoder
            list.addAll(PacketUtil.callDecode(this.minecraftDecoder, ctx, bytebuf));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PacketUtil.containsCause(cause, CancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
