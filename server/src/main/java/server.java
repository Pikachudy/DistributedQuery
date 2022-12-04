import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Server端
 * @author: Piakchudy
 * @date: 2022/12/3
 */
public class server {
    private String shell_dir;
    private String shell_name;
    private String xml_dir;
    server(){
        shell_dir = "/home/ubuntu/Distribution/DistributedQuery/script/src/main/shell";
        shell_name= "select.sh";
        xml_dir = "/home/ubuntu/Distribution/xmlBlocks/";
    }

    /**
     * 调用脚本查询文件
     * @param shell_dir shell所在路径
     * @param shell_name shell名称
     * @param args shell参数
     * @return shell返回值
     * @throws IOException 捕获shell输出失败
     */
    private String callShell(String shell_dir,String shell_name, String[] args) throws IOException {
        List<String> command = new ArrayList<String>();
        command.add("./" + shell_name);
        for(String s : args){
            command.add(s);
        }
        System.out.println(command);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(shell_dir));

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
            System.out.println("call shell failed.Error coed:"+runningStatus);
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
        server server = new server();
        String [] shell_args = new String[4];
        shell_args[0] = "Yuval Cassuto";
        shell_args[1] = "2019";
        shell_args[2] = "2022";
        shell_args[3] = server.xml_dir+"output_0002.xml";

        System.out.println(server.callShell(server.shell_dir,server.shell_name,shell_args));

    }
}
