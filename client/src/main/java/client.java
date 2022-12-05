import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Client端
 * @author: Piakchudy
 * @date: 2022/12/4
 */
public class client {
    private final int server_num;
    private final Socket[] sockets;
    private final int start_port;
    private client(int server_num, int start_port){
        this.server_num = server_num;
        this.start_port = start_port;
        try {
            this.sockets = this.establishConnection(this.server_num);
        } catch (IOException e) {
            System.out.println("Socket连接建立失败");
            throw new RuntimeException(e);
        }
    }

    /**
     * 工厂方法
     * @param server_num server数目
     * @param start_port 起始port
     * @return client实例
     */
    static client createClient(int server_num, int start_port) {
        return new client(server_num, start_port);
    }

    /**
     * 建立socket连接
     * @param server_num server数目
     * @return socket数组
     * @throws IOException
     */

    private Socket[] establishConnection(int server_num) throws IOException {
        Socket [] sockets = new Socket[server_num];
        for(int i=0;i<server_num;++i){
            int port = this.start_port+i;
            sockets[i] = new Socket(InetAddress.getLocalHost().getHostAddress(),port);
        }
        return sockets;
    }
    /**
     * 读取console输入的命令
     * @return 拆分后的List,若退出则返回null
     */
    private List<String> readConsole() throws IOException {

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("请输入所查询作者名称(输入quit则退出):");
        String s = console.readLine();
        if("quit".equals(s)){
            return null;
        }
        List<String> list = new ArrayList<>();
        list.add(s);
        System.out.println("是否需要年份筛选(y/n):");
        while (true){
            s = console.readLine();
            if("y".equals(s)|| "Y".equals(s)){
                break;
            }
            else if("N".equals(s)|| "n".equals(s)){
                return list;
            }
            else{
                System.out.println("请输入合法值(y/n)");
            }
        }

        System.out.println("请输入起止年份（若不限则输入'-'):");
        s=console.readLine();
        List<String> years = new ArrayList<>(List.of(s.split(" ")));
        for(String year : years){
            list.add(year);
        }
        return list;
    }

    /**
     * 参数列表字符序列化
     * @param arg_list 参数列表
     * @return 序列化后的字符串，以'|'分割
     */
    private String list2String(List<String> arg_list){
        String arg="";
        for(int i = 0; i<arg_list.size();++i){
            arg+= arg_list.get(i);
            if(i!=arg_list.size()-1){
                arg+="|";
            }
        }
        return arg;
    }
    private boolean sendRequest(List<String> arg_list) throws IOException {
        // 发送请求 ----- 采用多线程？此部分函数待定
        String args = this.list2String(arg_list);
        for(Socket socket : this.sockets){
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(args);
            writer.flush();
        }
        return true;
    }
    public static void main(String[] args) throws IOException {
        client c = createClient(2,1100);
        // 建立socket连接
        while (true){
            List<String> arg_list = c.readConsole();
            if(arg_list == null) {
                break;
            }
            else{
                c.sendRequest(arg_list);
            }
        }

    }
}
