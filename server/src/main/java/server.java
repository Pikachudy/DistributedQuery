import lombok.Data;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Server端
 * @author: Piakchudy
 * @date: 2022/12/3
 */
@Data
public class server {
    private final String shell_dir;
    private final String shell_name;
    private final String xml_dir;
    private final int server_num;
    private final int port;
    private Socket accept;
    server(int serverNumber){
        server_num= serverNumber;
        port=serverNumber+1100;
        shell_dir = "/home/ubuntu/Distribution/DistributedQuery/script/src/main/shell";
        shell_name= "select.sh";
        xml_dir = "/home/ubuntu/Distribution/xmlBlocks/";
    }
    /**
     * 监听相应端口，若端口被占用则抛出异常并输出
     * @return 若已建立连接返回true,添加监听成功返回false
     */
    public boolean startListen(){
        try{
            ServerSocket serverSocket = new ServerSocket(this.port);
            System.out.println(this.server_num+"号服务正在监听"+this.port+"端口……");
            this.accept = serverSocket.accept();
            System.out.println(port+"端口"+"已建立连接！");
            return true;
        } catch (IOException e) {
            System.out.println("端口"+this.port+"已被占用！");
            return false;
        }
    }

    /**
     * 接收socket流消息——查找参数
     * @return 返回消息中的--参数组成的列表
     */
    public List<String> waitingForRequest() throws IOException {
        System.out.println(this.port+":正在等待");
        BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
        // 获取请求
        String request = reader.readLine();
        return new ArrayList<>(List.of(request.split("\\|")));
    }

    /**
     * 返回检索结果至服务端
     * @param result 检索结果
     */
    public void replyRequest(String result) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(accept.getOutputStream()));
        writer.write(result+'\n');
        writer.flush();
    }
    /**
     * 调用脚本查询文件
     * @param shellDir shell所在路径
     * @param shellName shell名称
     * @param args shell参数
     * @return shell返回值
     * @throws IOException 捕获shell输出失败
     */
    public String callShell(String shellDir, String shellName, List<String> args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("./" + shellName);
        command.addAll(args);

        System.out.println(command);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(shellDir));

        // 调用脚本
        int runningStatus = 0;
        Process p = null;
        try {
            p = pb.start();
            try {
                runningStatus = p.waitFor();
            } catch (InterruptedException e) {
                System.out.println("shell run failed!\n"+e);
            }

        } catch (IOException e) {
            System.out.println("shell run failed\n"+e);
        }
        if (runningStatus != 0) {
            System.out.println("call shell failed.Error code:"+runningStatus);
        }

        // 返回值
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;
        while((line = reader.readLine())!=null){
            return line;
        }
        return null;
    }
    public static void main(String []args) throws IOException {
//        server server = new server(0);
//        String [] shell_args = new String[4];
//        shell_args[0] = "Yuval Cassuto";
//        shell_args[1] = "2019";
//        shell_args[2] = "2022";
//        shell_args[3] = server.xml_dir+"output_0002.xml";
//
//        System.out.println(server.callShell(server.shell_dir,server.shell_name,shell_args));
        final int SERVER_NUM = 2;
        Thread[] thread_list = new Thread[SERVER_NUM];
        for(int i=0;i<SERVER_NUM;++i){
            thread_list[i] = new Thread(new ServerRunner(i));
            thread_list[i].start();
        }

    }
}

