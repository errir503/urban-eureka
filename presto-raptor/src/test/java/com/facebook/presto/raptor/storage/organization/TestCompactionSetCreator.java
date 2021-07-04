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
import com.facebook.presto.raptor.metadata.Table;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.airlift.units.DataSize;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import static com.facebook.presto.common.type.DateType.DATE;
import static com.facebook.presto.common.type.TimestampType.TIMESTAMP;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.joda.time.DateTimeZone.UTC;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestCompactionSetCreator
{
    private static final long MAX_SHARD_ROWS = 100;
    private static final DataSize MAX_SHARD_SIZE = new DataSize(100, DataSize.Unit.BYTE);
    private static final Table tableInfo = new Table(1L, OptionalLong.empty(), Optional.empty(), OptionalInt.empty(), OptionalLong.empty(), false, false);
    private static final Table temporalTableInfo = new Table(1L, OptionalLong.empty(), Optional.empty(), OptionalInt.empty(), OptionalLong.of(1), false, false);
    private static final Table bucketedTableInfo = new Table(1L, OptionalLong.empty(), Optional.empty(), OptionalInt.of(3), OptionalLong.empty(), false, false);
    private static final Table bucketedTemporalTableInfo = new Table(1L, OptionalLong.empty(), Optional.empty(), OptionalInt.of(3), OptionalLong.of(1), false, false);

    private final CompactionSetCreator compactionSetCreator = new CompactionSetCreator(new TemporalFunction(UTC), MAX_SHARD_SIZE, MAX_SHARD_ROWS);

    @Test
    public void testNonTemporalOrganizationSetSimple()
    {
        List<ShardIndexInfo> inputShards = ImmutableList.of(
                shardWithSize(10, 10),
                shardWithSize(10, 10),
                shardWithSize(10, 10));

        Set<OrganizationSet> compactionSets = compactionSetCreator.createCompactionSets(tableInfo, inputShards);
        assertEquals(compactionSets.size(), 1);
        assertEquals(getOnlyElement(compactionSets).getShardsMap(), extractIndexes(inputShards, 0, 1, 2));
    }

    @Test
    public void testNonTemporalOrganizationSetDelta()
    {
        List<ShardIndexInfo> inputShards = ImmutableList.of(
                sharAndDeltaWithSize(10, 10),
                sharAndDeltaWithSize(10, 10),
                sharAndDeltaWithSize(10, 10));

        Set<OrganizationSet> compactionSets = compactionSetCreator.createCompactionSets(tableInfo, inputShards);
        assertEquals(compactionSets.size(), 1);
        assertEquals(getOnlyElement(compactionSets).getShardsMap(), extractIndexes(inputShards, 0, 1, 2));
    }

    @Test
    public void testNonTemporalSizeBasedOrganizationSet()
    {
        List<ShardIndexInfo> inputShards = ImmutableList.of(
                shardWithSize(10, 70),
                shardWithSize(10, 20),
                shardWithSize(10, 30),
                shardWithSize(10, 120));

        Set<OrganizationSet> compactionSets = compactionSetCreator.createCompactionSets(tableInfo, inputShards);

        Map<UUID, Optional<UUID>> actual = new HashMap<>();
        for (OrganizationSet set : compactionSets) {
            actual.putAll(set.getShardsMap());
        }
        assertTrue(extractIndexes(inputShards, 0, 1, 2).keySet().containsAll(actual.keySet()));
    }

    @Test
    public void testNonTemporalRowCountBasedOrganizationSet()
    {
        List<ShardIndexInfo> inputShards = ImmutableList.of(
                shardWithSize(50, 10),
                shardWithSize(100, 10),
                shardWithSize(20, 10),
                shardWithSize(30, 10));

        Set<OrganizationSet> compactionSets = compactionSetCreator.createCompactionSets(tableInfo, inputShards);

        Map<UUID, Optional<UUID>> actual = new HashMap<>();
        for (OrganizationSet set : compactionSets) {
            actual.putAll(set.getShardsMap());
        }

        assertTrue(extractIndexes(inputShards, 0, 2, 3).keySet().containsAll(actual.keySet()));
    }

    @Test
    public void testTemporalCompactionNoCompactionAcrossDays()
    {
        long day1 = Duration.ofDays(Duration.ofNanos(System.nanoTime()).toDays()).toMillis();
        long day2 = Duration.ofDays(Duration.ofMillis(day1).toDays() + 1).toMillis();
        long day3 = Duration.ofDays(Duration.ofMillis(day1).toDays() + 2).toMillis();

        List<ShardIndexInfo> inputShards = ImmutableList.of(
                shardWithTemporalRange(TIMESTAMP, day1, day1),
                shardWithTemporalRange(TIMESTAMP, day2, day2),
                shardWithTemporalRange(TIMESTAMP, day2, day2),
                shardWithTemporalRange(TIMESTAMP, day1, day1),
                shardWithTemporalRange(TIMESTAMP, day3, day3));

        Set<OrganizationSet> actual = compactionSetCreator.createCompactionSets(temporalTableInfo, inputShards);
        assertEquals(actual.size(), 2);

        Set<OrganizationSet> expected = ImmutableSet.of(
                new OrganizationSet(temporalTableInfo.getTableId(), false, extractIndexes(inputShards, 0, 3), OptionalInt.empty(), 0),
                new OrganizationSet(temporalTableInfo.getTableId(), false, extractIndexes(inputShards, 1, 2), OptionalInt.empty(), 0));
        assertEquals(actual, expected);
    }

    @Test
    public void testTemporalCompactionSpanningDays()
    {
        long day1 = Duration.ofDays(Duration.ofNanos(System.nanoTime()).toDays()).toMillis();
        long day2 = Duration.ofDays(Duration.ofMillis(day1).toDays() + 1).toMillis();
        long day3 = Duration.ofDays(Duration.ofMillis(day1).toDays() + 2).toMillis();
        long day4 = Duration.ofDays(Duration.ofMillis(day1).toDays() + 3).toMillis();

        List<ShardIndexInfo> inputShards = ImmutableList.of(
                shardWithTemporalRange(TIMESTAMP, day1, day3), // day2
                shardWithTemporalRange(TIMESTAMP, day2, day2), // day2
                shardWithTemporalRange(TIMESTAMP, day1, day1), // day1
                shardWithTemporalRange(TIMESTAMP, day1 + 100, day2 + 100), // day1
                shardWithTemporalRange(TIMESTAMP, day1 - 100, day2 - 100), // day1
                shardWithTemporalRange(TIMESTAMP, day2 - 100, day3 - 100),  // day2
                shardWithTemporalRange(TIMESTAMP, day1, day4)); // day2

        long tableId = temporalTableInfo.getTableId();
        Set<OrganizationSet> compactionSets = compactionSetCreator.createCompactionSets(temporalTableInfo, inputShards);

        assertEquals(compactionSets.size(), 2);

        Set<OrganizationSet> expected = ImmutableSet.of(
                new OrganizationSet(tableId, false, extractIndexes(inputShards, 0, 1, 5, 6), OptionalInt.empty(), 0),
                new OrganizationSet(tableId, false, extractIndexes(inputShards, 2, 3, 4), OptionalInt.empty(), 0));
        assertEquals(compactionSets, expected);
    }

    @Test
    public void testTemporalCompactionDate()
    {
        long day1 = Duration.ofNanos(System.nanoTime()).toDays();
        long day2 = day1 + 1;
        long day3 = day1 + 2;

        List<ShardIndexInfo> inputShards = ImmutableList.of(
                shardWithTemporalRange(DATE, day1, day1),
                shardWithTemporalRange(DATE, day2, day2),
                shardWithTemporalRange(DATE, day3, day3),
                shardWithTemporalRange(DATE, day1, day3),
                shardWithTemporalRange(DATE, day2, day3),
                shardWithTemporalRange(DATE, day1, day2));

        long tableId = temporalTableInfo.getTableId();
        Set<OrganizationSet> actual = compactionSetCreator.createCompactionSets(temporalTableInfo, inputShards);

        assertEquals(actual.size(), 2);

        Set<OrganizationSet> expected = ImmutableSet.of(
                new OrganizationSet(tableId, false, extractIndexes(inputShards, 0, 3, 5), OptionalInt.empty(), 0),
                new OrganizationSet(tableId, false, extractIndexes(inputShards, 1, 4), OptionalInt.empty(), 0));
        assertEquals(actual, expected);
    }

    @Test
    public void testBucketedTableCompaction()
    {
        List<ShardIndexInfo> inputShards = ImmutableList.of(
                shardWithBucket(1),
                shardWithBucket(2),
                shardWithBucket(2),
                shardWithBucket(1),
                shardWithBucket(2),
                shardWithBucket(1));

        long tableId = bucketedTableInfo.getTableId();
        Set<OrganizationSet> actual = compactionSetCreator.createCompactionSets(bucketedTableInfo, inputShards);

        assertEquals(actual.size(), 2);

        Set<OrganizationSet> expected = ImmutableSet.of(
                new OrganizationSet(tableId, false, extractIndexes(inputShards, 0, 3, 5), OptionalInt.of(1), 0),
                new OrganizationSet(tableId, false, extractIndexes(inputShards, 1, 2, 4), OptionalInt.of(2), 0));
        assertEquals(actual, expected);
    }

    static Map<UUID, Optional<UUID>> extractIndexes(List<ShardIndexInfo> inputShards, int... indexes)
    {
        ImmutableMap.Builder<UUID, Optional<UUID>> builder = ImmutableMap.builder();
        for (int index : indexes) {
            builder.put(inputShards.get(index).getShardUuid(), inputShards.get(index).getDeltaUuid());
        }
        return builder.build();
    }

    @Test
    public void testBucketedTemporalTableCompaction()
    {
        long day1 = 1;
        long day2 = 2;
        long day3 = 3;
        long day4 = 4;

        List<ShardIndexInfo> inputShards = ImmutableList.of(
                shardWithTemporalBucket(OptionalInt.of(1), DATE, day1, day1),
                shardWithTemporalBucket(OptionalInt.of(2), DATE, day2, day2),
                shardWithTemporalBucket(OptionalInt.of(1), DATE, day1, day1),
                shardWithTemporalBucket(OptionalInt.of(2), DATE, day2, day2),
                shardWithTemporalBucket(OptionalInt.of(1), DATE, day3, day3),
                shardWithTemporalBucket(OptionalInt.of(2), DATE, day4, day4));

        long tableId = bucketedTemporalTableInfo.getTableId();
        Set<OrganizationSet> actual = compactionSetCreator.createCompactionSets(bucketedTemporalTableInfo, inputShards);

        assertEquals(actual.size(), 2);

        Set<OrganizationSet> expected = ImmutableSet.of(
                new OrganizationSet(tableId, false, extractIndexes(inputShards, 0, 2), OptionalInt.of(1), 0),
                new OrganizationSet(tableId, false, extractIndexes(inputShards, 1, 3), OptionalInt.of(2), 0));
        assertEquals(actual, expected);
    }

    private static ShardIndexInfo shardWithSize(long rows, long size)
    {
        return new ShardIndexInfo(
                1,
                OptionalInt.empty(),
                UUID.randomUUID(),
                false,
                Optional.empty(),
                rows,
                size,
                Optional.empty(),
                Optional.empty());
    }

    private static ShardIndexInfo sharAndDeltaWithSize(long rows, long size)
    {
        return new ShardIndexInfo(
                1,
                OptionalInt.empty(),
                UUID.randomUUID(),
                false,
                Optional.of(UUID.randomUUID()),
                rows,
                size,
                Optional.empty(),
                Optional.empty());
    }

    private static ShardIndexInfo shardWithTemporalRange(Type type, Long start, Long end)
    {
        return shardWithTemporalBucket(OptionalInt.empty(), type, start, end);
    }

    private static ShardIndexInfo shardWithBucket(int bucketNumber)
    {
        return new ShardIndexInfo(
                1,
                OptionalInt.of(bucketNumber),
                UUID.randomUUID(),
                false,
                Optional.empty(),
                1,
                1,
                Optional.empty(),
                Optional.empty());
    }

    private static ShardIndexInfo shardWithTemporalBucket(OptionalInt bucketNumber, Type type, Long start, Long end)
    {
        if (type.equals(DATE)) {
            return new ShardIndexInfo(
                    1,
                    bucketNumber,
                    UUID.randomUUID(),
                    false,
                    Optional.empty(),
                    1,
                    1,
                    Optional.empty(),
                    Optional.of(ShardRange.of(new Tuple(type, start.intValue()), new Tuple(type, end.intValue()))));
        }
        return new ShardIndexInfo(
                1,
                bucketNumber,
                UUID.randomUUID(),
                false,
                Optional.empty(),
                1,
                1,
                Optional.empty(),
                Optional.of(ShardRange.of(new Tuple(type, start), new Tuple(type, end))));
    }
}
