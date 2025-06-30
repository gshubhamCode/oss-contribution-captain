package org.fa.oss.contribution.helper.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIChatCompletionResponse {

  public String id;
  public String object;
  public long created;
  public String model;
  public List<Choice> choices;
  public Usage usage;

  @Data
  public static class Choice {
    public int index;
    public Message message;
    public Object logprobs;
    public String finish_reason;
    public String stop_reason;
  }

  @Data
  public static class Message {
    public String role;
    public Object reasoning_content;
    public String content;
    public List<ToolCall> tool_calls;
  }

  @Data
  public static class ToolCall {
    public String id;
    public String type;
    public Function function;
  }

  @Data
  public static class Function {
    public String name;
    public String arguments;
  }

  @Data
  public static class Usage {
    public int prompt_tokens;
    public int total_tokens;
    public int completion_tokens;
    public Object prompt_tokens_details;
  }
}
