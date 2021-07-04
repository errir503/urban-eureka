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

import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.raptor.metadata.ForMetadata;
import com.facebook.presto.raptor.metadata.ShardManager;
import org.skife.jdbi.v2.IDBI;

import javax.inject.Inject;

import java.util.function.LongConsumer;

import static java.util.Objects.requireNonNull;

public class RaptorMetadataFactory
{
    protected final String connectorId;
    protected final IDBI dbi;
    protected final ShardManager shardManager;
    protected final TypeManager typeManager;

    @Inject
    public RaptorMetadataFactory(
            RaptorConnectorId connectorId,
            @ForMetadata IDBI dbi,
            ShardManager shardManager,
            TypeManager typeManager)
    {
        this.connectorId = requireNonNull(connectorId, "connectorId is null").toString();
        this.dbi = requireNonNull(dbi, "dbi is null");
        this.shardManager = requireNonNull(shardManager, "shardManager is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
    }

    public RaptorMetadata create(LongConsumer beginDeleteForTableId)
    {
        return new RaptorMetadata(connectorId, dbi, shardManager, typeManager, beginDeleteForTableId);
    }
}
