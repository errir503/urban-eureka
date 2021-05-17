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
package com.facebook.presto.spark;

import com.facebook.presto.testing.QueryRunner;
import com.facebook.presto.tests.AbstractTestJoinQueries;

import static com.facebook.presto.spark.PrestoSparkQueryRunner.createHivePrestoSparkQueryRunner;

public class TestPrestoSparkAbstractTestJoinQueries
        extends AbstractTestJoinQueries
{
    @Override
    protected QueryRunner createQueryRunner()
    {
        return createHivePrestoSparkQueryRunner();
    }

    @Override
    public void testExecuteUsingComplexJoinCriteria()
    {
        // prepared statement is not supported by Presto on Spark
    }

    @Override
    public void testExecuteUsingWithSubqueryInJoin()
    {
        // prepared statement is not supported by Presto on Spark
    }
}
