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
package com.facebook.presto.sql.rewrite;

import com.facebook.airlift.log.Logger;
import com.facebook.presto.Session;
import com.facebook.presto.common.QualifiedObjectName;
import com.facebook.presto.metadata.Metadata;
import com.facebook.presto.security.AccessControl;
import com.facebook.presto.spi.ConnectorMaterializedViewDefinition;
import com.facebook.presto.spi.MaterializedViewStatus;
import com.facebook.presto.sql.analyzer.MaterializedViewCandidateExtractor;
import com.facebook.presto.sql.analyzer.MaterializedViewQueryOptimizer;
import com.facebook.presto.sql.parser.SqlParser;
import com.facebook.presto.sql.relational.RowExpressionDomainTranslator;
import com.facebook.presto.sql.tree.QualifiedName;
import com.facebook.presto.sql.tree.Query;
import com.facebook.presto.sql.tree.Table;

import java.util.Set;

import static com.facebook.presto.SystemSessionProperties.isMaterializedViewDataConsistencyEnabled;
import static com.facebook.presto.common.RuntimeMetricName.MANY_PARTITIONS_MISSING_IN_MATERIALIZED_VIEW_COUNT;
import static com.facebook.presto.common.RuntimeMetricName.OPTIMIZED_WITH_MATERIALIZED_VIEW_COUNT;
import static com.facebook.presto.sql.ParsingUtil.createParsingOptions;

public class MaterializedViewOptimizationRewriteUtils
{
    private static final Logger log = Logger.get(MaterializedViewOptimizationRewriteUtils.class);

    private MaterializedViewOptimizationRewriteUtils() {}

    public static Query optimizeQueryUsingMaterializedView(
            Metadata metadata,
            Session session,
            SqlParser sqlParser,
            AccessControl accessControl,
            Query node)
    {
        MaterializedViewCandidateExtractor materializedViewCandidateExtractor = new MaterializedViewCandidateExtractor(session, metadata);
        materializedViewCandidateExtractor.process(node);
        Set<QualifiedObjectName> materializedViewCandidates = materializedViewCandidateExtractor.getMaterializedViewCandidates();
        // TODO: Select the most compatible and efficient materialized view for query rewrite optimization https://github.com/prestodb/presto/issues/16431
        // TODO: Refactor query optimization code https://github.com/prestodb/presto/issues/16759
        for (QualifiedObjectName candidate : materializedViewCandidates) {
            Query optimizedQuery = getQueryWithMaterializedViewOptimization(metadata, session, sqlParser, accessControl, node, candidate);
            if (node != optimizedQuery) {
                if (isMaterializedViewDataConsistencyEnabled(session)) {
                    //TODO: We should be able to leverage this information in the StatementAnalyzer as well.
                    MaterializedViewStatus materializedViewStatus = metadata.getMaterializedViewStatus(session, candidate);
                    if (materializedViewStatus.isFullyMaterialized() || materializedViewStatus.isPartiallyMaterialized()) {
                        session.getRuntimeStats().addMetricValue(OPTIMIZED_WITH_MATERIALIZED_VIEW_COUNT, 1);
                        return optimizedQuery;
                    }
                    else {
                        session.getRuntimeStats().addMetricValue(MANY_PARTITIONS_MISSING_IN_MATERIALIZED_VIEW_COUNT, 1);
                    }
                }
                else {
                    session.getRuntimeStats().addMetricValue(OPTIMIZED_WITH_MATERIALIZED_VIEW_COUNT, 1);
                    return optimizedQuery;
                }
            }
        }
        return node;
    }

    private static Query getQueryWithMaterializedViewOptimization(
            Metadata metadata,
            Session session,
            SqlParser sqlParser,
            AccessControl accessControl,
            Query statement,
            QualifiedObjectName materializedViewQualifiedObjectName)
    {
        try {
            ConnectorMaterializedViewDefinition materializedView = metadata.getMaterializedView(session, materializedViewQualifiedObjectName).get();
            Table materializedViewTable = new Table(QualifiedName.of(materializedView.getTable()));

            Query materializedViewDefinition = (Query) sqlParser.createStatement(materializedView.getOriginalSql(), createParsingOptions(session));
            return (Query) new MaterializedViewQueryOptimizer(metadata, session, sqlParser, accessControl, new RowExpressionDomainTranslator(metadata), materializedViewTable, materializedViewDefinition).rewrite(statement);
        }
        catch (RuntimeException ex) {
            log.warn("Failed to get materialized view for %s, with exception: %s", materializedViewQualifiedObjectName, ex.getMessage());
            return statement;
        }
    }
}
