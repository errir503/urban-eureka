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
package com.facebook.presto.connector.jmx;

import com.facebook.airlift.testing.EquivalenceTester;
import org.testng.annotations.Test;

import static com.facebook.presto.common.type.BigintType.BIGINT;
import static com.facebook.presto.common.type.VarcharType.createUnboundedVarcharType;
import static com.facebook.presto.connector.jmx.MetadataUtil.COLUMN_CODEC;
import static org.testng.Assert.assertEquals;

public class TestJmxColumnHandle
{
    @Test
    public void testJsonRoundTrip()
    {
        JmxColumnHandle handle = new JmxColumnHandle("columnName", createUnboundedVarcharType());
        String json = COLUMN_CODEC.toJson(handle);
        JmxColumnHandle copy = COLUMN_CODEC.fromJson(json);
        assertEquals(copy, handle);
    }

    @Test
    public void testEquivalence()
    {
        EquivalenceTester.equivalenceTester()
                .addEquivalentGroup(
                        new JmxColumnHandle("columnName", createUnboundedVarcharType()),
                        new JmxColumnHandle("columnName", createUnboundedVarcharType()))
                .addEquivalentGroup(
                        new JmxColumnHandle("columnNameX", createUnboundedVarcharType()),
                        new JmxColumnHandle("columnNameX", createUnboundedVarcharType()))
                .addEquivalentGroup(
                        new JmxColumnHandle("columnName", BIGINT),
                        new JmxColumnHandle("columnName", BIGINT))
                .check();
    }
}
