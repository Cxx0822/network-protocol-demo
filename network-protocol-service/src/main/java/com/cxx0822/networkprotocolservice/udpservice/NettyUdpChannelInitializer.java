package com.cxx0822.networkprotocolservice.udpservice;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Cxx
 * @Date: 2023/9/13 22:13
 * @Description: I/O数据读写处理类
 */
@Slf4j
public class NettyUdpChannelInitializer extends SimpleChannelInboundHandler<DatagramPacket> {

    /**
     * 读取客户端消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        try {
            // 解析客户端消息
            String clientMessage = packet.content().toString(CharsetUtil.UTF_8);
            log.info("get upd client {} data {}", packet.sender().getAddress(), clientMessage);

            ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(clientMessage, CharsetUtil.UTF_8), packet.sender()));
        } catch (Exception exception) {
            ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("exception", CharsetUtil.UTF_8), packet.sender()));
            log.error("get upd client {} data {} exception {}", packet.sender().getAddress(), packet.content().toString(CharsetUtil.UTF_8), exception.getMessage());
        }
    }
}
