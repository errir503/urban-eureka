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
package com.facebook.presto.jdbc;

import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;

interface ConnectionProperty<T>
{
    String getKey();

    Optional<String> getDefault();

    DriverPropertyInfo getDriverPropertyInfo(Properties properties);

    boolean isRequired(Properties properties);

    boolean isAllowed(Properties properties);

    Optional<T> getValue(Properties properties)
            throws SQLException;

    default T getRequiredValue(Properties properties)
            throws SQLException
    {
        return getValue(properties).orElseThrow(() ->
                new SQLException(format("Connection property '%s' is required", getKey())));
    }

    void validate(Properties properties)
            throws SQLException;
}
