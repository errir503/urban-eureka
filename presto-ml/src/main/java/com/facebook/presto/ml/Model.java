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
package com.facebook.presto.ml;

import com.facebook.presto.ml.type.ModelType;

public interface Model
{
    ModelType getType();

    byte[] getSerializedData();

    void train(Dataset dataset);

    /**
     * Models must also provide a public static deserialization method, which accepts a byte[] returned from getSerializedData
     */
    // TODO replace this with a ModelFactory interface
    //static Model deserialize(byte[] data)
}
