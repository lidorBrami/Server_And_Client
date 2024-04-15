package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.util.LinkedList;
import java.util.List;

public class TftpClientEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    public LinkedList<Byte> pack;
    private byte[] opCode=new byte[2];

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this

        if(pack==null){
            pack=new LinkedList();
            pack.add(nextByte);
            opCode[0]=nextByte;
            return null;
        }
        if(pack.size()==1){
            pack.add(nextByte);
            opCode[1]=nextByte;
            if(nextByte==6){
                return isDirc();
            }
            if(nextByte==10){
                return isDisc();
            }
            if(nextByte<0 | nextByte>10){
                pack=null;
                return opCode;
            }
            return null;
        }
        else{
            if(opCode[1]== 1){
                return isRrc(nextByte);
            }
            if(opCode[1]==2){
                return isWrc(nextByte);
            }
            if(opCode[1]==3){
                return isData(nextByte);
            }
            if(opCode[1]==4){
                return isAck(nextByte);
            }
            if(opCode[1]==5){
                return isError(nextByte);
            }
            if(opCode[1]==7){
                return isLogrq(nextByte);
            }
            if(opCode[1]==8){
                return isDelrq(nextByte);
            }
            if(opCode[1]==9){
                return isBcast(nextByte);
            }
        }


        

        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        //TODO: implement this

        return message;
    }

    //the following methods *decode* their respective packets
    private byte[] isRrc(byte nextByte){
        pack.add(nextByte);
        if(nextByte==0){
            Object[] ret = pack.toArray();
            byte[] ret1 = new byte[ret.length];
            for(int i=0;i<ret1.length;i++){
                ret1[i]=(byte)ret[i];
            }
            pack=null;
            return ret1;
        } 
        return null;   
    }

    private byte[] isWrc(byte nextByte){
        pack.add(nextByte);
        if(nextByte==0){
            Object[] ret = pack.toArray();
            byte[] ret1 = new byte[ret.length];
            for(int i=0;i<ret1.length;i++){
                ret1[i]=(byte)ret[i];
            }
            pack=null;
            return ret1;
        } 
        return null;   
    }

    private byte[] isData(byte nextByte){
        pack.add(nextByte);
        if(pack.size()>3){
            byte [] packetSizeInBytes = new byte []{pack.get(2) , pack.get(3)};
            int packetSize = ( int ) ((( int ) packetSizeInBytes [0]) << 8 | ( int ) ( packetSizeInBytes [1]) & 0x00ff);
            if(packetSize+6==pack.size()){
                Object[] ret = pack.toArray();
                byte[] ret1 = new byte[ret.length];
                for(int i=0;i<ret1.length;i++){
                    ret1[i]=(byte)ret[i];
                }
                pack=null;
                return ret1;
            }
    }
        return null;
    }

    private byte[] isAck(byte nextByte){
        pack.add(nextByte);
        if(pack.size()==4){
            Object[] ret = pack.toArray();
            byte[] ret1 = new byte[ret.length];
            for(int i=0;i<ret1.length;i++){
                ret1[i]=(byte)ret[i];
            }
            pack=null;
            return ret1;
        }
        return null;
    }

    private byte[] isError(byte nextByte){
        pack.add(nextByte);
        if(nextByte==0&&pack.size()>4){
            Object[] ret = pack.toArray();
            byte[] ret1 = new byte[ret.length];
            for(int i=0;i<ret1.length;i++){
                ret1[i]=(byte)ret[i];
            }
            pack=null;
            return ret1;
        }
        return null;
    }

    private byte[] isDirc(){
        pack=null;
        return opCode;
    }

    private byte[] isLogrq(byte nextByte){
        pack.add(nextByte);
        if(nextByte==0){
            Object[] ret = pack.toArray();
            byte[] ret1 = new byte[ret.length];
            for(int i=0;i<ret1.length;i++){
                ret1[i]=(byte)ret[i];
            }
            pack=null;
            return ret1;
        }
        return null;
    }

    private byte[] isDelrq(byte nextByte){
        pack.add(nextByte);
        if(nextByte==0){
            Object[] ret = pack.toArray();
            byte[] ret1 = new byte[ret.length];
            for(int i=0;i<ret1.length;i++){
                ret1[i]=(byte)ret[i];
            }
            pack=null;
            return ret1;
        }
        return null;
    }

    private byte[] isBcast(byte nextByte){
        pack.add(nextByte);
        if(nextByte==0 && pack.size()>3){
            Object[] ret = pack.toArray();
            byte[] ret1 = new byte[ret.length];
            for(int i=0;i<ret1.length;i++){
                ret1[i]=(byte)ret[i];
            }
            pack=null;
            return ret1;
        }
        return null;
    }

    private byte[] isDisc(){
        pack=null;
        return opCode;
    }
}
//     //TODO: Implement here the TFTP encoder and decoder
//     private List<Byte> pack=null;
//     private byte[] opCode=new byte[2];

//     @Override
//     public byte[] decodeNextByte(byte nextByte) {
//         // TODO: implement this
//         if(pack==null){
//             pack=new LinkedList();
//             pack.add(nextByte);
//             opCode[0]=nextByte;
//             return null;
//         }
//         if(pack.size()==1){
//             pack.add(nextByte);
//             opCode[1]=nextByte;
//             if(nextByte==6){
//                 return isDirc();
//             }
//             if(nextByte==10){
//                 return isDisc();
//             }
//             if(nextByte<0 | nextByte>10){
//                 pack=null;
//                 return opCode;
//             }
//             return null;
//         }
//         else{
//             if(nextByte==1){
//                 return isRrc(nextByte);
//             }
//             if(nextByte==2){
//                 return isWrc(nextByte);
//             }
//             if(nextByte==3){
//                 return isData(nextByte);
//             }
//             if(nextByte==4){
//                 return isAck(nextByte);
//             }
//             if(nextByte==5){
//                 return isError(nextByte);
//             }
//             if(nextByte==7){
//                 return isLogrq(nextByte);
//             }
//             if(nextByte==8){
//                 return isDelrq(nextByte);
//             }
//             if(nextByte==9){
//                 return isBcast(nextByte);
//             }
//         }


        

//         return null;
//     }

//     @Override
//     public byte[] encode(byte[] message) {
//         //TODO: implement this

//         return message;
//     }










//     //the following methods *decode* their respective packets
//     private byte[] isRrq() {
//         if (pack.size() > 2 && pack.get(pack.size() - 1) == 0) { // Assuming the packet ends with a 0 byte
//             return convertListToArray();
//         }
//         return null;
//     }
    
//     private byte[] isWrq() {
//         if (pack.size() > 2 && pack.get(pack.size() - 1) == 0) { // Assuming the packet ends with a 0 byte
//             return convertListToArray();
//         }
//         return null;
//     }
    
//     private byte[] isData() {
//         if (pack.size() >= 4) { // Minimum size for a DATA packet header
//             byte[] packetSizeInBytes = new byte[]{pack.get(2), pack.get(3)};
//             int packetSize = ((packetSizeInBytes[0] & 0xff) << 8) | (packetSizeInBytes[1] & 0xff);
//             if (pack.size() == packetSize + 4) { // Including the header
//                 return convertListToArray();
//             }
//         }
//         return null;
//     }
    
//     private byte[] isAck() {
//         if (pack.size() == 4) { // ACK packets are of fixed size
//             return convertListToArray();
//         }
//         return null;
//     }
    
//     private byte[] isError() {
//         if (pack.size() > 4 && pack.get(pack.size() - 1) == 0) { // Assuming the error message ends with a 0 byte
//             return convertListToArray();
//         }
//         return null;
//     }
    
//     private byte[] isDirq() {
//         pack.clear(); // Reset for next message
//         return opCode; // Assuming DIRQ packets have no payload, just opcode
//     }
    
//     private byte[] isLogrq() {
//         if (pack.size() > 2 && pack.get(pack.size() - 1) == 0) { // Assuming the request ends with a 0 byte
//             return convertListToArray();
//         }
//         return null;
//     }
    
//     private byte[] isDelrq() {
//         if (pack.size() > 2 && pack.get(pack.size() - 1) == 0) { // Assuming the request ends with a 0 byte
//             return convertListToArray();
//         }
//         return null;
//     }
    
//     private byte[] isBcast() {
//         if (pack.size() > 3 && pack.get(pack.size() - 1) == 0) { // BCAST might be fixed or variable size, adjust as needed
//             return convertListToArray();
//         }
//         return null;
//     }
    
//     private byte[] isDisc() {
//         pack.clear(); // Reset for next message
//         return opCode; // Assuming DISC packets have no payload, just opcode
//     }
    
//     private byte[] convertListToArray() {
//         byte[] ret = new byte[pack.size()];
//         for (int i = 0; i < pack.size(); i++) {
//             ret[i] = pack.get(i);
//         }
//         pack.clear(); // Reset for next message
//         return ret;
//     }
// }