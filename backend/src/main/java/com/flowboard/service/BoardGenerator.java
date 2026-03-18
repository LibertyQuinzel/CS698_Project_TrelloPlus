package com.flowboard.service;

import com.flowboard.dto.AIAnalysisResult;
import com.flowboard.entity.Board;
import com.flowboard.entity.Stage;
import com.flowboard.entity.Card;
import com.flowboard.repository.BoardRepository;
import com.flowboard.repository.StageRepository;
import com.flowboard.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardGenerator {
    private final BoardRepository boardRepository;
    private final StageRepository stageRepository;
    private final CardRepository cardRepository;

    public Board generateEmptyBoard(com.flowboard.entity.Project project) {
        Board board = Board.builder()
            .name(project.getName() + " Board")
            .project(project)
            .stages(new ArrayList<>())
            .build();

        board = boardRepository.save(board);

        String[] titles = {"To Do", "In Progress", "Review", "Done"};
        String[] colors = {"bg-gray-100", "bg-blue-100", "bg-yellow-100", "bg-green-100"};

        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            Stage stage = Stage.builder()
                .title(titles[i])
                .color(colors[i])
                .position(i)
                .board(board)
                .cards(new ArrayList<>())
                .build();
            stage = stageRepository.save(stage);
            stages.add(stage);
        }

        board.setStages(stages);
        return boardRepository.save(board);
    }

    public Board generateBoard(com.flowboard.entity.Project project, AIAnalysisResult analysisResult) {
        // Create board
        Board board = Board.builder()
            .name(project.getName() + " Board")
            .project(project)
            .stages(new ArrayList<>())
            .build();

        board = boardRepository.save(board);

        // Create stages
        List<Stage> stages = new ArrayList<>();
        for (AIAnalysisResult.StageInfo stageInfo : analysisResult.getStages()) {
            Stage stage = Stage.builder()
                .title(stageInfo.title)
                .color(stageInfo.color)
                .position(stageInfo.position)
                .board(board)
                .cards(new ArrayList<>())
                .build();
            stage = stageRepository.save(stage);
            stages.add(stage);
        }

        // Create cards
        List<Card> cards = new ArrayList<>();
        for (AIAnalysisResult.TaskInfo taskInfo : analysisResult.getTasks()) {
            // Find the corresponding stage
            Stage targetStage = stages.stream()
                .filter(s -> s.getTitle().equals(taskInfo.stageTitle))
                .findFirst()
                .orElse(stages.get(0)); // Default to first stage if not found

            Card card = Card.builder()
                .title(taskInfo.title)
                .description(taskInfo.description)
                .priority(Card.Priority.valueOf(taskInfo.priority))
                .stage(targetStage)
                .position(targetStage.getCards().size())
                .build();

            card = cardRepository.save(card);
            targetStage.getCards().add(card);
            cards.add(card);
        }

        board.setStages(stages);
        return boardRepository.save(board);
    }
}
