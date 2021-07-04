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
package com.facebook.presto.atop;

import com.facebook.presto.Session;
import com.facebook.presto.common.type.TimeZoneKey;
import com.facebook.presto.testing.LocalQueryRunner;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.TimeZone;

import static com.facebook.airlift.testing.Closeables.closeAllSuppress;
import static com.facebook.presto.testing.TestingSession.testSessionBuilder;

public final class LocalAtopQueryRunner
{
    private LocalAtopQueryRunner() {}

    public static LocalQueryRunner createQueryRunner()
    {
        return createQueryRunner(ImmutableMap.of(), TestingAtopFactory.class);
    }

    public static LocalQueryRunner createQueryRunner(Map<String, String> catalogProperties, Class<? extends AtopFactory> factoryClass)
    {
        Session session = testSessionBuilder()
                .setCatalog("atop")
                .setSchema("default")
                .setTimeZoneKey(TimeZoneKey.getTimeZoneKey(TimeZone.getDefault().getID()))
                .build();

        LocalQueryRunner queryRunner = new LocalQueryRunner(session);

        try {
            AtopConnectorFactory connectorFactory = new AtopConnectorFactory(factoryClass, LocalAtopQueryRunner.class.getClassLoader());
            ImmutableMap.Builder<String, String> properties = ImmutableMap.<String, String>builder()
                    .putAll(catalogProperties)
                    .put("atop.max-history-days", "1");
            queryRunner.createCatalog("atop", connectorFactory, properties.build());

            return queryRunner;
        }
        catch (Exception e) {
            closeAllSuppress(e, queryRunner);
            throw e;
        }
    }
}
