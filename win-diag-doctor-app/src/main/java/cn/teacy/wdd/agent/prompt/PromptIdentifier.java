package cn.teacy.wdd.agent.prompt;

import lombok.Getter;

@Getter
public enum PromptIdentifier {

    QUERY_EVENT_LOG("queryEventLog", "This tool can be used to query Windows event logs."),

    CHAT_AGENT_SYS_PROMPT("chatAgentSysPrompt", "You are a professional Windows system diagnostic expert. Provide accurate and concise answers to user questions based on the information available."),

    PRIVILEGE_CHECKER_SYS_PROMPT("privilegeCheckerSysPrompt", "You are a privilege escalation vulnerability detection expert. Analyze the provided system information to identify potential privilege escalation vulnerabilities and suggest remediation steps.")

    ;

    PromptIdentifier(String identifier, String defaultPrompt) {
        this.identifier = identifier;
        this.defaultPrompt = defaultPrompt;
    }

    private final String identifier;
    private final String defaultPrompt;

}
