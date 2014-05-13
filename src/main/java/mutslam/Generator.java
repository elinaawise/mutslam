package mutslam;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;

public class Generator {

  static Mutation createRandomMutation(Random rand) {
    byte row[] = new byte[16];

    rand.nextBytes(row);

    Mutation m = new Mutation(row);

    byte cq[] = new byte[8];
    byte val[] = new byte[16];

    for (int i = 0; i < 3; i++) {
      rand.nextBytes(cq);
      rand.nextBytes(val);
      m.put("cf".getBytes(), cq, val);
    }

    return m;
  }

  static class WriteTask implements Runnable {

    private int numToWrite;
    private int numToBatch;
    private BatchWriter bw;
    private volatile long time = -1;
    private boolean flush;

    WriteTask(BatchWriter bw, int numToWrite, int numToBatch, boolean flush) throws Exception {
      this.bw = bw;
      this.numToWrite = numToWrite;
      this.numToBatch = numToBatch;
      this.flush = flush;
    }

    @Override
    public void run() {
      Random rand = new Random();

      try {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < numToWrite; i++) {
          Mutation mut = createRandomMutation(rand);
          for (int j = 0; j < numToBatch; j++) {
            bw.addMutation(mut);
          }
          if (flush)
            bw.flush();
        }

        if (!flush)
          bw.flush();

        long t2 = System.currentTimeMillis();
        this.time = t2 - t1;
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          bw.close();
        } catch (MutationsRejectedException e) {
          e.printStackTrace();
        }

      }

    }

    long getTime() {
      return time;
    }
  }

  public static void main(String[] args) throws Exception {

    ZooKeeperInstance zki = new ZooKeeperInstance("test16", "localhost");
    Connector conn = zki.getConnector("root", new PasswordToken("secret"));

    boolean walog = true;

    int tests[] = new int[] {1, 2, 8, 16, 32, 128};

    for (int i = 0; i < 6; i++) {
      if (i > 0) {
        System.out.println();
        System.out.println("RERUNNING all test");
        System.out.println();
      }
      System.out.printf("Running tests w/ walog: %b  shared batch writer: %b\n", walog, true);
      for (int nt : tests) {
        runTest(conn, nt, 1, true, walog, true);
      }

      System.out.println();
      System.out.printf("Running tests w/ walog: %b  shared batch writer: %b\n", walog, false);
      for (int nt : tests) {
        runTest(conn, nt, 1, true, walog, false);
      }

      System.out.println();
      System.out.printf("Running tests w/ walog: %b  shared batch writer: %b\n", walog, false);
      for (int nb : tests) {
        runTest(conn, 1, nb, true, walog, false);
      }

      walog = !walog;
    }
  }

  private static void runTest(Connector conn, int numThreads, int numToBatch, boolean flush, boolean walog, boolean useSharedBW) throws AccumuloException,
      AccumuloSecurityException, TableNotFoundException, Exception, InterruptedException {

    try {
      conn.tableOperations().create("mutslam");
      if (!walog) {
        conn.tableOperations().setProperty("mutslam", Property.TABLE_WALOG_ENABLED.getKey(), "" + walog);
      }
    } catch (TableExistsException tee) {}

    // scan just to wait for tablet be online
    Scanner scanner = conn.createScanner("mutslam", Authorizations.EMPTY);
    for (Entry<Key,Value> entry : scanner) {
      entry.getValue();
    }

    // number of batches each thread should write
    int numToWrite = 100;

    ArrayList<WriteTask> wasks = new ArrayList<WriteTask>();
    ArrayList<Thread> threads = new ArrayList<Thread>();

    BatchWriter bw = null;
    SharedBatchWriter sbw = null;

    if (useSharedBW) {
      bw = conn.createBatchWriter("mutslam", new BatchWriterConfig().setMaxWriteThreads(1));
      sbw = new SharedBatchWriter(bw);
    }

    for (int i = 0; i < numThreads; i++) {
      WriteTask wask;
      if (useSharedBW)
        wask = new WriteTask(sbw, numToWrite, numToBatch, flush);
      else
        wask = new WriteTask(conn.createBatchWriter("mutslam", new BatchWriterConfig().setMaxWriteThreads(1)), numToWrite, numToBatch, flush);

      wasks.add(wask);
      Thread thread = new Thread(wask);
      threads.add(thread);
    }

    for (Thread thread : threads) {
      thread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    if (useSharedBW)
      bw.close();

    long sum = 0;
    for (WriteTask writeTask : wasks) {
      sum += writeTask.getTime();
    }

    int totalNumMutations = numToWrite * numThreads * numToBatch;
    double rate = totalNumMutations / (sum / (double) wasks.size());
    System.out.printf("\ttime: %8.2f #threads: %3d  #batch: %2d  #mutations: %4d rate: %6.2f\n", sum / (double) wasks.size(), numThreads, numToBatch,
        totalNumMutations, rate);

    conn.tableOperations().delete("mutslam");
  }
}
