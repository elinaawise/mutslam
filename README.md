Mutslam
=======

A simple project that test Accumulo's group commit performance.  

Use the following command to run this program against an Accumulo instance.

```
vim accumulo.properties
mvn clean compile
mvn exec:java -Dexec.mainClass=mutslam.Generator -Dexec.args=accumulo.properties
```

