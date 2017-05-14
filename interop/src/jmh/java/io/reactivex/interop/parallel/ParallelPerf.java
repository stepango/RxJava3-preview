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

package io.reactivex.interop.parallel;

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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.reactivex.common.Schedulers;
import io.reactivex.flowable.Flowable;
import io.reactivex.flowable.GroupedFlowable;
import io.reactivex.interop.PerfAsyncConsumer;
import kotlin.jvm.functions.Function1;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1,jvmArgsAppend = { "-XX:MaxInlineLevel=20" })
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class ParallelPerf implements Function1<Integer, Integer> {

    @Param({"10000"})
    public int count;

    @Param({"1", "10", "100", "1000", "10000"})
    public int compute;

    @Param({"1", "2", "3", "4"})
    public int parallelism;

    Flowable<Integer> flatMap;

    Flowable<Integer> groupBy;

    Flowable<Integer> parallel;

    @Override
    public Integer invoke(Integer t) {
        Blackhole.consumeCPU(compute);
        return t;
    }

    @Setup
    public void setup() {

        final int cpu = parallelism;

        Integer[] ints = new Integer[count];
        Arrays.fill(ints, 777);

        Flowable<Integer> source = Flowable.fromArray(ints);

        flatMap = source.flatMap(new Function1<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(Integer v) {
                return Flowable.just(v).subscribeOn(Schedulers.computation())
                        .map(ParallelPerf.this);
            }
        }, cpu);

        groupBy = source.groupBy(new Function1<Integer, Integer>() {
            int i;
            @Override
            public Integer invoke(Integer v) {
                return (i++) % cpu;
            }
        })
                .flatMap(new Function1<GroupedFlowable<Integer, Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> invoke(GroupedFlowable<Integer, Integer> g) {
                return g.observeOn(Schedulers.computation()).map(ParallelPerf.this);
            }
        });

        parallel = source.parallel(cpu).runOn(Schedulers.computation()).map(this).sequential();
    }

    void subscribe(Flowable<Integer> f, Blackhole bh) {
        PerfAsyncConsumer consumer = new PerfAsyncConsumer(bh);
        f.subscribe(consumer);
        consumer.await(count);
    }

    @Benchmark
    public void flatMap(Blackhole bh) {
        subscribe(flatMap, bh);
    }

    @Benchmark
    public void groupBy(Blackhole bh) {
        subscribe(groupBy, bh);
    }

    @Benchmark
    public void parallel(Blackhole bh) {
        subscribe(parallel, bh);
    }
}