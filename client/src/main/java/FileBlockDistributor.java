import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 文件块分配器
 * @author: Pikachudy
 * @date: 2022/12/7
 */
public class FileBlockDistributor {
    private final ConsistentHash consistentHash;
    FileBlockDistributor(String[] serverList){
        this.consistentHash = new ConsistentHash(serverList);
    }
    // 向文件块分配器中添加服务器
    public void addServer(String server) {
        consistentHash.addServer(server);
    }

    // 从文件块分配器中移除服务器
    public void removeServer(String server) {
        consistentHash.removeServer(server);
    }

    // 将文件块分配到服务器上
    public void distribute(String fileBlock) {
        // 计算文件块的哈希值
        long hash = consistentHash.hashFunction.hashString(fileBlock, StandardCharsets.UTF_8).asLong();
        // 根据哈希值找到离它最近的服务器
        String server = consistentHash.getServer(fileBlock);
        // 将文件块复制到多台服务器上
        List<String> servers = getReplicas(server, Constants.REPLICAS);
        for (String s : servers) {
            store(s, fileBlock);
        }
    }

    /**
     * 根据服务器找到其冗余服务器
     * @param server 服务器Label
     * @param count 冗余服务器数目
     * @return 冗余服务器列表
     */
    public List<String> getReplicas(String server, int count) {
        // 存储冗余服务器的列表
        List<String> replicas = new ArrayList<>();
        // 遍历服务器列表
        for (int j=0;j<consistentHash.serverList.length;++j) {
            // 选取后三个服务器
            if (consistentHash.serverList[j].equals(server)) {
                for(int i=1;i<=count;++i){
                    replicas.add(consistentHash.serverList[(j+i)%consistentHash.serverList.length]);
                }
                break;
            }
        }
        // 返回冗余服务器列表
        return replicas;
    }
    public String getServer(String block_key){
        return this.consistentHash.getServer(block_key);
    }

    /**
     * 打印文件块最近服务器、冗余服务器信息
     * @param blocks 文件块列表
     */
    public void computBlockServerAndReplicasServer(List<String> blocks){
        System.out.println("\t文件块\t最近服务器\t冗余服务器\t");
        for(String block : blocks){
            // 计算文件块的哈希值
            long hash = consistentHash.hashFunction.hashString(block, StandardCharsets.UTF_8).asLong();
            // 根据哈希值找到离它最近的服务器
            String server = consistentHash.getServer(block);
            // 服务器对应的冗余服务器
            List<String> servers = getReplicas(server, Constants.REPLICAS);
            System.out.println("\t"+block+"\t"+server+"\t"+servers+"\t");
        }

    }

    /**
     * 存储文件块至服务器
     * @param fileBlock 文件名称
     */
    private void blockStore(String fileBlock){
        // 计算文件块的哈希值
        long hash = consistentHash.hashFunction.hashString(fileBlock, StandardCharsets.UTF_8).asLong();
        // 找到离它最近的服务器及冗余服务器
        String server = consistentHash.getServer(fileBlock);
        // 服务器对应的冗余服务器
        List<String> servers = getReplicas(server, Constants.REPLICAS);
        servers.add(server);
        for(String s : servers){
            store(s,fileBlock);
        }
    }

    /**
     * 利用作业2，将文件块存至某一服务器
     * @param server 服务器label
     * @param fileBlock 文件块名称
     * @return 是否ok
     */
    private boolean store(String server, String fileBlock) {
        try {
            Socket s = new Socket();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String file_name = fileBlock;
            // 发起请求
            writer.write("put_block\n");
            writer.flush();
            // 等待响应
            String msg = reader.readLine();
            if(!msg.equals("request_ok")){
                System.out.println("块上传请求未被正确响应，传输中止");
                return false;
            }
            // 发送文件名
            writer.write(file_name+'\n');
            writer.flush();
            // 等待响应
            msg = reader.readLine();
            if(!msg.equals("name_ok")){
                System.out.println("块文件名未被正确响应，传输中止");
                return false;
            }
            // 从文件中读取
            int block_size =1024*1024*768;
            String file_path = "/home/ubuntu/Distribution/XmlBlocks/";
            byte [] block; // 缓冲区
            try{
                // 读取本地文件
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file_path+"\\"+file_name));
                block = new byte[block_size];
                int len=-1;
                while ((len = bis.read(block))!=-1){
                }

            } catch (FileNotFoundException e) {
                System.out.println("找不到指定文件，请检查文件路径及文件名称");
                return false;
            } catch (IOException e) {
                System.out.println("本地文件读取失败！");
                return false;
            }
            // 准备文件传输
            BufferedOutputStream bos = new BufferedOutputStream(s.getOutputStream());
            bos.write(block);
            bos.flush();
            // 等待响应
            msg = reader.readLine();
            if(!msg.equals("block_ok")){
                System.out.println("块文件上传失败，传输中止");
                return false;
            }
        } catch (IOException e) {
            //throw new RuntimeException(e);
            System.out.println(e);
            System.out.println("上传文件块时创建IO流失败");
            return false;
        }
        return true;
    }
}

/**
 * 哈希环
 * @author: Pikachudy
 * @date: 2022/12/7
 */
class ConsistentHash {
    // 用于存储服务器的哈希值
    private TreeMap<Long, String> servers = new TreeMap<>();
    // 存储服务器列表
    String[] serverList;
    // 用于计算服务器哈希值
    HashFunction hashFunction = Hashing.md5();

    ConsistentHash(String[]serverList){
        this.serverList = serverList;
        for(String server : this.serverList){
            this.addServer(server);
        }
    }

    /**
     * 向环中添加一个服务器
     * @param server 服务器label
     */
    public void addServer(String server) {
        long hash = hashFunction.hashString(server, StandardCharsets.UTF_8).asLong();
        servers.put(hash, server);
    }

    /**
     * 向环中移除一个服务器
     * @param server 服务器label
     */
    public void removeServer(String server) {
        long hash = hashFunction.hashString(server, StandardCharsets.UTF_8).asLong();
        servers.remove(hash);
    }

    /**
     * 根据文件块的哈希值找到环上离它最近的服务器
     * @param key 文件块标签
     * @return 最近服务器label
     */
    public String getServer(String key) {
        long hash = hashFunction.hashString(key, StandardCharsets.UTF_8).asLong();
        if (!servers.containsKey(hash)) {
            // 如果没有环上有恰好对应该哈希值的服务器，则找到离它最近的服务器
            SortedMap<Long, String> tailMap = servers.tailMap(hash);
            hash = tailMap.isEmpty() ? servers.firstKey() : tailMap.firstKey();
        }
        return servers.get(hash);
    }
}