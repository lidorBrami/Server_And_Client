package bgu.spl.net.srv;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.impl.tftp.TftpProtocol;


public abstract class BaseServer<T> implements Server<T> {
    private final int port;
    private final Supplier<TftpProtocol> protocolFactory;
    private final Supplier<MessageEncoderDecoder<byte[]>> encdecFactory;
    public final Connections<T> connection;
    private ServerSocket sock;
    private int idCounter;

    public BaseServer(
            int port,
            Supplier<TftpProtocol> protocolFactory,
            Supplier<MessageEncoderDecoder<byte[]>> encdecFactory,Connections<T> connection) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
        this.connection = connection;
		this.sock = null;
        idCounter = 1;
    }
    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();
                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<T>(
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get(),idCounter,this.connection);    
                connection.connect(idCounter,handler); 
                execute(handler);
                idCounter++;   

            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
        for (int i = 1;i<idCounter;i++) {
            connection.disconnect(i);
        }    
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}
