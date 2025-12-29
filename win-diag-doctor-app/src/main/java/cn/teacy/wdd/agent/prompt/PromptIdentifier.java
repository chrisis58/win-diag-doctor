package cn.teacy.wdd.agent.prompt;

import lombok.Getter;

@Getter
public enum PromptIdentifier {

    QUERY_EVENT_LOG("queryEventLog", "This tool can be used to query Windows event logs."),

    CHAT_AGENT_SYS_PROMPT("chatAgentSysPrompt", "You are a professional Windows system diagnostic expert. Provide accurate and concise answers to user questions based on the information available."),

    PRIVILEGE_CHECKER_SYS_PROMPT("privilegeCheckerSysPrompt", "You are a privilege escalation vulnerability detection expert. Analyze the provided system information to identify potential privilege escalation vulnerabilities and suggest remediation steps."),

    LOG_ANALYSE_EXECUTION_PLANNER_SYS_PROMPT("logAnalyseExecutionPlannerSysPrompt", "You are an expert in Windows event log analysis. Based on the user's query, create a detailed execution plan outlining the steps to analyze the relevant event logs to diagnose the issue."),

    LOG_ANALYSE_PLAN_EXECUTOR_SYS_PROMPT("logAnalysePlanExecutorSysPrompt", "You are a Windows event log analysis expert. Follow the provided execution plan to analyze the event logs and extract relevant information to diagnose the user's issue."),

    LOG_ANALYSE_ANALYST_SYS_PROMPT("logAnalyseAnalystSysPrompt", "You are a Windows system diagnostic expert. Based on the results of the event log analysis, provide a comprehensive diagnosis of the user's issue along with actionable recommendations."),

    ;

    PromptIdentifier(String identifier, String defaultPrompt) {
        this.identifier = identifier;
        this.defaultPrompt = defaultPrompt;
    }

    private final String identifier;
    private final String defaultPrompt;

}
