package bgu.spl.net.impl.tftp;
import java.util.function.Supplier;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.Server;
public class TftpServer<T>  {
    public static void main(String[] args) {
         int port = 7777; // Default port, replace with actual port if necessary

         // Create suppliers for protocol and encoder/decoder
         Supplier<TftpProtocol> protocolFactory = ()->new TftpProtocol();  // Ensure TftpProtocol has a no-argument constructor
         Supplier<MessageEncoderDecoder<byte[]>> encdecFactory = ()->new TftpEncoderDecoder();  // Ensure this matches your implementation
 
         // Create an instance of your Connections implementation
         TftpConnection connection = new TftpConnection(); 
 
         // Create and start the server
         Server<byte[]> server = threadPerClient(port, protocolFactory, encdecFactory, connection);
         server.serve(); // Start the server
    }

    public static <T> Server<T>  threadPerClient(
            int port,
            Supplier<TftpProtocol> protocolFactory,
            Supplier<MessageEncoderDecoder<byte[]>> encoderDecoderFactory, Connections<T> connection) {

        return new BaseServer<T>(port, protocolFactory, encoderDecoderFactory,connection) {
            @Override
            protected void execute(BlockingConnectionHandler<T>  handler) {
                new Thread(handler).start();
            }
        };
    }
}   
