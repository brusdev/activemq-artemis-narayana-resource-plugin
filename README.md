# activemq-artemis-narayana-resource-plugin
The narayana resource plugin for Apache ActivemMQ Artemis monitor XA transactions and
detect clients with same node name and different address.
The plugin uses a cache for client addresses and its default maximum size is 100.
You can set the `MAX_CACHE_SIZE` property to set a custom maximum cache size.

##Registering
To register the plugin you need to copy the jar to the `lib` folder of Apache ActivemMQ Artemis and
to add a `broker-plugin` element at `broker.xml`.

```xml
<broker-plugins>
    <broker-plugin class-name="org.apache.activemq.artemis.core.server.plugin.NarayanaResourcePlugin" />
</broker-plugins>
```

It is also possible to pass the `MAX_CACHE_SIZE` property to set a custom maximum cache size, ie:

```xml
<broker-plugins>
    <broker-plugin class-name="org.apache.activemq.artemis.core.server.plugin.NarayanaResourcePlugin">
        <property key="MAX_CACHE_SIZE" value="1000" />
    </broker-plugin>
</broker-plugins>
```

##Dependencies
* com.google.guava/failureaccess
* org.jboss.narayana.jta/narayana-jta
* org.jboss/jboss-transaction-spi
