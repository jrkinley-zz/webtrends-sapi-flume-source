# Webtrends SAPI Flume Source

A custom [Apache Flume](http://flume.apache.org) source that connects to the [Webtrends Streams API (SAPI)](http://webtrends.com/solutions/digital-measurement/streams) and sends the events through the configured data flow (Flume channels, sinks, and downstream agents) into Hadoop.

## Getting started

1. **[Install Flume](http://www.cloudera.com/content/cloudera-content/cloudera-docs/CDH4/latest/CDH4-Installation-Guide/cdh4ig_topic_12.html)**

2. **Build webtrends-sapi-flume-source**

    <pre>
    $ git clone https://github.com/jrkinley/webtrends-sapi-flume-source.git
    $ cd webtrends-sapi-flume-source
    $ mvn clean package
    $ ls target
    webtrends-sapi-flume-source-0.0.1-SNAPSHOT.jar
    </pre>
    
    Copy JAR to all Flume hosts.

3. **Add JAR to the Flume agents classpath**

    Create a plugins.d directory for webtrends-sapi-flume-source.
    
    The plugins.d directory is typically located at `$FLUME_HOME/plugins.d`. Or if using Cloudera Manager `/usr/lib/flume-ng/plugins.d` or `/var/lib/flume-ng/plugins.d`

    <pre>
    $ cd $FLUME_HOME/plugins.d
    $ mkdir -p webtrends-sapi-flume-source/lib
    $ mv ~/webtrends-sapi-flume-source-0.0.1-SNAPSHOT.jar webtrends-sapi-flume-source/lib/
    </pre>
   
4. **Set the Flume agents name to WebtrendsSAPIAgent**

    <pre>
    $ vi /etc/default/flume-ng-agent
    FLUME_AGENT_NAME=WebtrendsSAPIAgent
    </pre>

    Or if using Cloudera Manager:

    Services > flume1 > Configuration > View and Edit > Agent (Default) > Agent Name

    <pre>
    WebtrendsSAPIAgent
    </pre>

5. **Set the Flume agents configuration**

    Copy the [example agent configuration](https://github.com/jrkinley/webtrends-sapi-flume-source/blob/master/flume.conf) to `/etc/flume-ng/conf/flume.conf`.
    
    Or if using Cloudera Manager, copy the Flume configuration to: 
    Services > flume1 > Configuration > View and Edit > Agent (Default) > Configuration File

    Set the Webtrends SAPI configuration:

    <pre>
    WebtrendsSAPIAgent.sources.SAPI.clientId = [required]
    WebtrendsSAPIAgent.sources.SAPI.clientSecret = [required]
    WebtrendsSAPIAgent.sources.SAPI.sapiURL = ws://sapi.webtrends.com/streaming
    WebtrendsSAPIAgent.sources.SAPI.sapiStreamType = return_all
    WebtrendsSAPIAgent.sources.SAPI.sapiStreamQuery = select *
    WebtrendsSAPIAgent.sources.SAPI.sapiVersion = 2.1
    WebtrendsSAPIAgent.sources.SAPI.sapiSchemaVersion = 2.1
    </pre>

    Set where you would like to store the events in HDFS:

    <pre>
    WebtrendsSAPIAgent.sinks.HDFS.hdfs.path = hdfs://nameservice1/user/flume/webtrends/%Y/%m/%d/
    </pre>

    If using a secure cluster, set the kerberos principal and keytab:
    
    <pre>
    WebtrendsSAPIAgent.sinks.HDFS.hdfs.kerberosPrincipal = flume/_HOST@YOUR-REALM.COM
    WebtrendsSAPIAgent.sinks.HDFS.hdfs.kerberosKeytab = /etc/flume-ng/conf/flume.keytab
    WebtrendsSAPIAgent.sinks.HDFS.hdfs.proxyUser = [optional]

    # Note: if using a secure cluster managed by Cloudera Manager you can use the following substitution variables
    # to configure the principal name and the keytab file path:
    # WebtrendsSAPIAgent.sinks.HDFS.hdfs.kerberosPrincipal = $KERBEROS_PRINCIPAL
    # WebtrendsSAPIAgent.sinks.HDFS.hdfs.kerberosKeytab = $KERBEROS_KEYTAB
    </pre>

6. **Add Flume proxy user to HDFS [optional]**

    If using the `hdfs.proxyUser` option then you will need to add the following configuration to `core-site.xml`:
    
    ```xml
    <property>
      <name>hadoop.proxyuser.flume.groups</name>
      <value>*</value>
    </property>
    <property>
      <name>hadoop.proxyuser.flume.hosts</name>
      <value>*</value>
    </property>
    ```

    In Cloudera Manager this can be added to:
    Services > hdfs1 > Configuration > View and Edit > Service-Wide > Advanced > Cluster-wide Configuration Safety Valve for core-site.xml

7. **Create HDFS directory**

    <pre>
    $ hadoop fs -mkdir /user/flume/webtrends
    </pre>

8. **Start the Flume agent**

    <pre>
    $ sudo /etc/init.d/flume-ng-agent start
    $ tail -100f /var/log/flume-ng/flume.log
    </pre> 

    or simply start the process in Cloudera Manager.