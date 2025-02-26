/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ebyhr.trino.storage;

import com.google.inject.Injector;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.json.JsonModule;
import io.trino.hdfs.HdfsModule;
import io.trino.hdfs.authentication.HdfsAuthenticationModule;
import io.trino.plugin.base.session.SessionPropertiesProvider;
import io.trino.plugin.hive.azure.HiveAzureModule;
import io.trino.plugin.hive.gcs.HiveGcsModule;
import io.trino.plugin.hive.s3.HiveS3Module;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.connector.ConnectorFactory;
import org.ebyhr.trino.storage.operator.GPTApiPlugin;

import java.util.Map;

import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.util.Objects.requireNonNull;

public class StorageConnectorFactory
        implements ConnectorFactory
{
    @Override
    public String getName()
    {
        return "storage";
    }

    @Override
    public Connector create(String catalogName, Map<String, String> requiredConfig, ConnectorContext context)
    {
        requireNonNull(requiredConfig, "requiredConfig is null");
        try {
            // A plugin is not required to use Guice; it is just very convenient
            Bootstrap app = new Bootstrap(
                    new JsonModule(),
                    new StorageModule(context.getTypeManager()),
                    new HdfsModule(),
                    new HiveS3Module(),
                    new HiveGcsModule(),
                    new HiveAzureModule(),
                    new HdfsAuthenticationModule());
            String gpiApiKey = requiredConfig.get(StorageRecordSetProvider.GPT_API_KEY);
            System.out.println(gpiApiKey);
            Injector injector = app
                    .doNotInitializeLogging()
                    .setOptionalConfigurationProperty(StorageRecordSetProvider.GPT_API_KEY, gpiApiKey)
                    .setOptionalConfigurationProperties(requiredConfig)
                    .initialize();

            return injector.getInstance(StorageConnector.class);
        }
        catch (Exception e) {
            throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }
}
