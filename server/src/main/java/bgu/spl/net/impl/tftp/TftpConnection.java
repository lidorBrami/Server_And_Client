package bgu.spl.net.impl.tftp;
import bgu.spl.net.srv.Connections;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.BlockingConnectionHandler;
public class TftpConnection implements Connections<byte[]>  {
    public ConcurrentHashMap<Integer, BlockingConnectionHandler<byte[]>> connecting = new ConcurrentHashMap<>();
    public void connect(int connectionId, BlockingConnectionHandler<byte[]> handler){
        connecting.put(connectionId, handler);
    }
    public void send(int connectionId, byte[] msg) {
        BlockingConnectionHandler<byte[]> handler = this.connecting.get(connectionId);
        handler.send(msg);
    }
    public void disconnect(int connectionId){
        connecting.remove(connectionId);
    }

    public BlockingConnectionHandler<byte[]> getHandler(int connectionId){
        return connecting.get(connectionId);
    }
    public boolean contain(int id){
        return connecting.contains(id);
    }

}
