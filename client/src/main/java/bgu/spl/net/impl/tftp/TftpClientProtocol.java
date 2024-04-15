package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TftpClientProtocol {
    private FileOutputStream fileOutputStream;
    private String fileName;
    private int lastBlockReceived = 0;
    private Map<String, FileOutputStream> openFiles = new HashMap<>();
    private String currentOperation = "";
    private String currentFilename = "";
    private String wrqfile = null;
    private StringBuilder dirqData = new StringBuilder();
    private int blockNumber = 1;
    public boolean close = false;
    private byte[] dataTowrite;
    private String lastAction = "";
    String filesDirectory = "client";

    public byte[] process(byte[] message) {
        byte[] b = new byte[] { message[0], message[1] };
        int opcode = (short) (((short) b[0]) << 8 | (short) (b[1]));

        switch (opcode) {
            case 1:
                return rrqResponse(message);
            case 2:
                return handleWRQ(message);
            case 3:
                return dataResponse(message);
            case 4:
                byte[] bb = new byte[] { message[2], message[3]};
                int toPrint = (short) (((short) bb[0]) << 8 | (short) (bb[1]&0xff));
                if(wrqfile!=null){
                    if(this.blockNumber == 1){
                        Path path = Paths.get(wrqfile);
                        try{
                            this.dataTowrite = Files.readAllBytes(path);
                        }
                        catch(IOException e){}
                    }
                    System.out.println("ACK"+toPrint);
                    return sendData();
                }
                else{
                    if(lastAction.equals("Disc"))
                        close = true;
                    lastAction = "";
                    System.out.println("ACK"+toPrint);
                    return null;
                }
            case 5:
                gettingError(message);
                return null;
            case 6:
                return handleDirq(message);
            case 7:
                return handleLOGRQ(message);
            case 8:
                return delrqResponse(message);
            case 9:
                return bcastResponse(message);
            case 10:
                return discResponse(message);

            default:
                break;
        }
        return null;
    }
    private void gettingError(byte[]message){
        String error = new String(message,4,message.length-5);
        System.out.println(("Error"+message[3]+" "+error));
        currentFilename = "";
        wrqfile = null;
    }
    public byte[] handleWRQ(byte[] message) {
        // Extract the filename from the WRQ message
        String filename = extractFilename(message);
        String filename2 =  extractFilename(message); //"client/";
        // // Create a path in the current working directory for the new file
        // Path path = Paths.get(filename);
        // try {
        //     // Create the new file. If file already exists, it will not be replaced.
        //     Files.createFile(path);
        // } catch (FileAlreadyExistsException e) {
        // } catch (IOException e) {
        //     System.out.println("An error occurred: " + e.getMessage());
        // }
        File file = new File(filename2);
        // Check if the file does not exist and create a new file
        if (!file.exists()) {
            System.out.println("File doesn't exist in claint");
            return null;
        }      
        wrqfile = filename;
        return message;
    }
    public byte[] rrqResponse(byte[] message) {
        String filename = extractFilename(message); // Prepend 'target/' to the filename
        currentFilename = filename; // Store the expected filename for later use
        currentOperation = "RRQ";
        try {
            File file = new File(filename);
            // Check if the file does not exist and create a new file
            if (!file.exists()) {
                // boolean created = file.getParentFile().mkdirs(); // Ensure parent directories exist
                // created = file.createNewFile();
                // if (created) {
                    // Prepare to write data to the new file
                    fileOutputStream = new FileOutputStream(file, false); // 'false' means overwrite existing file
                // } else {
                //     System.out.println("Failed to create file: " + filename);
                // }
            } else {
                // If file already exists, prepare to overwrite it
                // 
                System.out.println("File already exist");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print the stack trace to standard error
        }
        return message;
    }
    private String extractFilename(byte[] message) {
        // Start after the opcode (2 bytes)
        int i = 2; // Start index after opcode
        // Find the zero byte indicating the end of the filename
        while (message[i] != 0 && i < message.length) {
            i++;
        }
        // Convert the bytes to a string assuming UTF-8 encoding
        return new String(message, 2, i - 2, StandardCharsets.UTF_8);
    }

    private byte[] handleLOGRQ(byte[] message){
        return message;
    }

    private byte[] delrqResponse(byte[] message){
        return message;
    }

    private byte[] bcastResponse(byte[] message){
        String filenme = new String(message, 3, message.length - 4, StandardCharsets.UTF_8);
        if(message[2]==0){
            System.out.println("BCAST"+" del "+ filenme);
        }
        if(message[2]==1){
            System.out.println("BCAST"+" add "+ filenme);
        }
        return null;
    }
    private byte[] discResponse(byte[] message){
        lastAction = "Disc";
        return message;
    }

    public byte[] dataResponse(byte[] message)  {
        try{
        ByteBuffer buffer = ByteBuffer.wrap(message);
        short opcode = buffer.getShort(); // Assuming opcode for data is at the start
        short size = buffer.getShort();
        short blockNumber = buffer.getShort(); // Following TFTP structure for DATA packets
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
            if ("RRQ".equals(currentOperation)) {
                FileOutputStream fos = openFiles.getOrDefault(currentFilename, null);
                if (fos == null) {
                    // If file isn't open, this is the first block of data for this file
                    File file = new File(currentFilename);
                    if (!file.exists()) file.createNewFile(); // Create file if it doesn't exist
                    fos = new FileOutputStream(file, true); // Append mode
                    openFiles.put(currentFilename, fos);
                }
                fos.write(data);
                if (data.length < 512) { // Last block
                    fos.close();
                    openFiles.remove(currentFilename); // Remove from map as no longer needed
                    System.out.println("RRQ "+ currentFilename + " complete");
                }
                // Send ACK back to server
                return sendAck(blockNumber);
            } else if ("DIRQ".equals(currentOperation)) {
                // Accumulate directory listing data
                dirqData.append(new String(data, StandardCharsets.UTF_8));

                // Check if this is the last packet (less than 512 bytes of data)
                if (data.length < 512) {
                    // Last packet, process and print the directory listing
                    processAndPrintDirectoryListing();
                    dirqData = new StringBuilder(); // Reset for next DIRQ if needed
                }
        }
            return sendAck(blockNumber);

        }
        catch(IOException e){}
        return null;
    }
    private byte[] handleDirq(byte[] message){
        currentOperation = "DIRQ";
        return message;
    }
    private void processAndPrintDirectoryListing() {
        // Assuming filenames are separated by newlines in the accumulated data
        String[] filenames = dirqData.toString().split("\0");
        for (String filename : filenames) {
            String trimmedFilename = filename.trim(); // Remove leading and trailing spaces
            if (!trimmedFilename.isEmpty()) { // Check if the trimmed filename is not empty
                System.out.println(trimmedFilename);
            }
        }
    }


    private byte[] sendAck(int blockNumber) {
        ByteBuffer ackBuffer = ByteBuffer.allocate(4); // ACK packets are 4 bytes long
        ackBuffer.putShort((short) 4); // Opcode for ACK is typically 4 in TFTP
        ackBuffer.putShort((short) blockNumber);
        return ackBuffer.array();
    }
    private void closeFile() throws IOException {
        if (fileOutputStream != null) {
            fileOutputStream.close();
            fileOutputStream = null; // Prevent writing to closed file
        }
    }
    private byte[] sendData(){
        if(dataTowrite == null)
                return null;
        Integer lengthTowrite = this.dataTowrite.length-(this.blockNumber-1)*512;
        if(lengthTowrite!=null && lengthTowrite>=0){
            byte[] data;
            if(lengthTowrite>=512){
                data = new byte[518];
            }    
            else {
                data = new byte[lengthTowrite+6];
            }
             byte[] opcode = new byte[]{(byte)(((short)3)>>8), (byte)(((short)3)&0xff)};  
             data[0] = opcode[0];
             data[1] = opcode[1];
             int size= Math.min(512, lengthTowrite);
             byte[] dataSize= new byte[]{(byte)(((short)size)>>8),(byte)(((short)size)& 0xff)};
             data[2]=dataSize[0];
             data[3]=dataSize[1];
             byte[] blockNumberInBytes=new byte[]{(byte)(((short)blockNumber)>>8),(byte)(((short)blockNumber)& 0xff)};
             data[4]=blockNumberInBytes[0];
             data[5]=blockNumberInBytes[1];
             for(short index=6; index<data.length;index++){
                data[index]=this.dataTowrite[(index-6)+512*(this.blockNumber-1)];
            }
            blockNumber++;
            return data;
        }
        else{
            System.out.println("WRQ " + wrqfile + " complete");
            blockNumber=1;
            dataTowrite=null;
            wrqfile=null;
            return null;
        }


    }

}