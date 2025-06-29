package org.fa.oss.contribution.helper.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.extern.slf4j.Slf4j;
import org.fa.oss.contribution.helper.cache.CentralCacheService;
import org.fa.oss.contribution.helper.constants.Ollama;
import org.fa.oss.contribution.helper.dto.request.OllamaRequest;
import org.fa.oss.contribution.helper.dto.response.IssueDTO;
import org.fa.oss.contribution.helper.dto.response.IssueSummaryResultListDTO;
import org.fa.oss.contribution.helper.dto.response.OllamaResponse;
import org.fa.oss.contribution.helper.dto.response.SummaryDTO;
import org.fa.oss.contribution.helper.model.IssueSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Profile("local")
@Service
@Slf4j
public class OllamaSummaryService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CentralCacheService centralCacheService;

    @Autowired PromptService promptService;

    @Autowired IssuesService issuesService;
    private WebClient webClient =
            WebClient.builder()
                    .baseUrl(Ollama.URL_AND_PORT)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .exchangeStrategies(
                            ExchangeStrategies.builder()
                                    .codecs(
                                            configurer ->
                                                    configurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)) // 100 MB
                                    .build())
                    .build();

    public IssueSummaryResultListDTO getSummaries(int limit) {
        IssueSummaryResultListDTO cached = centralCacheService.getSummaryCache().load();
        if (cached != null) {
            log.info("Loaded {} summaries from cache", cached.getCount());
            return cached;
        }

        List<IssueDTO> issues = issuesService.getIssues();
        List<IssueSummary> summaries = maybeLimit(issues.stream(),limit)
                .map( issueDTO -> generateIssueSummary(issueDTO))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return IssueSummaryResultListDTO.builder().summaries(summaries).count(summaries.size()).build();
    }

    private IssueSummary generateIssueSummary(IssueDTO issue) {

        String prompt = promptService.preparePrompt(issue);
        String format =
                "{ \"type\": \"object\", \"properties\": { \"main\": { \"type\": \"string\", \"description\": \"A concise summary of the issue or bug being reported.\" }, \"validationOrRequirement\": { \"type\": \"string\", \"description\": \"The expected behavior or requirement that needs to be met.\" }, \"attemptedFixes\": { \"type\": \"string\", \"description\": \"Any attempted solutions or approaches that have been tried so far.\" }, \"otherNotes\": { \"type\": \"string\", \"description\": \"Additional context, labels, or contributor guidance related to the issue.\" } }, \"required\": [ \"main\", \"validationOrRequirement\", \"attemptedFixes\", \"otherNotes\" ] }";
        Map<String, Object> formatSchema = null;
        try {
            formatSchema = objectMapper.readValue(format, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("ollama response format key failed to parse to json", e);
            formatSchema = Map.of("type", "json");
        }
        OllamaRequest request =
                OllamaRequest.builder().model(Ollama.MODEL).prompt(prompt).stream(false)
                        .format(formatSchema)
                        .build();

        log.info("Generating summary for issue: " + issue.getId());
        String response =
                webClient
                        .post()
                        .uri(Ollama.GENERATE_ENDPOINT)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

        try {
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            OllamaResponse ollamaResponse = objectMapper.readValue(response, OllamaResponse.class);
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
            log.info(objectMapper.writeValueAsString(ollamaResponse));
            SummaryDTO summaryDTO;
            try {
                summaryDTO = objectMapper.readValue(ollamaResponse.getResponse(), SummaryDTO.class);
                summaryDTO.setValidJson(true);
                summaryDTO.setSummaryText("");
            } catch (JsonParseException e) {
                summaryDTO =
                        SummaryDTO.builder().summaryText(ollamaResponse.getResponse()).validJson(false).build();
            }
            return IssueSummary.builder()
                    .issueDTO(issue)
                    .summary(summaryDTO)
                    .updatedAt(ollamaResponse.getCreatedAt().toEpochSecond())
                    .build();
        } catch (IOException e) {
            log.error("Failed to parse Ollama response", e);
            return IssueSummary.builder()
                    .issueDTO(issue)
                    .summary(SummaryDTO.builder().summaryText(response).validJson(false).build())
                    .updatedAt(ZonedDateTime.now().toEpochSecond())
                    .build();
        }
    }

    private <T> Stream<T> maybeLimit(Stream<T> stream, int limit) {
        return limit > 0 ? stream.limit(limit) : stream;
    }

}
