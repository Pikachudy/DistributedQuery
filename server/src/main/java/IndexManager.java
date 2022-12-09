import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * description : 哈希索引表相关
 * @author : Pikachudy
 * @date : 2022/12/9 19:56
 */
public class IndexManager {
    /**
     * 从xml文件建立作者-论文数索引表
     * @param path xml文件路径
     * @return 作者-论文数索引表
     */
    MineMap<String,Integer> createIndex(String path) throws ParserConfigurationException, SAXException, IOException {
        // 使用 SAXParserFactory 创建 SAXParser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        // 使用 SAXParser 解析 XML 文件
        AuthorPaperCountHandler handler = new AuthorPaperCountHandler();
        parser.parse(new File(path), handler);

        // 使用 AuthorPaperCountHandler 获取解析后的作者论文数量映射
        MineMap<String,Integer> res = new MineMap<>(handler.getAuthorPaperCount());
        return res;
    }

    /**
     * map序列化存储到指定路径
     * @param map 映射表
     * @param file_name 映射表名称（不含后缀）
     * @param path 存储路径
     */
    private void SerializeMap(MineMap<String,Integer> map,String file_name,String path){
        // 序列化
        try {
            // 创建序列化输出流
            FileOutputStream fos = new FileOutputStream(path+file_name+".ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // 序列化Map对象
            oos.writeObject(map);
            // 关闭输出流
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件中反序列化Map
     * @param file_name 文件名称
     * @param path 文件路径
     * @return 反序列化后的Map
     */
    private MineMap<String, Integer> DeserializeMap(String file_name,String path){
        try{
            // 创建序列化输入流
            FileInputStream fis = new FileInputStream(path+file_name+".ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            // 反序列化Map对象
            MineMap<String, Integer> map = (MineMap<String, Integer>) ois.readObject();
            // 关闭输入流
            ois.close();
            return map;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        IndexManager manager = new IndexManager();
        // 获取所有元素
        MineMap<String, Integer> map = manager.createIndex("xmlBlocks\\output_0001.xml");
        // 序列化
        manager.SerializeMap(map,"author_paper_1","D:\\homework\\Distribution\\XmlPartition\\src\\main\\java\\index\\");
        // 反序列化
        MineMap<String, Integer> map1 = manager.DeserializeMap("author_paper_1","D:\\homework\\Distribution\\XmlPartition\\src\\main\\java\\index\\");
        int count=0;
        for(int year =1900;year<=2022;++year){
            count+=map1.get("Yuval Cassuto_"+year,0);
        }
        System.out.println(count);
        //System.out.println(map);
    }


}

/**
 * discription : 用于解析 XML文件,构建Map的 handler
 * @author : Pikachudy
 * @date : 2022/12/9 20:34
 */
class AuthorPaperCountHandler extends DefaultHandler {
    private Map<Object, Integer> authorPaperCount;
    private List<String> currentAuthor;
    private String currentYear;
    private boolean isAuthor;
    private boolean isYear;

    public AuthorPaperCountHandler() {
        this.authorPaperCount = new HashMap<Object, Integer>();
        this.currentAuthor = new ArrayList<>();
        this.currentYear = null;
        this.isAuthor = false;
        this.isYear = false;
    }

    public Map<Object, Integer> getAuthorPaperCount() {
        return authorPaperCount;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // 在开始元素时记录当前元素是否未作者或年份
        if ("author".equals(qName)) {
            isAuthor = true;
        } else if ("year".equals(qName)) {
            isYear = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // 在 characters 方法中，若为作者或年份，则记录作者和发表年份
        if (isAuthor) {
            currentAuthor.add(new String(ch, start, length));
            isAuthor = false;
        } else if (isYear) {
            currentYear = new String(ch, start, length);
            isYear = false;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // 在结束元素时更新作者论文数量映射
        if (qName.equals("article") || qName.equals("inproceedings")
                || qName.equals("proceedings") || qName.equals("book")
                || qName.equals("incollection") || qName.equals("phdthesis")
                || qName.equals("mastersthesis") || qName.equals("www")
                || qName.equals("person") || qName.equals("data")) {
            if (!currentAuthor.isEmpty() && currentYear != null) {
                // 构建索引
                for (String author : currentAuthor) {
                    String index = author + "_" + currentYear;
                    int count = authorPaperCount.getOrDefault(index, 0);
                    authorPaperCount.put(index, count + 1);
                }
            } else if (!currentAuthor.isEmpty()) {
                // 主要是为了记录<www>标签中的作者
                // 构建索引
                for (String author : currentAuthor) {
                    String index = author + "_null";
                    int count = authorPaperCount.getOrDefault(index, 0);
                    authorPaperCount.put(index, count + 1);
                }
            }
            currentAuthor.clear();
            currentYear = null;
        }
    }
}

/**
 * discription : 封装Map实现可序列化
 * @author : Pikachudy
 * @date : 2022/12/9 23:48
 */
class MineMap<S, I extends Number> implements Serializable {
    private Map<Object, Integer> map;
    public MineMap(){
        this.map = new HashMap<>();
    }
    public MineMap(Map<Object, Integer> map) {
        this.map = map;
    }

    /**
     * 获取Map中某key对应键值,可设定默认值。若不设定默认值则返回null
     * @param key 键
     * @return 键值
     */
    public Integer get(String key) {
        if(map.containsKey(key)){
            return (Integer) map.get(key);
        }
        return null;
    }
    /**
     * 获取Map中某key对应键值,可设定默认值。若不设定默认值则返回null
     * @param key 键
     * @return 键值
     */
    public Integer get(String key,Integer defaultValue) {
        return map.getOrDefault(key,defaultValue);
    }

    /**
     * 添加键值对
     * @param key 键
     * @param value 值
     */
    public void put(String key,Integer value){
        map.put(key,value);
    }
}