package com.flowboard.service;

import com.flowboard.dto.AIAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIEngine {
    @Value("${ai.mock-enabled}")
    private boolean mockEnabled;

    private final ObjectMapper objectMapper;

    /**
     * Analyzes project description and generates AI-suggested board structure
     * Currently using mock data - in production would call OpenAI/Anthropic API
     */
    public AIAnalysisResult analyzeProjectDescription(String projectName, String description) {
        if (mockEnabled) {
            return generateMockAnalysis(projectName, description);
        }
        return generateMockAnalysis(projectName, description);
    }

    private AIAnalysisResult generateMockAnalysis(String projectName, String description) {
        AIAnalysisResult result = new AIAnalysisResult();

        // Generate stages based on keywords in description
        List<AIAnalysisResult.StageInfo> stages = generateStages(description);
        result.setStages(stages);

        // Generate tasks based on project context
        List<AIAnalysisResult.TaskInfo> tasks = generateTasks(projectName, description, stages);
        result.setTasks(tasks);

        return result;
    }

    private List<AIAnalysisResult.StageInfo> generateStages(String description) {
        List<AIAnalysisResult.StageInfo> stages = Arrays.asList(
            createStage("To Do", "bg-gray-100", 0),
            createStage("In Progress", "bg-blue-100", 1),
            createStage("Review", "bg-yellow-100", 2),
            createStage("Done", "bg-green-100", 3)
        );
        return stages;
    }

    private List<AIAnalysisResult.TaskInfo> generateTasks(String projectName, String description, List<AIAnalysisResult.StageInfo> stages) {
        String lowerDescription = description.toLowerCase();
        List<AIAnalysisResult.TaskInfo> tasks = Arrays.asList();

        if (lowerDescription.contains("design") || lowerDescription.contains("ui") || lowerDescription.contains("ux")) {
            tasks = Arrays.asList(
                createTask("Create wireframes", "Design low-fidelity wireframes for all major screens", "HIGH", "To Do"),
                createTask("Design system setup", "Set up color palette, typography, and component library", "MEDIUM", "To Do"),
                createTask("UI mockups review", "Review and iterate on high-fidelity mockups", "MEDIUM", "In Progress"),
                createTask("Design handoff", "Prepare design files and specifications for development", "MEDIUM", "Review"),
                createTask("Design approved", "Final design approval from stakeholders", "LOW", "Done")
            );
        } else if (lowerDescription.contains("develop") || lowerDescription.contains("build") || lowerDescription.contains("code") || lowerDescription.contains("app")) {
            tasks = Arrays.asList(
                createTask("Set up development environment", "Initialize repository and configure CI/CD", "CRITICAL", "To Do"),
                createTask("Implement core features", "Build the main functionality modules", "HIGH", "To Do"),
                createTask("Write unit tests", "Create comprehensive test coverage for core modules", "MEDIUM", "In Progress"),
                createTask("Code review", "Review pull requests and provide feedback", "MEDIUM", "Review"),
                createTask("Integration testing", "Test integration between components", "MEDIUM", "Done")
            );
        } else if (lowerDescription.contains("market") || lowerDescription.contains("campaign") || lowerDescription.contains("social")) {
            tasks = Arrays.asList(
                createTask("Define target audience", "Research and document target demographics", "HIGH", "To Do"),
                createTask("Create content calendar", "Plan content across all channels", "MEDIUM", "To Do"),
                createTask("Design marketing assets", "Create banners, social posts, and email templates", "MEDIUM", "In Progress"),
                createTask("Social media setup", "Configure social media accounts and profiles", "MEDIUM", "Review"),
                createTask("Campaign launch", "Launch marketing campaign across all channels", "HIGH", "Done")
            );
        } else {
            tasks = Arrays.asList(
                createTask("Define project scope", "Outline the goals and requirements for " + projectName, "HIGH", "To Do"),
                createTask("Create project plan", "Break down milestones and deliverables", "HIGH", "To Do"),
                createTask("Assign team roles", "Determine responsibilities for each team member", "MEDIUM", "To Do"),
                createTask("Initial research", "Research best practices and competitive landscape", "MEDIUM", "In Progress"),
                createTask("Stakeholder review", "Present progress to stakeholders and gather feedback", "MEDIUM", "Review"),
                createTask("Set up communication channels", "Configure Slack channels, meeting cadence", "LOW", "Done")
            );
        }

        return tasks;
    }

    private AIAnalysisResult.StageInfo createStage(String title, String color, int position) {
        AIAnalysisResult.StageInfo stage = new AIAnalysisResult.StageInfo();
        stage.title = title;
        stage.color = color;
        stage.position = position;
        return stage;
    }

    private AIAnalysisResult.TaskInfo createTask(String title, String description, String priority, String stageTitle) {
        AIAnalysisResult.TaskInfo task = new AIAnalysisResult.TaskInfo();
        task.title = title;
        task.description = description;
        task.priority = priority;
        task.stageTitle = stageTitle;
        return task;
    }

    /**
     * Analyzes meeting transcript to extract action items, decisions, and suggested changes
     * Currently using mock data - in production would call OpenAI/Anthropic API with LangChain4j
     */
    public MeetingAnalysisResult analyzeMeetingTranscript(String transcript) {
        return generateMockMeetingAnalysis(transcript);
    }

    private MeetingAnalysisResult generateMockMeetingAnalysis(String transcript) {
        MeetingAnalysisResult result = new MeetingAnalysisResult();

        String lower = transcript.toLowerCase();
        
        // Extract mock action items based on keywords
        if (lower.contains("task") || lower.contains("do") || lower.contains("action")) {
            result.addActionItem(
                "Complete implementation of user authentication module",
                "Discussed need for improved security measures",
                "HIGH"
            );
            result.addActionItem(
                "Schedule follow-up meeting with stakeholders",
                "Need to confirm requirements and timeline",
                "MEDIUM"
            );
            result.addActionItem(
                "Document API specification",
                "Required before development phase",
                "MEDIUM"
            );
        }

        // Always generate mock decisions (for testing with any transcript)
        // In production, these would be extracted from AI analysis
        result.addDecision(
            "Technology stack: React + Spring Boot",
            "Decision made after evaluating multiple options"
        );
        result.addDecision(
            "Sprint duration: 2 weeks",
            "Agreed upon by all team members"
        );
        result.addDecision(
            "Database: PostgreSQL for production data",
            "Chosen for scalability and compatibility with current infrastructure"
        );

        // Extract mock changes based on keywords
        if (lower.contains("move") || lower.contains("update") || lower.contains("card") || lower.contains("task")) {
            result.addChange(
                "MOVE_CARD",
                "Move 'API Design' card from To Do to In Progress",
                "Priority change based on stakeholder feedback"
            );
            result.addChange(
                "UPDATE_CARD",
                "Update 'User Auth' card description with security requirements",
                "Clarification from team discussion"
            );
        }

        return result;
    }

    // Inner class for meeting analysis results
    public static class MeetingAnalysisResult {
        private List<ActionItemData> actionItems = new java.util.ArrayList<>();
        private List<DecisionData> decisions = new java.util.ArrayList<>();
        private List<ChangeData> changes = new java.util.ArrayList<>();

        public void addActionItem(String description, String context, String priority) {
            actionItems.add(new ActionItemData(description, context, priority));
        }

        public void addDecision(String description, String context) {
            decisions.add(new DecisionData(description, context));
        }

        public void addChange(String type, String description, String context) {
            changes.add(new ChangeData(type, description, context));
        }

        public List<ActionItemData> getActionItems() { return actionItems; }
        public List<DecisionData> getDecisions() { return decisions; }
        public List<ChangeData> getChanges() { return changes; }

        public static class ActionItemData {
            public String description;
            public String sourceContext;
            public String priority;

            public ActionItemData(String description, String sourceContext, String priority) {
                this.description = description;
                this.sourceContext = sourceContext;
                this.priority = priority;
            }
        }

        public static class DecisionData {
            public String description;
            public String sourceContext;

            public DecisionData(String description, String sourceContext) {
                this.description = description;
                this.sourceContext = sourceContext;
            }
        }

        public static class ChangeData {
            public String type;
            public String description;
            public String context;

            public ChangeData(String type, String description, String context) {
                this.type = type;
                this.description = description;
                this.context = context;
            }
        }
    }
}
