package com.sunsetbeach.bot;

import com.sunsetbeach.entity.BotConversationEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.repository.BotConversationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The multi-step dialogue of a chat, in the database so a restart doesn't lose it; one per chat, fifteen minutes. */
@Component
public class BotConversations {

    static final Duration LIFETIME = Duration.ofMinutes(15);

    private final BotConversationRepository repository;
    private final Clock clock;

    public BotConversations(BotConversationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** The live dialogue of {@code chatId}; an expired one is removed and counts as none. */
    @Transactional
    public Optional<BotConversationEntity> current(String chatId) {
        Optional<BotConversationEntity> found = repository.findById(chatId);
        if (found.isPresent() && found.get().getExpiresAt().isBefore(TimestampFormat.nowUtc(clock))) {
            repository.delete(found.get());
            return Optional.empty();
        }
        return found;
    }

    /** Starts (replacing any earlier) a dialogue in {@code state}. */
    @Transactional
    public BotConversationEntity start(String chatId, String userId, String state, String entryId, String requestType, LocalDate targetDate) {
        BotConversationEntity c = repository.findById(chatId).orElseGet(BotConversationEntity::new);
        c.setChatId(chatId);
        c.setUserId(userId);
        c.setState(state);
        c.setEntryId(entryId);
        c.setRequestType(requestType);
        c.setTargetDate(targetDate);
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        c.setExpiresAt(nowUtc.plus(LIFETIME));
        return repository.saveAndFlush(c);
    }

    @Transactional
    public void clear(String chatId) {
        repository.findById(chatId).ifPresent(repository::delete);
    }
}
