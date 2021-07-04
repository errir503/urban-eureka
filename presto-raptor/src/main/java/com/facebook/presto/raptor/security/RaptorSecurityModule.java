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
package com.facebook.presto.raptor.security;

import com.facebook.airlift.configuration.AbstractConfigurationAwareModule;
import com.facebook.presto.plugin.base.security.AllowAllAccessControlModule;
import com.facebook.presto.plugin.base.security.FileBasedAccessControlModule;
import com.facebook.presto.plugin.base.security.ReadOnlySecurityModule;
import com.google.inject.Binder;
import com.google.inject.Module;

import static com.facebook.airlift.configuration.ConditionalModule.installModuleIf;

public class RaptorSecurityModule
        extends AbstractConfigurationAwareModule
{
    @Override
    protected void setup(Binder binder)
    {
        bindSecurityModule("none", new AllowAllAccessControlModule());
        bindSecurityModule("read-only", new ReadOnlySecurityModule());
        bindSecurityModule("file", new FileBasedAccessControlModule());
    }

    private void bindSecurityModule(String name, Module module)
    {
        install(installModuleIf(
                RaptorSecurityConfig.class,
                security -> name.equalsIgnoreCase(security.getSecuritySystem()),
                module));
    }
}
