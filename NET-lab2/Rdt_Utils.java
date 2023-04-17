import java.net.DatagramPacket;
import java.net.DatagramSocket;

/*
* Rdt传输协议的工具类*/
public class Rdt_Utils {
    /**构造一个ACK数据包
     * @param AckSeq 表示一个数据包的ACK是0还是1
     * @return 返回一个ACK数据包
     */
    public static DatagramPacket makeACKPacket(int AckSeq) {
        byte[] data = new byte[2];
        data[0] = (byte)AckSeq;
        DatagramPacket packet = new DatagramPacket(data,data.length);
        return packet;
    }

    /**构造一个携带数据的数据包
     * @param seq 表示一个数据包的序号
     * @param data 携带数据的字节数组
     * @return 返回一个UDP携带数据的数据包
     */
    public static DatagramPacket makeDataPacket(int seq,byte[] data) {
        int length = data.length;
        byte[] dataToSend = new byte[length+2]; // 多出两位存放ACK和seq
        dataToSend[0] = 2;
        dataToSend[1] = (byte) seq;
        for(int i=0; i<length; i++) {
            dataToSend[i+2] = data[i];
        }
        DatagramPacket packet = new DatagramPacket(dataToSend,length+2);
        return packet;
    }

    /**得到ACK分组的ACK值
     * @return 返回ACK报文的值
     *
     */
    public static int getAckSeq(DatagramPacket packet) {
        byte[] bytes = packet.getData();
        if(bytes[0]==2) {   // 报文是含有数据的UDP数据包
            return -1;
        }
        else {
            return bytes[0];
        }
    }

    /**从含有数据的数据包中提取数据
     * @packet数据包
     * @return byte类型的数组
     *
     */
    public static byte[] getPacketData(DatagramPacket packet) {
        byte[] bytes = packet.getData();
        int length = bytes.length;
        if(bytes[0]!=2) {   //说明是ACK数据包
            return null;
        }
        else {
            byte[] data = new byte[length-2];
            for(int i=2; i<length; i++) {
                data[i-2] = bytes[i];
            }
            return data;
        }
    }

    /**从含有数据的数据包中提取数据字符串
     * @packet 数据包
     * @return String
     *
     */
    public static String getPacketDataString(DatagramPacket packet) {
        byte[] bytes = getPacketData(packet);
        if(bytes == null) {
            return null;
        }
        else {
            return new String(bytes,0,packet.getLength()-2);
        }
    }

    /**从含有数据的数据包中提取seq
     * @packet 数据包
     * @return int
     *
     */
    public static int getPacketSeq(DatagramPacket packet) {
        byte[] bytes = packet.getData();
        if(bytes[0]!=2) {
            return -1;
        }
        else {
            return bytes[1];
        }
    }

}
