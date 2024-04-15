package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.impl.tftp.TftpProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.api.BidiMessagingProtocol;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final TftpProtocol protocol;
    private final MessageEncoderDecoder<byte[]> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    public final Connections connection;
    private final int connectionId;


    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<byte[]> reader, TftpProtocol protocol,int id,Connections<T> connections) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        connectionId = id;
        connection = connections;
        this.protocol.start(connectionId, connection);
    }

    @Override
    public  void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                    byte[] nextMessage = encdec.decodeNextByte((byte) read);
                    if (nextMessage != null) {
                        protocol.process(nextMessage);
                        // if (response != null) {
                        //     out.write(encdec.encode(response));
                        //     out.flush();
                        // }
                    }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public  boolean send(byte[] msg) {
        //IMPLEMENT IF NEEDED
        synchronized(this){
            if (msg != null) {
                try {
                    out.write(encdec.encode(msg));
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }    
        return true;
    }  
}
