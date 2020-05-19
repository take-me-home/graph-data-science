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
package org.neo4j.graphalgo.compat;

import org.neo4j.configuration.Config;
import org.neo4j.internal.batchimport.AdditionalInitialIds;
import org.neo4j.internal.batchimport.BatchImporter;
import org.neo4j.internal.batchimport.BatchImporterFactory;
import org.neo4j.internal.batchimport.Configuration;
import org.neo4j.internal.batchimport.ImportLogic;
import org.neo4j.internal.batchimport.InputIterable;
import org.neo4j.internal.batchimport.cache.LongArray;
import org.neo4j.internal.batchimport.cache.NumberArrayFactory;
import org.neo4j.internal.batchimport.cache.OffHeapLongArray;
import org.neo4j.internal.batchimport.input.Collector;
import org.neo4j.internal.batchimport.input.IdType;
import org.neo4j.internal.batchimport.input.Input;
import org.neo4j.internal.batchimport.input.PropertySizeCalculator;
import org.neo4j.internal.batchimport.input.ReadableGroups;
import org.neo4j.internal.batchimport.staging.ExecutionMonitor;
import org.neo4j.internal.kernel.api.CursorFactory;
import org.neo4j.internal.kernel.api.NodeCursor;
import org.neo4j.internal.kernel.api.NodeLabelIndexCursor;
import org.neo4j.internal.kernel.api.PropertyCursor;
import org.neo4j.internal.kernel.api.Read;
import org.neo4j.internal.kernel.api.RelationshipScanCursor;
import org.neo4j.internal.schema.IndexOrder;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.io.pagecache.PagedFile;
import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.io.pagecache.tracing.cursor.PageCursorTracer;
import org.neo4j.kernel.api.query.ExecutingQuery;
import org.neo4j.kernel.impl.store.NodeLabelsField;
import org.neo4j.kernel.impl.store.NodeStore;
import org.neo4j.kernel.impl.store.RecordStore;
import org.neo4j.kernel.impl.store.format.RecordFormat;
import org.neo4j.kernel.impl.store.format.RecordFormats;
import org.neo4j.kernel.impl.store.record.AbstractBaseRecord;
import org.neo4j.kernel.impl.store.record.NodeRecord;
import org.neo4j.kernel.impl.store.record.RecordLoad;
import org.neo4j.kernel.impl.transaction.log.files.TransactionLogInitializer;
import org.neo4j.logging.FormattedLog;
import org.neo4j.logging.Log;
import org.neo4j.logging.internal.LogService;
import org.neo4j.memory.EmptyMemoryTracker;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.scheduler.JobScheduler;

import java.io.IOException;
import java.io.PrintWriter;

public final class KernelApiProxy {

    public static <RECORD extends AbstractBaseRecord> void read(
        RecordFormat<RECORD> recordFormat,
        RECORD record,
        PageCursor cursor,
        RecordLoad mode,
        int recordSize,
        int recordsPerPage
    ) throws IOException {
        recordFormat.read(record, cursor, mode, recordSize, recordsPerPage);
    }

    public static long getHighestPossibleIdInUse(
        RecordStore<? extends AbstractBaseRecord> recordStore,
        PageCursorTracer pageCursorTracer
    ) {
        return recordStore.getHighestPossibleIdInUse(pageCursorTracer);
    }

    public static <RECORD extends AbstractBaseRecord> PageCursor openPageCursorForReading(
        RecordStore<RECORD> recordStore,
        long pageId,
        PageCursorTracer pageCursorTracer
    ) {
        return recordStore.openPageCursorForReading(pageId, pageCursorTracer);
    }

    public static PageCursor pageFileIO(
        PagedFile pagedFile,
        long pageId,
        int pageFileFlags,
        PageCursorTracer pageCursorTracer
    ) throws IOException {
        return pagedFile.io(pageId, pageFileFlags, pageCursorTracer);
    }

    public static PropertyCursor allocatePropertyCursor(CursorFactory cursorFactory, PageCursorTracer cursorTracer, MemoryTracker memoryTracker) {
        return cursorFactory.allocatePropertyCursor(cursorTracer, memoryTracker);
    }

    public static NodeCursor allocateNodeCursor(CursorFactory cursorFactory, PageCursorTracer cursorTracer ) {
        return cursorFactory.allocateNodeCursor(cursorTracer);
    }

    public static RelationshipScanCursor allocateRelationshipScanCursor(CursorFactory cursorFactory, PageCursorTracer cursorTracer ) {
        return cursorFactory.allocateRelationshipScanCursor(cursorTracer);
    }

    public static NodeLabelIndexCursor allocateNodeLabelIndexCursor(CursorFactory cursorFactory, PageCursorTracer cursorTracer ) {
        return cursorFactory.allocateNodeLabelIndexCursor(cursorTracer);
    }

    public static long[] getNodeLabelFields(NodeRecord node, NodeStore nodeStore, PageCursorTracer cursorTracer) {
        return NodeLabelsField.get(node, nodeStore, cursorTracer);
    }

    public static void nodeLabelScan(Read dataRead, int label, NodeLabelIndexCursor cursor) {
        dataRead.nodeLabelScan(label, cursor, IndexOrder.NONE);
    }

    public static OffHeapLongArray newOffHeapLongArray(long length, long defaultValue, long base) {
        return new OffHeapLongArray(length, defaultValue, base, EmptyMemoryTracker.INSTANCE);
    }

    public static LongArray newChunkedLongArray(NumberArrayFactory numberArrayFactory, int size, long defaultValue) {
        return numberArrayFactory.newLongArray(size, defaultValue, EmptyMemoryTracker.INSTANCE);
    }

    public static BatchImporter instantiateBatchImporter(
        BatchImporterFactory factory,
        DatabaseLayout directoryStructure,
        FileSystemAbstraction fileSystem,
        PageCache externalPageCache,
        PageCacheTracer pageCacheTracer,
        Configuration config,
        LogService logService,
        ExecutionMonitor executionMonitor,
        AdditionalInitialIds additionalInitialIds,
        Config dbConfig,
        RecordFormats recordFormats,
        ImportLogic.Monitor monitor,
        JobScheduler jobScheduler,
        Collector badCollector
    ) {
        return factory.instantiate(
            directoryStructure,
            fileSystem,
            externalPageCache,
            pageCacheTracer,
            config,
            logService,
            executionMonitor,
            additionalInitialIds,
            dbConfig,
            recordFormats,
            monitor,
            jobScheduler,
            badCollector,
            TransactionLogInitializer.getLogFilesInitializer(),
            EmptyMemoryTracker.INSTANCE
        );
    }

    public static Input batchInputFrom(CompatInput compatInput) {
        return new InputFromCompatInput(compatInput);
    }

    public static String queryText(ExecutingQuery query) {
        return query.rawQueryText();
    }

    public static Log toPrintWriter(FormattedLog.Builder builder, PrintWriter writer) {
        return builder.toPrintWriter(() -> writer);
    }

    private static final class InputFromCompatInput implements Input {
        private final CompatInput delegate;

        private InputFromCompatInput(CompatInput delegate) {
            this.delegate = delegate;
        }

        @Override
        public InputIterable nodes(Collector badCollector) {
            return delegate.nodes(badCollector);
        }

        @Override
        public InputIterable relationships(Collector badCollector) {
            return delegate.relationships(badCollector);
        }

        @Override
        public IdType idType() {
            return delegate.idType();
        }

        @Override
        public ReadableGroups groups() {
            return delegate.groups();
        }

        @Override
        public Estimates calculateEstimates(PropertySizeCalculator propertySizeCalculator) throws IOException {
            return delegate.calculateEstimates(propertySizeCalculator::calculateSize);
        }
    }


    private KernelApiProxy() {
        throw new UnsupportedOperationException("No instances");
    }
}