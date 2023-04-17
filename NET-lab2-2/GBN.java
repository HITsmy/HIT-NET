import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class GBN {

    private int aimPort; // 目的端口号
    private int srcPort; // 源端口号
    private InetAddress aimHost; // 目的主机
    private static final int windowSize = 9; // 滑动窗口大小
    private static final int TIME_OUT = 3000; // 超时时间
    private final int TIMER = 3000; // 用来充当计时器

    private static final int seqNum = 256; // 序号空间大小
    private int sendBase = 0; // 滑动窗口内seq的最小值(一定不可以为static)

    private static final int MAX_LEN = 1024; // 数据报包含最大字节数
    private int hasReceivedSeq = -1; // 已经收到的数据报的序号，初始设置为-1

    /**
     * 构造函数
     * @param aimHost
     * @param aimPort
     * @param srcPort
     * @throws UnknownHostException
     */
    public GBN(String aimHost,int aimPort,int srcPort) throws UnknownHostException {
        this.aimHost = InetAddress.getByName(aimHost);
        this.aimPort = aimPort;
        this.srcPort = srcPort;
    }

    /**
     *  根据接收窗口判断滑动窗口是否可以滑动
     * @param ACKs
     * @return
     */
    public boolean isAbleSlide(List<Integer> ACKs) {
        for(Integer ACK : ACKs) {
            if(ACK == 0)
                return false;
        }
        return true;
    }

    /**
     *
     * @param dataToSend
     * @throws IOException
     */
    public void send(byte[] dataToSend) throws IOException {
        int hasSendBytes = 0; // 已经发送的字节数
        int length; // 一次可以发送的长度
        List<Integer> receiveWindow = new ArrayList<>(); // 接收窗口
        List<DatagramPacket> sendWindow = new ArrayList<>(); // 发送窗口
        int hasSendSeq = sendBase; // 已发送的数据报序号
        int time = 0; // 计时器
        DatagramSocket socket = new DatagramSocket(srcPort); // 发送socket
        do {
            while(receiveWindow.size() < windowSize && hasSendSeq <= seqNum && hasSendBytes < dataToSend.length) {
                // 将发送窗口中增加一个数据报
                sendWindow.add(new DatagramPacket(new byte[1],1));
                // 确定发送数据包长度
                length = dataToSend.length - hasSendBytes;
                if(length > MAX_LEN)
                    length = MAX_LEN;
                byte[] temp = new byte[length];
                System.arraycopy(dataToSend,hasSendBytes,temp,0,length);
                DatagramPacket sendPacket = makePacket(temp,hasSendSeq,sendBase); // 要发送的数据报
                socket.send(sendPacket);
                sendWindow.set(hasSendSeq-sendBase,sendPacket); // 发送窗口中记录下来发送的数据报
                receiveWindow.add(0); // 接收窗口增加一位待接收
                System.out.println("已发送序号为"+hasSendSeq+"的数据报");
                hasSendSeq++;
                hasSendBytes += length;
            }
            socket.setSoTimeout(TIME_OUT);
            DatagramPacket receivePacket = null; // 用于接收ACK数据报
            int lastAckSeq = -2; // 上一次接收ACK号
            int currentAckSeq = -1; // 本次接收ACK号
            boolean isRepeat = false; // 稍后判断是否有重复分组

            while(!isAbleSlide(receiveWindow)) {
                try {
                    byte[] receiveBytes = new byte[MAX_LEN];
                    receivePacket = new DatagramPacket(receiveBytes,receiveBytes.length);
                    socket.receive(receivePacket);
                    currentAckSeq = (int) (receiveBytes[0] - sendBase); // 在窗口内的序号
                    if(currentAckSeq == lastAckSeq) {
                        // 发现重复ACK
                        System.out.println("发现重复ACK");
                        System.out.println("即将重新发送"+receiveBytes[0]+1+"到"+sendBase+windowSize+"的所有数据报");
                        isRepeat = true;
                        break;
                    }
                    if(currentAckSeq >= 0)
                        receiveWindow.set(currentAckSeq,1);
                    lastAckSeq = currentAckSeq;
                    System.out.println("收到ACK"+currentAckSeq);
                    time = 0; // 计时器重新计时
                } catch (SocketTimeoutException e) {
                    time += 1000;
                    System.out.println("接收ACK超时");
                    if(time > TIMER)
                        break;
                }
            }
            // 判断是否超时或出现重复ACK
            if(time>TIMER || isRepeat) {
                int i = sendBase;
                for (Integer j : receiveWindow) {
                    if(j==0) {
                        i = j;
                        break;
                    }
                }
                //重传之后的所有数据报
                for(int j=i; j<sendWindow.size(); j++) {
                    byte[] bytes = new byte[MAX_LEN];
                    byte[] data = sendWindow.get(j).getData();
                    System.arraycopy(data,2,bytes,0,data.length-2);
                    DatagramPacket sendPacket = makePacket(dataToSend,sendBase+j,sendBase);
                    socket.send(sendPacket);
                    System.out.println("已重新发送序号为"+sendBase+j+"的数据报");
                }
                time = 0; // 重启计时器
            }
            //滑动窗口
            int i = 0;
            int sum = receiveWindow.size();
            while(i<sum) {
                if(receiveWindow.get(i)==1) {
                    System.out.println("滑动");
                    receiveWindow.remove(i);
                    sendWindow.remove(i);
                    sum--;
                }
                else
                    break;
            }
            // 检查序号seq是否用尽，用尽后要回到初始
            if(sendBase>seqNum) {
                sendBase -= 256;
                hasSendSeq -= 256;
            }
        }while (hasSendBytes<dataToSend.length || receiveWindow.size()!=0);
        //关闭套接字
        socket.close();
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public ByteArrayOutputStream receive() throws IOException {
        DatagramSocket socket = new DatagramSocket(srcPort);
        DatagramPacket receivePacket = null;
        socket.setSoTimeout(TIME_OUT);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int time = 0; //计时器
        while(true) {
            try {
                byte[] receiveData = new byte[MAX_LEN];
                receivePacket = new DatagramPacket(receiveData,receiveData.length);
                socket.receive(receivePacket);
                int seq =  receiveData[0] & 0xFF;
                System.out.println("接收到序号为"+seq+"的数据报");
                if(seq - hasReceivedSeq == 1) { // 是期望接收到的序号
                    System.out.println("接收到正确的数据报");
                    buffer.write(receiveData,0,receiveData.length);
                    DatagramPacket AckPacket = makeAckPacket(seq);
                    socket.send(AckPacket);
                    hasReceivedSeq++;
                    time = 0;
                }
                else { // 不是期待收到的数据报序号
                    System.out.println("不是期待收到的数据报序号");
                    DatagramPacket AckPacket = makeAckPacket(hasReceivedSeq);
                    socket.send(AckPacket);
                    // 可进行人为干预
                }
            } catch (SocketTimeoutException e) {
                time += 1000;
            }
            if(time > TIME_OUT) {
                System.out.println("超时，停止等待");
                break;
            }
        }
        socket.close();
        return buffer;
    }


    /**
     * 制作数据报函数
     * @param dataToSend
     * @param seq
     * @param base 窗口最左侧seq值
     * @return
     */

    public DatagramPacket makePacket(byte[] dataToSend,int seq,int base) {
        byte[] bytes = new byte[dataToSend.length+2];
        bytes[0] = (byte) seq; // 0号位存放数据报seq号
        bytes[1] = (byte) base; // 1号位存放当前窗口基值
        System.arraycopy(dataToSend,0,bytes,2,dataToSend.length);
        DatagramPacket packet = new DatagramPacket(bytes,dataToSend.length+2);
        packet.setAddress(aimHost);
        packet.setPort(aimPort);
        return packet;
    }
    public DatagramPacket makeAckPacket(int seq) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) seq;
        bytes[1] = 0;
        DatagramPacket AckPacket = new DatagramPacket(bytes,bytes.length);
        AckPacket.setPort(aimPort);
        AckPacket.setAddress(aimHost);
        return AckPacket;
    }


}
