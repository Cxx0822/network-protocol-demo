package com.cxx0822.networkprotocolservice.config;

import com.cxx0822.networkprotocolservice.tcpservice.NettyTcpServer;
import com.cxx0822.networkprotocolservice.udpservice.NettyUdpServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

/**
 * @Author: Cxx
 * @Date: 2023/9/13 22:55
 * @Description: Netty服务器启动类
 */
@Component
@EnableAsync
public class NettyDemoApplication implements CommandLineRunner {
    @Async
    @Override
    public void run(String... args) throws Exception{
        // 使用异步注解方式启动netty服务端服务
        // new NettyTcpServer().bind(6655);

        new NettyUdpServer().bind(6677);
    }
}
