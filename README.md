
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

3. **Add JAR to the Flume agents classpath**

    <pre>
    $ sudo cp /etc/flume-ng/conf/flume-env.sh.template /etc/flume-ng/conf/flume-env.sh
    $ vi /etc/flume-ng/conf/flume-env.sh
    FLUME_CLASSPATH=/path/to/file/webtrends-sapi-flume-source-0.0.1-SNAPSHOT.jar
    </pre>

    or if using Cloudera Manager:

    Services > flume1 > Configuration > View and Edit > Agent (Default) > Advanced > Java Configuration Options for Flume Agent

    <pre>
    --classpath /path/to/file/webtrends-sapi-flume-source-0.0.1-SNAPSHOT.jar
    </pre>

4. **Set the Flume agents name to WebtrendsSAPISource**

    <pre>
    $ vi /etc/default/flume-ng-agent
    FLUME_AGENT_NAME=WebtrendsSAPISource
    </pre>

    or if using Cloudera Manager:

    Services > flume1 > Configuration > View and Edit > Agent (Default) > Agent Name

    <pre>
    WebtrendsSAPISource
    </pre>

5. **Set the Flume agents configuration**

    Copy the example agent configuration to /etc/flume-ng/conf/flume.conf.

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

    or if using Cloudera Manager, add the Flume configuration to:

    Services > flume1 > Configuration > View and Edit > Agent (Default) > Configuration File

6. **Create HDFS directory**

    <pre>
    $ hadoop fs -mkdir /user/flume/webtrends
    </pre>

7. **Start the Flume agent**

    <pre>
    $ sudo /etc/init.d/flume-ng-agent start
    $ tail -100f /var/log/flume-ng/flume.log
    </pre> 

    or simply start the process in Cloudera Manager.