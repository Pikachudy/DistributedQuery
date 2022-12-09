import lombok.Data;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Client端
 * @author: Pikachudy
 * @date: 2022/12/4
 */
public class client {
    private final int server_num;
    private final int block_num;
    private final Socket[] sockets;
    private final String[] server_list;
    private final int start_port;
    private Logger logger = Logger.getLogger("client");
    private client(int server_num, int block_num,int start_port){
        try {
            FileHandler fileHandler = new FileHandler("log.txt");
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.server_num = server_num;
        this.block_num = block_num;
        this.start_port = start_port;
        this.server_list=new String[this.server_num];
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
    static client createClient(int server_num, int block_num,int start_port) {
        return new client(server_num,block_num,Constants.START_PORT);
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
            sockets[i].setSoTimeout(8000);
            server_list[i]= String.valueOf(sockets[i].getPort()-this.start_port);
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
        list.addAll(years);
        return list;
    }

    /**
     * 参数列表字符序列化
     * @param arg_list 参数列表
     * @return 序列化后的字符串，以'|'分割
     */
    private String list2String(List<String> arg_list){
        StringBuilder arg= new StringBuilder();
        for(int i = 0; i<arg_list.size();++i){
            arg.append(arg_list.get(i));
            if(i!=arg_list.size()-1){
                arg.append("|");
            }
        }
        return arg.toString();
    }

    /**
     * 处理查询
     * @param arg_list 查询参数列表
     * @return 是否成功
     */
    private boolean dealRequest(List<String> arg_list){
        RequestCounter counter = new RequestCounter(this.block_num);
        counter.resetCount();
        // 发送请求 ----- 采用多线程
        String args = this.list2String(arg_list);
        Thread[] thread_list = new Thread[this.block_num];
        long start_time = System.currentTimeMillis();
        for(int i = 0; i< this.block_num;++i){
            // 在参数最后增加查询的块号
            thread_list[i] = new Thread(new CommunicationManager(this.logger,args+"|"+i,counter,sockets,this.server_list,i));
            thread_list[i].start();
        }
        int i=0;
        while (counter.getCount()>0){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // 若运行到此处，则ok了（吧?
        if(counter.getCount()==0){
            logger.info("--------------------------------------" +
                    "\n查询结果为:"+counter.getResult()+
                    "\n查询用时" +(System.currentTimeMillis()-(double)start_time)/1000+"s");
        }
        return true;
    }
    public static void main(String[] args) throws IOException {
        Logger logger = Logger.getLogger("client");

        client c = createClient(Constants.SERVER_NUM,Constants.BLOCK_NUM,1100);

        while (true){
            List<String> arg_list = c.readConsole();
            if(arg_list == null) {
                break;
            }
            else{
                c.dealRequest(arg_list);
            }
        }

//        FileBlockDistributor distributor = new FileBlockDistributor(new String[]{"0","1","2","3","4","5"});
//        distributor.computBlockServerAndReplicasServer(List.of((new String[]{"0","1","2","3","4","5"})));

    }
}

/**
 * 客户端-服务端通信管理器——便于多线程
 * @author: Pikachudy
 * @date: 2022/12/6
 */
@Data
class CommunicationManager implements Runnable{
    private FileBlockDistributor distributor;
    private final RequestCounter counter;
    private Socket[] sockets;
    private String[] server_list;
    private String request_msg;
    private int block_index;
    private Logger logger;
    @Override
    public void run() {
        // 取出socket
        Socket socket = this.sockets[Integer.parseInt(this.distributor.getServer(String.valueOf(block_index)))];
        String socket_label = String.valueOf(socket.getPort()-Constants.START_PORT);
        // 取出冗余服务器对应label列表便于查找冗余服务器上的文件块,便于正式服务器异常时的处理
        List<String> replicas_server_label = this.distributor.getReplicas(this.distributor.getServer(String.valueOf(block_index)),Constants.REPLICAS);
        // 尝试次数--便于出错时遍历冗余服务器label列表
        int try_count=0;
        while (true){
            try {
                // 上锁防止数据混淆
                synchronized (socket){
                    this.sendMessage(socket,this.request_msg);
                    logger.info("等待"+socket_label+"服务端回应……");
                    String result= this.waitingForRes(socket);

                    // 若能运行到此处说明未发生异常
                    logger.info(socket_label+"返回结果为:"+result);
                    // 修改counter
                    synchronized (this.counter){
                        this.counter.addResult(Integer.parseInt(result));
                        this.counter.countDown();
                    }
                    // 退出
                    break;
                }
            } catch (IOException e) {
                // 发生异常，则此时无法跳出循环
                if(try_count<Constants.REPLICAS){
                    socket = this.sockets[Integer.parseInt(replicas_server_label.get(try_count++))];
                    socket_label = String.valueOf(socket.getPort()-Constants.START_PORT);
                    logger.warning("等待超时，尝试从"+socket_label+"查询第"+(try_count+1)+"个冗余块……");

                }
                else{
                    logger.severe("已有超过"+Constants.REPLICAS+"个服务端宕机，查询失败");
                    // 修改counter
                    synchronized (this.counter){
                        this.counter.setCount(-1);
                    }
                    break;
                }
            }
        }

    }

    /**
     *
     * @param counter 计数器
     * @param sockets socket表——便于遇到异常时处理
     * @param block_index 当前尝试搜索的文件块号
     */
    CommunicationManager(Logger logger,String requestMsg,RequestCounter counter,Socket[] sockets,String[] server_list,int block_index){
        this.request_msg = requestMsg;
        this.counter = counter;
        this.sockets = sockets;
        this.distributor = new FileBlockDistributor(server_list);
        this.block_index = block_index;
        this.logger = logger;
    }

    /**
     * 发送信息至server
     * 若连接断开则抛出异常
     * @param socket 尝试连接的服务端的socket
     * @param msg 发送信息
     * @throws IOException socket连接断开
     */
    void sendMessage(Socket socket,String msg) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        writer.write(msg+'\n');
        writer.flush();
    }

    /**
     * 等待接收消息
     * 若连接断开则抛出异常
     * @param socket 尝试连接的服务端的socket
     * @return 接收到的查询结果
     * @throws IOException socket连接断开
     */
    String waitingForRes(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return reader.readLine();
    }



}

/**
 * 请求计数器
 * @author: Pikachudy
 * @date: 2022/12/6
 */
@Data
class RequestCounter{
    private int count;
    private int request_num;
    private int result;
    RequestCounter(int request_num){
        this.request_num = request_num;
        this.result=0;
    }

    /**
     * 将计数器设定为 request_num
     */
    public void resetCount(){
        this.count = this.request_num;
    }

    /**
     * 计数减 1
     */
    public void countDown(){
            this.count--;
    }

    /**
     * 增加结果
     * @param result
     */
    public void addResult(int result){
        this.result+=result;
    }

}

