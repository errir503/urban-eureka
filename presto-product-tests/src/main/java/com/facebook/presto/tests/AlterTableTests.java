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
package com.facebook.presto.tests;

import com.facebook.airlift.log.Logger;
import io.prestodb.tempto.AfterTestWithContext;
import io.prestodb.tempto.BeforeTestWithContext;
import io.prestodb.tempto.ProductTest;
import io.prestodb.tempto.Requires;
import io.prestodb.tempto.fulfillment.table.hive.tpch.ImmutableTpchTablesRequirements.ImmutableNationTable;
import org.testng.annotations.Test;

import static com.facebook.presto.tests.TestGroups.ALTER_TABLE;
import static com.facebook.presto.tests.TestGroups.SMOKE;
import static io.prestodb.tempto.assertions.QueryAssert.Row.row;
import static io.prestodb.tempto.assertions.QueryAssert.assertThat;
import static io.prestodb.tempto.query.QueryExecutor.query;
import static java.lang.String.format;

@Requires(ImmutableNationTable.class)
public class AlterTableTests
        extends ProductTest
{
    private static final String TABLE_NAME = "table_name";
    private static final String RENAMED_TABLE_NAME = "renamed_table_name";

    @BeforeTestWithContext
    @AfterTestWithContext
    public void dropTestTables()
    {
        try {
            query(format("DROP TABLE IF EXISTS %s", TABLE_NAME));
            query(format("DROP TABLE IF EXISTS %s", RENAMED_TABLE_NAME));
        }
        catch (Exception e) {
            Logger.get(getClass()).warn(e, "failed to drop table");
        }
    }

    @Test(groups = {ALTER_TABLE, SMOKE})
    public void renameTable()
    {
        query(format("CREATE TABLE %s AS SELECT * FROM nation", TABLE_NAME));

        assertThat(query(format("ALTER TABLE %s RENAME TO %s", TABLE_NAME, RENAMED_TABLE_NAME)))
                .hasRowsCount(1);

        assertThat(query(format("SELECT * FROM %s", RENAMED_TABLE_NAME)))
                .hasRowsCount(25);

        // rename back to original name
        assertThat(query(format("ALTER TABLE %s RENAME TO %s", RENAMED_TABLE_NAME, TABLE_NAME)))
                .hasRowsCount(1);
    }

    @Test(groups = {ALTER_TABLE, SMOKE})
    public void renameColumn()
    {
        query(format("CREATE TABLE %s AS SELECT * FROM nation", TABLE_NAME));

        assertThat(query(format("ALTER TABLE %s RENAME COLUMN n_nationkey TO nationkey", TABLE_NAME)))
                .hasRowsCount(1);
        assertThat(query(format("SELECT count(nationkey) FROM %s", TABLE_NAME)))
                .containsExactly(row(25));
        assertThat(() -> query(format("ALTER TABLE %s RENAME COLUMN nationkey TO nATIoNkEy", TABLE_NAME)))
                .failsWithMessage("Column 'nationkey' already exists");
        assertThat(() -> query(format("ALTER TABLE %s RENAME COLUMN nationkey TO n_regionkeY", TABLE_NAME)))
                .failsWithMessage("Column 'n_regionkey' already exists");

        assertThat(query(format("ALTER TABLE %s RENAME COLUMN nationkey TO n_nationkey", TABLE_NAME)));
    }

    @Test(groups = {ALTER_TABLE, SMOKE})
    public void addColumn()
    {
        query(format("CREATE TABLE %s AS SELECT * FROM nation", TABLE_NAME));

        assertThat(query(format("SELECT count(1) FROM %s", TABLE_NAME)))
                .containsExactly(row(25));
        assertThat(query(format("ALTER TABLE %s ADD COLUMN some_new_column BIGINT", TABLE_NAME)))
                .hasRowsCount(1);
        assertThat(() -> query(format("ALTER TABLE %s ADD COLUMN n_nationkey BIGINT", TABLE_NAME)))
                .failsWithMessage("Column 'n_nationkey' already exists");
        assertThat(() -> query(format("ALTER TABLE %s ADD COLUMN n_naTioNkEy BIGINT", TABLE_NAME)))
                .failsWithMessage("Column 'n_naTioNkEy' already exists");
    }

    @Test(groups = {ALTER_TABLE, SMOKE})
    public void dropColumn()
    {
        query(format("CREATE TABLE %s AS SELECT n_nationkey, n_regionkey, n_name FROM nation", TABLE_NAME));

        assertThat(query(format("SELECT count(n_nationkey) FROM %s", TABLE_NAME)))
                .containsExactly(row(25));
        assertThat(query(format("ALTER TABLE %s DROP COLUMN n_name", TABLE_NAME)))
                .hasRowsCount(1);
        assertThat(query(format("ALTER TABLE %s DROP COLUMN n_nationkey", TABLE_NAME)))
                .hasRowsCount(1);
        assertThat(() -> query(format("ALTER TABLE %s DROP COLUMN n_regionkey", TABLE_NAME)))
                .failsWithMessage("Cannot drop the only column in a table");
        query(format("DROP TABLE IF EXISTS %s", TABLE_NAME));
    }
}
