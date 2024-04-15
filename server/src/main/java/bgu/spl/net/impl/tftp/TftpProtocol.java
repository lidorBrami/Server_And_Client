package bgu.spl.net.impl.tftp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    static ConcurrentHashMap<Integer, String> idsLogin = new ConcurrentHashMap<>();

    private int connectionId;

    public Connections<byte[]> connections;

    String filesDir = "Files";// path to Files directory

    private byte[] blockNumArray;// byte array with block number for upload/download

    private short blockNum;

    private boolean transferComplete;

    private boolean shouldTerminate;

    private int indexInData;// the curent index in data array

    private byte[] dataArray;// array that stores all of the data we will get/send

    private String wrqFileName;// WRQ filename

    private boolean loggedIN;// flag to know if this client already logged in or not

    

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this

        this.connectionId = connectionId;
        this.connections = connections;
        this.dataArray = new byte[0];
        this.blockNumArray = new byte[2];
        indexInData = 0;
        this.shouldTerminate = false;
        this.loggedIN = false;
        this.transferComplete = false;
    }

    @Override
    public void process(byte[] message) {

        short opCode = (short) (((short) message[0] << 8 | (short) (message[1] & 0xFF)));// convert the first bytes into short
        if(!loggedIN && opCode!=7){
            byte errorNum = 6;
            byte[] error = errorMessage("USER NOT LOGGED IN", errorNum);
            connections.send(this.connectionId, error);
            return;
        }
        switch (opCode) {
            case 1:// RRQ
                handleRRQ(message);
                break;

            case 2:// WRQ
                handleWRQ(message);// TODO broadcast
                break;

            case 3:// DATA
                handleData(message);// TODO broadcast
                break;

            case 4:// ACK
                handleAck();
                break;

            case 6:// DRQ
                handleDirq();
                break;

            case 7:// LOGRQ
                handleLogrq(message);// TODO fix this method
                break;

            case 8:// DELRQ
                handleDelrq(message);// TODO broadcast
                break;

            case 10:// DISC
                handleDisc();
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    public void setShouldTerminate(boolean shouldTerminate) {
        this.shouldTerminate = shouldTerminate;
    }

    private void handleLogrq(byte[] message) {
        // find if this client already exist
        if (loggedIN) {
            byte errorNum = 7;
            byte[] error = errorMessage("User already logged in", errorNum);
            connections.send(this.connectionId, error);
            return;
        }
        String clientName = new String(message, 2, message.length - 2, StandardCharsets.UTF_8);// client name

        for (Map.Entry<Integer, String> entry : idsLogin.entrySet()) {
            if (entry.getValue().equals(clientName)) {// if the client name exist
                byte errorNum = 0;
                byte[] error = errorMessage("Username already exist", errorNum);
                connections.send(this.connectionId, error);
                return;
            }

       }
        idsLogin.putIfAbsent(connectionId, clientName);
        loggedIN = true;
        connections.send(this.connectionId, ackResponse());
    }

    private void handleData(byte[] message) {
        // increase data size by 512;
        byte[] temp = new byte[dataArray.length + message.length-6];
        System.arraycopy(dataArray, 0, temp, 0, dataArray.length);
        dataArray = temp;

        // insert the new packet into data
        System.arraycopy(message, 6, dataArray, indexInData, message.length-6);
        indexInData += (message.length-6);

        // send ack with curent block number
        blockNumArray[0] = message[4];
        blockNumArray[1] = message[5];
        connections.send(this.connectionId, acknowledgeTheClient(blockNumArray));

        // reset block number
        blockNumArray[0] = 0;
        blockNumArray[1] = 0;

        // if the data transfer is complete
        if (message.length < 518) {
            try {// create new file
                Files.write(Paths.get(filesDir + "/" + wrqFileName), dataArray, StandardOpenOption.CREATE);
                byte[] fileNameInBytes = wrqFileName.getBytes();
                byte[] broadcastMessage = new byte[fileNameInBytes.length + 4];
                broadcastMessage[0] = 0;
                broadcastMessage[1] = 9;
                broadcastMessage[2] = 1;
                System.arraycopy(fileNameInBytes, 0, broadcastMessage, 3, fileNameInBytes.length);
                //broadcastMessage[broadcastMessage.length - 1] = 0;

                broadcast(broadcastMessage);
            } catch (IOException e) {
                byte errorNum = 2;
                byte[] error = errorMessage("Access violation", errorNum);
                connections.send(this.connectionId, error);
            }
            dataArray = new byte[0];// reset the data array
            indexInData = 0;

        }

    }

    private void handleAck() {
        splitIntoManyPackets();
    }

    private void handleRRQ(byte[] message) {
        String fileName = new String(message, 2, message.length - 2, StandardCharsets.UTF_8);
        fileName = fileName.trim();                                                                                                                                                                     
        if (!fileExist(fileName)) {
            byte errorNum = 1;
            byte[] error = errorMessage("File not found", errorNum);
            connections.send(this.connectionId, error);
            return;
        }
        Path path = Paths.get(filesDir + "/" + fileName);
        try {
            dataArray = Files.readAllBytes(path);
        } catch (IOException e) {
            byte errorNum = 2;
            byte[] error = errorMessage("Access violation", errorNum);
            connections.send(this.connectionId, error);
            return;

        }
        if (dataArray.length >= 512) {
            splitIntoManyPackets();
        } else {
            short packetSize = (short) dataArray.length;
            byte[] sizeInBytes = new byte[] { (byte) (packetSize >> 8), (byte) (packetSize & 0xff) };
            byte[] dataPacket = new byte[packetSize + 6];

            // this is for op code
            dataPacket[0] = 0;
            dataPacket[1] = 3;

            // this is for data size
            dataPacket[2] = sizeInBytes[0];
            dataPacket[3] = sizeInBytes[1];

            // this is for block number
            dataPacket[4] = 0;
            dataPacket[5] = 1;
            System.arraycopy(dataArray, 0, dataPacket, 6, packetSize);// insert the data into the new packet
            connections.send(this.connectionId, dataPacket);
            transferComplete = true;
        }
    }

    private void handleWRQ(byte[] message) {
        wrqFileName = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
        if (fileExist(wrqFileName)) {// if the file already exists, dont need to upload it
            byte errorNum = 5;
            byte[] error = errorMessage("File already exist", errorNum);
            connections.send(this.connectionId, error);
            return;
        }

        // send ack 0
        connections.send(this.connectionId, ackResponse());
    }

    private void handleDelrq(byte[] message) {
        String fileName = new String(message, 2, message.length - 2, StandardCharsets.UTF_8);
        fileName = fileName.trim(); 
        if (!fileExist(fileName)) {// if the file doesn't exists we can't delete it
            byte errorNum = 1;
            byte[] error = errorMessage("File not found", errorNum);
            connections.send(this.connectionId, error);
            return;
        }
        Path path = Paths.get(filesDir + "/" + fileName);// the file path
        try {
            Files.delete(path);
        } catch (IOException e) {
            byte errorNum = 2;
            byte[] error = errorMessage("Access violation", errorNum);
            connections.send(this.connectionId, error);
            return;
        }
        connections.send(this.connectionId, ackResponse());
        byte[] fileNameInBytes = fileName.getBytes();
        byte[] broadcastMessage = new byte[fileNameInBytes.length +4];
        broadcastMessage[0] = 0;
        broadcastMessage[1] = 9;
        broadcastMessage[2] = 0;
        System.arraycopy(fileNameInBytes, 0, broadcastMessage, 3, fileNameInBytes.length);
        // broadcastMessage[broadcastMessage.length - 1] = 0;
        broadcast(broadcastMessage);
    }

    private void handleDirq() {
        try {
            List<byte[]> filesBytes = new ArrayList<>();// initialize list of byte arrays

            // go over all files in Files directory
            Files.list(Paths.get(filesDir)).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    byte[] fileNameInBytes = path.getFileName().toString().getBytes();// initialize byte array with the
                                                                                    // name of the file

                    // initialize new array and adding zero to the end
                    byte[] fileNameInBytesWithZero = new byte[fileNameInBytes.length + 1];
                    System.arraycopy(fileNameInBytes, 0, fileNameInBytesWithZero, 0, fileNameInBytes.length);
                    fileNameInBytesWithZero[fileNameInBytesWithZero.length - 1] = 0;
                    filesBytes.add(fileNameInBytesWithZero);// add the array to the list
                }
            });

            int sze = filesBytes.stream().mapToInt(arr -> arr.length).sum();// total size of all the arrays

            dataArray = new byte[sze];
            int index = 0;

            // insert all the byte arrays into the data array
            for (byte[] fileBytes : filesBytes) {
                System.arraycopy(fileBytes, 0, dataArray, index, fileBytes.length);
                index += fileBytes.length;
            }
        } catch (IOException e) {
            byte errorNum = 2;
            byte[] error = errorMessage("Access violation", errorNum);
            connections.send(this.connectionId, error);
            dataArray = new byte[0];// reset
            return;
        }
        if (dataArray.length > 512) {// if we can't send in 1 packet
            blockNum++;
            splitIntoManyPackets();
        } else {// if we can send in 1 packet
            short allFilesBytesSize = (short) dataArray.length;
            byte[] sizeInBytes = new byte[] { (byte) (allFilesBytesSize >> 8), (byte) (allFilesBytesSize & 0xff) };
            byte[] dataPacket = new byte[allFilesBytesSize + 6];// initialize new data packet

            // this is for op code
            dataPacket[0] = 0;
            dataPacket[1] = 3;

            // tis is for data size
            dataPacket[2] = sizeInBytes[0];
            dataPacket[3] = sizeInBytes[1];

            // this is for block number
            dataPacket[4] = 0;
            dataPacket[5] = 1;
            System.arraycopy(dataArray, 0, dataPacket, 6, allFilesBytesSize);// copy the data into the new packet
            connections.send(this.connectionId, dataPacket);
            dataArray = new byte[0];// reset
            // transferComplete = true;
        }
    }

    private void handleDisc() {
        shouldTerminate = true;
        BlockingConnectionHandler<byte[]> handler = connections.getHandler(this.connectionId);
        connections.send(connectionId,ackResponse());
        idsLogin.remove(this.connectionId);
        connections.disconnect(this.connectionId);
        try {
            handler.close();
        } catch (IOException e) {
        }

    }

    // methode to generate error message
    private byte[] errorMessage(String error, byte errorNum) {
        byte[] errorMessageBytes = error.getBytes(StandardCharsets.UTF_8);
        byte[] errorArray = new byte[4 + errorMessageBytes.length + 1];
        errorArray[0] = 0;
        errorArray[1] = 5;
        errorArray[2] = 0;
        errorArray[3] = errorNum;
        System.arraycopy(errorMessageBytes, 0, errorArray, 4, errorMessageBytes.length);
        errorArray[errorArray.length - 1] = 0;
        return errorArray;
    }

    // method to ack the client that we get the curent data packet
    private byte[] acknowledgeTheClient(byte[] blockNum) {
        return new byte[] { 0, 4, blockNum[0], blockNum[1] };
    }

    // method that split data that bigger then 512 byte
    private void splitIntoManyPackets() {
        if (transferComplete){
            transferComplete = false;
            return;
        }
        
        transferComplete = false;

        // decide if this is the last packet or not
        short chunkSize = (short) Math.min(dataArray.length - indexInData, 512);
        byte[] currDataPacket = new byte[chunkSize + 6];// initialize new packet

        // this is for op code
        currDataPacket[0] = 0;
        currDataPacket[1] = 3;

        // this is for packet size
        byte[] packetSize = new byte[] { (byte) (chunkSize >> 8), (byte) (chunkSize & 0xff) };
        currDataPacket[2] = packetSize[0];
        currDataPacket[3] = packetSize[1];

        // this is for block number
        blockNumArray = new byte[] { (byte) (blockNum >> 8), (byte) (blockNum & 0xff) };
        currDataPacket[4] = blockNumArray[0];
        currDataPacket[5] = blockNumArray[1];

        // copy 512 or less bytes from datta array to the packet that sent right now
        System.arraycopy(dataArray, indexInData, currDataPacket, 6, chunkSize);

        connections.send(this.connectionId, currDataPacket);

        indexInData += chunkSize;
        blockNum++;
        // blockNumArray = new byte[] { (byte) (blockNum >> 8), (byte) (blockNum & 0xff) };

        // if this is the last packet
        if (currDataPacket.length < 518) {

            // reset all
            transferComplete = true;
            dataArray = new byte[0];
            blockNum = 0;
            blockNumArray[0] = 0;
            blockNumArray[1] = 0;
            indexInData = 0;
        }
    }

    private void broadcast(byte[] message) {
        for (Integer id : idsLogin.keySet()) {
            connections.send(id, message);
        }
    }

    // method to ack the client that the request accept
    private byte[] ackResponse() {
        return new byte[] { 0, 4, 0, 0 };
    }

    // method for check if specific file exists in File
    private boolean fileExist(String fileName) {
        return (new File(filesDir, fileName).exists());
    }

}
