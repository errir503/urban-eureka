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
package com.facebook.presto.raptor;

import com.facebook.presto.Session;
import com.facebook.presto.benchmark.BenchmarkSuite;
import com.facebook.presto.spi.connector.ConnectorFactory;
import com.facebook.presto.testing.LocalQueryRunner;
import com.facebook.presto.tpch.TpchConnectorFactory;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.facebook.presto.testing.TestingSession.testSessionBuilder;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Objects.requireNonNull;

public final class RaptorBenchmarkQueryRunner
{
    private static final String TPCH_CACHE_DIR = System.getProperty("tpchCacheDir", "/tmp/presto_tpch/data_cache");

    private RaptorBenchmarkQueryRunner()
    {
    }

    public static void main(String[] args)
            throws IOException
    {
        String outputDirectory = requireNonNull(System.getProperty("outputDirectory"), "Must specify -DoutputDirectory=...");
        try (LocalQueryRunner localQueryRunner = createLocalQueryRunner()) {
            new BenchmarkSuite(localQueryRunner, outputDirectory).runAllBenchmarks();
        }
    }

    public static LocalQueryRunner createLocalQueryRunner()
    {
        Session session = testSessionBuilder()
                .setCatalog("raptor")
                .setSchema("benchmark")
                .build();
        LocalQueryRunner localQueryRunner = new LocalQueryRunner(session);

        // add tpch
        localQueryRunner.createCatalog("tpch", new TpchConnectorFactory(1), ImmutableMap.of());

        // add raptor
        ConnectorFactory raptorConnectorFactory = getOnlyElement(new RaptorPlugin()
                .getConnectorFactories());
        Map<String, String> config = createRaptorConfig(TPCH_CACHE_DIR);
        localQueryRunner.createCatalog("raptor", raptorConnectorFactory, config);

        if (!localQueryRunner.tableExists(session, "orders")) {
            localQueryRunner.execute("CREATE TABLE orders AS SELECT * FROM tpch.sf1.orders");
        }
        if (!localQueryRunner.tableExists(session, "lineitem")) {
            localQueryRunner.execute("CREATE TABLE lineitem AS SELECT * FROM tpch.sf1.lineitem");
        }
        return localQueryRunner;
    }

    private static Map<String, String> createRaptorConfig(String cacheDir)
    {
        File dataDir = new File(cacheDir);
        File databaseDir = new File(dataDir, "db");

        return ImmutableMap.<String, String>builder()
                .put("metadata.db.type", "h2")
                .put("metadata.db.filename", databaseDir.getAbsolutePath())
                .put("storage.data-directory", dataDir.getAbsolutePath())
                .put("storage.compress", "false")
                .build();
    }
}
