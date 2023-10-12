Java网络应用程序框架——Netty简介与应用

# 前言
## java网络编程模型
&emsp;&emsp;目前Java有BIO(blocking I/O)同步阻塞、NIO(non-blocking I/O)同步非阻塞和AIO(asynchronous I/O)异步非阻塞三种网络编程模型。
1. BIO 同步并阻塞，一个连接一个线程。每当客户端请求连接时，服务器就会启动一个线程进行处理。如果这个连接不做任何事情会造成不必要的线程开销。
2. NIO 同步非阻塞，一个请求一个线程。客户端请求的连接会先注册到多路复用器上，当多路复用器轮询到该连接有I/O请求时，服务器才会启动一个线程并处理。
3. AIO 异步非阻塞，一个有效请求一个线程。客户端的I/O请求需要先等待操作系统完成了再去通知服务器启动线程处理。

## NIO简介
&emsp;&emsp;服务器会创建一个选择器/多用复用器(Selector)的线程，该线程会管理多个已经注册的通道/连接(Channel)，实时轮询检查每个通道的状态。每个通道(Channel)通过数据缓冲区(Buffer)和客户端通信。     
&emsp;&emsp;BIO以流的方式处理数据，而NIO以缓冲区(块)的方式处理数据，NIO的数据总是从通道读取到缓冲区或者从缓冲区写入到通道中，Selector用于监听多个通道上的事件，因此使用单线程即可实现多客户端的通信。   
&emsp;&emsp;参考资料[Java NIO全面详解](https://juejin.cn/post/7131937244067315720)，[Java高性能：干货分享——让你快速掌握《NIO》](https://zhuanlan.zhihu.com/p/71098532)。

# Netty原理简介
&emsp;&emsp;[Netty](https://github.com/netty/netty)是一个异步事件驱动的网络应用程序框架，用户快速开发可维护的高性能协议服务器和客户端。
## BossGroup和WorkerGroup
&emsp;&emsp;Netty包含两组线程池:BossGroup和WorkerGroup，每个线程池中都有NioEventLoop线程。 BossGroup中的线程专门负责和客户端建立连接，WorkerGroup中的线程专门负责连接上的读写操作。两者的类型都是NioEventLoopGroup。
## NioEventLoopGroup
&emsp;&emsp;NioEventLoopGroup是一个事件循环组，该组包含多个事件循环，每个事件循环就是一个NioEventLoop。NioEventLoop表示一个不断循环的执行事件处理的线程。每个NioEventLoop都包含一个Selector，用于监听注册在其上的Scoket网络连接。
## BossNioEventLoop
&emsp;&emsp;每个BossNioEventLoop中循环执行以下步骤:
1. select: 轮询注册在其上的channel的accept事件(是否有新通道连接)。
2. processSelectedKeys: 处理accept事件，与客户端建立连接，并将其注册到WorkerNioEventLoop上的Selector上。
3. runAllTasks: 依次循环处理任务队列中的其他任务。
## WorkerNioEventLoop
&emsp;&emsp;每个WorkerNioEventLoop中循环执行以下步骤:
1. select: 轮询注册在其上的channel的I/O事件(是否有新I/O处理)。
2. processSelectedKeys: 处理对应channel上的read/write事件。
3. runAllTasks: 依次循环处理任务队列中的其他任务。

&emsp;&emsp;参考资料: [超详细Netty入门](https://zhuanlan.zhihu.com/p/181239748)，[Netty入门长文](https://zhuanlan.zhihu.com/p/299396057)，[Netty高性能原理和框架架构解析](http://www.52im.net/thread-2043-1-1.html)，[45 张图深度解析 Netty 架构与原理](https://cloud.tencent.com/developer/article/1754078)

# Netty工程创建
## 添加依赖
```xml
<!-- netty -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
</dependency>
```

## TCP服务器
### 服务器设置和启动类NettyTcpServer
```java
@Slf4j
public class NettyTcpServer {

    public void bind(int port) throws Exception {
        // 配置服务端的NIO线程组
        // NioEventLoopGroup 是用来处理I/O操作的Reactor线程组
        // bossGroup: 用来接收进来的连接，workerGroup: 用来处理已经被接收的连接，进行socketChannel的网络读写，
        // bossGroup: 接收到连接后就会把连接信息注册到workerGroup
        // workerGroup的EventLoopGroup默认的线程数是CPU核数的二倍
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            log.info("netty tcp server ready to start");

            // ServerBootstrap 是一个启动NIO服务的辅助启动类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 设置group，将bossGroup， workerGroup线程组传递到ServerBootstrap
            serverBootstrap = serverBootstrap.group(bossGroup， workerGroup);
            // ServerSocketChannel是以NIO的selector为基础进行实现的，用来接收新的连接
            // 这里告诉Channel通过NioServerSocketChannel获取新的连接
            serverBootstrap = serverBootstrap.channel(NioServerSocketChannel.class);

            //  option是设置 bossGroup，childOption是设置workerGroup

            // 服务端接受连接的队列长度，如果队列已满，客户端连接将被拒绝
            // 队列被接收后，拒绝的客户端下次连接上来只要队列有空余就能连上
            serverBootstrap = serverBootstrap.option(ChannelOption.SO_BACKLOG， 128);

            // 立即发送数据，默认值为Ture（Netty默认为True而操作系统默认为False）。
            // 该值设置Nagle算法的启用，改算法将小的碎片数据连接成更大的报文来最小化所发送的报文的数量，
            // 如果需要发送一些较小的报文，则需要禁用该算法。
            // Netty默认禁用该算法，从而最小化报文传输延时。
            serverBootstrap = serverBootstrap.childOption(ChannelOption.TCP_NODELAY， true);

            // 连接保活，默认值为False。启用该功能时，TCP会主动探测空闲连接的有效性。
            // 可以将此功能视为TCP的心跳机制，默认的心跳间隔是7200s即2小时， Netty默认关闭该功能。
            serverBootstrap = serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE， true);

            // 设置 I/O处理类，主要用于网络I/O事件，记录日志，编码、解码消息
            serverBootstrap = serverBootstrap.childHandler(new NettyTcpChannelInitializer<SocketChannel>());

            // 绑定端口，同步等待成功
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            if (channelFuture.isSuccess()) {
                log.info("netty tcp server start success! listened on {}"， channelFuture.channel().localAddress());

                // 等待服务器监听端口关闭
                channelFuture.channel().closeFuture().sync();
            }
        } catch (InterruptedException exception) {
            log.error("netty tcp server start failed {}"， exception.getMessage());
        } finally {
            // 退出 释放线程池资源
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
            log.info("netty tcp server stop success!");
        }
    }
}
```

1. 首先声明bossGroup，workerGroup两个线程组和ServerBootstrap辅助启动类。
2. 然后设置ServerBootstrap的group为线程组，设置channel为NioServerSocketChannel，并利用option和childOption依次设置bossGroup和workerGroup的参数。
3. 然后设置childHandler为自定义的Channel处理类。
4. 最后绑定端口，同步等待成功。

### Channel初始化类NettyTcpChannelInitializer
```java
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
        channel.pipeline().addLast(new IdleStateHandler(READ_TIME_OUT， WRITE_TIME_OUT， ALL_TIME_OUT， TimeUnit.SECONDS));

        //解决TCP粘包问题，以"$_"作为分隔
        String tcpMessageEndFlag = "$";
        ByteBuf delimiter = Unpooled.copiedBuffer(tcpMessageEndFlag.getBytes());
        channel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024， delimiter));
        channel.pipeline().addLast(new StringDecoder());

        // 设置编码
        channel.pipeline().addLast("encoder"， new StringEncoder(CharsetUtil.UTF_8));
        channel.pipeline().addLast("decoder"， new StringDecoder(CharsetUtil.UTF_8));

        // 自定义ChannelInboundHandlerAdapter
        channel.pipeline().addLast(new NettyTcpChannelHandler());
    }
}
```

&emsp;&emsp;该部分主要设置心跳，粘包，编码和自定义I/O处理。

### I/O读写处理类NettyTcpChannelHandler
```java
@ChannelHandler.Sharable
@Slf4j
public class NettyTcpChannelHandler extends ChannelInboundHandlerAdapter {
    /**
     * 管理一个全局map，保存连接进服务端的通道数量
     */
    private static final ConcurrentHashMap<ChannelId， ChannelHandlerContext> CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 注册时执行
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        CHANNEL_MAP.put(ctx.channel().id()， ctx);
        log.info("{} register tcp chanel"， getClientIp(ctx));
    }

    /**
     * 离线时执行
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        CHANNEL_MAP.remove(ctx.channel().id());
        log.info("{} unregister tcp chanel"， getClientIp(ctx));
    }

    /**
     * 从客户端收到新的数据时，这个方法会在收到消息时被调用
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx， Object msg) {
        try {
            if (msg == null || msg == "") {
                log.info("get tcp client {} data empty"， getClientIp(ctx));
                return;
            }

            channelWrite(ctx.channel().id()， msg);
        } catch (Exception exception) {
            exception.printStackTrace();
            log.error("get tcp client data failed: {}"， exception.getMessage());
        }
    }

    /**
     * 发送客户端消息
     *
     * @param channelId     通道Id
     * @param clientMessage 接收到客户端的消息
     */
    public void channelWrite(ChannelId channelId， Object clientMessage) {
        ChannelHandlerContext ctx = CHANNEL_MAP.get(channelId);

        if (ctx == null) {
            log.error("tcp channel is not exist");
            return;
        }

        // 解析客户端发送的消息
        String data = (String) clientMessage;
        data = data.replaceAll("[\r\n]"， "");
        log.info("get tcp client {} data {}"， getClientIp(ctx)， data);

        // 返回客户端的消息 增加分隔符
        String tcpMessageEndFlag = "$";
        ctx.writeAndFlush(Unpooled.buffer().writeBytes((data + tcpMessageEndFlag).getBytes()));
    }

    /**
     * 当出现 Throwable 对象才会被调用，即当 Netty 由于 IO 错误或者处理器在处理事件时抛出的异常时
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx， Throwable cause) {
        log.error("{} get tcp client message exception: {}"， getClientIp(ctx)， cause.getMessage());

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
        log.info("tcp client {} connect successfully"， getClientIp(ctx));
    }

    /**
     * 客户端与服务端 断连时 执行
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("tcp client {} disconnect successfully"， getClientIp(ctx));
        // 断开连接时，必须关闭，否则造成资源浪费，并发量很大情况下可能造成宕机
        ctx.close();
    }

    /**
     * 服务端当read超时， 会调用这个方法
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx， Object evt) throws Exception {
        super.userEventTriggered(ctx， evt);
        // 超时时断开连接
        CHANNEL_MAP.remove(ctx.channel().id());
        ctx.close();
        log.error("tcp server {} read message timeout"， getClientIp(ctx));
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
```

&emsp;&emsp;该部分主要处理客户端连接/断开、注册/断线和读写数据等业务，可结合项目的具体业务逻辑处理。

## UDP服务器
### 服务器设置和启动类NettyUdpServer
```java
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
            serverBootstrap = serverBootstrap.option(ChannelOption.SO_BROADCAST， true);
            // 5、设置通道处理类
            serverBootstrap = serverBootstrap.handler(new NettyUdpChannelInitializer());
            // 6、绑定server，通过调用sync方法异步阻塞，直到绑定成功
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            log.info("netty udp server start success! listened on {}"， channelFuture.channel().localAddress());
            // 7、监听通道关闭事件，应用程序会一直等待，直到channel关闭
            channelFuture.channel().closeFuture().sync();
        } catch (Exception exception) {
            log.error("netty udp server start failed {}"， exception.getMessage());
        } finally {
            // 8 关闭EventLoopGroup，
            bossLoopGroup.shutdownGracefully();

            log.info("netty udp server stop success!");
        }
    }
}
```

&emsp;&emsp;由于UDP是无连接的，因此只需要bossLoopGroup即可，其余和TCP服务器类似。

### I/O读写处理类NettyUdpChannelInitializer
```java
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
```

&emsp;&emsp;该部分主要为数据读写功能，可以结合具体的业务逻辑处理。

## 服务器启动
```java
@Component
@EnableAsync
public class NettyDemoApplication implements CommandLineRunner {
    @Async
    @Override
    public void run(String... args) throws Exception{
        // 使用异步注解方式启动netty服务端服务
        new NettyTcpServer().bind(6655);

        // new NettyUdpServer().bind(6677);
    }
}
```

&emsp;&emsp;可以写在Spring Boot自启动类中，通过异步注解，绑定端口的形式启动服务。

## 客户端实现
&emsp;&emsp;客户端可以采用任何语言，任何框架的形式，本项目中提供了Unity C#的形式，仅供参考。  

# 附: TCP和UDP区别
1. TCP面向连接（如打电话要先拨号建立连接）;UDP是无连接的，即发送数据之前不需要建立连接
2. TCP提供可靠的服务，通过TCP连接传送的数据，无差错，不丢失，不重复，且按序到达;UDP尽最大努力交付，即不保证可靠交付
3. TCP面向字节流，实际上是TCP把数据看成一连串无结构的字节流;UDP是面向报文的，UDP没有阻塞控制，因此网络出现阻塞不会使源主机的发送速率降低（对实时应用很有用，如IP电话，实时视频会议等）
4. 每一条TCP连接只能是点到点的;UDP支持一对一，一对多，多对一和多对多的交互通信
5. TCP首部开销20字节;UDP的首部开销小，只有8个字节
6. TCP的逻辑通信信道是全双工的可靠信道，UDP则是不可靠信道