package org.apache.activemq.artemis.core.server.plugin;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.internal.jta.xa.XID;
import com.arjuna.ats.jta.xa.XATxConverter;
import com.arjuna.ats.jta.xa.XidImple;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import javax.transaction.xa.Xid;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class NarayanaResourcePluginTest {

   @Test
   public void testXAClientMisconfiguration() throws Exception {
      NarayanaResourcePlugin plugin = new NarayanaResourcePlugin();
      plugin.init(Collections.emptyMap());

      System.out.println(new File("ObjectStore").getAbsolutePath());

      List<LogRecord> misconfigurationRecords = new ArrayList<>();
      Logger logger = Logger.getLogger(NarayanaResourcePlugin.class.getName());
      logger.addHandler(new Handler() {
         @Override
         public void publish(LogRecord record) {
            if (record.getMessage().contains("XA client misconfiguration")) {
               misconfigurationRecords.add(record);
            }
         }

         @Override
         public void flush() {

         }

         @Override
         public void close() throws SecurityException {

         }
      });

      Xid xid = XATxConverter.getXid(new Uid(), false, XATxConverter.FORMAT_ID);

      RemotingConnection connection1 = Mockito.mock(RemotingConnection.class);
      Mockito.when(connection1.getRemoteAddress()).thenReturn("10.0.0.1:12345");
      plugin.afterPutTransaction(xid, null, connection1);

      RemotingConnection connection2 = Mockito.mock(RemotingConnection.class);
      Mockito.when(connection2.getRemoteAddress()).thenReturn("10.0.0.2:12345");
      plugin.afterPutTransaction(xid, null, connection2);

      Assert.assertEquals(1, misconfigurationRecords.size());
   }

   @Test
   public void testDefaultMaxCacheSize() throws Exception {
      NarayanaResourcePlugin plugin = new NarayanaResourcePlugin();
      plugin.init(Collections.emptyMap());

      testMaxCacheSize(plugin, NarayanaResourcePlugin.DEFAULT_MAX_CACHE_SIZE);
   }

   @Test
   public void testCustomMaxCacheSize() throws Exception {
      final int maxCacheSize = 10;
      NarayanaResourcePlugin plugin = new NarayanaResourcePlugin();
      plugin.init(Collections.singletonMap(NarayanaResourcePlugin
         .MAX_CACHE_SIZE_PROPERTY_NAME, Integer.toString(maxCacheSize)));

      testMaxCacheSize(plugin, maxCacheSize);
   }

   private void testMaxCacheSize(NarayanaResourcePlugin plugin, int maxCacheSize) throws Exception {
      XID xid = ((XidImple)XATxConverter.getXid(new Uid(), false, XATxConverter.FORMAT_ID)).getXID();

      for (int i = 0; i < maxCacheSize + 1; i++) {
         XATxConverter.setNodeName(xid, Integer.toString(i));
         RemotingConnection connection1 = Mockito.mock(RemotingConnection.class);
         Mockito.when(connection1.getRemoteAddress()).thenReturn("10.0.0.1:12345");
         plugin.afterPutTransaction(new XidImple(xid), null, connection1);
      }

      Assert.assertTrue(plugin.getClientCache().size() <= maxCacheSize);
   }
}