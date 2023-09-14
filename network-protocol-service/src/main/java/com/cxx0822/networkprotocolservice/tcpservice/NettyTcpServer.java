package com.cxx0822.networkprotocolservice.tcpservice;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Cxx
 * @Date: 2023/9/14 21:57
 * @Description: Tcp服务器
 */
@Slf4j
public class NettyTcpServer {

    public void bind(int port) throws Exception {
        // 配置服务端的NIO线程组
        // NioEventLoopGroup 是用来处理I/O操作的Reactor线程组
        // bossGroup: 用来接收进来的连接，workerGroup: 用来处理已经被接收的连接,进行socketChannel的网络读写，
        // bossGroup: 接收到连接后就会把连接信息注册到workerGroup
        // workerGroup的EventLoopGroup默认的线程数是CPU核数的二倍
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            log.info("netty tcp server ready to start");

            // ServerBootstrap 是一个启动NIO服务的辅助启动类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 设置group，将bossGroup， workerGroup线程组传递到ServerBootstrap
            serverBootstrap = serverBootstrap.group(bossGroup, workerGroup);
            // ServerSocketChannel是以NIO的selector为基础进行实现的，用来接收新的连接
            // 这里告诉Channel通过NioServerSocketChannel获取新的连接
            serverBootstrap = serverBootstrap.channel(NioServerSocketChannel.class);

            //  option是设置 bossGroup，childOption是设置workerGroup

            // 服务端接受连接的队列长度，如果队列已满，客户端连接将被拒绝
            // 队列被接收后，拒绝的客户端下次连接上来只要队列有空余就能连上
            serverBootstrap = serverBootstrap.option(ChannelOption.SO_BACKLOG, 128);

            // 立即发送数据，默认值为Ture（Netty默认为True而操作系统默认为False）。
            // 该值设置Nagle算法的启用，改算法将小的碎片数据连接成更大的报文来最小化所发送的报文的数量，
            // 如果需要发送一些较小的报文，则需要禁用该算法。
            // Netty默认禁用该算法，从而最小化报文传输延时。
            serverBootstrap = serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);

            // 连接保活，默认值为False。启用该功能时，TCP会主动探测空闲连接的有效性。
            // 可以将此功能视为TCP的心跳机制，默认的心跳间隔是7200s即2小时, Netty默认关闭该功能。
            serverBootstrap = serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            // 设置 I/O处理类,主要用于网络I/O事件，记录日志，编码、解码消息
            serverBootstrap = serverBootstrap.childHandler(new NettyTcpChannelInitializer<SocketChannel>());

            // 绑定端口，同步等待成功
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            if (channelFuture.isSuccess()) {
                log.info("netty tcp server start success! listened on {}", channelFuture.channel().localAddress());

                // 等待服务器监听端口关闭
                channelFuture.channel().closeFuture().sync();
            }
        } catch (InterruptedException exception) {
            log.error("netty tcp server start failed {}", exception.getMessage());
        } finally {
            // 退出 释放线程池资源
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
            log.info("netty tcp server stop success!");
        }
    }
}