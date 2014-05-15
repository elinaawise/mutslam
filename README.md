Mutslam
=======

A simple project that test Accumulo's group commit performance.  

Use the following command to run this program against an Accumulo instance.

```
vim accumulo.properties
mvn clean compile
mvn exec:java -Dexec.mainClass=mutslam.Generator -Dexec.args=accumulo.properties
```

If you do not have an Accumulo instance, you can use the following command to
run one.  This will use MiniDFS and MiniAccumulo.  This command will create an
accumulo.properties file with the info needed to connect.

```
mvn exec:java -Dexec.mainClass=mutslam.MiniRunner -Dexec.args="/tmp/mac accumulo.properties" -Dexec.classpathScope=test
```
