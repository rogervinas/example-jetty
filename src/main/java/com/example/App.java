package com.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

class ServerManager implements Resource
{
    Server server;
    Thread preventExitThread;

    public ServerManager(int port, Handler handler) throws Exception {
        server = new Server(8080);
        server.setHandler(handler);
        server.start();
        Core.getGlobalContext().register(this);

        // beforeCheckpoint implemented in simplest manner: shutdown the jetty
        // server. There may be no non-daemon threads left, so JVM may exit.
        // Here we provides a thread that will keep JVM running.
        preventExitThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1_000_000);
                } catch (InterruptedException e) {
                }
            }
        });
        preventExitThread.start();
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        server.stop();
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        server.start();
    }
}

class Counter implements Resource {

    private int counter = 0;
    private ScheduledExecutorService executor;
    private static final Logger LOG = Log.getLogger(Counter.class);

    public Counter() {
        Core.getGlobalContext().register(this);
    }

    public void start() {
        LOG.info("start");
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::count, 1, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        LOG.info("stop");
        executor.shutdown();
    }

    public int counter() {
        return counter;
    }

    private Void count() {
        counter++;
        LOG.info("count " + counter);
        return null;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        stop();
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        start();
    }
}

public class App extends AbstractHandler
{
    static ServerManager serverManager;

    static Counter counter = new Counter();

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println("Hello World counter is " + counter.counter());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        counter.start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        counter.stop();
    }

    public static void main(String[] args ) throws Exception
    {
        serverManager = new ServerManager(8080, new App());
    }
}
