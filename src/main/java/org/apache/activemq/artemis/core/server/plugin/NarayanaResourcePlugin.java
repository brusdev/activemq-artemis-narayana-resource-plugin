/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.core.server.plugin;

import com.arjuna.ats.jta.xa.XATxConverter;
import com.arjuna.ats.jta.xa.XidImple;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.jboss.logging.Logger;

import javax.transaction.xa.Xid;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class NarayanaResourcePlugin implements ActiveMQServerPlugin {

   public static final String MAX_CACHE_SIZE_PROPERTY_NAME = "MAX_CACHE_SIZE";

   public static final int DEFAULT_MAX_CACHE_SIZE = 100;


   private static final Logger logger = Logger.getLogger(NarayanaResourcePlugin.class);

   private static final char SOCKET_ADDRESS_DELIMITER = ':';


   private Cache<String, String> clientCache;


   public Cache<String, String> getClientCache() {
      return clientCache;
   }


   @Override
   public void init(Map<String, String> properties) {
      int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;

      String maxCacheSizePropertyValue = properties.get(MAX_CACHE_SIZE_PROPERTY_NAME);
      if (maxCacheSizePropertyValue != null) {
         maxCacheSize = Integer.parseInt(maxCacheSizePropertyValue);
      }

      clientCache = CacheBuilder.newBuilder().maximumSize(maxCacheSize).build();
   }

   @Override
   public void afterPutTransaction(Xid xid, Transaction tx, RemotingConnection remotingConnection) throws ActiveMQException {
      if (xid.getFormatId() == XATxConverter.FORMAT_ID) {
         String previousClientAddress = null;
         String clientAddress = getClientAddress(remotingConnection);
         String nodeName = XATxConverter.getNodeName(new XidImple(xid).getXID());

         try {
            previousClientAddress = clientCache.get(nodeName, () -> clientAddress);
         } catch (ExecutionException e) {
            logger.debug("Error on loading address " + clientAddress + " for node " + nodeName, e);
         }

         if (previousClientAddress != null && !previousClientAddress.equals(clientAddress)) {
            logger.warn("Possible XA client misconfiguration. Two addresses with the same node name " +
               nodeName + ": " + previousClientAddress + "/" + clientAddress);
         }
      }
   }

   private String getClientAddress(RemotingConnection remotingConnection) {
      String remoteAddress = remotingConnection.getRemoteAddress();

      if (remoteAddress != null) {
         int delimiterIndex = remoteAddress.lastIndexOf(SOCKET_ADDRESS_DELIMITER);

         if (delimiterIndex > 0) {
            return remoteAddress.substring(0, delimiterIndex);
         }
      }

      return remoteAddress;
   }
}
