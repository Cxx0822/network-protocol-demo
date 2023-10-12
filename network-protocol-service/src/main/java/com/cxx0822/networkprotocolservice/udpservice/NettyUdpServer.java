package com.cxx0822.networkprotocolservice.udpservice;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Cxx
 * @Date: 2023/9/13 22:53
 * @Description: nettyUdp服务器
 */
@Slf4j
public class NettyUdpServer {

    /**
     * 启动服务
     */
    public void bind(int port) {

        // 表示服务器连接监听线程组，专门接受 accept 新的客户端client 连接
        EventLoopGroup bossLoopGroup = new NioEventLoopGroup();
        try {
            log.info("netty udp server ready to start");

            // 1，创建netty bootstrap 启动类
            Bootstrap serverBootstrap = new Bootstrap();
            // 2、设置boostrap 的eventLoopGroup线程组
            serverBootstrap = serverBootstrap.group(bossLoopGroup);
            // 3、设置NIO UDP连接通道
            serverBootstrap = serverBootstrap.channel(NioDatagramChannel.class);
            // 4、设置通道参数 SO_BROADCAST广播形式
            serverBootstrap = serverBootstrap.option(ChannelOption.SO_BROADCAST, true);
            // 5、设置通道处理类
            serverBootstrap = serverBootstrap.handler(new NettyUdpChannelInitializer());
            // 6、绑定server，通过调用sync方法异步阻塞，直到绑定成功
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            log.info("netty udp server start success! listened on {}", channelFuture.channel().localAddress());
            // 7、监听通道关闭事件，应用程序会一直等待，直到channel关闭
            channelFuture.channel().closeFuture().sync();
        } catch (Exception exception) {
            log.error("netty udp server start failed {}", exception.getMessage());
        } finally {
            // 8 关闭EventLoopGroup，
            bossLoopGroup.shutdownGracefully();

            log.info("netty udp server stop success!");
        }
    }
}
