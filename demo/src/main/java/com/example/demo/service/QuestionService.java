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
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

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
    public QuestionResponse getIncorrectQuestions(UUID userId, String genre, String language, int page, int size) {
        // 1. ジャンルが空文字や特定のキーワード（"all"など）ならnullに変換して全件対象にする
        String genreParam = (genre == null || genre.isEmpty() || "all".equalsIgnoreCase(genre)) ? null : genre;

        // 2. Repositoryを呼び出し
        Page<Question> questionPage = userProgressRepository.findIncorrectQuestionsByUserId(
                userId, genreParam, language, PageRequest.of(page, size));

        // 3. 既存の convertToDto を再利用して変換
        List<QuestionDto> dtos = questionPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 4. 普通の問題取得と同じレスポンス形式で返却
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtos);
        response.setHasMore(questionPage.hasNext());
        response.setNextCursor(questionPage.hasNext() ? page + 1 : null);

        return response;
    }

    // 4. 途中から再開 (/api/questions/resume)
    public QuestionResponse resumeQuestions(UUID userId, String genre, String language, int page, int limit) {
        // 1. ジャンルが "all" や空文字の場合は null として扱う（仕組み化の共通処理）
        String genreParam = (genre == null || genre.isEmpty() || "all".equalsIgnoreCase(genre)) ? null : genre;

        // 2. 指定された「言語」かつ「ジャンル」で、ユーザーの最新の解答履歴を1件取得
        // ※リポジトリにこの条件（genre含む）のメソッドを追加する必要があります
        Optional<UserProgress> lastLogOpt = userProgressRepository
                .findFirstByUserIdAndQuestionLanguageAndQuestionGenreOrderByAnsweredAtDesc(userId, language,
                        genreParam);

        Page<Question> questionPage;
        // フロントから page が届く設計ならそれを使いますが、通常 Resume は 0ページ目から「続き」を取得します
        PageRequest pageRequest = PageRequest.of(page, limit);

        if (lastLogOpt.isPresent()) {
            // 3. 履歴がある場合：その問題の seq より大きい問題を、条件を絞って取得
            UserProgress lastLog = lastLogOpt.get();
            Integer lastSeq = lastLog.getQuestion().getSeq();

            questionPage = questionRepository.findByLanguageAndGenreAndSeqGreaterThanOrderBySeqAsc(
                    language, genreParam, lastSeq, pageRequest);
        } else {
            // 4. 履歴がない場合：その条件の最初（seq=0より大きいもの）から開始
            questionPage = questionRepository.findByLanguageAndGenreAndSeqGreaterThanOrderBySeqAsc(
                    language, genreParam, 0, pageRequest);
        }

        // 5. 既存の convertToDto を再利用して DTO リストを作成
        List<QuestionDto> dtoList = questionPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // 6. レスポンスの構築
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(dtoList);
        response.setHasMore(questionPage.hasNext());

        if (questionPage.hasContent()) {
            // 次回の続き番号（seq）をセット
            response.setNextCursor(questionPage.getContent().get(questionPage.getContent().size() - 1).getSeq());
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