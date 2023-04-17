import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Server {

    private static int senderState = 1; // 发送状态 包含1234四种
    private static int receiverState = 1; // 接收状态 包含12两种状态
    private static final int PORT = 8080; // 服务器所在端口号
    private static final int TIME_OUT = 1000; // 超时时间设置为1秒
    private static final int MAX_LEN = 1500; //传输数据包的最大字节数

    public static void main(String[] args) throws IOException {
        InetAddress clientAddress = InetAddress.getByName("localhost");
        int clientPort = 8081;
        System.out.println("Server: 即将向客户机发送 Hello client!");
        rdtSend("Hello client!".getBytes(StandardCharsets.UTF_8),clientAddress,clientPort);
        byte[] receiveData = rdtReceive();
        System.out.println("Server: 收到相应"+new String(receiveData));

        System.out.println("Server: 即将向客户机发送 Good Morning!");
        rdtSend("Good Morning!".getBytes(StandardCharsets.UTF_8),clientAddress,clientPort);
        byte[] receiveData1 = rdtReceive();
        System.out.println("Server: 收到相应"+new String(receiveData1));

        System.out.println("Server: 即将向客户机发送 Good Afternoon!");
        rdtSend("Good Afternoon!".getBytes(StandardCharsets.UTF_8),clientAddress,clientPort);
        byte[] receiveData2 = rdtReceive();
        System.out.println("Server: 收到相应"+new String(receiveData2));

        System.out.println("---------------------------------------");
        System.out.println("模拟出错情况");
        System.out.println("Server: 即将向客户机发送 Good Night!");
        rdtSend("Good Night!".getBytes(StandardCharsets.UTF_8),clientAddress,clientPort);


        System.out.println("---------------------------------------");
        System.out.println("文件传输功能：");
        FileReader fileReader = new FileReader("./ServerFile/server.txt");
        char[] fileData = new char[MAX_LEN];
        int n = fileReader.read(fileData);
        fileReader.close();
        byte[] fileDataBytes = new String(fileData,0,n).getBytes(StandardCharsets.UTF_8);
        rdtSend(fileDataBytes,clientAddress,clientPort);
        System.out.println("Server: server.txt文件传输成功");
        System.out.println("开始从客户端接收文件2.txt");
        fileDataBytes = rdtReceive();
        FileWriter fileWriter = new FileWriter("./ServerFile/client.txt");
        fileWriter.write(new String(fileDataBytes,0,fileDataBytes.length));
        fileWriter.close();
        System.out.println("文件传输完毕");
    }

    /**发送数据包函数
     *
     * @param dataToSend 要发出的数据字节数组
     * @param address 目标ip
     * @param port 目标端口号
     */
    public static void rdtSend(byte[] dataToSend, InetAddress address, int port) throws IOException {
        DatagramSocket socketToSend = null;
        DatagramSocket socketToReceive = null;
        DatagramPacket packetToSend = null;
        DatagramPacket packetToReceive = null;

        // 判断当前发送状态
        if(senderState==1) {
            // 生成数据包
            packetToSend = Rdt_Utils.makeDataPacket(0,dataToSend);
            packetToSend.setAddress(address);
            packetToSend.setPort(port);
            // 发送数据包
            socketToSend = new DatagramSocket(PORT);
            socketToSend.send(packetToSend);
            socketToSend.close();
            // 更改senderState状态为2（等待回应状态）
            senderState = 2;
            byte[] dataToReceive = new byte[MAX_LEN];
            socketToReceive = new DatagramSocket(PORT);
            socketToReceive.setSoTimeout(TIME_OUT); // 设置超时时间
            packetToReceive = new DatagramPacket(dataToReceive,dataToReceive.length);
            while(true) {
                try {
                    socketToReceive.receive(packetToReceive);
                    socketToReceive.close();
                    int askSeq = Rdt_Utils.getAckSeq(packetToReceive);
                    if(askSeq == 0) { //接收到正确的ACK
                        System.out.println("接收到ACK0，数据发送成功");
                        senderState = 3;
                        break;
                    }
                    else {
                        System.out.println("没有接收到ACK0，接收到了ACK1，即将重新发送数据包并重启计时器");
                        throw new SocketTimeoutException();
                    }
                }catch (SocketTimeoutException e) {
                    socketToReceive.close();
                    System.out.println("响应超时，即将重新发送数据包并重启计时器");
                    socketToSend = new DatagramSocket(PORT);
                    socketToSend.send(packetToSend);
                    socketToSend.close();
                    socketToReceive = new DatagramSocket(PORT);
                    socketToReceive.setSoTimeout(TIME_OUT);
                }

            }

        }
        else if(senderState == 3) {
            packetToSend = Rdt_Utils.makeDataPacket(1,dataToSend);
            packetToSend.setAddress(address);
            packetToSend.setPort(port);
            // 发送数据包
            socketToSend = new DatagramSocket(PORT);
            socketToSend.send(packetToSend);
            socketToSend.close();
            // 更改senderState状态为4（等待回应状态）
            senderState = 4;
            byte[] dataToReceive = new byte[MAX_LEN];
            socketToReceive = new DatagramSocket(PORT);
            socketToReceive.setSoTimeout(TIME_OUT); // 设置超时时间
            packetToReceive = new DatagramPacket(dataToReceive,dataToReceive.length);
            while(true) {
                try {
                    socketToReceive.receive(packetToReceive);
                    socketToReceive.close();
                    int askSeq = Rdt_Utils.getAckSeq(packetToReceive);
                    if(askSeq == 1) { //接收到正确的ACK
                        System.out.println("接收到ACK1，数据发送成功");
                        senderState = 1;
                        break;
                    }
                    else {
                        System.out.println("没有接收到ACK1，接收到了ACK0，即将重新发送数据包并重启计时器");
                        throw new SocketTimeoutException();
                    }
                }catch (SocketTimeoutException e) {
                    socketToReceive.close();
                    System.out.println("响应超时，即将重新发送数据包并重启计时器");
                    socketToSend = new DatagramSocket(PORT);
                    socketToSend.send(packetToSend);
                    socketToSend.close();
                    socketToReceive = new DatagramSocket(PORT);
                    socketToReceive.setSoTimeout(TIME_OUT);
                }

            }
        }
    }

    /**接收数据包函数
     *
     * @return 返回字节数组便于调用者打印
     */
    public static byte[] rdtReceive() throws IOException {
        DatagramSocket socketToSend = null;
        DatagramSocket socketToReceive = null;
        DatagramPacket packetToSend = null;
        DatagramPacket packetToReceive = null;
        byte[] dataToReceive = new byte[MAX_LEN];
        packetToReceive = new DatagramPacket(dataToReceive,dataToReceive.length);
        // 判断当前接收状态
        if(receiverState == 1) {
            while(true) {
                socketToReceive = new DatagramSocket(PORT);
                socketToReceive.receive(packetToReceive);
                socketToReceive.close();
                int seq = Rdt_Utils.getPacketSeq(packetToReceive); // 检查当前数据包seq值
                if(seq == 0) {
                    // 制作ACK数据包并发送
                    packetToSend = Rdt_Utils.makeACKPacket(0);
                    packetToSend.setAddress(packetToReceive.getAddress());
                    packetToSend.setPort(packetToReceive.getPort());
                    socketToSend = new DatagramSocket(PORT);
                    socketToSend.send(packetToSend);
                    socketToSend.close();
                    receiverState = 2; // 更新接收状态
                    return Rdt_Utils.getPacketDataString(packetToReceive).getBytes(StandardCharsets.UTF_8);
                }
            }
        }
        else if(receiverState == 2) {
            while(true) {
                socketToReceive = new DatagramSocket(PORT);
                socketToReceive.receive(packetToReceive);
                socketToReceive.close();
                int seq = Rdt_Utils.getPacketSeq(packetToReceive);
                if(seq == 1) {
                    // 制作ACK数据包并发送
                    packetToSend = Rdt_Utils.makeACKPacket(1);
                    packetToSend.setAddress(packetToReceive.getAddress());
                    packetToSend.setPort(packetToReceive.getPort());
                    socketToSend = new DatagramSocket(PORT);
                    socketToSend.send(packetToSend);
                    socketToSend.close();
                    receiverState = 1; // 更新接收状态
                    return Rdt_Utils.getPacketDataString(packetToReceive).getBytes(StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }
}
