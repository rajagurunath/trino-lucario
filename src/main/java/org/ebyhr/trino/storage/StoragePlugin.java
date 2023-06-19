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

import com.google.common.collect.ImmutableSet;
import io.trino.spi.Plugin;
import io.trino.spi.connector.ConnectorFactory;
import org.ebyhr.trino.storage.functions.ChatGPTFunction;
import org.ebyhr.trino.storage.functions.CohereGPTFunction;

import java.util.List;
import java.util.Set;

public class StoragePlugin
        implements Plugin
{
    @Override
    public Iterable<ConnectorFactory> getConnectorFactories()
    {
        return List.of(new StorageConnectorFactory());
    }
    @Override
    public Set<Class<?>> getFunctions() {
        return ImmutableSet.<Class<?>>builder().add(ChatGPTFunction.class).add(CohereGPTFunction.class).build();
    }
}
