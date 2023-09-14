package com.cxx0822.networkprotocolservice.tcpservice;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Cxx
 * @Date: 2023/9/14 21:59
 * @Description: Tcp通道初始化
 */
@ChannelHandler.Sharable
public class NettyTcpChannelInitializer<SocketChannel> extends ChannelInitializer<Channel> {

    public static long READ_TIME_OUT = 60;

    public static long WRITE_TIME_OUT = 60;

    /**
     * 最长连接时间 超过该时间连接会断开
     */
    public static long ALL_TIME_OUT = 60;

    @Override
    protected void initChannel(Channel channel) throws Exception {
        // 设置心跳机制 超过最长等待时间未收到消息 则关闭该通道
        channel.pipeline().addLast(new IdleStateHandler(READ_TIME_OUT, WRITE_TIME_OUT, ALL_TIME_OUT, TimeUnit.SECONDS));

        //解决TCP粘包问题,以"$_"作为分隔
        String tcpMessageEndFlag = "$";
        ByteBuf delimiter = Unpooled.copiedBuffer(tcpMessageEndFlag.getBytes());
        channel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter));
        channel.pipeline().addLast(new StringDecoder());

        // 设置编码
        channel.pipeline().addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
        channel.pipeline().addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));

        // 自定义ChannelInboundHandlerAdapter
        channel.pipeline().addLast(new NettyTcpChannelHandler());
    }
}
