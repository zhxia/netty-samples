package com.test.app;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DynamicChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;

public class ServerHandler extends SimpleChannelUpstreamHandler{

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
         HttpResponse response=new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        //get Request
        HttpRequest request=(HttpRequest)e.getMessage();
        String uri=request.getUri();
        System.out.println(String.format("Request url:%s",uri));
        //get User-Agent
        String userAgent=request.getHeader(HttpHeaders.Names.USER_AGENT);
        System.out.println("UserAgent is:"+userAgent);
        //Process queryString
        QueryStringDecoder queryStringDecoder=new QueryStringDecoder(uri,CharsetUtil.UTF_8);
        System.out.println("request path is:"+queryStringDecoder.getPath());
        Map<String,List<String>> queryParams=queryStringDecoder.getParameters();
        if(!queryParams.isEmpty()){
            for(Entry<String, List<String>> item:queryParams.entrySet()){
                String paramName=item.getKey();
                for(String val:item.getValue()){
                    System.out.println(String.format("name:%s value:%s\r\n", paramName,val));
                }
            }
        }

        //Process Cookie
        String cookieString=request.getHeader(HttpHeaders.Names.COOKIE);
        if(null!=cookieString){
            CookieDecoder cookieDecoder=new CookieDecoder();
            Set<Cookie> cookies=cookieDecoder.decode(cookieString);
            if(!cookies.isEmpty()){
                CookieEncoder cookieEncoder=new CookieEncoder(true);
                for(Cookie cookie:cookies){
                    cookieEncoder.addCookie(cookie);
                    System.out.println(String.format("cookieName:%s cookieValue:%s", cookie.getName(),cookie.getValue()));
                }
                response.addHeader(HttpHeaders.Names.SET_COOKIE, cookieEncoder.encode());
            }
        }
        //send Response

        ChannelBuffer buffer=new DynamicChannelBuffer(2048);
        buffer.writeBytes("hello world,这是测试信息".getBytes(CharsetUtil.UTF_8));
        response.setContent(buffer);
        response.setHeader("Content-Type","text/html;charset=UTF-8");
        response.setHeader("Content-Length",response.getContent().writerIndex());
        response.setHeader("Server", "XServer");
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "public,max-age=100,s-maxage=120,must-revalidate");
//        response.setHeader(HttpHeaders.Names.ETAG, "cctv");
        Channel channel=e.getChannel();
        channel.write(response);
        channel.disconnect();
        channel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e){
        Channel channel=e.getChannel();
        Throwable cause=e.getCause();
        if(cause instanceof TooLongFrameException){
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
        }
        cause.printStackTrace();
        if(channel.isConnected()){
            sendError(ctx,HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendError(ChannelHandlerContext ctx,HttpResponseStatus status){
        HttpResponse response=new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.setHeader("Content-Type", "text/html;charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer(String.format("Failure:%s\r\n", status.toString()), CharsetUtil.UTF_8));
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

}
