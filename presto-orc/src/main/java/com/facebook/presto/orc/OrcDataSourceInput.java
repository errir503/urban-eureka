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
package com.facebook.presto.orc;

import io.airlift.slice.FixedLengthSliceInput;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public final class OrcDataSourceInput
{
    private final FixedLengthSliceInput input;
    private final int retainedSizeInBytes;

    public OrcDataSourceInput(FixedLengthSliceInput input, int retainedSizeInBytes)
    {
        this.input = requireNonNull(input, "input is null");
        checkArgument(retainedSizeInBytes >= 0, "retainedSizeInBytes is negative");
        this.retainedSizeInBytes = retainedSizeInBytes;
    }

    public FixedLengthSliceInput getInput()
    {
        return input;
    }

    public int getRetainedSizeInBytes()
    {
        return retainedSizeInBytes;
    }
}
