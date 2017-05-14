/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.interop;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;

import java.util.concurrent.TimeUnit;

import io.reactivex.flowable.Flowable;
import io.reactivex.observable.Observable;
import kotlin.jvm.functions.Function1;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class FlatMapJustPerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int times;

    Flowable<Integer> flowable;

    Observable<Integer> observable;

    @Setup
    public void setup() {
        Integer[] array = new Integer[times];

        flowable = Flowable.fromArray(array).flatMap(new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return Flowable.just(v);
            }
        });

        observable = Observable.fromArray(array).flatMap(new Function1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> invoke(Integer v) {
                return Observable.just(v);
            }
        });
    }

    @Benchmark
    public void flowable(Blackhole bh) {
        flowable.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void observable(Blackhole bh) {
        observable.subscribe(new PerfConsumer(bh));
    }
}
