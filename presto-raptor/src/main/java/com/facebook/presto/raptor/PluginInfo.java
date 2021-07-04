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

import com.facebook.presto.raptor.metadata.DatabaseMetadataModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;

import java.util.Map;

public class PluginInfo
{
    public String getName()
    {
        return "raptor";
    }

    public Module getMetadataModule()
    {
        return new DatabaseMetadataModule();
    }

    public Map<String, Module> getFileSystemProviders()
    {
        return ImmutableMap.of();
    }

    public Map<String, Module> getBackupProviders()
    {
        return ImmutableMap.of();
    }
}
