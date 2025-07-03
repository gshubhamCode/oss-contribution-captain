package org.fa.oss.contribution.helper.model;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    public String description;
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
    Map<String, OpenAIChatCompletionRequest.Property> properties = new HashMap<>();

    OpenAIChatCompletionRequest.Property mainProp = new OpenAIChatCompletionRequest.Property();
    mainProp.type = "string";
    mainProp.description = "A brief summary of the main goal or objective of the GitHub issue";
    properties.put("main", mainProp);

    OpenAIChatCompletionRequest.Property validationProp =
        new OpenAIChatCompletionRequest.Property();
    validationProp.type = "string";
    validationProp.description = "Any validations, constraints, or specific requirements mentioned";
    properties.put("validationOrRequirement", validationProp);

    OpenAIChatCompletionRequest.Property fixesProp = new OpenAIChatCompletionRequest.Property();
    fixesProp.type = "string";
    fixesProp.description = "Any fixes attempted, blockers encountered, or partial progress made";
    properties.put("attemptedFixes", fixesProp);

    OpenAIChatCompletionRequest.Property notesProp = new OpenAIChatCompletionRequest.Property();
    notesProp.type = "string";
    notesProp.description = "Additional context or comments that may help the contributor";
    properties.put("otherNotes", notesProp);

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
