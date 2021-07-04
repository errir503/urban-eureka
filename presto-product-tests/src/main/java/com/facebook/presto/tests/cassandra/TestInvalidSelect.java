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
package com.facebook.presto.tests.cassandra;

import io.prestodb.tempto.ProductTest;
import io.prestodb.tempto.Requirement;
import io.prestodb.tempto.RequirementsProvider;
import io.prestodb.tempto.configuration.Configuration;
import org.testng.annotations.Test;

import static com.facebook.presto.tests.TestGroups.CASSANDRA;
import static com.facebook.presto.tests.cassandra.CassandraTpchTableDefinitions.CASSANDRA_NATION;
import static com.facebook.presto.tests.cassandra.TestConstants.CONNECTOR_NAME;
import static com.facebook.presto.tests.cassandra.TestConstants.KEY_SPACE;
import static io.prestodb.tempto.assertions.QueryAssert.assertThat;
import static io.prestodb.tempto.fulfillment.table.TableRequirements.immutableTable;
import static io.prestodb.tempto.query.QueryExecutor.query;
import static java.lang.String.format;

public class TestInvalidSelect
        extends ProductTest
        implements RequirementsProvider
{
    @Override
    public Requirement getRequirements(Configuration configuration)
    {
        return immutableTable(CASSANDRA_NATION);
    }

    @Test(groups = CASSANDRA)
    public void testInvalidTable()
    {
        String tableName = format("%s.%s.%s", CONNECTOR_NAME, KEY_SPACE, "bogus");
        assertThat(() -> query(format("SELECT * FROM %s", tableName)))
                .failsWithMessage(format("Table %s does not exist", tableName));
    }

    @Test(groups = CASSANDRA)
    public void testInvalidSchema()
    {
        String tableName = format("%s.%s.%s", CONNECTOR_NAME, "does_not_exist", "bogus");
        assertThat(() -> query(format("SELECT * FROM %s", tableName)))
                .failsWithMessage("Schema does_not_exist does not exist");
    }

    @Test(groups = CASSANDRA)
    public void testInvalidColumn()
    {
        String tableName = format("%s.%s.%s", CONNECTOR_NAME, KEY_SPACE, CASSANDRA_NATION.getName());
        assertThat(() -> query(format("SELECT bogus FROM %s", tableName)))
                .failsWithMessage("Column 'bogus' cannot be resolved");
    }
}
