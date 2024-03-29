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
package com.facebook.presto.operator.aggregation.noisyaggregation.sketch;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Note: Due to finite-precision implementation details, usage of floating-point functions
 * as random noise, while cryptographically secure, may leak information from a privacy context.
 * See "On Significance of the Least Significant Bits For Differential Privacy" by Mironov
 * and use judiciously.
 */
public class ThreadLocalRandomizationStrategy
        extends RandomizationStrategy
{
    public double nextDouble()
    {
        return ThreadLocalRandom.current().nextDouble();
    }
}
