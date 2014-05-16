Mutslam
=======

A simple project that test Accumulo's group commit performance.  This test was
created to assess the impact of [ACCUMULO-2766][1]  

Use the following command to run this program against an Accumulo instance.

```
vim accumulo.properties
mvn clean compile
mvn exec:java -Dexec.mainClass=mutslam.Generator -Dexec.args=accumulo.properties
```

Accumulo Settings
-----------------

This test runs a lot of client threads, the server will need enough threads to
process these connections w/o making client threads wait.  So add the following
to the accumulo-site.xml...

```
  <property>
    <name>tserver.server.threads.minimum</name>
    <value>128</value>
  </property>
```

...or execute the following command in the shell.  I think the following should
work w/ a running tserver, but have not tried it.


```
  config -s tserver.server.threads.minimum=128
```


Running Accumulo
----------------

If you do not have an Accumulo instance, you can use the following command to
run one.  This will use MiniDFS and MiniAccumulo.  This command will create an
accumulo.properties file with the info needed to connect.

```
mvn exec:java -Dexec.mainClass=mutslam.MiniRunner -Dexec.args="/tmp/mac accumulo.properties" -Dexec.classpathScope=test
```

[1]:https://issues.apache.org/jira/browse/ACCUMULO-2766

