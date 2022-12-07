import lombok.Data;

import java.io.IOException;
import java.util.List;

/**
 * Server Runner 实现 Runnable
 * @author: Piakchudy
 * @date: 2022/12/5
 */
public class ServerRunner implements Runnable {
    private final server m_server;
    private final int server_label;
    private Thread thread;
    /**
     *
     * @param mServer 需要run的server
     * @param serverLabel server序号
     */
    ServerRunner(server mServer,int serverLabel,Thread thread){
        m_server = mServer;
        server_label = serverLabel;
        this.thread=thread;
    }

    @Override
    public void run() {
            while (true){
                if(m_server.startListen()){
                    // 与客户端成功建立socket连接
                    while(true){
                        try {
                            List<String> args = m_server.waitingForRequest();
                            System.out.println(m_server.getServer_label()+"号服务端正在检索:"+args);
                            String result = m_server.callShell(m_server.getShell_dir(), m_server.getShell_name(),args);
                            System.out.println(m_server.getServer_label()+"号服务端检索结果为:"+result);
                            m_server.replyRequest(result);
                        } catch (IOException | NullPointerException e) {
                            System.out.println(m_server.getServer_label()+"号服务端与客户端断开连接！重新监听……");
                            break;
                        }
                    }
                }
                else{
                    // 若断联则log并重新监听
                    System.out.println(m_server.getServer_label()+"号服务端与客户端socket连接失败！重新监听……");
                }
            }

    }
}
