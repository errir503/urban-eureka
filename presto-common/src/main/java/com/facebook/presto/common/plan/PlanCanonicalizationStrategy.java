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
package com.facebook.presto.common.plan;

public enum PlanCanonicalizationStrategy
{
    /**
     * DEFAULT strategy is used to canonicalize plans with minimal changes. It only processes
     * following basic plan nodes: Scan, Filter, Project, Aggregation, Unnest.
     *
     * We remove any unimportant information like source location, make the variable
     * names consistent, and order them.
     * For example:
     * `SELECT * FROM table WHERE id IN (1, 2)` will be equivalent to `SELECT * FROM table WHERE id IN (2, 1)`
     *
     * This is used in context of fragment result caching
     */
    DEFAULT(0),
    /**
     * CONNECTOR strategy will canonicalize plan according to DEFAULT strategy, and additionally
     * canoncialize `TableScanNode` by giving a connector specific implementation. Unlike DEFAULT strategy,
     * it supports all Plan nodes(like union, join etc.)
     *
     * With this approach, we call ConnectorTableLayoutHandle.getIdentifier() for all `TableScanNode`.
     * Each connector can have a specific implementation to canonicalize table layout handles however they want.
     *
     * For example, Hive connector removes constants from constraints on partition keys:
     * `SELECT * FROM table WHERE ds = '2020-01-01'` will be equivalent to `SELECT * FROM table WHERE ds = '2020-01-02'`
     * where `ds` is a partition column in `table`
     *
     * This is used in context of history based optimizations.
     */
    CONNECTOR(1),
    /**
     * IGNORE_SAFE_CONSTANTS strategy is used to canonicalize plan with
     * CONNECTOR strategy and will additionally remove constants from plan
     * which are not bound to have impact on plan statistics.
     *
     * This includes removing constants from ProjectNode, but keeps constants
     * in FilterNode since they can have major impact on plan statistics.
     *
     * For example:
     * `SELECT *, 1 FROM table` will be equivalent to `SELECT *, 2 FROM table`
     * `SELECT * FROM table WHERE id > 1` will NOT be equivalent to `SELECT * FROM table WHERE id > 1000`
     *
     * This is used in context of history based optimizations.
     */
    IGNORE_SAFE_CONSTANTS(2),

    /**
     * IGNORE_SCAN_CONSTANTS further relaxes over the IGNORE_SAFE_CONSTANTS strategy.
     * In IGNORE_SAFE_CONSTANTS, only predicate on partitioned column in scan node is canonicalized, but
     * in IGNORE_SCAN_CONSTANTS, predicates on non-partitioned columns in scan node are also canonicalized
     *
     * For example:
     * `SELECT *, 1 FROM table` will be equivalent to `SELECT *, 2 FROM table`
     * `SELECT * FROM table WHERE id = 1` will also be equivalent to `SELECT * FROM table WHERE id = 1000` even if id is not partitioned column
     *
     * This is used in context of history based optimizations.
     */
    IGNORE_SCAN_CONSTANTS(3);

    /**
     * Creates a list of PlanCanonicalizationStrategy to be used for history based optimizations.
     * Output is ordered by decreasing accuracy of statistics, at benefit of more coverage.
     * TODO: Remove CONNECTOR strategy
     */

    // Smaller value means more accurate
    private final int errorLevel;

    PlanCanonicalizationStrategy(int errorLevel)
    {
        this.errorLevel = errorLevel;
    }

    public int getErrorLevel()
    {
        return errorLevel;
    }
}
