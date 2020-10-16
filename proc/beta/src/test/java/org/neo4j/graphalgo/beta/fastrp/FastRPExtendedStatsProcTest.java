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
package org.neo4j.graphalgo.beta.fastrp;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.embeddings.fastrp.FastRP;
import org.neo4j.graphalgo.AlgoBaseProc;
import org.neo4j.graphalgo.GdsCypher;
import org.neo4j.graphalgo.api.DefaultValue;
import org.neo4j.graphalgo.core.CypherMapWrapper;

import java.util.List;
import java.util.Optional;

class FastRPExtendedStatsProcTest extends FastRPExtendedProcTest<FastRPExtendedStatsConfig> {

    @Override
    public Class<? extends AlgoBaseProc<FastRP, FastRP, FastRPExtendedStatsConfig>> getProcedureClazz() {
        return FastRPExtendedStatsProc.class;
    }

    @Override
    public FastRPExtendedStatsConfig createConfig(CypherMapWrapper mapWrapper) {
        return FastRPExtendedStatsConfig.of(getUsername(), Optional.empty(), Optional.empty(), mapWrapper);
    }

    @Test
    void shouldNotCrash() {
        var query = GdsCypher.call()
            .withNodeLabel("Node")
            .withRelationshipType("REL")
            .withNodeProperties(List.of("f1", "f2"), DefaultValue.of(0D))
            .algo("gds.beta.fastRPExtended")
            .statsMode()
            .addParameter("embeddingDimension", 4)
            .addParameter("propertyDimension", 2)
            .addParameter("nodePropertyNames", List.of("f1", "f2"))
            .yields();

        runQuery(query);
    }
}