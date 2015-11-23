/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.plugin.ingest.transport.simulate;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;

public class SimulatePipelineResponseTests extends ESTestCase {

    public void testSerialization() throws IOException {
        boolean isVerbose = randomBoolean();
        int numResults = randomIntBetween(1, 10);
        List<SimulateDocumentResult> results = new ArrayList<>(numResults);
        for (int i = 0; i < numResults; i++) {
            boolean isFailure = randomBoolean();
            IngestDocument ingestDocument = new IngestDocument(randomAsciiOfLengthBetween(1, 10), randomAsciiOfLengthBetween(1, 10), randomAsciiOfLengthBetween(1, 10),
                    Collections.singletonMap(randomAsciiOfLengthBetween(1, 10), randomAsciiOfLengthBetween(1, 10)));
            if (isVerbose) {
                int numProcessors = randomIntBetween(1, 10);
                List<SimulateProcessorResult> processorResults = new ArrayList<>(numProcessors);
                for (int j = 0; j < numProcessors; j++) {
                    String processorId = randomAsciiOfLengthBetween(1, 10);
                    SimulateProcessorResult processorResult;
                    if (isFailure) {
                        processorResult = new SimulateProcessorResult(processorId, new IllegalArgumentException("test"));
                    } else {
                        processorResult = new SimulateProcessorResult(processorId, ingestDocument);
                    }
                    processorResults.add(processorResult);
                }
                results.add(new SimulateDocumentVerboseResult(processorResults));
            } else {
                results.add(new SimulateDocumentSimpleResult(ingestDocument));
                SimulateDocumentSimpleResult simulateDocumentSimpleResult;
                if (isFailure) {
                    simulateDocumentSimpleResult = new SimulateDocumentSimpleResult(new IllegalArgumentException("test"));
                } else {
                    simulateDocumentSimpleResult = new SimulateDocumentSimpleResult(ingestDocument);
                }
                results.add(simulateDocumentSimpleResult);
            }
        }

        SimulatePipelineResponse response = new SimulatePipelineResponse(randomAsciiOfLengthBetween(1, 10), isVerbose, results);
        BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);
        StreamInput streamInput = StreamInput.wrap(out.bytes());
        SimulatePipelineResponse otherResponse = new SimulatePipelineResponse();
        otherResponse.readFrom(streamInput);

        assertThat(otherResponse.getPipelineId(), equalTo(response.getPipelineId()));
        assertThat(otherResponse.getResults().size(), equalTo(response.getResults().size()));

        Iterator<SimulateDocumentResult> expectedResultIterator = response.getResults().iterator();
        for (SimulateDocumentResult result : otherResponse.getResults()) {
            if (isVerbose) {
                SimulateDocumentVerboseResult expectedSimulateDocumentVerboseResult = (SimulateDocumentVerboseResult) expectedResultIterator.next();
                assertThat(result, instanceOf(SimulateDocumentVerboseResult.class));
                SimulateDocumentVerboseResult simulateDocumentVerboseResult = (SimulateDocumentVerboseResult) result;
                assertThat(simulateDocumentVerboseResult.getProcessorResults().size(), equalTo(expectedSimulateDocumentVerboseResult.getProcessorResults().size()));
                Iterator<SimulateProcessorResult> expectedProcessorResultIterator = expectedSimulateDocumentVerboseResult.getProcessorResults().iterator();
                for (SimulateProcessorResult simulateProcessorResult : simulateDocumentVerboseResult.getProcessorResults()) {
                    SimulateProcessorResult expectedProcessorResult = expectedProcessorResultIterator.next();
                    assertThat(simulateProcessorResult.getProcessorId(), equalTo(expectedProcessorResult.getProcessorId()));
                    assertThat(simulateProcessorResult.getData(), equalTo(expectedProcessorResult.getData()));
                    if (expectedProcessorResult.getFailure() == null) {
                        assertThat(simulateProcessorResult.getFailure(), nullValue());
                    } else {
                        assertThat(simulateProcessorResult.getFailure(), instanceOf(IllegalArgumentException.class));
                        IllegalArgumentException e = (IllegalArgumentException) simulateProcessorResult.getFailure();
                        assertThat(e.getMessage(), equalTo("test"));
                    }
                }
            } else {
                SimulateDocumentSimpleResult expectedSimulateDocumentSimpleResult = (SimulateDocumentSimpleResult) expectedResultIterator.next();
                assertThat(result, instanceOf(SimulateDocumentSimpleResult.class));
                SimulateDocumentSimpleResult simulateDocumentSimpleResult = (SimulateDocumentSimpleResult) result;
                assertThat(simulateDocumentSimpleResult.getData(), equalTo(expectedSimulateDocumentSimpleResult.getData()));
                if (expectedSimulateDocumentSimpleResult.getFailure() == null) {
                    assertThat(simulateDocumentSimpleResult.getFailure(), nullValue());
                } else {
                    assertThat(simulateDocumentSimpleResult.getFailure(), instanceOf(IllegalArgumentException.class));
                    IllegalArgumentException e = (IllegalArgumentException) simulateDocumentSimpleResult.getFailure();
                    assertThat(e.getMessage(), equalTo("test"));
                }
            }
        }
    }
}
