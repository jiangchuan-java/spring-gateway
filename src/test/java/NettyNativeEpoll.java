import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

/**
 * @Des:
 * @Author: jiangchuan
 * <p>
 * @Date: 20-11-20
 */
public class NettyNativeEpoll {

    @Test
    public void bootsrapTest() throws Exception{

        EpollEventLoopGroup eventLoopGroup = new EpollEventLoopGroup();
        NioEventLoopGroup eventLoopGroup2 = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap
                .group(eventLoopGroup, eventLoopGroup)
                .channel(EpollServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                System.out.println(msg);
                                ctx.fireChannelRead(msg);
                            }
                        });
                    }
                }).bind(9090).sync();

        System.out.println("server start");

        System.in.read();

    }
}
