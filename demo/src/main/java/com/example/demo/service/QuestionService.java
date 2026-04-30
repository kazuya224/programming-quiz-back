package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.demo.repository.LearningSessionRepository;
import com.example.demo.repository.OptionRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserProgressRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.projection.GenreMasterProjection;
import com.example.demo.repository.projection.GenreStatsProjection;

import lombok.RequiredArgsConstructor;

import com.example.demo.dto.response.DiffStats;
import com.example.demo.dto.response.GenreDto;
import com.example.demo.dto.response.OptionDto;
import com.example.demo.dto.response.QuestionDto;
import com.example.demo.dto.response.QuestionResponse;
import com.example.demo.dto.response.UserStatsDto;
import com.example.demo.dto.response.WeekStats;
import com.example.demo.entity.LearningSession;
import com.example.demo.entity.Option;
import com.example.demo.entity.Question;
import com.example.demo.entity.UserProgress;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final UserProgressRepository userProgressRepository;
    private final OptionRepository optionRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final SubscriptionService subscriptionService;
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    // ----------------------------------------------------------------
    // 共通ヘルパー
    // ----------------------------------------------------------------

    /** 無料ユーザーは difficulty=0、有料ユーザーはフィルターなし(null) */
    private Integer getDifficultyFilter(UUID userId) {
        return subscriptionService.isPremium(userId) ? null : 0;
    }

    /** genre パラメータの正規化（null / 空文字 / "all" → null） */
    private String normalizeGenre(String genre) {
        return (genre == null || genre.isEmpty() || "all".equalsIgnoreCase(genre)) ? null : genre;
    }

    /**
     * 取得済み Question リストからレスポンスを組み立てる共通処理。
     * questions は「表示する件数ぴったり」に trim 済みであること。
     */
    private QuestionResponse buildResponse(List<Question> questions, boolean hasMore) {
        List<UUID> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        List<Option> allOptions = optionRepository.findByQuestionIdIn(questionIds);

        // Option::getQuestionId を直接使う（getQuestion() 経由のN+1を避ける）
        Map<UUID, List<Option>> optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(Option::getQuestionId));

        List<QuestionDto> dtos = questions.stream()
                .map(q -> convertToDtoWithSubData(
                        q, optionMap.getOrDefault(q.getQuestionId(), List.of())))
                .collect(Collectors.toList());

        Long nextCursor = questions.get(questions.size() - 1).getSeq();

        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtos);
        response.setHasMore(hasMore);
        response.setNextCursor(nextCursor);
        return response;
    }

    // ----------------------------------------------------------------
    // 1. ページングされた問題取得 (/api/questions)
    // ----------------------------------------------------------------
    public QuestionResponse getQuestions(UUID userId, String language, Long cursor, int size) {
        Integer difficulty = getDifficultyFilter(userId);
        int fetchSize = size + 1; // hasMore判定のために1件多く取得

        // cursor の有無に関わらず findQuestionsWithCursorAndDifficulty で統一
        // （cursor=null のとき JPQL 側で `:cursor IS NULL` を評価してくれる）
        List<Question> questions = questionRepository.findQuestionsWithCursorAndDifficulty(
                language, null, cursor, difficulty, PageRequest.of(0, fetchSize));

        if (questions.isEmpty()) {
            return new QuestionResponse(new ArrayList<>(), null, false);
        }

        boolean hasMore = questions.size() > size;
        if (hasMore) {
            questions = questions.subList(0, size);
        }

        return buildResponse(questions, hasMore);
    }

    // ----------------------------------------------------------------
    // 2. 間違えた問題だけ取得 (/api/questions/mistakes)
    // ----------------------------------------------------------------
    public QuestionResponse getIncorrectQuestions(
            UUID userId, String genre, String language, Long cursor, int size) {

        Integer difficulty = getDifficultyFilter(userId); // ← 追加
        String genreParam = normalizeGenre(genre);
        int fetchSize = size + 1;

        List<Question> questions = userProgressRepository.findIncorrectQuestionsWithCursor(
                userId, genreParam, language, cursor, difficulty, // ← difficulty追加
                PageRequest.of(0, fetchSize));

        if (questions.isEmpty()) {
            return new QuestionResponse(new ArrayList<>(), null, false);
        }

        boolean hasMore = questions.size() > size;
        if (hasMore) {
            questions = questions.subList(0, size);
        }

        return buildResponse(questions, hasMore);
    }

    // ----------------------------------------------------------------
    // 3. 途中から再開 (/api/questions/resume)
    // ----------------------------------------------------------------
    public QuestionResponse resumeQuestions(
            UUID userId, String genre, String language, Long cursor, int size) {

        Integer difficulty = getDifficultyFilter(userId); // ← 追加
        String genreParam = normalizeGenre(genre);
        int fetchSize = size + 1;

        // cursor未指定 → LearningSession から最後の位置を復元
        if (cursor == null) {
            Optional<LearningSession> sessionOpt = learningSessionRepository.findByUserIdAndLanguage(userId, language);
            if (sessionOpt.isPresent()) {
                cursor = questionRepository
                        .findSeqByQuestionId(sessionOpt.get().getCurrentQuestionId())
                        .orElse(0L);
            } else {
                cursor = 0L;
            }
        }

        // findQuestionsWithCursorAndDifficulty を流用（findQuestionsWithCursor は使わない）
        List<Question> questions = questionRepository.findQuestionsWithCursorAndDifficulty(
                language, genreParam, cursor, difficulty, // ← difficulty追加
                PageRequest.of(0, fetchSize));

        if (questions.isEmpty()) {
            return new QuestionResponse(new ArrayList<>(), null, false);
        }

        boolean hasMore = questions.size() > size;
        if (hasMore) {
            questions = questions.subList(0, size);
        }

        return buildResponse(questions, hasMore);
    }

    // ----------------------------------------------------------------
    // DTO変換（変更なし）
    // ----------------------------------------------------------------
    private QuestionDto convertToDtoWithSubData(Question entity, List<Option> options) {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(entity.getQuestionId());
        dto.setQuestionText(entity.getQuestionText());
        dto.setCodeSnippet(entity.getCodeSnippet());
        dto.setTitle(entity.getTitle());
        dto.setLanguage(entity.getLanguage());
        dto.setExplanation(entity.getExplanation());
        dto.setDifficultyLevel(entity.getDifficultyLevel());

        List<OptionDto> optionDtos = options.stream()
                .map(option -> {
                    OptionDto oDto = new OptionDto();
                    oDto.setOptionId(option.getOptionId());
                    oDto.setOptionText(option.getOptionText());
                    oDto.setOptionOrder(option.getOptionOrder());
                    oDto.setCorrect(option.isCorrect());
                    return oDto;
                }).collect(Collectors.toList());

        dto.setOptions(optionDtos);
        return dto;
    }

    // ----------------------------------------------------------------
    // 4. 統計データ取得（/api/questions/stats/{userId}）以降は変更なし
    // ----------------------------------------------------------------
    public UserStatsDto getUserStats(UUID userId) {
        LocalDate today = LocalDate.now(JST);
        Instant startOfToday = today.atStartOfDay(JST).toInstant();
        Instant endOfToday = today.plusDays(1).atStartOfDay(JST).toInstant();
        LocalDateTime start = LocalDateTime.ofInstant(startOfToday, ZoneOffset.UTC);
        LocalDateTime end = LocalDateTime.ofInstant(endOfToday, ZoneOffset.UTC);
        int todayCount = (int) userProgressRepository.countByPeriod(userId, start, end);

        LocalDate startOfWeekDate = today.with(DayOfWeek.MONDAY);
        Instant startOfWeekInstant = startOfWeekDate.atStartOfDay(JST).toInstant();
        Instant endOfThisWeekInstant = today.plusDays(1).atStartOfDay(JST).toInstant();
        LocalDateTime endOfThisWeek = LocalDateTime.ofInstant(endOfThisWeekInstant, ZoneOffset.UTC);

        Instant startOfLastWeekInstant = startOfWeekDate.minusWeeks(1).atStartOfDay(JST).toInstant();
        Instant endOfLastWeekInstant = startOfWeekInstant;

        LocalDateTime startOfWeek = LocalDateTime.ofInstant(startOfWeekInstant, ZoneOffset.UTC);
        LocalDateTime startOfLastWeek = LocalDateTime.ofInstant(startOfLastWeekInstant, ZoneOffset.UTC);
        LocalDateTime endOfLastWeek = LocalDateTime.ofInstant(endOfLastWeekInstant, ZoneOffset.UTC);

        int thisTotal = (int) userProgressRepository.countByPeriod(userId, startOfWeek, endOfThisWeek);
        int thisCorrect = (int) userProgressRepository.countCorrectByPeriod(userId, startOfWeek, endOfThisWeek);
        int thisAccuracy = calcRate(thisCorrect, thisTotal);

        int lastTotal = (int) userProgressRepository.countByPeriod(userId, startOfLastWeek, endOfLastWeek);
        int lastCorrect = (int) userProgressRepository.countCorrectByPeriod(userId, startOfLastWeek, endOfLastWeek);
        int lastAccuracy = calcRate(lastCorrect, lastTotal);

        int totalDiff = thisTotal - lastTotal;
        int accuracyDiff = thisAccuracy - lastAccuracy;

        int streak = calculateStreak(userId, today);
        long totalCount = userProgressRepository.countByUserId(userId);
        boolean hasResume = totalCount > 0;

        return new UserStatsDto(
                todayCount, streak,
                new WeekStats(thisTotal, thisCorrect, thisAccuracy),
                new WeekStats(lastTotal, lastCorrect, lastAccuracy),
                new DiffStats(totalDiff, accuracyDiff),
                hasResume);
    }

    private int calcRate(int correct, int total) {
        if (total == 0)
            return 0;
        return (int) ((double) correct / total * 100);
    }

    private int calculateStreak(UUID userId, LocalDate today) {
        List<LocalDate> days = userProgressRepository.findAnsweredDatesJST(userId);
        int streak = 1;
        for (LocalDate day : days) {
            if (day.equals(today.minusDays(streak))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    public Map<String, List<GenreDto>> getGenreStats(UUID userId) {
        boolean isPremium = subscriptionService.isPremium(userId);
        Integer difficulty = isPremium ? null : 0;

        List<GenreStatsProjection> userResults = userProgressRepository.getGenreStats(userId, difficulty);
        Map<String, GenreStatsProjection> userResultMap = new HashMap<>();
        for (GenreStatsProjection r : userResults) {
            userResultMap.put(r.getLanguage() + "-" + r.getGenre(), r);
        }

        List<GenreDto> allTotalCounts = questionRepository.findAllTotalCounts(difficulty);
        Map<String, Long> totalMap = allTotalCounts.stream()
                .collect(Collectors.toMap(
                        dto -> dto.language() + "-" + dto.genre(),
                        GenreDto::totalCount));

        Map<String, List<GenreDto>> finalMap = new HashMap<>();
        getGenreMasterFromDb().forEach((language, genres) -> {
            List<GenreDto> list = new ArrayList<>();
            for (String genre : genres) {
                String key = language + "-" + genre;
                GenreStatsProjection userStat = userResultMap.get(key);
                long answeredCount = userStat != null ? userStat.getTotalCount() : 0;
                long correctCount = userStat != null ? userStat.getCorrectCount() : 0;
                long totalInDb = totalMap.getOrDefault(key, 0L);
                Double accuracy = answeredCount > 0 ? (double) correctCount / answeredCount : null;
                list.add(new GenreDto(genre, language, accuracy, totalInDb, correctCount));
            }
            finalMap.put(language, list);
        });
        return finalMap;
    }

    private Map<String, List<String>> getGenreMasterFromDb() {
        List<GenreMasterProjection> results = questionRepository.findAllLanguagesAndGenres();
        return results.stream()
                .filter(x -> x.getLanguage() != null && x.getGenre() != null)
                .collect(Collectors.groupingBy(
                        GenreMasterProjection::getLanguage,
                        Collectors.mapping(
                                GenreMasterProjection::getGenre,
                                Collectors.collectingAndThen(
                                        Collectors.toSet(),
                                        ArrayList::new))));
    }
}