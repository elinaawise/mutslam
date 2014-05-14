package mutslam;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;

public class MiniRunner {
  public static void main(String[] args) throws Exception {
    MiniDFSCluster dfscluster = new MiniDFSCluster.Builder(new Configuration()).build();
    //System.out.println(dfscluster.getURI());
    
    MiniAccumuloConfig macConfig = new MiniAccumuloConfig(new File(args[0]), "secret");
    Map<String, String> site = new HashMap<String, String>();
    site.put("instance.volumes", dfscluster.getURI().toString());
    
    MiniAccumuloCluster mac = new MiniAccumuloCluster(macConfig);
    mac.start();
    
    System.out.println("instance.zookeeper.host="+mac.getZooKeepers());
    System.out.println("instance.name="+mac.getInstanceName());
    System.out.println("user.name=root");
    System.out.println("user.password=secret");
  }
}
