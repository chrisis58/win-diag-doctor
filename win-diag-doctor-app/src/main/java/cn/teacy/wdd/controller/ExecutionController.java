package cn.teacy.wdd.controller;

import cn.teacy.wdd.support.ProbeContext;
import com.alibaba.cloud.ai.agent.studio.dto.AgentResumeRequest;
import com.alibaba.cloud.ai.agent.studio.dto.AgentRunRequest;
import com.alibaba.cloud.ai.agent.studio.dto.messages.AgentRunResponse;
import com.alibaba.cloud.ai.agent.studio.dto.messages.MessageDTO;
import com.alibaba.cloud.ai.agent.studio.dto.messages.ToolRequestConfirmMessageDTO;
import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

/**
 * Overwriting official controller {@link com.alibaba.cloud.ai.agent.studio.controller.ExecutionController}
 * Search "MODIFY HERE" to see the modifications.
 *
 */
@RestController
public class ExecutionController {

    private static final Logger log = LoggerFactory.getLogger(ExecutionController.class);
    final ObjectMapper mapper = new ObjectMapper();
    private final AgentLoader agentLoader;
    private final ProbeContext probeContext; // MODIFY HERE: add ProbeContext field

    @Autowired
    public ExecutionController(AgentLoader agentLoader, ProbeContext probeContext) {
        this.agentLoader = agentLoader;
        this.probeContext = probeContext; // MODIFY HERE: inject ProbeContext
    }


    /**
     * Executes an agent run and streams the resulting events using Server-Sent Events (SSE).
     *
     * @param request The AgentRunRequest containing run details.
     * @return A Flux that will stream events to the client in standard SSE format.
     */
    @PostMapping(value = "/run_sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> agentRunSse(@RequestBody AgentRunRequest request) {
        if (request.appName == null || request.appName.trim().isEmpty()) {
            log.warn(
                    "appName cannot be null or empty in SSE request for appName: {}, session: {}",
                    request.appName,
                    request.threadId);
            return Flux.error(
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "appName cannot be null or empty"));
        }
        if (request.threadId == null || request.threadId.trim().isEmpty()) {
            log.warn(
                    "threadId cannot be null or empty in SSE request for appName: {}, session: {}",
                    request.appName,
                    request.threadId);
            return Flux.error(
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "threadId cannot be null or empty"));
        }

        try {
            BaseAgent agent = agentLoader.loadAgent(request.appName);
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(request.threadId)
                    .addMetadata("user_id", request.userId)
                    .addMetadata("probe_id", probeContext.getProbeId()) // MODIFY HERE: add probe_id metadata
                    .build();

            return executeAgent(request.newMessage.toUserMessage(), agent, runnableConfig);
        }
        catch (Exception e) {
            log.error("Error during agent run for session {}", request.threadId, e);
            return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run failed", e));
        }
    }

    @PostMapping(value = "/resume_sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> agentResumeSse(@RequestBody AgentResumeRequest request) {
        if (request.appName == null || request.appName.trim().isEmpty()) {
            log.warn(
                    "appName cannot be null or empty in SSE request for appName: {}, session: {}",
                    request.appName,
                    request.threadId);
            return Flux.error(
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "appName cannot be null or empty"));
        }
        if (request.threadId == null || request.threadId.trim().isEmpty()) {
            log.warn(
                    "threadId cannot be null or empty in SSE request for appName: {}, session: {}",
                    request.appName,
                    request.threadId);
            return Flux.error(
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "threadId cannot be null or empty"));
        }

        try {
            BaseAgent agent = agentLoader.loadAgent(request.appName);

            InterruptionMetadata.Builder metadataBuilder = InterruptionMetadata.builder();

            // Convert ToolFeedback from request to InterruptionMetadata.ToolFeedback
            if (request.toolFeedbacks != null && !request.toolFeedbacks.isEmpty()) {
                for (ToolRequestConfirmMessageDTO.ToolFeedback toolFeedback : request.toolFeedbacks) {
                    // Convert FeedbackResult from DTO to InterruptionMetadata enum
                    InterruptionMetadata.ToolFeedback.FeedbackResult result =
                            toolFeedback.getResult() != null
                                    ? InterruptionMetadata.ToolFeedback.FeedbackResult.valueOf(toolFeedback.getResult()
                                    .name())
                                    : InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED;

                    // Create InterruptionMetadata.ToolFeedback
                    InterruptionMetadata.ToolFeedback feedback = new InterruptionMetadata.ToolFeedback(
                            toolFeedback.getId(),
                            toolFeedback.getName(),
                            toolFeedback.getArguments(),
                            result,
                            toolFeedback.getDescription()
                    );

                    metadataBuilder.addToolFeedback(feedback);
                }
            }

            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(request.threadId)
                    .addMetadata("user_id", request.userId)
                    .addMetadata("probe_id", probeContext.getProbeId()) // MODIFY HERE: add probe_id metadata
                    .addHumanFeedback(metadataBuilder.build())
                    .build();

            return executeAgent(null, agent, runnableConfig);
        }
        catch (Exception e) {
            log.error("Error during agent run for session {}", request.threadId, e);
            return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Agent run failed", e));
        }
    }

    @NotNull
    private Flux<ServerSentEvent<String>> executeAgent(UserMessage userMessage, BaseAgent agent, RunnableConfig runnableConfig) throws GraphRunnerException {

        Flux<NodeOutput> agentStream;

        if (userMessage != null) {
            agentStream = agent.stream(userMessage, runnableConfig);
        }
        else {
            agentStream = agent.stream("", runnableConfig);
        }

        // Convert Flux<NodeOutput> to Flux<ServerSentEvent<String>>
        return agentStream.map(nodeOutput -> {
                    String node = nodeOutput.node();
                    String agentName = nodeOutput.agent();
                    Usage tokenUsage = nodeOutput.tokenUsage();


                    // For streaming, we can use the message content as chunk
                    StringBuilder chunkBuilder = new StringBuilder();
                    AgentRunResponse agentResponse = null;
                    if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
                        Message message = streamingOutput.message();
                        if (message == null) { // no update, typically output responses from nodes that does not produce messages
                            return ServerSentEvent.<String>builder()
                                    .data("{}")
                                    .build();
                        }
                        if (message instanceof AssistantMessage assistantMessage) {
                            if (assistantMessage.hasToolCalls()) {
                                agentResponse = new AgentRunResponse(node, agentName, assistantMessage, tokenUsage, "");
                            }
                            else {
//						chunkBuilder.append(assistantMessage.getText());
                                agentResponse = new AgentRunResponse(node, agentName, assistantMessage, tokenUsage, assistantMessage.getText());
                            }
                        }
                        else {
                            agentResponse = new AgentRunResponse(node, agentName, message, tokenUsage, "");
                        }
                    }
                    else if (nodeOutput instanceof InterruptionMetadata interruptionMetadata) {
                        // Use the specialized method to convert InterruptionMetadata to ToolRequestMessageDTO
                        ToolRequestConfirmMessageDTO toolRequestMessage = MessageDTO.MessageDTOFactory.fromInterruptionMetadata(interruptionMetadata);
                        agentResponse = new AgentRunResponse(node, agentName, toolRequestMessage, tokenUsage, "");
                    }
                    else {
                        // Handle other NodeOutput types if necessary
//					agentResponse = new AgentRunResponse(node, agentName, null, tokenUsage, "");
                    }


                    // Serialize to JSON string
                    try {
                        if (agentResponse != null) {
                            String jsonData = mapper.writeValueAsString(agentResponse);
                            return ServerSentEvent.<String>builder()
                                    .data(jsonData)
                                    .build();
                        }
                    }
                    catch (Exception e) {
                        log.error("Failed to serialize AgentRunResponse to JSON", e);
                        return ServerSentEvent.<String>builder()
                                .data("{\"error\":\"Failed to serialize response\"}")
                                .build();
                    }
                    return ServerSentEvent.<String>builder()
                            .data("{}")
                            .build();
                })
                .onErrorResume(error -> {
                    // Handle errors from the agent stream and convert to SSE error event
                    log.error("Error occurred during agent stream execution", error);

                    // Create error response
                    String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown error occurred";
                    String errorType = error.getClass().getSimpleName();

                    try {
                        // Create a structured error response
                        String errorJson = String.format(
                                "{\"error\":true,\"errorType\":\"%s\",\"errorMessage\":\"%s\"}",
                                errorType.replace("\"", "\\\""),
                                errorMessage.replace("\"", "\\\"").replace("\n", "\\n")
                        );

                        // Return the error as an SSE event and complete the stream
                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data(errorJson)
                                        .build()
                        );
                    }
                    catch (Exception e) {
                        log.error("Failed to create error SSE event", e);
                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data("{\"error\":true,\"errorMessage\":\"Internal error occurred\"}")
                                        .build()
                        );
                    }
                });
    }
}
