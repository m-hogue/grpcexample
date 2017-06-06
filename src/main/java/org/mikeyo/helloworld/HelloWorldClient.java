package org.mikeyo.helloworld;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;

public class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /** Construct client connecting to HelloWorld server at {@code host:port}. */
    public HelloWorldClient(String host, int port) {
        //  GRPC-java 1.3.0 doesn't _nicely_ support proxying RPCs
        // you can set an environment variable that it'll pick up: https://github.com/grpc/grpc-java/blob/master/netty/src/main/java/io/grpc/netty/NettyChannelBuilder.java#L360
        // However, they're planning to add it to the API: https://github.com/grpc/grpc-java/issues/2193
        this(NettyChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true)
                // set SSL context for TLS
                //.sslContext()
                // default (de)compression instances leverage Gzip codec.
                .compressorRegistry(CompressorRegistry.getDefaultInstance())
                .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                //.loadBalancerFactory()
                //.maxInboundMessageSize(500000)
                // custom, optional user-agent
                //.userAgent("hello-world-client-127.0.0.1)
                .build());
    }

    /** Construct client for accessing RouteGuide server using the existing channel. */
    HelloWorldClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Say hello to server. */
    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        logger.info("request is " + request.getSerializedSize() + " bytes.");
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
            logger.info("response is " + response.getSerializedSize() + " bytes.");
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws Exception {
        HelloWorldClient client = new HelloWorldClient("localhost", 50051);
        try {
      /* Access a service running on the local machine on port 50051 */
            String user = "world";
            if (args.length > 0) {
                user = args[0]; /* Use the arg as the name to greet if provided */
            }
            client.greet(user);
        } finally {
            client.shutdown();
        }
    }
}
