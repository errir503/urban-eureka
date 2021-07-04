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
package com.facebook.presto.raptor.metadata;

import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class ShardNodes
{
    private final UUID shardUuid;
    private final Optional<UUID> deltaShardUuid;
    private final Set<String> nodeIdentifiers;

    public ShardNodes(UUID shardUuid, Optional<UUID> deltaShardUuid, Set<String> nodeIdentifiers)
    {
        this.shardUuid = requireNonNull(shardUuid, "shardUuid is null");
        this.deltaShardUuid = requireNonNull(deltaShardUuid, "deltaShardUuid is null");
        this.nodeIdentifiers = ImmutableSet.copyOf(requireNonNull(nodeIdentifiers, "nodeIdentifiers is null"));
    }

    public UUID getShardUuid()
    {
        return shardUuid;
    }

    public Optional<UUID> getDeltaShardUuid()
    {
        return deltaShardUuid;
    }

    public Set<String> getNodeIdentifiers()
    {
        return nodeIdentifiers;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ShardNodes other = (ShardNodes) obj;
        return Objects.equals(this.shardUuid, other.shardUuid) &&
                Objects.equals(this.deltaShardUuid, other.deltaShardUuid) &&
                Objects.equals(this.nodeIdentifiers, other.nodeIdentifiers);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(shardUuid, nodeIdentifiers);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("shardUuid", shardUuid)
                .add("deltaShardUuid", deltaShardUuid)
                .add("nodeIdentifiers", nodeIdentifiers)
                .toString();
    }
}
