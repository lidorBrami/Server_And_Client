package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class TftpClient {

    private static final String SERVER_ADDRESS = "127.0.0.1"; // change to the current ip
    private static final int SERVER_PORT = 7777; // Default TFTP port or whichever your server uses
    public volatile static boolean shouldTerminate = false;
    static private DataOutputStream outToServer;
    static private BufferedReader inFromServer;
    static private TftpClientProtocol protocol;
    static private TftpClientEncoderDecoder encdec;
    private static BufferedReader InFromUser =  new BufferedReader(new InputStreamReader(System.in));
    public static void main(String[] args) {
        
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)){
            System.out.println("Client start");
            outToServer = new DataOutputStream(socket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Creating a thread to handle server responses
            protocol = new TftpClientProtocol();
            encdec = new TftpClientEncoderDecoder();
            Thread listenerThread = new Thread(() -> {
                try {
                    InputStream inputFromServer = socket.getInputStream();
                    int read;
                    while ((read = inputFromServer.read()) >= 0 ) {
                        byte[] nextMessage = encdec.decodeNextByte((byte) read);
                        if (nextMessage != null) {
                            
                            byte[] response = protocol.process(nextMessage);
                            if(protocol.close){
                                shouldTerminate = true;
                                break;
                            }    
                            if(response!=null){
                                synchronized(outToServer){
                                    outToServer.write((response));  
                                    outToServer.flush();
                                }
                            }
                            
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection to server lost.");
                }
            });
            listenerThread.start(); // Start listening to server messages

            // Main loop for user input
            while (!shouldTerminate) { 
                String commandLine = InFromUser.readLine();
                // chack if byte[] can be null
                if(commandLine.equals("DISC"))
                    shouldTerminate = true;

                byte[] command = encode(commandLine);
                if (protocol.close) {
                    break;
                }
                if(command!=null){
                    byte[] ans = protocol.process(command);
                    if(ans!=null){
                        synchronized(outToServer){
                            outToServer.write(ans); // Send user command to server
                            outToServer.flush();
                        }
                    }
                }
                else{
                    System.out.println("The command is anvalid");
                }    

            }
            try{
                listenerThread.join();
            }
            catch(InterruptedException e){}
            listenerThread.interrupt(); // Stop listening thread
            socket.close(); // Close connection to server

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to server.");
        }
    }
    
    public static byte[] encode(String message) {
        //TODO: implement this
        if(message.length()<4){return null;}
        if(message.substring(0,3).equals("RRQ")){
            return encodeRrq(message.substring(4));
        }
        if(message.substring(0,3).equals("WRQ")){
            return encodeWrq(message.substring(4));
        }
        
        if(message.substring(0,4).equals("DIRQ")){
            return encodeDirq();
        }
        if(message.substring(0,4).equals("DISC")){
            return encodeDisc();
        }
        if(message.length()<6){return null;}
        if(message.substring(0,5).equals("LOGRQ")){
            return encodeLogrq(message.substring(6));
        }
        if(message.substring(0,5).equals("DELRQ")){
            return encodeDelrq(message.substring(6));
        }
        
        
        return null;
    }
    //the following methods *encode* their respective packets
    private static byte[] encodeLogrq(String message) {
        byte[] ret = new byte[3 + message.length()];
        ret[0] = 0;
        ret[1] = 7;
        System.arraycopy(message.getBytes(StandardCharsets.UTF_8), 0, ret, 2, message.length());
        ret[message.length() + 2] = 0;
        return ret;
    }
    private static byte[] encodeDelrq(String message) {
        byte[] ret = new byte[3 + message.length()];
        ret[0] = 0;
        ret[1] = 8;
        System.arraycopy(message.getBytes(StandardCharsets.UTF_8), 0, ret, 2, message.length());
        ret[message.length() + 2] = 0;
        return ret;
    }
    private static byte[] encodeRrq(String message) {
        byte[] ret = new byte[3 + message.length()];
        ret[0] = 0;
        ret[1] = 1;
        System.arraycopy(message.getBytes(StandardCharsets.UTF_8), 0, ret, 2, message.length());
        ret[message.length() + 2] = 0;
        return ret;
    }
    
    private static byte[] encodeWrq(String message) {
        byte[] ret = new byte[3 + message.length()];
        ret[0] = 0;
        ret[1] = 2;
        System.arraycopy(message.getBytes(StandardCharsets.UTF_8), 0, ret, 2, message.length());
        ret[message.length() + 2] = 0;
        return ret;
    }
    private static byte[] encodeDirq(){
        byte[] ret= new byte[2];
        ret[0]=0;
        ret[1]=6;
        return ret;
    }
    private static byte[] encodeDisc(){
        byte[] ret= new byte[2];
        ret[0]=0;
        ret[1]=10;
        return ret;
    }

    
}
