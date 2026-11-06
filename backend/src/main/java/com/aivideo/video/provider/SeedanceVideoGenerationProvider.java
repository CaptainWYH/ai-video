package com.aivideo.video.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Component
public class SeedanceVideoGenerationProvider implements VideoGenerationProvider {

    private final SeedanceVideoProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient client;

    public SeedanceVideoGenerationProvider(SeedanceVideoProperties properties,
                                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.toIntExact(properties.getRequestTimeout().toMillis());
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        this.client = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    @Override
    public String name() {
        return "seedance";
    }

    @Override
    public ProviderSubmission submit(VideoProviderRequest request) {
        VideoGenerationOptions options = request.options();
        CreateTaskRequest body = buildCreateRequest(request);
        log.info("提交 Seedance 任务: model={}, resolution={}, ratio={}, duration={}, "
                        + "generateAudio={}, watermark={}, outputFormat={}, callback={}",
                properties.getModel(),
                options.resolution(),
                options.ratio(),
                options.durationSeconds(),
                options.generateAudio(),
                options.watermark(),
                options.outputFormat(),
                StringUtils.hasText(request.callbackUrl()));
        try {
            CreateTaskResponse response = client.post()
                    .uri("/contents/generations/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(CreateTaskResponse.class);
            if (response == null || !StringUtils.hasText(response.id())) {
                throw new VideoProviderException("Seedance 未返回任务 ID", null, "EmptyTaskId", true);
            }
            log.info("Seedance 创建任务成功: providerTaskId={}, model={}, resolution={}, duration={}",
                    response.id(), properties.getModel(), options.resolution(), options.durationSeconds());
            return new ProviderSubmission(response.id(), "queued");
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            throw new VideoProviderException("Seedance 请求超时或网络不可用", null, "NetworkError", true);
        }
    }

    @Override
    public ProviderTaskResult query(String providerTaskId) {
        try {
            TaskResponse response = client.get()
                    .uri("/contents/generations/tasks/{id}", providerTaskId)
                    .retrieve()
                    .body(TaskResponse.class);
            if (response == null) {
                throw new VideoProviderException("Seedance 未返回任务详情", null, "EmptyTask", true);
            }
            ProviderTaskResult result = toProviderResult(response);
            log.info("Seedance 查询任务: providerTaskId={}, status={}, videoUrl={}, tokens={}",
                    result.providerTaskId(),
                    result.providerStatus(),
                    result.videoUrl(),
                    result.completionTokens());
            return result;
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            throw new VideoProviderException("查询 Seedance 任务超时", null, "NetworkError", true);
        }
    }

    @Override
    public void cancel(String providerTaskId) {
        try {
            client.delete()
                    .uri("/contents/generations/tasks/{id}", providerTaskId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            throw new VideoProviderException("取消 Seedance 任务超时", null, "NetworkError", true);
        }
    }

    private ProviderTaskResult toProviderResult(TaskResponse response) {
        VideoContent output = response.content();
        Usage usage = response.usage();
        ErrorDetail error = response.error();
        return new ProviderTaskResult(
                response.id(),
                response.status(),
                output == null ? null : output.videoUrl(),
                error == null ? null : error.code(),
                error == null ? null : error.message(),
                usage == null ? null : usage.completionTokens(),
                usage == null ? null : usage.totalTokens(),
                response.duration(),
                response.resolution(),
                response.ratio(),
                response.seed());
    }

    private VideoProviderException translate(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        ProviderError providerError = parseProviderError(ex.getResponseBodyAsString());
        String code = providerError.code();
        String providerMessage = providerError.message();
        String parameter = providerError.param();
        boolean retryable = status == 429 || status >= 500;
        String fallbackMessage = status == 401 || status == 403
                ? "Seedance 鉴权失败，请检查 ARK_API_KEY"
                : "Seedance 请求失败（HTTP " + status + "）";
        String message = StringUtils.hasText(providerMessage) ? providerMessage : fallbackMessage;
        log.warn("Seedance 请求失败: status={}, code={}, param={}, message={}",
                status, code, parameter, message);
        return new VideoProviderException(message, status, code, retryable);
    }

    private ProviderError parseProviderError(String body) {
        if (!StringUtils.hasText(body)) {
            return new ProviderError(null, null, null);
        }
        try {
            ErrorEnvelope envelope = objectMapper.readValue(body, ErrorEnvelope.class);
            if (envelope.error() == null) {
                return new ProviderError(null, null, null);
            }
            return new ProviderError(
                    envelope.error().code(),
                    envelope.error().message(),
                    envelope.error().param());
        } catch (Exception ignored) {
            return new ProviderError(null, null, null);
        }
    }

    private CreateTaskRequest buildCreateRequest(VideoProviderRequest request) {
        VideoGenerationOptions options = request.options();
        return new CreateTaskRequest(
                properties.getModel(),
                List.of(new Content("text", request.prompt())),
                options.resolution(),
                options.ratio(),
                options.durationSeconds(),
                options.generateAudio(),
                options.watermark(),
                options.outputFormat(),
                "aivideo-user-" + request.userId(),
                request.callbackUrl());
    }

    String serializeCreateRequestForTest(VideoProviderRequest request) {
        try {
            return objectMapper.writeValueAsString(buildCreateRequest(request));
        } catch (Exception ex) {
            throw new IllegalStateException("Seedance 请求序列化失败", ex);
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CreateTaskRequest(
            String model,
            List<Content> content,
            String resolution,
            String ratio,
            int duration,
            @JsonProperty("generate_audio") boolean generateAudio,
            boolean watermark,
            @JsonProperty("output_format") String outputFormat,
            @JsonProperty("safety_identifier") String safetyIdentifier,
            @JsonProperty("callback_url") String callbackUrl) {
    }

    private record Content(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CreateTaskResponse(String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TaskResponse(
            String id,
            String model,
            String status,
            VideoContent content,
            Usage usage,
            ErrorDetail error,
            Integer duration,
            String resolution,
            String ratio,
            Long seed,
            @JsonProperty("created_at") Long createdAt,
            @JsonProperty("updated_at") Long updatedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VideoContent(@JsonProperty("video_url") String videoUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(
            @JsonProperty("completion_tokens") Long completionTokens,
            @JsonProperty("total_tokens") Long totalTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorDetail(String code, String message, String param, String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorEnvelope(ErrorDetail error) {
    }

    private record ProviderError(String code, String message, String param) {
    }
}
