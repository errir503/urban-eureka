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
package com.facebook.presto.raptor.storage.organization;

import com.facebook.presto.common.type.Type;
import com.facebook.presto.raptor.metadata.MetadataDao;
import com.facebook.presto.raptor.metadata.ShardMetadata;
import com.facebook.presto.raptor.metadata.Table;
import com.facebook.presto.raptor.metadata.TableColumn;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimaps;
import org.skife.jdbi.v2.IDBI;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static com.facebook.presto.raptor.metadata.DatabaseShardManager.maxColumn;
import static com.facebook.presto.raptor.metadata.DatabaseShardManager.minColumn;
import static com.facebook.presto.raptor.metadata.DatabaseShardManager.shardIndexTable;
import static com.facebook.presto.raptor.storage.ColumnIndexStatsUtils.jdbcType;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.partition;
import static com.google.common.collect.Maps.uniqueIndex;
import static io.airlift.slice.Slices.wrappedBuffer;
import static java.lang.String.format;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toSet;

public class ShardOrganizerUtil
{
    private ShardOrganizerUtil() {}

    public static Collection<ShardIndexInfo> getOrganizationEligibleShards(
            IDBI dbi,
            MetadataDao metadataDao,
            Table tableInfo,
            Collection<ShardMetadata> shards,
            boolean includeSortColumns)
    {
        Map<Long, ShardMetadata> shardsById = uniqueIndex(shards, ShardMetadata::getShardId);
        long tableId = tableInfo.getTableId();

        ImmutableList.Builder<String> columnsBuilder = ImmutableList.builder();
        columnsBuilder.add("shard_id");

        // include temporal columns if present
        Optional<TableColumn> temporalColumn = Optional.empty();
        if (tableInfo.getTemporalColumnId().isPresent()) {
            long temporalColumnId = tableInfo.getTemporalColumnId().getAsLong();
            temporalColumn = Optional.of(metadataDao.getTableColumn(tableId, temporalColumnId));
            columnsBuilder.add(minColumn(temporalColumnId), maxColumn(temporalColumnId));
        }

        // include sort columns if needed
        Optional<List<TableColumn>> sortColumns = Optional.empty();
        if (includeSortColumns) {
            sortColumns = Optional.of(metadataDao.listSortColumns(tableId));
            for (TableColumn column : sortColumns.get()) {
                columnsBuilder.add(minColumn(column.getColumnId()), maxColumn(column.getColumnId()));
            }
        }
        String columnToSelect = Joiner.on(",\n").join(columnsBuilder.build());

        ImmutableList.Builder<ShardIndexInfo> indexInfoBuilder = ImmutableList.builder();
        try (Connection connection = dbi.open().getConnection()) {
            for (List<ShardMetadata> partitionedShards : partition(shards, 1000)) {
                String shardIds = Joiner.on(",").join(nCopies(partitionedShards.size(), "?"));

                String sql = format("" +
                                "SELECT %s\n" +
                                "FROM %s\n" +
                                "WHERE shard_id IN (%s)",
                        columnToSelect, shardIndexTable(tableId), shardIds);

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (int i = 0; i < partitionedShards.size(); i++) {
                        statement.setLong(i + 1, partitionedShards.get(i).getShardId());
                    }
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            long shardId = resultSet.getLong("shard_id");

                            Optional<ShardRange> sortRange = Optional.empty();
                            if (includeSortColumns) {
                                sortRange = getShardRange(sortColumns.get(), resultSet);
                                if (!sortRange.isPresent()) {
                                    continue;
                                }
                            }
                            Optional<ShardRange> temporalRange = Optional.empty();
                            if (temporalColumn.isPresent()) {
                                temporalRange = getShardRange(ImmutableList.of(temporalColumn.get()), resultSet);
                                if (!temporalRange.isPresent()) {
                                    continue;
                                }
                            }
                            ShardMetadata shardMetadata = shardsById.get(shardId);
                            indexInfoBuilder.add(toShardIndexInfo(shardMetadata, temporalRange, sortRange));
                        }
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return indexInfoBuilder.build();
    }

    private static ShardIndexInfo toShardIndexInfo(ShardMetadata shardMetadata, Optional<ShardRange> temporalRange, Optional<ShardRange> sortRange)
    {
        return new ShardIndexInfo(
                shardMetadata.getTableId(),
                shardMetadata.getBucketNumber(),
                shardMetadata.getShardUuid(),
                shardMetadata.isDelta(),
                shardMetadata.getDeltaUuid(),
                shardMetadata.getRowCount(),
                shardMetadata.getUncompressedSize(),
                sortRange,
                temporalRange);
    }

    public static Collection<Collection<ShardIndexInfo>> getShardsByDaysBuckets(Table tableInfo, Collection<ShardIndexInfo> shards, TemporalFunction temporalFunction)
    {
        if (shards.isEmpty()) {
            return ImmutableList.of();
        }

        // Neither bucketed nor temporal, no partitioning required
        if (!tableInfo.getBucketCount().isPresent() && !tableInfo.getTemporalColumnId().isPresent()) {
            return ImmutableList.of(shards);
        }

        // if only bucketed, partition by bucket number
        if (tableInfo.getBucketCount().isPresent() && !tableInfo.getTemporalColumnId().isPresent()) {
            return Multimaps.index(shards, shard -> shard.getBucketNumber().getAsInt()).asMap().values();
        }

        // if temporal, partition into days first
        ImmutableMultimap.Builder<Long, ShardIndexInfo> shardsByDaysBuilder = ImmutableMultimap.builder();
        shards.stream()
                .filter(shard -> shard.getTemporalRange().isPresent())
                .forEach(shard -> {
                    long day = temporalFunction.getDayFromRange(shard.getTemporalRange().get());
                    shardsByDaysBuilder.put(day, shard);
                });

        Collection<Collection<ShardIndexInfo>> byDays = shardsByDaysBuilder.build().asMap().values();

        // if table is bucketed further partition by bucket number
        if (!tableInfo.getBucketCount().isPresent()) {
            return byDays;
        }

        ImmutableList.Builder<Collection<ShardIndexInfo>> sets = ImmutableList.builder();
        for (Collection<ShardIndexInfo> s : byDays) {
            sets.addAll(Multimaps.index(s, ShardIndexInfo::getBucketNumber).asMap().values());
        }
        return sets.build();
    }

    private static Optional<ShardRange> getShardRange(List<TableColumn> columns, ResultSet resultSet)
            throws SQLException
    {
        ImmutableList.Builder<Object> minValuesBuilder = ImmutableList.builder();
        ImmutableList.Builder<Object> maxValuesBuilder = ImmutableList.builder();
        ImmutableList.Builder<Type> typeBuilder = ImmutableList.builder();

        for (TableColumn tableColumn : columns) {
            long columnId = tableColumn.getColumnId();
            Type type = tableColumn.getDataType();

            Object min = getValue(resultSet, type, minColumn(columnId));
            Object max = getValue(resultSet, type, maxColumn(columnId));

            if (min == null || max == null) {
                return Optional.empty();
            }

            minValuesBuilder.add(min);
            maxValuesBuilder.add(max);
            typeBuilder.add(type);
        }

        List<Type> types = typeBuilder.build();
        Tuple minTuple = new Tuple(types, minValuesBuilder.build());
        Tuple maxTuple = new Tuple(types, maxValuesBuilder.build());

        return Optional.of(ShardRange.of(minTuple, maxTuple));
    }

    private static Object getValue(ResultSet resultSet, Type type, String columnName)
            throws SQLException
    {
        JDBCType jdbcType = jdbcType(type);
        Object value = getValue(resultSet, type, columnName, jdbcType);
        return resultSet.wasNull() ? null : value;
    }

    private static Object getValue(ResultSet resultSet, Type type, String columnName, JDBCType jdbcType)
            throws SQLException
    {
        switch (jdbcType) {
            case BOOLEAN:
                return resultSet.getBoolean(columnName);
            case INTEGER:
                return resultSet.getInt(columnName);
            case BIGINT:
                return resultSet.getLong(columnName);
            case DOUBLE:
                return resultSet.getDouble(columnName);
            case VARBINARY:
                return wrappedBuffer(resultSet.getBytes(columnName)).toStringUtf8();
        }
        throw new IllegalArgumentException("Unhandled type: " + type);
    }

    static OrganizationSet createOrganizationSet(long tableId, boolean tableSupportsDeltaDelete, Set<ShardIndexInfo> shardsToCompact, int priority)
    {
        Map<UUID, Optional<UUID>> uuidsMap = shardsToCompact.stream()
                .collect(toImmutableMap(ShardIndexInfo::getShardUuid, ShardIndexInfo::getDeltaUuid));

        Set<OptionalInt> bucketNumber = shardsToCompact.stream()
                .map(ShardIndexInfo::getBucketNumber)
                .collect(toSet());

        checkArgument(bucketNumber.size() == 1);
        return new OrganizationSet(tableId, tableSupportsDeltaDelete, uuidsMap, getOnlyElement(bucketNumber), priority);
    }
}
