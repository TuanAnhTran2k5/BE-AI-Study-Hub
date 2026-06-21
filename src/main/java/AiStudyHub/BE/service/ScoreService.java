package AiStudyHub.BE.service;

import AiStudyHub.BE.entity.Document;
import AiStudyHub.BE.entity.ScoreLog;
import AiStudyHub.BE.entity.ScoreType;
import AiStudyHub.BE.entity.User;
import AiStudyHub.BE.repository.ScoreLogRepo;
import AiStudyHub.BE.repository.ScoreTypeRepo;
import AiStudyHub.BE.repository.UserRepo;
import AiStudyHub.BE.service.impl.IScore;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ScoreService implements IScore {

    UserRepo userRepo;
    ScoreTypeRepo scoreTypeRepo;
    ScoreLogRepo scoreLogRepo;

    Map<String, ScoreType> scoreTypeCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int awardScore(User user, Document document, ScoreTypeSpec spec, int scoreChange, String description) {
        ScoreType type = scoreTypeCache.computeIfAbsent(spec.code(), code ->
                scoreTypeRepo.findByTypeCode(code)
                        .orElseGet(() -> scoreTypeRepo.save(ScoreType.builder()
                                .typeCode(code)
                                .typeName(spec.name())
                                .defaultPoint(spec.defaultPoint())
                                .description(spec.description())
                                .build())));               // find-or-create (cached)

        long current = user.getTotalScore() == null ? 0L : user.getTotalScore();
        user.setTotalScore(current + scoreChange);          // accumulate
        userRepo.save(user);

        scoreLogRepo.save(ScoreLog.builder()
                .user(user)
                .document(document)
                .scoreType(type)
                .scoreChange(scoreChange)
                .description(description)
                .build());

        return scoreChange;
    }
}
