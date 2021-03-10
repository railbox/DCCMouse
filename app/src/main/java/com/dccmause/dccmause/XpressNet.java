package com.dccmause.dccmause;

import android.os.Handler;
import android.util.Log;

class XpressNet {
    private static final byte XNET_HEADER       = 0;	//Message header
    private static final byte XNET_DATA_1	    = 1;	//Data byte 1
    private static final byte XNET_DATA_2	    = 2;	//Data byte 2
    private static final byte XNET_DATA_3	    = 3;	//Data byte 3
    private static final byte XNET_DATA_4	    = 4;	//Data byte 4
    //private static final byte XNET_DATA_5	    = 5;	//Data byte 5
    //private static final byte XNET_DATA_6	    = 6;	//Data byte 6
    //private static final byte XNET_DATA_7	    = 7;	//Data byte 7
    private static final byte XNET_BUF_SIZE     = 8;

// certain global Xpressnet status indicators
    static final byte CS_NORMAL         = 0x00; // Normal Operation Resumed
    static final byte CS_ESTOP          = 0x01; // Emergency stop
    static final byte CS_TRACK_OFF      = 0x02; // Track voltage off
    static final byte CS_TRACK_SHORTED  = 0x04; // Track short circuit
    static final byte CS_SERV_MODE      = 0x08; // Service Mode


    private static final short func_group_mask[] = {0x0F,0x0F,0x0F,0xFF,0xFF};
    private static final byte func_group_shift[] = {1,5,9,13,21};

    private static final Handler mHandler = new Handler();
    private static final byte[] MsgBuf = new byte[XNET_BUF_SIZE];
    private static byte MsgBufStorePos = 0;

    private static boolean inServiceMode = false;
    private static boolean exitServiceMode = false;
    private static boolean serviceModeResponded = false;

    interface Callback {
        void notifyXNetPower(byte status);
        void notifyXNetExtControl(short locoAddress);
        void notifyXNetExtSpeed(short locoAddress, byte steps, short value);
        void notifyXNetExtFunc(short locoAddress, int funcMask, int funcStatus);
        void notifyXNetService(boolean directMode, short CV, short value);
        void notifyXNetServiceError();
        void notifyXNetFeedback(short address, byte stateMask, byte state);
        boolean sendData(byte[] data);
    }
    private static Callback mXpressNetCallback;

    static void setCallbackHandler(Callback callback) {
        mXpressNetCallback = callback;
    }
    //--------------------------------------------------------------------------------------------
    private static final Runnable TimeoutHandler = new Runnable() {
        @Override
        public void run() {
            MsgBufStorePos = 0;
        }
    };

    static void ParseReceivedData(byte[] data) { //work on received data
        for (byte dataByte : data) {
            if (MsgBufStorePos == 0){
                mHandler.postDelayed(TimeoutHandler, 100);
            }
            MsgBuf[MsgBufStorePos] = dataByte; //First byte is a callbyte
            MsgBufStorePos++;

            if ((MsgBufStorePos >= 2) && ((MsgBuf[0] & 0x0F) + 2 == MsgBufStorePos)) {
                mHandler.removeCallbacks(TimeoutHandler);
                ParseXNetMsg(MsgBuf,MsgBufStorePos);
                MsgBufStorePos = 0;
            }
            else if (MsgBufStorePos >= XNET_BUF_SIZE) {
                MsgBufStorePos = 0;
            }
        }
    }

    private static void ParseXNetMsg(byte[] XNetMsg, byte len){
        if (checkXOR(XNetMsg, len)) {
            switch (XNetMsg[XNET_HEADER]) {
                case 0x61:  //Broadcast
                    switch (XNetMsg[XNET_DATA_1]){
                        case 0x00: // Track power off
                            mXpressNetCallback.notifyXNetPower(CS_TRACK_OFF);
                            break;
                        case 0x01: // Normal Operation Resumed
                            mXpressNetCallback.notifyXNetPower(CS_NORMAL);
                            break;
                        case 0x02: // Service Mode Entry
                            inServiceMode = true;
                            mXpressNetCallback.notifyXNetPower(CS_SERV_MODE);
                            break;
                        case 0x08: // Track Short
                            mXpressNetCallback.notifyXNetPower(CS_TRACK_SHORTED);
                            break;
                        case 0x12: // Service mode: short circuit
                            mXpressNetCallback.notifyXNetServiceError();
                            serviceModeResponded = true;
                            if (inServiceMode && exitServiceMode)
                                setPower(CS_NORMAL);
                            break;
                        case 0x13: // Service mode: no ACK
                            mXpressNetCallback.notifyXNetServiceError();
                            serviceModeResponded = true;
                            if (inServiceMode && exitServiceMode)
                                setPower(CS_NORMAL);
                            break;
                        case (byte)0x80: //Transfer error
                            Log.i("XpressNet","Transfer error");
                            break;
                        case (byte)0x81: //Command station busy
                            Log.i("XpressNet","Command station busy");
                            break;
                        case (byte)0x82: //Instruction not supported
                            Log.i("XpressNet","Instruction not supported");
                            break;
                    }
                case 0x62: //Command status response
                    if (XNetMsg[XNET_DATA_1] == 0x22) {
                        switch (XNetMsg[XNET_DATA_2]) {
                            case 0x00:    //CS_NORMAL
                                mXpressNetCallback.notifyXNetPower(CS_NORMAL);
                                break;
                            case 0x01:    //CS_TRACK_OFF
                                mXpressNetCallback.notifyXNetPower(CS_TRACK_OFF);
                                break;
                            case 0x02:    //CS_ESTOP
                                mXpressNetCallback.notifyXNetPower(CS_ESTOP);
                                break;
                            case 0x08:    //CS_SERV_MODE
                                mXpressNetCallback.notifyXNetPower(CS_SERV_MODE);
                                break;
                        }
                    }
                    break;
                case 0x63: //Service mode response
                    short CV = (short)(XNetMsg[XNET_DATA_2] & 0xFF);
                    short value = (short)(XNetMsg[XNET_DATA_3] & 0xFF);
                    switch (XNetMsg[XNET_DATA_1]){
                        case 0x10: //Register and Page mode
                            mXpressNetCallback.notifyXNetService(false,CV,value);
                            break;
                        case 0x14: //Direct mode
                            mXpressNetCallback.notifyXNetService(true,CV,value);
                            break;
                    }
                    serviceModeResponded = true;
                    if (inServiceMode && exitServiceMode)
                        setPower(CS_NORMAL);
                    break;
                case (byte) 0x81: //Emergency Stop
                    if (XNetMsg[XNET_DATA_1] == 0x00) {
                        mXpressNetCallback.notifyXNetPower(CS_ESTOP);
                    }
                    break;
                case (byte) 0xE3:
                    if (XNetMsg[XNET_DATA_1] == 0x40) { //Locomotive is started to control from other device
                        short address = (short)((XNetMsg[XNET_DATA_2]<<8) + XNetMsg[XNET_DATA_3]);
                        mXpressNetCallback.notifyXNetExtControl(address);
                    }
                case (byte) 0xE4:
                    byte funcGroup;
                    short address = (short)((XNetMsg[XNET_DATA_2]<<8) + XNetMsg[XNET_DATA_3]);
                    switch (XNetMsg[XNET_DATA_1])
                    { //TODO calculate the correct speed for 14,27,28 steps(see page 36)
                        case 0x10: //Speed and direction instruction for 14 speed steps
                            mXpressNetCallback.notifyXNetExtSpeed(address,(byte)14,XNetMsg[XNET_DATA_4]);
                            break;
                        case 0x11: //Speed and direction instruction for 27 speed steps
                            mXpressNetCallback.notifyXNetExtSpeed(address,(byte)27,XNetMsg[XNET_DATA_4]);
                            break;
                        case 0x12: //Speed and direction instruction for 28 speed steps
                            mXpressNetCallback.notifyXNetExtSpeed(address,(byte)28,XNetMsg[XNET_DATA_4]);
                            break;
                        case 0x13: //Speed and direction instruction for 128 speed steps
                            mXpressNetCallback.notifyXNetExtSpeed(address,(byte)128,XNetMsg[XNET_DATA_4]);
                            break;
                        case 0x20: //Function instruction group 1
                            funcGroup = 0;
                            mXpressNetCallback.notifyXNetExtFunc(address,1+getFuncMask(funcGroup),
                                    (byte)(((XNetMsg[XNET_DATA_4]&0x10)>>4) + getFuncState(funcGroup,XNetMsg[XNET_DATA_4])));
                            break;
                        case 0x21: //Function instruction group 2
                            funcGroup = 1;
                            mXpressNetCallback.notifyXNetExtFunc(address,getFuncMask(funcGroup),getFuncState(funcGroup,XNetMsg[XNET_DATA_4]));
                            break;
                        case 0x22: //Function instruction group 3
                            funcGroup = 2;
                            mXpressNetCallback.notifyXNetExtFunc(address,getFuncMask(funcGroup),getFuncState(funcGroup,XNetMsg[XNET_DATA_4]));
                            break;
                        case 0x23: //Function instruction group 4
                            funcGroup = 3;
                            mXpressNetCallback.notifyXNetExtFunc(address,getFuncMask(funcGroup),getFuncState(funcGroup,XNetMsg[XNET_DATA_4]));
                            break;
                        case 0x28: //Function instruction group 5
                            funcGroup = 4;
                            mXpressNetCallback.notifyXNetExtFunc(address,getFuncMask(funcGroup),getFuncState(funcGroup,XNetMsg[XNET_DATA_4]));
                            break;
                    }
                default:
                    break;
            }
            if ((XNetMsg[XNET_HEADER] >= 0x42) && (XNetMsg[XNET_HEADER] <= 0x4E)){
                byte size = (byte)(XNetMsg[XNET_HEADER]&0xF);
                for (byte i=0; i < size; i+=2){
                    if ((XNetMsg[size+2] & 0x10) != 0){
                        mXpressNetCallback.notifyXNetFeedback(XNetMsg[size+1], (byte)0xF0, (byte) ((XNetMsg[size+2] & 0x0F) << 4));
                    }else {
                        mXpressNetCallback.notifyXNetFeedback(XNetMsg[size + 1], (byte) 0x0F, (byte) (XNetMsg[size + 2] & 0x0F));
                    }
                }
            }
        }
        //else XOR is wrong
    }

    //--------------------------------------------------------------------------------------------
    static boolean setPower(byte power)
    {
        boolean ret = false;
        switch (power) {
            case CS_NORMAL:
                //byte[] PowerAn = {0x21, (byte)0x81, (byte)0xA0, 0x21, (byte)0x81, (byte)0xA0};
                byte[] PowerAn = {0x21, (byte)0x81, (byte)0xA0};
                ret = mXpressNetCallback.sendData(PowerAn);
                break;
            case CS_ESTOP:
                //byte[] EmStop = {(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80};
                byte[] EmStop = {(byte)0x80, (byte)0x80};
                ret = mXpressNetCallback.sendData(EmStop);
                break;
            case CS_TRACK_OFF:
                //byte[] PowerAus = {0x21, (byte)0x80, (byte)0xA1, 0x21, (byte)0x80, (byte)0xA1};
                byte[] PowerAus = {0x21, (byte)0x80, (byte)0xA1};
                ret = mXpressNetCallback.sendData(PowerAus);
                break;
        }
        return ret;
    }

    //--------------------------------------------------------------------------------------------
    static boolean setSpeed(short locoAddress, byte steps, byte speed) {
        byte LocoInfo[] = {(byte)0xE4, 0x13, 0x00, 0x00, speed, 0x00 };
        switch (steps) {
            case 14: LocoInfo[1] = 0x10; break;
            case 27: LocoInfo[1] = 0x11; break;
            case 28: LocoInfo[1] = 0x12; break;
            //case (byte)128: LocoInfo[1] = 0x13; break; //default to 128 Steps!
        }
        LocoInfo[2] = (byte)(locoAddress >> 8);
        LocoInfo[3] = (byte)(locoAddress & 0xFF);
        getXOR(LocoInfo);
        return mXpressNetCallback.sendData(LocoInfo);
    }

    //--------------------------------------------------------------------------------------------
    static boolean setLocoFunc(short locoAddress, byte num, int funcStates) {
        byte LocoInfo[] = {(byte)0xE4, 0x00, 0x00, 0x00, 0x00, 0x00 };
        byte stateByte;
        LocoInfo[2] = (byte)(locoAddress >> 8);
        LocoInfo[3] = (byte)(locoAddress & 0xFF);
        //Group 1: 0 0 0 F0 F4 F3 F2 F1
        if (num <= 4) {
            stateByte = (byte)(((funcStates&0x1)<<4) + getFuncByte((byte)0,funcStates));
            LocoInfo[1] = 0x20;
            LocoInfo[4] = stateByte;
        }
        //Group 2: 0 0 0 0 F8 F7 F6 F5
        else if (num <= 8){
            stateByte = getFuncByte((byte)1,funcStates);
            LocoInfo[1] = 0x21;
            LocoInfo[4] = stateByte;
        }
        //Group 3: 0 0 0 0 F12 F11 F10 F9
        else if (num <= 12){
            stateByte = getFuncByte((byte)2,funcStates);
            LocoInfo[1] = 0x22;
            LocoInfo[4] = stateByte;
        }
        //Group 4: F20 F19 F18 F17 F16 F15 F14 F13
        else if (num <= 20){
            stateByte = getFuncByte((byte)3,funcStates);
            byte[] LocoInfoMM = {(byte)0xE4, (byte)0xF3, 0x00, 0x00, stateByte, 0x00};	//normal: 0x23!
            LocoInfoMM[3] = (byte)(locoAddress >> 8);
            LocoInfoMM[4] = (byte)(locoAddress & 0xFF);
            getXOR(LocoInfoMM);
            mXpressNetCallback.sendData(LocoInfoMM);

            LocoInfo[1] = 0x23;
            LocoInfo[4] = stateByte;
        }
        //Group 5: F28 F27 F26 F25 F24 F23 F22 F21
        else if (num <= 28){
            stateByte = getFuncByte((byte)4,funcStates);
            LocoInfo[1] = 0x28;
            LocoInfo[4] = stateByte;
        }
        getXOR(LocoInfo);
        return mXpressNetCallback.sendData(LocoInfo);
    }

    //--------------------------------------------------------------------------------------------
    //Trnt Change position
    static boolean setTrntPos(short address, boolean state, boolean active) {
        byte TrntInfo[] = {0x52, (byte)0x80, 0x00, 0x00 };
        TrntInfo[1] = (byte)(address >> 2);
        TrntInfo[2] |= (byte)((address & 0x03) << 1);
        TrntInfo[2] |= (active ? 1: 0) << 3;
        TrntInfo[2] |= state ? 1 : 0;
        getXOR(TrntInfo);
        return mXpressNetCallback.sendData(TrntInfo);
    }

    //--------------------------------------------------------------------------------------------
    //set CV
    static boolean setCV(short address, byte value) {
        byte cvInfo[] = {0x23, 0x16, 0x00, 0x00, 0x00 };
        cvInfo[2] = (byte)address;
        cvInfo[3] = value;
        getXOR(cvInfo);
        if (mXpressNetCallback.sendData(cvInfo)){
            exitServiceMode = true;
            requestForServiceModeResult();
            return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------
    //read CV Request
    static boolean requestReadCV(short address){
        byte cvInfo[] = {0x22, 0x15, 0x00, 0x00 };
        cvInfo[2] = (byte)address;
        getXOR(cvInfo);
        if (mXpressNetCallback.sendData(cvInfo)) {
            exitServiceMode = true;
            requestForServiceModeResult();
            return true;
        }
        return false;
    }

    private final static Runnable mServiceModeResponseChecker = new Runnable() {
        byte counter = 0;
        @Override
        public void run() {
            if ((!serviceModeResponded) && (counter < 3)) {
                counter++;
                byte serviceMode[] = {0x21, 0x10, 0x31};
                mXpressNetCallback.sendData(serviceMode);
                mHandler.postDelayed(mServiceModeResponseChecker, 200);
            }else if (!serviceModeResponded){
                counter = 0;
                setPower(CS_NORMAL);
            }else counter = 0;
        }
    };

    private static void requestForServiceModeResult(){
        serviceModeResponded = false;
        mHandler.postDelayed(mServiceModeResponseChecker, 200);
    }

    //--------------------------------------------------------------------------------------------
    // calculate the XOR
    private static void getXOR (byte[] data) {
        byte XOR = 0x00;
        for (short i = 0; i < data.length-1; i++) {
            XOR = (byte)(XOR ^ data[i]);
        }
	    data[data.length-1] = XOR;
    }

    private static boolean checkXOR(byte[] data, byte size) {
        byte XOR = 0x00;
        for (short i = 0; i < size-1; i++) {
            XOR = (byte)(XOR ^ data[i]);
        }
        return data[size-1] == XOR;
    }


    private static byte getFuncByte(byte group, int funcStates){
        return (byte)( (funcStates >> func_group_shift[group]) & func_group_mask[group] );
    }

    private static int getFuncState(byte group, byte funcByte){
        return (funcByte & func_group_mask[group]) << func_group_shift[group];
    }

    private static int getFuncMask(byte group){
        return func_group_mask[group] << func_group_shift[group];
    }
}
