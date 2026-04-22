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
    public QuestionResponse getQuestions(String language, int page, int size) {
        // 1. DBからQuestion Entityのページを取得 (クエリ1回目)
        // リポジトリ側で @EntityGraph を外しているため、ここでは options は Lazy 状態
        Page<Question> questionPage = questionRepository.findByLanguage(language, PageRequest.of(page, size));
        List<Question> questions = questionPage.getContent();

        if (questions.isEmpty()) {
            return new QuestionResponse(new ArrayList<>(), null, false);
        }

        // 2. 取得した問題のIDリストを作成
        List<UUID> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        // 3. 該当する選択肢をすべて一括取得 (クエリ2回目)
        // リポジトリの findByQuestionIn(questionIds) または相当するメソッドを使用
        List<Option> allOptions = optionRepository.findByQuestionIdIn(questionIds);

        // 4. 親の QuestionID をキーにした Map に分類
        Map<UUID, List<Option>> optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(option -> option.getQuestionId()));

        // 5. Entityのリストを DTO に変換（Mapから選択肢を渡す）
        List<QuestionDto> dtos = questions.stream()
                .map(q -> convertToDtoWithSubData(q, optionMap.getOrDefault(q.getQuestionId(), List.of())))
                .collect(Collectors.toList());

        // 6. レスポンス構築
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtos);
        response.setHasMore(questionPage.hasNext());
        response.setNextCursor(questionPage.hasNext() ? page + 1 : null);

        return response;
    }

    // 修正版：外部から渡された options を使って DTO を組み立てる
    private QuestionDto convertToDtoWithSubData(Question entity, List<Option> options) {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(entity.getQuestionId());
        dto.setQuestionText(entity.getQuestionText());
        dto.setCodeSnippet(entity.getCodeSnippet());
        dto.setTitle(entity.getTitle());
        dto.setLanguage(entity.getLanguage());
        dto.setExplanation(entity.getExplanation());
        dto.setDifficultyLevel(entity.getDifficultyLevel());

        // 引数で受け取った options を DTO に変換
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
    public QuestionResponse getIncorrectQuestions(UUID userId, String genre, String language, int page, int size) {

        String genreParam = (genre == null || genre.isEmpty() || "all".equalsIgnoreCase(genre)) ? null : genre;

        // ① Question取得（クエリ1回）
        Page<Question> questionPage = userProgressRepository.findIncorrectQuestionsByUserId(
                userId, genreParam, language, PageRequest.of(page, size));

        List<Question> questions = questionPage.getContent();

        if (questions.isEmpty()) {
            return new QuestionResponse(new ArrayList<>(), null, false);
        }

        // ② QuestionIDリスト作成
        List<UUID> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        // ③ Option一括取得（クエリ2回目）
        List<Option> allOptions = optionRepository.findByQuestionIdIn(questionIds);

        // ④ Map化
        Map<UUID, List<Option>> optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getQuestionId()));

        // ⑤ DTO変換
        List<QuestionDto> dtos = questions.stream()
                .map(q -> convertToDtoWithSubData(
                        q,
                        optionMap.getOrDefault(q.getQuestionId(), new ArrayList<>())))
                .collect(Collectors.toList());

        // ⑥ レスポンス
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtos);
        response.setHasMore(questionPage.hasNext());
        response.setNextCursor(questionPage.hasNext() ? page + 1 : null);

        return response;
    }

    // 4. 途中から再開 (/api/questions/resume)
    public QuestionResponse resumeQuestions(UUID userId, String genre, String language, int page, int limit) {

        String genreParam = (genre == null || genre.isEmpty() || "all".equalsIgnoreCase(genre)) ? null : genre;

        Optional<UserProgress> lastLogOpt = userProgressRepository
                .findFirstByUserIdAndQuestionLanguageAndQuestionGenreOrderByAnsweredAtDesc(
                        userId, language, genreParam);

        Page<Question> questionPage;
        PageRequest pageRequest = PageRequest.of(page, limit);

        if (lastLogOpt.isPresent()) {
            Integer lastSeq = lastLogOpt.get().getQuestion().getSeq();

            questionPage = questionRepository.findByLanguageAndGenreAndSeqGreaterThanOrderBySeqAsc(
                    language, genreParam, lastSeq, pageRequest);
        } else {
            questionPage = questionRepository.findByLanguageAndGenreAndSeqGreaterThanOrderBySeqAsc(
                    language, genreParam, 0, pageRequest);
        }

        List<Question> questions = questionPage.getContent();

        // ★ 空チェック（無駄クエリ防止）
        if (questions.isEmpty()) {
            return new QuestionResponse(new ArrayList<>(), null, false);
        }

        // ① QuestionID収集
        List<UUID> questionIds = questions.stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList());

        // ② Option一括取得（これがN+1対策の本体）
        List<Option> allOptions = optionRepository.findByQuestionIdIn(questionIds);

        // ③ Map化
        Map<UUID, List<Option>> optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getQuestionId()));

        // ④ DTO変換
        List<QuestionDto> dtoList = questions.stream()
                .map(q -> convertToDtoWithSubData(
                        q,
                        optionMap.getOrDefault(q.getQuestionId(), new ArrayList<>())))
                .collect(Collectors.toList());

        // ⑤ レスポンス構築
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtoList);
        response.setHasMore(questionPage.hasNext());

        if (questionPage.hasContent()) {
            response.setNextCursor(
                    questions.get(questions.size() - 1).getSeq());
        }

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