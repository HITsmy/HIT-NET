import java.io.*;
import java.net.UnknownHostException;

public class Client {
    public static void main(String[] args) throws IOException {
        File file1 = new File("./clientFile/client.txt");
        File file2 = new File("./clientFile/server.txt");
        GBN client = new GBN("localhost",8080,8081);
        FileInputStream input = new FileInputStream(file1);
        ByteArrayOutputStream sendBuffer = StreamCopy(input);
        System.out.println("开始向localhost:8080发送client.txt文件");
        System.out.println("长度"+sendBuffer.size());
        client.send(sendBuffer.toByteArray());

        System.out.println("------------------------");
        // 开始发送文件
        ByteArrayOutputStream receiveBuffer = null;
        while(true) {
            receiveBuffer = client.receive();
            if(receiveBuffer.size() != 0) {
                System.out.println(receiveBuffer.size());
                FileOutputStream out = new FileOutputStream(file2);
                byte[] receiveData = receiveBuffer.toByteArray();
                out.write(receiveData,2,receiveData.length-2);
                out.close();
                System.out.println("成功接收到文件并保存到clientFile文件夹下的server.txt");
                break;
            }
        }
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
