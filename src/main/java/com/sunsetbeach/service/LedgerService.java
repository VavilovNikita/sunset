package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.FolioPaymentEntity;
import com.sunsetbeach.entity.JournalEntryEntity;
import com.sunsetbeach.entity.JournalLineEntity;
import com.sunsetbeach.entity.LedgerAccountEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.JournalEntry;
import com.sunsetbeach.model.JournalEntryCreateInput;
import com.sunsetbeach.model.JournalEntryReverseInput;
import com.sunsetbeach.model.JournalLine;
import com.sunsetbeach.model.JournalLineInput;
import com.sunsetbeach.model.JournalSourceType;
import com.sunsetbeach.model.LedgerAccount;
import com.sunsetbeach.model.LedgerAccountCreateInput;
import com.sunsetbeach.model.LedgerAccountType;
import com.sunsetbeach.model.LedgerNormalBalance;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.RevenueCode;
import com.sunsetbeach.model.TrialBalanceReport;
import com.sunsetbeach.model.TrialBalanceRow;
import com.sunsetbeach.repository.JournalEntryRepository;
import com.sunsetbeach.repository.JournalLineRepository;
import com.sunsetbeach.repository.LedgerAccountRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The double-entry ledger (V120): the chart of accounts, journal entries, and the trial balance
 * (Z120). The one place a {@link JournalEntryEntity} is written, and every write goes through
 * {@link #post}, which refuses an entry whose debits and credits differ.
 *
 * <p><b>Automatic postings</b> are called by the service method that performs the business
 * operation, inside that method's own transaction - unlike printing/email/audit, a posting is not
 * best-effort: a close or settlement the ledger silently missed would leave a trial balance that
 * still balances but no longer matches what happened, which is worse than the operation failing.
 * <ul>
 *   <li>{@link BookingService#updateStatus}: a booking becoming {@code PAID} - the only record this
 *       system keeps of the room portion being collected (there is no room payment row, and no
 *       method) - posts Dr Cash/Bank, Cr Room Revenue + VAT Payable at {@code totalPrice}. Leaving
 *       {@code PAID} posts the exact mirror of every such entry not already reversed. A price
 *       change while already {@code PAID} posts nothing, matching {@code PAID}'s existing meaning
 *       everywhere else (outstanding balance reads zero).</li>
 *   <li>{@link OrderService#close}: Dr Cash/Bank - or Guest Ledger for {@code ROOM_CHARGE}, money
 *       charged but not yet collected - Cr F&amp;B/SPA Revenue by each line's menu department (the
 *       same {@link RevenueClassification} Z410 uses) + VAT Payable. A closed order has no refund
 *       or cancel path in this system ({@code cancel} refuses a {@code PAID} order), so nothing
 *       ever reverses one automatically.</li>
 *   <li>{@link BookingService#recordFolioPayment}: Dr Cash/Bank, Cr Guest Ledger. Folio payments
 *       are append-only with no undo, so nothing reverses one automatically either.</li>
 * </ul>
 *
 * <p>VAT is extracted per transaction at the rate stored at the moment of posting, so each entry
 * is a historical record; Z410 instead recomputes whole ranges at today's rate.
 */
@Service
public class LedgerService {

    static final String CASH = "1000";
    static final String GUEST_LEDGER = "1100";
    static final String VAT_PAYABLE = "2100";
    static final String ROOM_REVENUE = "4000";
    static final String FNB_REVENUE = "4100";
    static final String SPA_REVENUE = "4200";

    /** numeric(12,2): anything at or above this doesn't fit a line's column. */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000000");

    private final LedgerAccountRepository accountRepository;
    private final JournalEntryRepository entryRepository;
    private final JournalLineRepository lineRepository;
    private final MenuItemRepository menuItemRepository;
    private final VatSettingsService vatSettingsService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public LedgerService(
            LedgerAccountRepository accountRepository,
            JournalEntryRepository entryRepository,
            JournalLineRepository lineRepository,
            MenuItemRepository menuItemRepository,
            VatSettingsService vatSettingsService,
            AuditLogService auditLogService,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.lineRepository = lineRepository;
        this.menuItemRepository = menuItemRepository;
        this.vatSettingsService = vatSettingsService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /** One line of an entry being posted - exactly one side non-zero. */
    record Line(String accountCode, BigDecimal debit, BigDecimal credit) {
        static Line debit(String accountCode, BigDecimal amount) {
            return new Line(accountCode, amount, BigDecimal.ZERO);
        }

        static Line credit(String accountCode, BigDecimal amount) {
            return new Line(accountCode, BigDecimal.ZERO, amount);
        }

        Line mirrored() {
            return new Line(accountCode, credit, debit);
        }
    }

    // --- Automatic postings -------------------------------------------------------------------

    /** A booking just became {@code PAID} - see the class javadoc. Nothing to post for a zero price. */
    @Transactional
    public void postBookingSettlement(BookingEntity booking) {
        BigDecimal gross = booking.getTotalPrice().setScale(2, RoundingMode.UNNECESSARY);
        if (gross.signum() <= 0) {
            return;
        }
        List<Line> lines = new ArrayList<>();
        lines.add(Line.debit(CASH, gross));
        lines.addAll(revenueCredits(Map.of(ROOM_REVENUE, gross)));
        post(today(), "Room settled (PAID): " + booking.getGuestName() + ", " + booking.getCheckIn() + " to " + booking.getCheckOut(),
                JournalSourceType.BOOKING, booking.getId(), null, currentUserId(), lines);
    }

    /**
     * A booking just left {@code PAID}: mirror every settlement entry for it that nothing has
     * reversed yet (including by hand, via {@link #reverse}), so the two paths never
     * double-reverse. A booking settled before ledger go-live has nothing to mirror.
     */
    @Transactional
    public void reverseBookingSettlement(BookingEntity booking) {
        List<JournalEntryEntity> settlements = entryRepository
                .findBySourceTypeAndSourceIdOrderByCreatedAtAsc(JournalSourceType.BOOKING, booking.getId()).stream()
                .filter(e -> e.getReversesEntryId() == null)
                .toList();
        if (settlements.isEmpty()) {
            return;
        }
        List<String> alreadyReversed = entryRepository
                .findByReversesEntryIdIn(settlements.stream().map(JournalEntryEntity::getId).toList()).stream()
                .map(JournalEntryEntity::getReversesEntryId)
                .toList();
        for (JournalEntryEntity settlement : settlements) {
            if (alreadyReversed.contains(settlement.getId())) {
                continue;
            }
            post(today(), "Room settlement reversed (booking now " + booking.getStatus().getValue() + "): " + booking.getGuestName(),
                    JournalSourceType.BOOKING, booking.getId(), settlement.getId(), currentUserId(), mirrorOf(settlement));
        }
    }

    /**
     * A POS order was just closed: gross per revenue account from its lines (Σ lines = the
     * order's {@code total} = its {@code Payment.amount}), debited to Cash/Bank or, for a room
     * charge, to Guest Ledger. Nothing to post for a zero total.
     */
    @Transactional
    public void postPosOrderClose(OrderEntity order, List<OrderItemEntity> items, PaymentMethod method, String cashierUserId) {
        Map<String, MenuItemEntity> menuItems = menuItemRepository
                .findAllById(items.stream().map(OrderItemEntity::getMenuItemId).distinct().toList()).stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, Function.identity()));
        Map<RevenueCode, BigDecimal> grossByCode = new EnumMap<>(RevenueCode.class);
        for (OrderItemEntity item : items) {
            RevenueCode code = RevenueClassification.of(menuItems.get(item.getMenuItemId()).getDepartment());
            grossByCode.merge(code, item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())), BigDecimal::add);
        }
        Map<String, BigDecimal> grossByAccount = new HashMap<>();
        grossByCode.forEach((code, gross) -> grossByAccount.put(revenueAccountOf(code), gross.setScale(2, RoundingMode.UNNECESSARY)));
        BigDecimal total = grossByAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            return;
        }
        List<Line> lines = new ArrayList<>();
        lines.add(Line.debit(method == PaymentMethod.ROOM_CHARGE ? GUEST_LEDGER : CASH, total));
        lines.addAll(revenueCredits(grossByAccount));
        post(today(), "POS order closed (" + method.getValue() + ")" + (order.getGuestName() != null ? ": " + order.getGuestName() : ""),
                JournalSourceType.POS_ORDER, order.getId(), null, cashierUserId, lines);
    }

    /** Money collected against a booking's room charges: Dr Cash/Bank, Cr Guest Ledger. */
    @Transactional
    public void postFolioPayment(FolioPaymentEntity payment, String guestName) {
        BigDecimal amount = payment.getAmount().setScale(2, RoundingMode.UNNECESSARY);
        post(today(), "Folio payment (" + payment.getMethod().getValue() + "): " + guestName,
                JournalSourceType.FOLIO_PAYMENT, payment.getId(), null, payment.getRecordedByUserId(),
                List.of(Line.debit(CASH, amount), Line.credit(GUEST_LEDGER, amount)));
    }

    private static String revenueAccountOf(RevenueCode code) {
        return switch (code) {
            case ROOM -> ROOM_REVENUE;
            case FNB -> FNB_REVENUE;
            case SPA -> SPA_REVENUE;
        };
    }

    /**
     * Credit lines for VAT-inclusive gross amounts: each revenue account gets its net, and the
     * VAT extracted from each (the same per-code extraction Z410 does) is credited to VAT Payable
     * as one line. The credits add back to Σ gross exactly.
     */
    private List<Line> revenueCredits(Map<String, BigDecimal> grossByAccount) {
        BigDecimal rate = vatSettingsService.currentRate();
        List<Line> lines = new ArrayList<>();
        BigDecimal vatTotal = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : grossByAccount.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            BigDecimal gross = entry.getValue();
            if (gross.signum() <= 0) {
                continue;
            }
            BigDecimal vat = RevenueClassification.vatInside(gross, rate);
            BigDecimal net = gross.subtract(vat);
            if (net.signum() > 0) {
                lines.add(Line.credit(entry.getKey(), net));
            }
            vatTotal = vatTotal.add(vat);
        }
        if (vatTotal.signum() > 0) {
            lines.add(Line.credit(VAT_PAYABLE, vatTotal));
        }
        return lines;
    }

    // --- Chart of accounts --------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LedgerAccount> listAccounts() {
        return accountRepository.findAllByOrderByCodeAsc().stream().map(LedgerService::toDto).toList();
    }

    @Transactional
    public LedgerAccount createAccount(LedgerAccountCreateInput input) {
        String name = input.getName().trim();
        if (name.isEmpty()) {
            throw ValidationException.field("name", "must not be blank");
        }
        if (accountRepository.existsById(input.getCode())) {
            throw new ConflictException("Account " + input.getCode() + " already exists");
        }
        LedgerAccountEntity account = new LedgerAccountEntity();
        account.setCode(input.getCode());
        account.setName(name);
        account.setType(input.getType());
        try {
            account = accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Account " + input.getCode() + " already exists");
        }
        auditLogService.record(
                AuditAction.LEDGER_ACCOUNT_CREATED,
                AuditEntityType.LEDGER_ACCOUNT,
                account.getCode(),
                "Ledger account " + account.getCode() + " " + account.getName() + " (" + account.getType().getValue() + ") created");
        return toDto(account);
    }

    private static LedgerAccount toDto(LedgerAccountEntity account) {
        return new LedgerAccount(account.getCode(), account.getName(), account.getType(), normalBalanceOf(account.getType()));
    }

    static LedgerNormalBalance normalBalanceOf(LedgerAccountType type) {
        return switch (type) {
            case ASSET, EXPENSE -> LedgerNormalBalance.DEBIT;
            case LIABILITY, EQUITY, REVENUE -> LedgerNormalBalance.CREDIT;
        };
    }

    // --- Manual entries -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<JournalEntry> listEntries(String from, String to) {
        ReportDateRange range = ReportDateRange.parse(from, to);
        return toDtos(entryRepository.findByEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(range.from(), range.to()));
    }

    /** {@code POST /ledger/entries}: every rule checked here, on the server, before anything is written. */
    @Transactional
    public JournalEntry createManualEntry(JournalEntryCreateInput input) {
        String description = requireDescription(input.getDescription());
        LocalDate entryDate = parseDate("entryDate", input.getEntryDate());
        if (entryDate.isAfter(today())) {
            throw ValidationException.field("entryDate", "must not be in the future");
        }
        List<JournalLineInput> inputs = input.getLines();
        if (inputs == null || inputs.size() < 2) {
            throw ValidationException.field("lines", "an entry needs at least two lines");
        }
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            JournalLineInput line = inputs.get(i);
            String field = "lines[" + i + "]";
            if (line.getAccountCode() == null || !accountRepository.existsById(line.getAccountCode())) {
                throw ValidationException.field(field + ".accountCode", "no such account");
            }
            boolean hasDebit = line.getDebit() != null && !line.getDebit().isBlank();
            boolean hasCredit = line.getCredit() != null && !line.getCredit().isBlank();
            if (hasDebit == hasCredit) {
                throw ValidationException.field(field, "give exactly one of debit or credit");
            }
            BigDecimal amount = parseAmount(hasDebit ? field + ".debit" : field + ".credit", hasDebit ? line.getDebit() : line.getCredit());
            lines.add(hasDebit ? Line.debit(line.getAccountCode(), amount) : Line.credit(line.getAccountCode(), amount));
        }
        BigDecimal debits = sum(lines, Line::debit);
        BigDecimal credits = sum(lines, Line::credit);
        if (debits.compareTo(credits) != 0) {
            throw ValidationException.field("lines", "Debits (" + PriceFormat.asDecimalString(debits) + ") must equal credits ("
                    + PriceFormat.asDecimalString(credits) + ")");
        }

        JournalEntryEntity entry = post(entryDate, description, JournalSourceType.MANUAL, null, null, currentUserId(), lines);
        auditLogService.record(
                AuditAction.LEDGER_ENTRY_POSTED,
                AuditEntityType.LEDGER_ENTRY,
                entry.getId(),
                "Manual journal entry of " + PriceFormat.asDecimalString(debits) + " dated " + entryDate + ": " + description);
        return toDtos(List.of(entry)).get(0);
    }

    /**
     * {@code POST /ledger/entries/{id}/reverse}: the exact mirror of an entry, dated today. At most
     * once per entry - checked here for the message, guaranteed by the unique
     * {@code reversesEntryId} for two concurrent attempts.
     */
    @Transactional
    public JournalEntry reverse(String id, JournalEntryReverseInput input) {
        String description = requireDescription(input.getDescription());
        JournalEntryEntity original = entryRepository.findById(id).orElseThrow(() -> new NotFoundException("Journal entry not found"));
        if (original.getReversesEntryId() != null) {
            throw ValidationException.field("id", "a reversal cannot itself be reversed - post the correct entry again instead");
        }
        if (entryRepository.existsByReversesEntryId(id)) {
            throw new ConflictException("This entry has already been reversed");
        }
        JournalEntryEntity reversal;
        try {
            reversal = post(today(), description, JournalSourceType.MANUAL, null, id, currentUserId(), mirrorOf(original));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This entry has already been reversed");
        }
        auditLogService.record(
                AuditAction.LEDGER_ENTRY_REVERSED,
                AuditEntityType.LEDGER_ENTRY,
                reversal.getId(),
                "Reversed journal entry " + id + " (" + original.getDescription() + "): " + description);
        return toDtos(List.of(reversal)).get(0);
    }

    private List<Line> mirrorOf(JournalEntryEntity entry) {
        return lineRepository.findByEntryIdInOrderByEntryIdAscLineOrderAsc(List.of(entry.getId())).stream()
                .map(l -> new Line(l.getAccountCode(), l.getDebit(), l.getCredit()).mirrored())
                .toList();
    }

    // --- Trial balance (Z120) -----------------------------------------------------------------

    /** {@code GET /reports/trial-balance} - see that operation's description. */
    @Transactional(readOnly = true)
    public TrialBalanceReport trialBalance(String asOf) {
        LocalDate date = parseDate("asOf", asOf);
        Map<String, BigDecimal[]> sums = new HashMap<>();
        for (Object[] row : lineRepository.sumByAccountAsOf(date)) {
            sums.put((String) row[0], new BigDecimal[] {(BigDecimal) row[1], (BigDecimal) row[2]});
        }
        List<TrialBalanceRow> rows = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO, totalCredit = BigDecimal.ZERO;
        for (LedgerAccountEntity account : accountRepository.findAllByOrderByCodeAsc()) {
            BigDecimal[] sum = sums.getOrDefault(account.getCode(), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            LedgerNormalBalance side = normalBalanceOf(account.getType());
            BigDecimal balance = side == LedgerNormalBalance.DEBIT ? sum[0].subtract(sum[1]) : sum[1].subtract(sum[0]);
            rows.add(new TrialBalanceRow(account.getCode(), account.getName(), account.getType(), side, money(sum[0]), money(sum[1]), money(balance)));
            totalDebit = totalDebit.add(sum[0]);
            totalCredit = totalCredit.add(sum[1]);
        }
        return new TrialBalanceReport(date.toString(), rows, money(totalDebit), money(totalCredit), totalDebit.compareTo(totalCredit) == 0);
    }

    // --- Posting ------------------------------------------------------------------------------

    /**
     * The one write path. Refuses (as a bug, not a 400 - manual input is validated before it gets
     * here) any entry with fewer than two lines, a line without exactly one positive side, or
     * debits different from credits.
     */
    private JournalEntryEntity post(
            LocalDate entryDate, String description, JournalSourceType sourceType, String sourceId, String reversesEntryId, String createdByUserId,
            List<Line> lines) {
        if (lines.size() < 2) {
            throw new IllegalStateException("Journal entry needs at least two lines: " + lines);
        }
        for (Line line : lines) {
            if ((line.debit().signum() > 0) == (line.credit().signum() > 0) || line.debit().signum() < 0 || line.credit().signum() < 0) {
                throw new IllegalStateException("Journal line must have exactly one positive side: " + line);
            }
        }
        if (sum(lines, Line::debit).compareTo(sum(lines, Line::credit)) != 0) {
            throw new IllegalStateException("Unbalanced journal entry: " + lines);
        }

        JournalEntryEntity entry = new JournalEntryEntity();
        entry.setEntryDate(entryDate);
        entry.setDescription(description);
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        entry.setReversesEntryId(reversesEntryId);
        entry.setCreatedByUserId(createdByUserId);
        entry = entryRepository.saveAndFlush(entry);

        List<JournalLineEntity> rows = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            JournalLineEntity row = new JournalLineEntity();
            row.setEntryId(entry.getId());
            row.setLineOrder(i);
            row.setAccountCode(lines.get(i).accountCode());
            row.setDebit(lines.get(i).debit());
            row.setCredit(lines.get(i).credit());
            rows.add(row);
        }
        lineRepository.saveAllAndFlush(rows);
        return entry;
    }

    // --- Mapping and helpers ------------------------------------------------------------------

    private List<JournalEntry> toDtos(List<JournalEntryEntity> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        List<String> ids = entries.stream().map(JournalEntryEntity::getId).toList();
        Map<String, List<JournalLineEntity>> linesByEntry = lineRepository.findByEntryIdInOrderByEntryIdAscLineOrderAsc(ids).stream()
                .collect(Collectors.groupingBy(JournalLineEntity::getEntryId));
        Map<String, String> reversedBy = entryRepository.findByReversesEntryIdIn(ids).stream()
                .collect(Collectors.toMap(JournalEntryEntity::getReversesEntryId, JournalEntryEntity::getId));
        Map<String, String> accountNames = accountRepository.findAll().stream()
                .collect(Collectors.toMap(LedgerAccountEntity::getCode, LedgerAccountEntity::getName));
        return entries.stream().map(entry -> {
            List<JournalLineEntity> lines = linesByEntry.getOrDefault(entry.getId(), List.of());
            List<JournalLine> lineDtos = lines.stream()
                    .map(l -> new JournalLine(l.getAccountCode(), accountNames.get(l.getAccountCode()), money(l.getDebit()), money(l.getCredit())))
                    .toList();
            return new JournalEntry(
                    entry.getId(),
                    entry.getEntryDate().toString(),
                    entry.getDescription(),
                    entry.getSourceType(),
                    entry.getSourceId(),
                    entry.getReversesEntryId(),
                    reversedBy.get(entry.getId()),
                    entry.getCreatedByUserId(),
                    TimestampFormat.toUtc(entry.getCreatedAt()),
                    lineDtos,
                    money(lines.stream().map(JournalLineEntity::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add)),
                    money(lines.stream().map(JournalLineEntity::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add)));
        }).toList();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    /** The signed-in staff member, or null when there is none (never the case for an HTTP request). */
    private static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof StaffPrincipal staff ? staff.id() : null;
    }

    private static String requireDescription(String description) {
        String trimmed = description != null ? description.trim() : "";
        if (trimmed.isEmpty()) {
            throw ValidationException.field("description", "must not be blank");
        }
        return trimmed;
    }

    private static LocalDate parseDate(String field, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ValidationException.field(field, "must be a valid date (YYYY-MM-DD)");
        }
    }

    private static BigDecimal parseAmount(String field, String value) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw ValidationException.field(field, "must be a decimal amount");
        }
        if (amount.signum() <= 0) {
            throw ValidationException.field(field, "must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw ValidationException.field(field, "at most two decimals");
        }
        if (amount.compareTo(MAX_AMOUNT) >= 0) {
            throw ValidationException.field(field, "too large");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal sum(List<Line> lines, Function<Line, BigDecimal> side) {
        return lines.stream().map(side).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String money(BigDecimal value) {
        return PriceFormat.asDecimalString(value.setScale(2, RoundingMode.HALF_UP));
    }
}
