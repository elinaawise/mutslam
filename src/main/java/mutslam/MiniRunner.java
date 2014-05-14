package mutslam;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.MiniDFSCluster;

public class MiniRunner {
  public static void main(String[] args) throws Exception {
    MiniDFSCluster dfscluster = new MiniDFSCluster.Builder(new Configuration()).build();
    //System.out.println(dfscluster.getURI());
    
    MiniAccumuloConfig macConfig = new MiniAccumuloConfig(new File(args[0]), "secret");
    Map<String, String> site = new HashMap<String, String>();
    // instance.volumes was not working, and the following does not seem to work either
    site.put("instance.dfs.uri", dfscluster.getURI().toString());
    macConfig.setSiteConfig(site);
    
    System.out.println(site);

    MiniAccumuloCluster mac = new MiniAccumuloCluster(macConfig);
    mac.start();
    
    System.out.println("instance.zookeeper.host="+mac.getZooKeepers());
    System.out.println("instance.name="+mac.getInstanceName());
    System.out.println("user.name=root");
    System.out.println("user.password=secret");
  }
}
