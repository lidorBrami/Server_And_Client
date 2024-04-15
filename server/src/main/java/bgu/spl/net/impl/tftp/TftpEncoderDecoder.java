package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.util.LinkedList;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private LinkedList<Byte> pack;
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
        if(pack.size()<=3){
            return null;
        }
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

    private byte[] isDisc(){
        pack=null;
        return opCode;
    }
}