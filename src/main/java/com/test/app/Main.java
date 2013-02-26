package com.test.app;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

/**
 * Hello world!
 *
 */
public class Main
{
    public static void main( String[] args )
    {
        int port=10086;
        if(args.length>0){
            port=Integer.parseInt(args[0]);
        }
        ChannelFactory channelFactory=new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),Executors.newCachedThreadPool());
        ServerBootstrap bootstrap=new ServerBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new ServerPipelineFactory());
        //bind port
        bootstrap.bind(new InetSocketAddress(port));
        System.out.println("Server start on port:"+port);
    }

    public static class ServerPipelineFactory implements ChannelPipelineFactory{

        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline=Channels.pipeline();
            pipeline.addLast("decoder", new HttpRequestDecoder());
            pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
            pipeline.addLast("encoder", new HttpResponseEncoder());
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
            pipeline.addLast("handler",new ServerHandler());
            return pipeline;
        }
    }
}
