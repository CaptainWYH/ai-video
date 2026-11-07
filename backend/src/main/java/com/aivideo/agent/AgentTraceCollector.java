package com.aivideo.agent;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 聚合 Agent 推理步骤。按步骤 id 保序 upsert，限制步骤数与文本长度，
 * 并把每一步的增量/快照同步转发给下游（WebSocket 推送）。
 */
@Slf4j
public class AgentTraceCollector implements AgentProgressListener {

    private static final int MAX_STEPS = 40;
    private static final int MAX_THINKING_CHARS = 8000;
    private static final int MAX_TOOL_CHARS = 1500;
    private static final int MAX_TOTAL_CHARS = 12000;
    private static final String TRUNCATED = "\n…（已截断）";

    private final Map<String, StepState> steps = new LinkedHashMap<>();
    private final Consumer<AgentProgressEvent> downstream;
    private boolean finished;

    public AgentTraceCollector(Consumer<AgentProgressEvent> downstream) {
        this.downstream = downstream == null ? event -> {
        } : downstream;
    }

    @Override
    public void onProgress(AgentProgressEvent event) {
        if (finished || event == null || event.step() == null
                || event.step().id() == null || event.step().id().isBlank()) {
            return;
        }
        AgentTraceStep incoming = event.step();
        StepState state = steps.get(incoming.id());
        if (state == null) {
            if (steps.size() >= MAX_STEPS) {
                return;
            }
            state = new StepState(incoming.id());
            steps.put(incoming.id(), state);
        }
        apply(state, incoming);

        if (event.delta() != null && !event.delta().isEmpty()) {
            appendDelta(state, event.delta());
            forward(new AgentProgressEvent(state.toDeltaStep(), event.delta()));
        } else {
            forward(new AgentProgressEvent(state.toStep(), null));
        }
    }

    /**
     * 正常完成：RUNNING 步骤收敛为 SUCCESS，并补充「本轮完成」。
     * 空轨迹（Mock 模式）保持为空，不产生任何步骤。
     */
    public List<AgentTraceStep> complete() {
        if (finished) {
            return snapshot();
        }
        finished = true;
        if (steps.isEmpty()) {
            return List.of();
        }
        finishRunning(AgentTraceStatus.SUCCESS, false);
        addFinalStep("status:complete", AgentTraceType.STATUS,
                "本轮完成", AgentTraceStatus.SUCCESS, null);
        return snapshot();
    }

    /**
     * 等待用户确认：非工具 RUNNING 步骤收敛为 SUCCESS，工具步骤保留/收敛为 PENDING。
     */
    public List<AgentTraceStep> awaitingConfirmation() {
        if (finished) {
            return snapshot();
        }
        finished = true;
        if (steps.isEmpty()) {
            return List.of();
        }
        finishRunning(AgentTraceStatus.SUCCESS, true);
        return snapshot();
    }

    /**
     * 调用失败：RUNNING 步骤收敛为 ERROR，并补充错误步骤。
     */
    public List<AgentTraceStep> fail(String message) {
        if (finished) {
            return snapshot();
        }
        finished = true;
        if (steps.isEmpty()) {
            return List.of();
        }
        finishRunning(AgentTraceStatus.ERROR, false);
        addFinalStep("status:error", AgentTraceType.ERROR,
                "本轮失败", AgentTraceStatus.ERROR, message);
        return snapshot();
    }

    public List<AgentTraceStep> snapshot() {
        return List.copyOf(steps.values().stream().map(StepState::toStep).toList());
    }

    private void apply(StepState state, AgentTraceStep incoming) {
        if (incoming.type() != null) {
            state.type = incoming.type();
        }
        if (incoming.title() != null) {
            state.title = incoming.title();
        }
        if (incoming.toolName() != null) {
            state.toolName = incoming.toolName();
        }
        if (incoming.toolCallId() != null) {
            state.toolCallId = incoming.toolCallId();
        }
        if (incoming.status() != null && canApplyStatus(state.status, incoming.status())) {
            state.status = incoming.status();
        }
        if (incoming.startedAt() != null) {
            if (state.startedAt == null) {
                state.startedAt = incoming.startedAt();
            }
        } else if (state.startedAt == null) {
            state.startedAt = now();
        }
        if (incoming.detail() != null) {
            int detailLimit = state.type == AgentTraceType.THINKING
                    ? MAX_THINKING_CHARS : MAX_TOOL_CHARS;
            state.detail = truncate(incoming.detail(), detailLimit);
        }
        if (incoming.result() != null) {
            state.result = truncate(incoming.result(), MAX_TOOL_CHARS);
        }
        if (incoming.status() != null && incoming.status() != AgentTraceStatus.RUNNING) {
            state.endedAt = incoming.endedAt() != null ? incoming.endedAt() : now();
        }
    }

    private boolean canApplyStatus(AgentTraceStatus current, AgentTraceStatus incoming) {
        if (current == null) {
            return true;
        }
        if (current == AgentTraceStatus.SUCCESS
                || current == AgentTraceStatus.ERROR
                || current == AgentTraceStatus.DENIED) {
            return incoming != AgentTraceStatus.RUNNING && incoming != AgentTraceStatus.PENDING;
        }
        return true;
    }

    private void appendDelta(StepState state, String delta) {
        boolean thinking = state.type == AgentTraceType.THINKING;
        boolean toolResult = state.type == AgentTraceType.TOOL_RESULT;
        String current = toolResult ? state.result : state.detail;
        if (current == null) {
            current = "";
        }
        int limit = thinking ? MAX_THINKING_CHARS : MAX_TOOL_CHARS;
        int allowed = Math.min(limit - current.length(), MAX_TOTAL_CHARS - totalTextChars());
        if (allowed <= 0) {
            appendMarker(state);
            return;
        }
        String part = delta.length() <= allowed ? delta : delta.substring(0, allowed);
        String next = current + part;
        if (toolResult) {
            state.result = next;
        } else {
            state.detail = next;
        }
        if (part.length() < delta.length()
                || next.length() >= limit
                || totalTextChars() >= MAX_TOTAL_CHARS) {
            appendMarker(state);
        }
    }

    private void appendMarker(StepState state) {
        boolean toolResult = state.type == AgentTraceType.TOOL_RESULT;
        String current = toolResult ? state.result : state.detail;
        if (current != null && current.endsWith(TRUNCATED)) {
            return;
        }
        String marked = (current == null ? "" : current) + TRUNCATED;
        if (toolResult) {
            state.result = marked;
        } else {
            state.detail = marked;
        }
    }

    private String truncate(String text, int limit) {
        if (text == null) {
            return null;
        }
        int allowed = Math.max(0, Math.min(limit, MAX_TOTAL_CHARS - totalTextChars()));
        if (text.length() <= allowed) {
            return text;
        }
        return text.substring(0, allowed) + TRUNCATED;
    }

    private int totalTextChars() {
        int total = 0;
        for (StepState state : steps.values()) {
            if (state.detail != null) {
                total += state.detail.length();
            }
            if (state.result != null) {
                total += state.result.length();
            }
        }
        return total;
    }

    private void finishRunning(AgentTraceStatus status, boolean toolsToPending) {
        for (StepState state : steps.values()) {
            if (state.status != AgentTraceStatus.RUNNING) {
                continue;
            }
            boolean toolStep = state.type == AgentTraceType.TOOL_CALL
                    || state.type == AgentTraceType.TOOL_RESULT;
            state.status = toolsToPending && toolStep ? AgentTraceStatus.PENDING : status;
            state.endedAt = now();
            forward(new AgentProgressEvent(state.toStep(), null));
        }
    }

    private void addFinalStep(String id, AgentTraceType type, String title,
                              AgentTraceStatus status, String detail) {
        if (steps.size() >= MAX_STEPS && !steps.containsKey(id)) {
            return;
        }
        StepState state = new StepState(id);
        state.type = type;
        state.title = title;
        state.status = status;
        state.detail = detail;
        state.startedAt = now();
        state.endedAt = state.startedAt;
        steps.put(id, state);
        forward(new AgentProgressEvent(state.toStep(), null));
    }

    private void forward(AgentProgressEvent event) {
        try {
            downstream.accept(event);
        } catch (RuntimeException ex) {
            log.warn("推送 Agent 推理进度失败: {}", ex.getMessage());
        }
    }

    private String now() {
        return Instant.now().toString();
    }

    private static final class StepState {

        private final String id;
        private AgentTraceType type;
        private String title;
        private AgentTraceStatus status;
        private String detail;
        private String result;
        private String toolName;
        private String toolCallId;
        private String startedAt;
        private String endedAt;

        private StepState(String id) {
            this.id = id;
        }

        private AgentTraceStep toStep() {
            return new AgentTraceStep(id, type, title, status, detail, result,
                    toolName, toolCallId, startedAt, endedAt);
        }

        private AgentTraceStep toDeltaStep() {
            return new AgentTraceStep(id, type, title, status, null, null,
                    toolName, toolCallId, startedAt, endedAt);
        }
    }
}
