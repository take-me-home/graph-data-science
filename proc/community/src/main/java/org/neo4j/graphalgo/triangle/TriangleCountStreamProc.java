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
package org.neo4j.graphalgo.triangle;

import org.neo4j.graphalgo.AlgorithmFactory;
import org.neo4j.graphalgo.StreamProc;
import org.neo4j.graphalgo.config.GraphCreateConfig;
import org.neo4j.graphalgo.core.CypherMapWrapper;
import org.neo4j.graphalgo.core.utils.paged.PagedAtomicIntegerArray;
import org.neo4j.graphalgo.core.write.PropertyTranslator;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class TriangleCountStreamProc
    extends StreamProc<IntersectingTriangleCount, PagedAtomicIntegerArray,
    TriangleCountStreamProc.Result, TriangleCountStreamConfig> {

    @Description(TriangleCountCompanion.DESCRIPTION)
    @Procedure(name = "gds.triangleCount.stream", mode = Mode.READ)
    public Stream<Result> stream(
        @Name(value = "graphName") Object graphNameOrConfig,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return stream(compute(graphNameOrConfig, configuration));
    }

    @Override
    protected Stream<Result> stream(ComputationResult<IntersectingTriangleCount, PagedAtomicIntegerArray, TriangleCountStreamConfig> computationResult) {
        var graph = computationResult.graph();
        var result = computationResult.result();

        return LongStream.range(0, graph.nodeCount())
            .mapToObj(i -> new Result(
                graph.toOriginalNodeId(i),
                Objects.requireNonNull(result).get(i)
            ));
    }

    @Override
    protected TriangleCountStreamProc.Result streamResult(long originalNodeId, double value) {
        throw new UnsupportedOperationException("TriangleCount handles result building individually.");
    }

    @Override
    protected TriangleCountStreamConfig newConfig(
        String username,
        Optional<String> graphName,
        Optional<GraphCreateConfig> maybeImplicitCreate,
        CypherMapWrapper config
    ) {
        return TriangleCountStreamConfig.of(
            username,
            graphName,
            maybeImplicitCreate,
            config
        );
    }

    @Override
    protected AlgorithmFactory<IntersectingTriangleCount, TriangleCountStreamConfig> algorithmFactory(
        TriangleCountStreamConfig config
    ) {
        return new IntersectingTriangleCountFactory<>();
    }

    @Override
    protected PropertyTranslator<PagedAtomicIntegerArray> nodePropertyTranslator(
        ComputationResult<IntersectingTriangleCount, PagedAtomicIntegerArray, TriangleCountStreamConfig> computationResult
    ) {
        return TriangleCountCompanion.nodePropertyTranslator();
    }

    public static class Result {

        public final long nodeId;
        public final long triangles;

        public Result(long nodeId, long triangles) {
            this.nodeId = nodeId;
            this.triangles = triangles;
        }
    }
}