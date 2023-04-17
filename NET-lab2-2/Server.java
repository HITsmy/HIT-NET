import java.io.*;
import java.net.UnknownHostException;

public class Server {
    public static void main(String[] args) throws IOException {
        File file1 = new File("./serverFile/server.txt");
        File file2 = new File("./serverFile/client.txt");
        GBN server = new GBN("localhost",8081,8080);
        System.out.println("下面开始进行文件传输");
        System.out.println("------------------------");
        System.out.println("开始从localhost:8080接收文件");
        //开始接收文件
        ByteArrayOutputStream receiveBuffer = null;
        while(true) {
            receiveBuffer = server.receive();
            if(receiveBuffer.size() != 0) {
                System.out.println(receiveBuffer.size());
                FileOutputStream out = new FileOutputStream(file2);
                byte[] receiveData = receiveBuffer.toByteArray();
                out.write(receiveData,2,receiveData.length-2);

                out.close();
                System.out.println("成功接收到文件并保存到serverFile文件夹下的client.txt");
                break;
            }
        }
        System.out.println("------------------------");
        //开始发送文件
        System.out.println("开始向localhost:8080发送文件");
        FileInputStream input = new FileInputStream(file1);
        ByteArrayOutputStream sendBuffer = StreamCopy(input);
        System.out.println("开始向localhost:8080发送server.txt文件");
        System.out.println("长度"+sendBuffer.size());
        server.send(sendBuffer.toByteArray());
    }
    public static ByteArrayOutputStream StreamCopy(InputStream input) throws IOException {
        byte[] bytes = new byte[100];
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int len;
        while((len = input.read(bytes)) != -1) {
            result.write(bytes,0,len);
        }
        return result;
    }
}
