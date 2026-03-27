package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.demo.repository.OptionRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserProgressRepository;

import lombok.RequiredArgsConstructor;

import com.example.demo.dto.response.OptionDto;
import com.example.demo.dto.response.QuestionDto;
import com.example.demo.dto.response.QuestionResponse;
import com.example.demo.dto.response.UserStatsDto;
import com.example.demo.entity.Question;
import com.example.demo.entity.UserProgress;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final UserProgressRepository userProgressRepository;

    // 1. ページングされた問題取得 (/api/questions)
    public QuestionResponse getQuestions(String language, int page, int size) {
        // 1. DBからEntityのページを取得
        Page<Question> questionPage = questionRepository.findByLanguage(language, PageRequest.of(page, size));

        // 2. Entityのリストを DTO(QuestionDto) のリストに変換
        List<QuestionDto> dtos = questionPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 3. 最終的なレスポンス型に詰める
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtos);
        response.setHasMore(questionPage.hasNext());
        // 次のページ番号をカーソルとして渡す（設計書のnextCursorに対応）
        response.setNextCursor(questionPage.hasNext() ? page + 1 : null);
        System.out.println("レスポンス" + response);

        return response;
    }

    // EntityからDTOへの詰め替え（仕組みの共通化）
    private QuestionDto convertToDto(Question entity) {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(entity.getQuestionId());
        dto.setQuestionText(entity.getQuestionText());
        dto.setCodeSnippet(entity.getCodeSnippet());
        dto.setTitle(entity.getTitle());
        dto.setLanguage(entity.getLanguage());
        dto.setExplanation(entity.getExplanation()); // これで解説が届くようになる
        dto.setDifficultyLevel(entity.getDifficultyLevel());

        // OptionsもDTOに変換
        List<OptionDto> optionDtos = entity.getOptions().stream()
                .map(option -> {
                    OptionDto oDto = new OptionDto();
                    oDto.setOptionId(option.getOptionId()); // Stringに変換
                    oDto.setOptionText(option.getOptionText());
                    oDto.setOptionOrder(option.getOptionOrder());
                    oDto.setCorrect(option.isCorrect());
                    return oDto;
                }).collect(Collectors.toList());

        dto.setOptions(optionDtos);
        return dto;
    }

    // 2. 解答履歴取得 (/api/answers/history/{userId})
    public List<UserProgress> getHistory(UUID userId) {
        return userProgressRepository.findByUserIdOrderByAnsweredAtDesc(userId);
    }

    // 3. 間違えた問題だけ取得 (/api/questions/mistakes)
    public List<Question> getMistakenQuestions(UUID userId) {
        List<UUID> ids = userProgressRepository.findDistinctQuestionIdsByUserIdAndIsCorrectFalse(userId);
        return questionRepository.findAllById(ids);
    }

    // 4. 途中から再開 (/api/questions/resume)
    // 最後に解いた問題の「次」から取得するロジックの雛形
    public QuestionResponse resumeQuestions(UUID userId, int limit) {
        // 1. ユーザーの最新の解答履歴を取得
        Page<Question> page = userProgressRepository.findFirstByUserIdOrderByAnsweredAtDesc(userId)
                .map(lastLog -> {
                    // 2. その問題の seq 番号を特定する（Historyにseqを持たせていない場合はQuestionを引く）
                    Integer lastSeq = questionRepository.findById(lastLog.getQuestionId())
                            .map(Question::getSeq)
                            .orElse(0);

                    // 3. 最後に解いた seq より大きい問題を、seq順に limit件 取得する
                    return questionRepository.findBySeqGreaterThanOrderBySeqAsc(
                            lastSeq,
                            PageRequest.of(0, limit));
                })
                // 履歴がない場合は最初（seq=0より大きいもの）から開始
                .orElse(questionRepository.findBySeqGreaterThanOrderBySeqAsc(0, PageRequest.of(0, limit)));
        List<QuestionDto> dtoList = page.getContent().stream().map(q -> {
            QuestionDto qDto = new QuestionDto();
            qDto.setQuestionId(q.getQuestionId());
            qDto.setQuestionText(q.getQuestionText());
            qDto.setCodeSnippet(q.getCodeSnippet());
            qDto.setTitle(q.getTitle());
            qDto.setExplanation(q.getExplanation());
            qDto.setLanguage(q.getLanguage());
            qDto.setGenre(q.getGenre());
            qDto.setDifficultyLevel(q.getDifficultyLevel());

            List<OptionDto> oDtos = q.getOptions().stream().map(o -> {
                OptionDto oDto = new OptionDto();
                oDto.setOptionId(o.getOptionId());
                oDto.setOptionText(o.getOptionText());
                oDto.setOptionOrder(o.getOptionOrder());
                oDto.setCorrect(o.isCorrect());
                return oDto;
            }).toList();

            qDto.setOptions(oDtos);

            return qDto;
        }).toList();

        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtoList);
        response.setHasMore(page.hasNext());

        if (page.hasContent()) {
            response.setNextCursor(page.getContent().get(page.getContent().size() - 1).getSeq());
        }
        return response;
    }

    // 5. 統計データ取得（/api/questions/stats/{userId}）
    public UserStatsDto getuserStats(UUID userId) {
        long total = userProgressRepository.countByUserId(userId);
        if (total == 0)
            return new UserStatsDto(0, 0, 0, 0);
        long correct = userProgressRepository.countByUserIdAndIsCorrectTrue(userId);
        int rate = (int) (correct * 100 / total);
        long mastered = userProgressRepository.countDistinctQuestionIdByUserIdAndIsCorrectTrue(userId);
        return new UserStatsDto(total, correct, rate, mastered);
    }
}