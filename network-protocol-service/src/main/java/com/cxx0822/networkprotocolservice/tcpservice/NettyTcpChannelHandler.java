package com.cxx0822.networkprotocolservice.tcpservice;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Cxx
 * @Date: 2023/9/14 22:01
 * @Description: I/O数据读写处理类
 */
@ChannelHandler.Sharable
@Slf4j
public class NettyTcpChannelHandler extends ChannelInboundHandlerAdapter {
    /**
     * 管理一个全局map，保存连接进服务端的通道数量
     */
    private static final ConcurrentHashMap<ChannelId, ChannelHandlerContext> CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 注册时执行
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        CHANNEL_MAP.put(ctx.channel().id(), ctx);
        log.info("{} register tcp chanel", getClientIp(ctx));
    }

    /**
     * 离线时执行
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        CHANNEL_MAP.remove(ctx.channel().id());
        log.info("{} unregister tcp chanel", getClientIp(ctx));
    }

    /**
     * 从客户端收到新的数据时，这个方法会在收到消息时被调用
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg == null || msg == "") {
                log.info("get tcp client {} data empty", getClientIp(ctx));
                return;
            }

            channelWrite(ctx.channel().id(), msg);
        } catch (Exception exception) {
            exception.printStackTrace();
            log.error("get tcp client data failed: {}", exception.getMessage());
        }
    }

    /**
     * 发送客户端消息
     *
     * @param channelId     通道Id
     * @param clientMessage 接收到客户端的消息
     */
    public void channelWrite(ChannelId channelId, Object clientMessage) {
        ChannelHandlerContext ctx = CHANNEL_MAP.get(channelId);

        if (ctx == null) {
            log.error("tcp channel is not exist");
            return;
        }

        // 解析客户端发送的消息
        String data = (String) clientMessage;
        data = data.replaceAll("[\r\n]", "");
        log.info("get tcp client {} data {}", getClientIp(ctx), data);

        // 返回客户端的消息 增加分隔符
        String tcpMessageEndFlag = "$";
        ctx.writeAndFlush(Unpooled.buffer().writeBytes((data + tcpMessageEndFlag).getBytes()));
    }

    /**
     * 当出现 Throwable 对象才会被调用，即当 Netty 由于 IO 错误或者处理器在处理事件时抛出的异常时
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("{} get tcp client message exception: {}", getClientIp(ctx), cause.getMessage());

        cause.printStackTrace();
        CHANNEL_MAP.remove(ctx.channel().id());
        // 抛出异常，断开与客户端的连接
        ctx.close();
    }

    /**
     * 客户端与服务端第一次建立连接时 执行
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.channel().read();
        // 此处不能使用ctx.close()，否则客户端始终无法与服务端建立连接
        log.info("tcp client {} connect successfully", getClientIp(ctx));
    }

    /**
     * 客户端与服务端 断连时 执行
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("tcp client {} disconnect successfully", getClientIp(ctx));
        // 断开连接时，必须关闭，否则造成资源浪费，并发量很大情况下可能造成宕机
        ctx.close();
    }

    /**
     * 服务端当read超时, 会调用这个方法
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        // 超时时断开连接
        CHANNEL_MAP.remove(ctx.channel().id());
        ctx.close();
        log.error("tcp server {} read message timeout", getClientIp(ctx));
    }

    /**
     * 获取客户端Ip
     *
     * @param ctx 通道
     * @return 客户端Ip
     */
    private String getClientIp(ChannelHandlerContext ctx) {
        InetSocketAddress inSocket = (InetSocketAddress) ctx.channel().remoteAddress();
        return inSocket.getAddress().getHostAddress();
    }
}

