package org.fa.oss.contribution.helper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIChatCompletionRequest {

  public String model;
  public List<Message> messages;
  public List<Tool> tools;
  public ToolChoice tool_choice;

  public static class Message {
    public String role;
    public String content;
  }

  public static class Tool {
    public String type;
    public Function function;
  }

  public static class Function {
    public String name;
    public Parameters parameters;
  }

  public static class Parameters {
    public String type;
    public Map<String, Property> properties;
    public List<String> required;
  }

  public static class Property {
    public String type;
  }

  public static class ToolChoice {
    public String type;
    public FunctionRef function;
  }

  public static class FunctionRef {
    public String name;
  }

  public static OpenAIChatCompletionRequest getDefaultChatRequest(String prompt) {
    // Root object
    OpenAIChatCompletionRequest request = new OpenAIChatCompletionRequest();
    request.model = "meta-llama/Meta-Llama-3-8B-Instruct";

    // Messages
    OpenAIChatCompletionRequest.Message systemMessage = new OpenAIChatCompletionRequest.Message();
    systemMessage.role = "system";
    systemMessage.content =
        "You are a helpful assistant that replies only with JSON according to the function definition.";

    OpenAIChatCompletionRequest.Message userMessage = new OpenAIChatCompletionRequest.Message();
    userMessage.role = "user";
    userMessage.content = prompt;

    request.messages = Arrays.asList(systemMessage, userMessage);

    // Tool Function Parameters
    OpenAIChatCompletionRequest.Property stringProperty =
        new OpenAIChatCompletionRequest.Property();
    stringProperty.type = "string";

    Map<String, OpenAIChatCompletionRequest.Property> properties = new HashMap<>();
    properties.put("main", stringProperty);
    properties.put("validationOrRequirement", stringProperty);
    properties.put("attemptedFixes", stringProperty);
    properties.put("otherNotes", stringProperty);

    OpenAIChatCompletionRequest.Parameters parameters =
        new OpenAIChatCompletionRequest.Parameters();
    parameters.type = "object";
    parameters.properties = properties;
    parameters.required =
        Arrays.asList("main", "validationOrRequirement", "attemptedFixes", "otherNotes");

    // Tool Function
    OpenAIChatCompletionRequest.Function function = new OpenAIChatCompletionRequest.Function();
    function.name = "summarizeIssue";
    function.parameters = parameters;

    OpenAIChatCompletionRequest.Tool tool = new OpenAIChatCompletionRequest.Tool();
    tool.type = "function";
    tool.function = function;

    request.tools = Collections.singletonList(tool);

    // Tool choice
    OpenAIChatCompletionRequest.FunctionRef functionRef =
        new OpenAIChatCompletionRequest.FunctionRef();
    functionRef.name = "summarizeIssue";

    OpenAIChatCompletionRequest.ToolChoice toolChoice =
        new OpenAIChatCompletionRequest.ToolChoice();
    toolChoice.type = "function";
    toolChoice.function = functionRef;

    request.tool_choice = toolChoice;

    return request;
  }
}
