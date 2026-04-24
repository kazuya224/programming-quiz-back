package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.demo.repository.OptionRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserProgressRepository;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final UserProgressRepository userProgressRepository;
    private final OptionRepository optionRepository;
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    // 1. ページングされた問題取得 (/api/questions)
    public QuestionResponse getQuestions(String language, Long cursor, int size) {
        // 1. Repository呼び出し：cursorの有無でクエリを分岐 (指摘②)
        List<Question> questions;
        // PageRequest.of(0, size) を使うことで LIMIT size だけを発行し、OFFSETを避ける
        if (cursor == null) {
            questions = questionRepository.findByLanguageOrderBySeqAsc(
                    language, PageRequest.of(0, size));
        } else {
            questions = questionRepository.findByLanguageAndSeqGreaterThanOrderBySeqAsc(
                    language, cursor, PageRequest.of(0, size));
        }

        // 取得結果が空の場合の早期リターン
        if (questions.isEmpty()) {
            return new QuestionResponse(new ArrayList<>(), null, false);
        }

        // 2. 取得した問題のIDリストを作成（N+1回避の準備：ここは継続してGood!）
        List<UUID> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        // 3. 該当する選択肢を一括取得 (クエリ2回目)
        List<Option> allOptions = optionRepository.findByQuestionIdIn(questionIds);

        // 4. 親IDをキーにしたMapに分類
        Map<UUID, List<Option>> optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(Option::getQuestionId));

        // 5. Entityリストを DTO に変換
        List<QuestionDto> dtos = questions.stream()
                .map(q -> convertToDtoWithSubData(q, optionMap.getOrDefault(q.getQuestionId(), List.of())))
                .collect(Collectors.toList());

        // 6. hasMoreの判定：取得件数がリクエストサイズと同じなら「次あり」とみなす (指摘③)
        boolean hasMore = questions.size() == size;

        // 7. nextCursorの取得 (指摘④)
        Long nextCursor = null;
        if (!questions.isEmpty()) {
            nextCursor = questions.get(questions.size() - 1).getSeq();
        }

        // 8. レスポンス構築 (指摘⑤)
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtos);
        response.setHasMore(hasMore);
        response.setNextCursor(nextCursor);

        return response;
    }

    // DTO変換メソッド（ロジックは変更なし：きれいな設計を維持）
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

    // 2. 解答履歴取得 (/api/answers/history/{userId})
    public List<UserProgress> getHistory(UUID userId) {
        return userProgressRepository.findByUserIdOrderByAnsweredAtDesc(userId);
    }

    // 3. 間違えた問題だけ取得 (/api/questions/mistakes)
    public QuestionResponse getIncorrectQuestions(
            UUID userId,
            String genre,
            String language,
            Long cursor,
            int size) {

        String genreParam = (genre == null || genre.isEmpty() || "all".equalsIgnoreCase(genre))
                ? null
                : genre;

        int fetchSize = size + 1;

        // ① 間違えた問題をcursorで取得
        List<Question> questions = userProgressRepository
                .findIncorrectQuestionsWithCursor(
                        userId, genreParam, language, cursor, PageRequest.of(0, fetchSize));

        if (questions.isEmpty()) {
            return new QuestionResponse(new ArrayList<>(), null, false);
        }

        // ② hasMore判定
        boolean hasMore = questions.size() > size;

        if (hasMore) {
            questions = questions.subList(0, size);
        }

        // ③ QuestionID収集
        List<UUID> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        // ④ Option一括取得
        List<Option> allOptions = optionRepository.findByQuestionIdIn(questionIds);

        // ⑤ Map化
        Map<UUID, List<Option>> optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getQuestionId()));

        // ⑥ DTO変換
        List<QuestionDto> dtos = questions.stream()
                .map(q -> convertToDtoWithSubData(
                        q,
                        optionMap.getOrDefault(q.getQuestionId(), List.of())))
                .collect(Collectors.toList());

        // ⑦ nextCursor
        Long nextCursor = questions.get(questions.size() - 1).getSeq();

        // ⑧ レスポンス
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtos);
        response.setHasMore(hasMore);
        response.setNextCursor(nextCursor);

        return response;
    }

    // 4. 途中から再開 (/api/questions/resume)
    public QuestionResponse resumeQuestions(
            UUID userId,
            String genre,
            String language,
            Long cursor,
            int size) {

        String genreParam = (genre == null || genre.isEmpty() || "all".equalsIgnoreCase(genre))
                ? null
                : genre;

        int fetchSize = size + 1;

        // ① cursorがない場合 → DBから最後の位置取得
        if (cursor == null) {
            Optional<UserProgress> lastLogOpt = userProgressRepository
                    .findFirstByUserIdAndQuestionLanguageAndQuestionGenreOrderByAnsweredAtDesc(
                            userId, language, genreParam);

            cursor = lastLogOpt
                    .map(log -> log.getQuestion().getSeq())
                    .orElse(0L);
        }

        // ② cursorベースで取得
        List<Question> questions = questionRepository
                .findQuestionsWithCursor(
                        language, genreParam, cursor, PageRequest.of(0, fetchSize));

        if (questions.isEmpty()) {
            return new QuestionResponse(new ArrayList<>(), null, false);
        }

        // ③ hasMore判定
        boolean hasMore = questions.size() > size;

        if (hasMore) {
            questions = questions.subList(0, size);
        }

        // ④ QuestionID収集
        List<UUID> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        // ⑤ Option一括取得
        List<Option> allOptions = optionRepository.findByQuestionIdIn(questionIds);

        // ⑥ Map化
        Map<UUID, List<Option>> optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getQuestionId()));

        // ⑦ DTO変換
        List<QuestionDto> dtoList = questions.stream()
                .map(q -> convertToDtoWithSubData(
                        q,
                        optionMap.getOrDefault(q.getQuestionId(), List.of())))
                .collect(Collectors.toList());

        // ⑧ nextCursor
        Long nextCursor = questions.get(questions.size() - 1).getSeq();

        // ⑨ レスポンス
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtoList);
        response.setHasMore(hasMore);
        response.setNextCursor(nextCursor);

        return response;
    }

    // 5. 統計データ取得（/api/questions/stats/{userId}）
    public UserStatsDto getUserStats(UUID userId) {

        // 今日
        LocalDate today = LocalDate.now(JST);
        Instant startOfToday = today.atStartOfDay(JST).toInstant();
        Instant endOfToday = today.plusDays(1).atStartOfDay(JST).toInstant();
        LocalDateTime start = LocalDateTime.ofInstant(startOfToday, ZoneOffset.UTC);
        LocalDateTime end = LocalDateTime.ofInstant(endOfToday, ZoneOffset.UTC);
        int todayCount = (int) userProgressRepository.countByPeriod(userId, start, end);

        // 今週
        LocalDate startOfWeekDate = today.with(DayOfWeek.MONDAY);
        Instant startOfWeekInstant = startOfWeekDate.atStartOfDay(JST).toInstant();
        Instant endOfThisWeekInstant = today.plusDays(1).atStartOfDay(JST).toInstant();
        LocalDateTime endOfThisWeek = LocalDateTime.ofInstant(endOfThisWeekInstant, ZoneOffset.UTC);

        // 前週
        Instant startOfLastWeekInstant = startOfWeekDate.minusWeeks(1).atStartOfDay(JST).toInstant();
        Instant endOfLastWeekInstant = startOfWeekInstant;

        // LocalDateTimeに変換（DBと合わせる為）
        LocalDateTime startOfWeek = LocalDateTime.ofInstant(startOfWeekInstant, ZoneOffset.UTC);
        LocalDateTime startOfLastWeek = LocalDateTime.ofInstant(startOfLastWeekInstant, ZoneOffset.UTC);
        LocalDateTime endOfLastWeek = LocalDateTime.ofInstant(endOfLastWeekInstant, ZoneOffset.UTC);

        // 今週データ
        int thisTotal = (int) userProgressRepository.countByPeriod(userId, startOfWeek, endOfThisWeek);
        int thisCorrect = (int) userProgressRepository.countCorrectByPeriod(userId, startOfWeek, endOfThisWeek);
        int thisAccuracy = calcRate(thisCorrect, thisTotal);

        // 前週データ
        int lastTotal = (int) userProgressRepository.countByPeriod(userId, startOfLastWeek, endOfLastWeek);
        int lastCorrect = (int) userProgressRepository.countCorrectByPeriod(userId, startOfLastWeek, endOfLastWeek);
        int lastAccuracy = calcRate(lastCorrect, lastTotal);

        // 差分
        int totalDiff = thisTotal - lastTotal;
        int accuracyDiff = thisAccuracy - lastAccuracy;

        // streak(JSTベース)
        int streak = calculateStreak(userId, today);

        // 再開
        long totalCount = userProgressRepository.countByUserId(userId);
        boolean hasResume = totalCount > 0;

        return new UserStatsDto(
                todayCount,
                streak,
                new WeekStats(thisTotal, thisCorrect, thisAccuracy),
                new WeekStats(lastTotal, lastCorrect, lastAccuracy),
                new DiffStats(totalDiff, accuracyDiff),
                hasResume);
    }

    // 正解率
    private int calcRate(int correct, int total) {
        if (total == 0)
            return 0;
        return (int) ((double) correct / total * 100);
    }

    // streak計算
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

    // ダッシュボードに言語、ジャンル表示
    public Map<String, List<GenreDto>> getGenreStats(UUID userId) {
        // ① ユーザーの進捗を取得（すでに解いた問題の統計）
        List<GenreStatsProjection> userResults = userProgressRepository.getGenreStats(userId);
        Map<String, GenreStatsProjection> userResultMap = new HashMap<>();
        for (GenreStatsProjection r : userResults) {
            userResultMap.put(r.getLanguage() + "-" + r.getGenre(), r);
        }

        // ② DBにある「全問題数」を取得（解いていない問題も含むカウント）
        List<GenreDto> allTotalCounts = questionRepository.findAllTotalCounts();
        // 言語を特定するために、Questionエンティティにlanguageがある前提でMap化
        // ※クエリに language を含めるように修正が必要かもしれません。
        // ここでは一旦、ジャンル名をキーにします。
        Map<String, Long> totalMap = allTotalCounts.stream()
                .collect(Collectors.toMap(
                        dto -> dto.language() + "-" + dto.genre(),
                        GenreDto::totalCount));

        Map<String, List<GenreDto>> finalMap = new HashMap<>();

        getGenreMasterFromDb().forEach((language, genres) -> {
            List<GenreDto> list = new ArrayList<>();
            for (String genre : genres) {
                String key = language + "-" + genre;

                // ユーザーの統計（解いた数、正解数）
                GenreStatsProjection userStat = userResultMap.get(key);
                long answeredCount = userStat != null ? userStat.getTotalCount() : 0;
                long correctCount = userStat != null ? userStat.getCorrectCount() : 0;

                // DB上の「全問題数」
                // totalMapから取得するか、もしなければ0にする
                long totalInDb = totalMap.getOrDefault(language + "-" + genre, 0L);

                // 正答率の計算
                Double accuracy = answeredCount > 0 ? (double) correctCount / answeredCount : null;

                // recordのコンパクトな記述でDTOを作成
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
                                        Collectors.toSet(), // 重複排除
                                        ArrayList::new // Listに戻す
                                ))));
    }
}