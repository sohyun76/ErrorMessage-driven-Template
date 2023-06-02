package io.github.hapjava.server.impl.http.impl;

import io.github.hapjava.server.impl.http.HomekitClientConnectionFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NettyHomekitHttpService {

  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;

  private static final Logger logger = LoggerFactory.getLogger(NettyHomekitHttpService.class);
  private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
  private final InetAddress localAddress;
  private final int port;
  private final int nThreads;

  public static NettyHomekitHttpService create(InetAddress localAddress, int port, int nThreads) {
    return new NettyHomekitHttpService(localAddress, port, nThreads);
  }

  private NettyHomekitHttpService(InetAddress localAddress, int port, int nThreads) {
    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();
    this.localAddress = localAddress;
    this.port = port;
    this.nThreads = nThreads;
  }

  public CompletableFuture<Integer> create(HomekitClientConnectionFactory connectionFactory) {
    final CompletableFuture<Integer> portFuture = new CompletableFuture<Integer>();
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ServerInitializer(connectionFactory, allChannels, nThreads))
        .option(ChannelOption.SO_BACKLOG, 128)
        .childOption(ChannelOption.SO_KEEPALIVE, true);
    final ChannelFuture bindFuture = b.bind(localAddress, port);
    bindFuture.addListener(
        new GenericFutureListener<Future<? super Void>>() {

          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            try {
              future.get();
              SocketAddress socketAddress = bindFuture.channel().localAddress();
              if (socketAddress instanceof InetSocketAddress) {
                logger.trace("Bound homekit listener to " + socketAddress.toString());
                portFuture.complete(((InetSocketAddress) socketAddress).getPort());
              } else {
                throw new RuntimeException(
                    "Unknown socket address type: " + socketAddress.getClass().getName());
              }
            } catch (Exception e) {
              portFuture.completeExceptionally(e);
            }
          }
        });
    return portFuture;
  }

  public void shutdown() {
    workerGroup.shutdownGracefully();
    bossGroup.shutdownGracefully();
  }

  public void resetConnections() {
    logger.trace("Resetting connections");
    allChannels.close();
  }
}
