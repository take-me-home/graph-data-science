/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphalgo.core.utils;

import org.apache.commons.lang3.mutable.MutableLong;
import org.neo4j.logging.Log;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

public class BatchingProgressLogger implements ProgressLogger {
    public static final long MAXIMUM_LOG_INTERVAL = (long) Math.pow(2, 13);

    private final Log log;
    private long taskVolume;
    private long batchSize;
    private final String task;
    private final LongAdder progressCounter;
    private final ThreadLocal<MutableLong> callCounter;

    private static long calculateBatchSize(long taskVolume) {
        return (BitUtil.nearbyPowerOfTwo(taskVolume) >>> 6);
    }

    public BatchingProgressLogger(Log log, long taskVolume, String task) {
        this(log, taskVolume, calculateBatchSize(taskVolume), task);
    }

    public BatchingProgressLogger(Log log, long taskVolume, long batchSize, String task) {
        this.log = log;
        this.taskVolume = taskVolume;
        this.batchSize = batchSize;
        this.task = task;

        this.progressCounter = new LongAdder();
        this.callCounter = ThreadLocal.withInitial(MutableLong::new);
    }

    @Override
    public void logProgress(Supplier<String> msgFactory) {
        progressCounter.increment();
        long localProgress = callCounter.get().incrementAndGet();

        if ((localProgress & (batchSize - 1)) == 0) {
            String message = msgFactory != ProgressLogger.NO_MESSAGE ? msgFactory.get() : null;
            int percent = (int) ((progressCounter.sum() / (double) taskVolume) * 100);
            if (message == null || message.isEmpty()) {
                log.info("[%s] %s %d%%", Thread.currentThread().getName(), task, percent);
            } else {
                log.info("[%s] %s %d%% %s", Thread.currentThread().getName(), task, percent, message);
            }
        }
    }

    @Override
    public void logProgress(long progress, Supplier<String> msgFactory) {
        if (progress == 0) {
            return;
        }
        progressCounter.add(progress);
        long localProgress = callCounter.get().incrementAndGet();

        if ((localProgress & (batchSize -1)) == 0) {
            int percent = (int) ((progressCounter.sum() / (double) taskVolume) * 100);
            log.info("[%s] %s %d%%", Thread.currentThread().getName(), task, percent);
        }
    }

    @Override
    public void logMessage(Supplier<String> msg) {
        log.info("[%s] %s %s", Thread.currentThread().getName(), task, msg.get());
    }

    @Override
    public void reset(long newTaskVolume) {
        this.taskVolume = newTaskVolume;
        this.batchSize = calculateBatchSize(newTaskVolume);
        progressCounter.reset();
    }

    @Override
    public Log getLog() {
        return this.log;
    }

    @Override
    public void logProgress(double percentDone, Supplier<String> msg) {
        throw new UnsupportedOperationException("BatchProgressLogger does not support logging percentages");
    }
}
