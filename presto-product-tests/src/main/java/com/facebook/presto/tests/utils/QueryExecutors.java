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
package com.facebook.presto.tests.utils;

import io.prestodb.tempto.query.QueryExecutor;

import static io.prestodb.tempto.context.ThreadLocalTestContextHolder.testContext;

public class QueryExecutors
{
    public static QueryExecutor onPresto()
    {
        return testContext().getDependency(QueryExecutor.class, "presto");
    }

    public static QueryExecutor connectToPresto(String prestoConfig)
    {
        return testContext().getDependency(QueryExecutor.class, prestoConfig);
    }

    public static QueryExecutor onHive()
    {
        return testContext().getDependency(QueryExecutor.class, "hive");
    }

    public static QueryExecutor onSqlServer()
    {
        return testContext().getDependency(QueryExecutor.class, "sqlserver");
    }

    public static QueryExecutor onMySql()
    {
        return testContext().getDependency(QueryExecutor.class, "mysql");
    }

    private QueryExecutors() {}
}
