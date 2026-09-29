package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.JournalEntryEntity;
import com.sunsetbeach.entity.JournalLineEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.ShiftEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.entity.VatSettingsEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.CloseOrderInput;
import com.sunsetbeach.model.FolioPaymentInput;
import com.sunsetbeach.model.FolioPaymentMethod;
import com.sunsetbeach.model.JournalEntry;
import com.sunsetbeach.model.JournalEntryCreateInput;
import com.sunsetbeach.model.JournalEntryReverseInput;
import com.sunsetbeach.model.JournalLineInput;
import com.sunsetbeach.model.JournalSourceType;
import com.sunsetbeach.model.LedgerAccountCreateInput;
import com.sunsetbeach.model.LedgerAccountType;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.RevenueCode;
import com.sunsetbeach.model.RevenueStatisticReport;
import com.sunsetbeach.model.RevenueStatisticRow;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftStatus;
import com.sunsetbeach.model.TrialBalanceReport;
import com.sunsetbeach.model.TrialBalanceRow;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.JournalEntryRepository;
import com.sunsetbeach.repository.JournalLineRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.ShiftRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.repository.VatSettingsRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The double-entry ledger: every automatic posting balances and lands on the revenue account Z410
 * would classify it under, manual entries are validated server-side, reversals net to zero, and
 * the trial balance's debits equal its credits.
 *
 * <p>{@code @Transactional} (auto rollback). The ledger may already hold rows committed by other,
 * non-transactional test classes that close orders, so every assertion is about this test's own
 * entries (by source id) or a before/after delta - never an absolute total.
 */
@SpringBootTest
@Transactional
class LedgerServiceTests extends AbstractIntegrationTest {

    @Autowired private LedgerService ledgerService;
    @Autowired private BookingService bookingService;
    @Autowired private OrderService orderService;
    @Autowired private ReportService reportService;
    @Autowired private JournalEntryRepository entryRepository;
    @Autowired private JournalLineRepository lineRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private VatSettingsRepository vatSettingsRepository;
    @Autowired private Clock clock;

    private RoomEntity room;
    private UserEntity cashier;

    @BeforeEach
    void setUp() {
        VatSettingsEntity vat = vatSettingsRepository.findById(VatSettingsEntity.SINGLETON_ID).orElseThrow();
        vat.setVatRate(new BigDecimal("7.00"));
        vatSettingsRepository.saveAndFlush(vat);

        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Ledger Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by LedgerServiceTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);

        UserEntity user = new UserEntity();
        user.setEmail("ledger-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.ADMIN);
        cashier = userRepository.saveAndFlush(user);

        ShiftEntity shift = new ShiftEntity();
        shift.setOpenedByUserId(cashier.getId());
        shift.setStatus(ShiftStatus.OPEN);
        shiftRepository.saveAndFlush(shift);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new StaffPrincipal(cashier.getId(), cashier.getEmail(), Role.ADMIN), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- Fixtures -----------------------------------------------------------------------------

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private BookingEntity persistBooking(String totalPrice) {
        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setRoomId(room.getId());
        booking.setGuestName("Ledger Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.of(2034, 3, 1));
        booking.setCheckOut(LocalDate.of(2034, 3, 3));
        booking.setTotalPrice(new BigDecimal(totalPrice));
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.saveAndFlush(booking);
    }

    private MenuItemEntity menuItem(String name, MenuDepartment department, String price) {
        MenuItemEntity item = new MenuItemEntity();
        item.setName(name + " " + UUID.randomUUID());
        item.setDescription("Ledger test item");
        item.setCategory(department == MenuDepartment.SPA ? "Treatments" : "Mains");
        item.setDepartment(department);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(true);
        return menuItemRepository.saveAndFlush(item);
    }

    /** An OPEN order holding the given (menu item, quantity) lines, total = Σ lines, ready to close. */
    private OrderEntity persistOrder(Map<MenuItemEntity, Integer> lines) {
        OrderEntity order = new OrderEntity();
        order.setOpenedByUserId(cashier.getId());
        order = orderRepository.saveAndFlush(order);
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<MenuItemEntity, Integer> line : lines.entrySet()) {
            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(order.getId());
            item.setMenuItemId(line.getKey().getId());
            item.setQuantity(line.getValue());
            item.setUnitPrice(line.getKey().getPrice());
            orderItemRepository.saveAndFlush(item);
            total = total.add(line.getKey().getPrice().multiply(BigDecimal.valueOf(line.getValue())));
        }
        order.setTotal(total);
        return orderRepository.saveAndFlush(order);
    }

    private List<JournalEntryEntity> entriesFor(JournalSourceType type, String sourceId) {
        return entryRepository.findBySourceTypeAndSourceIdOrderByCreatedAtAsc(type, sourceId);
    }

    /** accountCode -> [debit, credit] for one entry. */
    private Map<String, BigDecimal[]> linesOf(JournalEntryEntity entry) {
        Map<String, BigDecimal[]> byAccount = new HashMap<>();
        for (JournalLineEntity line : lineRepository.findByEntryIdInOrderByEntryIdAscLineOrderAsc(List.of(entry.getId()))) {
            byAccount.put(line.getAccountCode(), new BigDecimal[] {line.getDebit(), line.getCredit()});
        }
        return byAccount;
    }

    private static void assertBalanced(Map<String, BigDecimal[]> lines) {
        BigDecimal debits = lines.values().stream().map(l -> l[0]).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = lines.values().stream().map(l -> l[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debits).isPositive().isEqualByComparingTo(credits);
    }

    private static void assertLine(Map<String, BigDecimal[]> lines, String account, String debit, String credit) {
        assertThat(lines).containsKey(account);
        assertThat(lines.get(account)[0]).as(account + " debit").isEqualByComparingTo(debit);
        assertThat(lines.get(account)[1]).as(account + " credit").isEqualByComparingTo(credit);
    }

    /** Net (debit - credit) per account across entries - zero everywhere means they cancel out. */
    private Map<String, BigDecimal> netOf(List<JournalEntryEntity> entries) {
        Map<String, BigDecimal> net = new HashMap<>();
        for (JournalEntryEntity entry : entries) {
            linesOf(entry).forEach((account, sides) -> net.merge(account, sides[0].subtract(sides[1]), BigDecimal::add));
        }
        return net;
    }

    private static JournalLineInput debit(String account, String amount) {
        return new JournalLineInput(account).debit(amount);
    }

    private static JournalLineInput credit(String account, String amount) {
        return new JournalLineInput(account).credit(amount);
    }

    private static BigDecimal gross(RevenueStatisticReport report, RevenueCode code) {
        return report.getRows().stream().filter(r -> r.getCode() == code).map(RevenueStatisticRow::getGross).map(BigDecimal::new).findFirst().orElseThrow();
    }

    // --- Booking settlement -------------------------------------------------------------------

    @Test
    void bookingBecomingPaid_postsCashAgainstRoomRevenueAndVat() {
        BookingEntity booking = persistBooking("1070.00");

        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.PAID));

        List<JournalEntryEntity> entries = entriesFor(JournalSourceType.BOOKING, booking.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getEntryDate()).isEqualTo(today());
        assertThat(entries.get(0).getCreatedByUserId()).isEqualTo(cashier.getId());
        Map<String, BigDecimal[]> lines = linesOf(entries.get(0));
        assertBalanced(lines);
        assertLine(lines, LedgerService.CASH, "1070.00", "0");
        assertLine(lines, LedgerService.ROOM_REVENUE, "0", "1000.00");
        assertLine(lines, LedgerService.VAT_PAYABLE, "0", "70.00");
        assertThat(lines).hasSize(3);
    }

    @Test
    void bookingStatusChangeNotInvolvingPaid_postsNothing() {
        BookingEntity booking = persistBooking("1070.00");
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CANCELLED));
        assertThat(entriesFor(JournalSourceType.BOOKING, booking.getId())).isEmpty();
    }

    @Test
    void bookingLeavingPaid_postsExactMirror_thatNetsToZeroOnEveryAccount() {
        BookingEntity booking = persistBooking("1070.00");
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.PAID));

        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CANCELLED));

        List<JournalEntryEntity> entries = entriesFor(JournalSourceType.BOOKING, booking.getId());
        assertThat(entries).hasSize(2);
        assertThat(entries.get(1).getReversesEntryId()).isEqualTo(entries.get(0).getId());
        assertBalanced(linesOf(entries.get(1)));
        assertThat(netOf(entries)).hasSize(3).allSatisfy((account, net) -> assertThat(net).as(account).isZero());
    }

    @Test
    void bookingPaidAgainAfterLeavingPaid_postsANewSettlement() {
        BookingEntity booking = persistBooking("1070.00");
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.PAID));
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CONFIRMED));
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.PAID));

        List<JournalEntryEntity> entries = entriesFor(JournalSourceType.BOOKING, booking.getId());
        assertThat(entries).hasSize(3);
        assertThat(netOf(entries).get(LedgerService.CASH)).isEqualByComparingTo("1070.00");
    }

    @Test
    void bookingSettlementReversedByHand_isNotReversedAgainWhenTheBookingLeavesPaid() {
        BookingEntity booking = persistBooking("1070.00");
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.PAID));
        String settlementId = entriesFor(JournalSourceType.BOOKING, booking.getId()).get(0).getId();
        ledgerService.reverse(settlementId, new JournalEntryReverseInput("Posted in error"));

        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CONFIRMED));

        assertThat(entriesFor(JournalSourceType.BOOKING, booking.getId())).hasSize(1); // only the original
    }

    // --- POS order close ----------------------------------------------------------------------

    @Test
    void posOrderClose_splitsFnbAndSpa_matchingTheRevenueStatisticClassification() {
        MenuItemEntity padThai = menuItem("Pad Thai", MenuDepartment.KITCHEN, "107.00");
        MenuItemEntity beer = menuItem("Beer", MenuDepartment.BAR, "107.00");
        MenuItemEntity massage = menuItem("Thai massage", MenuDepartment.SPA, "1070.00");
        OrderEntity order = persistOrder(Map.of(padThai, 1, beer, 1, massage, 1));
        // A wide window around today, so the Bangkok-day bucketing of the payment's timestamp
        // can't push it out; the other test data in it cancels out of the before/after delta.
        String from = today().minusDays(2).toString(), to = today().plusDays(2).toString();
        RevenueStatisticReport before = reportService.revenueStatistic(from, to);

        orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CASH), cashier.getId());

        RevenueStatisticReport after = reportService.revenueStatistic(from, to);
        assertThat(gross(after, RevenueCode.FNB).subtract(gross(before, RevenueCode.FNB))).isEqualByComparingTo("214.00");
        assertThat(gross(after, RevenueCode.SPA).subtract(gross(before, RevenueCode.SPA))).isEqualByComparingTo("1070.00");

        List<JournalEntryEntity> entries = entriesFor(JournalSourceType.POS_ORDER, order.getId());
        assertThat(entries).hasSize(1);
        Map<String, BigDecimal[]> lines = linesOf(entries.get(0));
        assertBalanced(lines);
        assertLine(lines, LedgerService.CASH, "1284.00", "0");
        assertLine(lines, LedgerService.FNB_REVENUE, "0", "200.00");
        assertLine(lines, LedgerService.SPA_REVENUE, "0", "1000.00");
        assertLine(lines, LedgerService.VAT_PAYABLE, "0", "84.00");
        assertThat(lines).hasSize(4);
    }

    @Test
    void roomChargedOrder_debitsGuestLedger_andAFolioPaymentClearsIt() {
        BookingEntity booking = persistBooking("1000.00");
        OrderEntity order = persistOrder(Map.of(menuItem("Pad Thai", MenuDepartment.KITCHEN, "107.00"), 2));

        orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.ROOM_CHARGE).bookingId(booking.getId()), cashier.getId());
        String folioPaymentId = bookingService.recordFolioPayment(booking.getId(), new FolioPaymentInput(FolioPaymentMethod.CARD, "214.00")).getId();

        JournalEntryEntity close = entriesFor(JournalSourceType.POS_ORDER, order.getId()).get(0);
        Map<String, BigDecimal[]> closeLines = linesOf(close);
        assertBalanced(closeLines);
        assertLine(closeLines, LedgerService.GUEST_LEDGER, "214.00", "0");
        assertThat(closeLines).doesNotContainKey(LedgerService.CASH);

        List<JournalEntryEntity> folio = entriesFor(JournalSourceType.FOLIO_PAYMENT, folioPaymentId);
        assertThat(folio).hasSize(1);
        Map<String, BigDecimal[]> folioLines = linesOf(folio.get(0));
        assertLine(folioLines, LedgerService.CASH, "214.00", "0");
        assertLine(folioLines, LedgerService.GUEST_LEDGER, "0", "214.00");

        Map<String, BigDecimal> net = netOf(List.of(close, folio.get(0)));
        assertThat(net.get(LedgerService.GUEST_LEDGER)).isZero();
        assertThat(net.get(LedgerService.CASH)).isEqualByComparingTo("214.00");
    }

    // --- Manual entries -----------------------------------------------------------------------

    @Test
    void manualEntry_unbalanced_isRejectedAndNothingIsSaved() {
        long before = entryRepository.count();
        JournalEntryCreateInput input = new JournalEntryCreateInput(
                today().toString(), "Electricity bill", List.of(debit("6000", "500.00"), credit("1000", "499.99")));

        assertThatThrownBy(() -> ledgerService.createManualEntry(input))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must equal credits");
        assertThat(entryRepository.count()).isEqualTo(before);
    }

    @Test
    void manualEntry_malformedLines_areRejected() {
        String date = today().toString();
        List<List<JournalLineInput>> invalid = List.of(
                List.of(debit("6000", "100.00")), // one line
                List.of(new JournalLineInput("6000").debit("100.00").credit("100.00"), credit("1000", "100.00")), // both sides
                List.of(new JournalLineInput("6000"), credit("1000", "100.00")), // neither side
                List.of(debit("NOPE", "100.00"), credit("1000", "100.00")), // unknown account
                List.of(debit("6000", "-100.00"), credit("1000", "-100.00")), // negative
                List.of(debit("6000", "100.001"), credit("1000", "100.001")), // three decimals
                List.of(debit("6000", "abc"), credit("1000", "100.00"))); // not a number
        for (List<JournalLineInput> lines : invalid) {
            assertThatThrownBy(() -> ledgerService.createManualEntry(new JournalEntryCreateInput(date, "Bad entry", lines)))
                    .as(lines.toString())
                    .isInstanceOf(ValidationException.class);
        }
        List<JournalLineInput> fine = List.of(debit("6000", "100.00"), credit("1000", "100.00"));
        assertThatThrownBy(() -> ledgerService.createManualEntry(new JournalEntryCreateInput(today().plusDays(2).toString(), "Future", fine)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ledgerService.createManualEntry(new JournalEntryCreateInput(date, "   ", fine)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void manualEntry_balanced_isPostedWithItsLines() {
        JournalEntry entry = ledgerService.createManualEntry(new JournalEntryCreateInput(
                today().minusDays(1).toString(), "Owner draw",
                List.of(debit("3000", "250.00"), debit("6000", "50.00"), credit("1000", "300.00"))));

        assertThat(entry.getSourceType()).isEqualTo(JournalSourceType.MANUAL);
        assertThat(entry.getSourceId().get()).isNull();
        assertThat(entry.getEntryDate()).isEqualTo(today().minusDays(1).toString());
        assertThat(entry.getTotalDebit()).isEqualTo("300.00");
        assertThat(entry.getTotalCredit()).isEqualTo("300.00");
        assertThat(entry.getLines()).extracting("accountCode").containsExactly("3000", "6000", "1000");
        assertThat(entry.getCreatedByUserId().get()).isEqualTo(cashier.getId());
        assertThat(ledgerService.listEntries(today().minusDays(1).toString(), today().minusDays(1).toString()))
                .extracting(JournalEntry::getId).contains(entry.getId());
    }

    @Test
    void reversal_mirrorsTheEntry_netsToZero_andOnlyOnce() {
        JournalEntry original = ledgerService.createManualEntry(new JournalEntryCreateInput(
                today().toString(), "Supplies", List.of(debit("6000", "120.50"), credit("1000", "120.50"))));

        JournalEntry reversal = ledgerService.reverse(original.getId(), new JournalEntryReverseInput("Wrong account"));

        assertThat(reversal.getReversesEntryId().get()).isEqualTo(original.getId());
        assertThat(reversal.getDescription()).isEqualTo("Wrong account");
        assertThat(reversal.getLines()).extracting("accountCode", "debit", "credit")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("6000", "0.00", "120.50"), org.assertj.core.groups.Tuple.tuple("1000", "120.50", "0.00"));
        assertThat(netOf(entryRepository.findAllById(List.of(original.getId(), reversal.getId()))))
                .allSatisfy((account, net) -> assertThat(net).as(account).isZero());
        assertThat(ledgerService.listEntries(today().toString(), today().toString()))
                .filteredOn(e -> e.getId().equals(original.getId()))
                .singleElement()
                .satisfies(e -> assertThat(e.getReversedByEntryId().get()).isEqualTo(reversal.getId()));

        assertThatThrownBy(() -> ledgerService.reverse(original.getId(), new JournalEntryReverseInput("Again")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> ledgerService.reverse(reversal.getId(), new JournalEntryReverseInput("Undo the undo")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createAccount_rejectsADuplicateCode_andTheNewAccountCanBePostedTo() {
        ledgerService.createAccount(new LedgerAccountCreateInput("6100", "Utilities", LedgerAccountType.EXPENSE));
        assertThatThrownBy(() -> ledgerService.createAccount(new LedgerAccountCreateInput("6100", "Again", LedgerAccountType.EXPENSE)))
                .isInstanceOf(ConflictException.class);

        JournalEntry entry = ledgerService.createManualEntry(new JournalEntryCreateInput(
                today().toString(), "Water bill", List.of(debit("6100", "80.00"), credit("1000", "80.00"))));
        assertThat(entry.getLines().get(0).getAccountName()).isEqualTo("Utilities");
    }

    // --- Trial balance ------------------------------------------------------------------------

    @Test
    void trialBalance_debitsEqualCredits_andEachAccountMovesByWhatWasPosted() {
        String asOf = today().plusDays(1).toString();
        TrialBalanceReport before = ledgerService.trialBalance(asOf);

        BookingEntity booking = persistBooking("2140.00");
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.PAID));
        OrderEntity order = persistOrder(Map.of(menuItem("Massage", MenuDepartment.SPA, "535.00"), 1));
        orderService.close(order.getId(), new CloseOrderInput(PaymentMethod.CARD), cashier.getId());
        ledgerService.createManualEntry(new JournalEntryCreateInput(
                today().toString(), "Laundry service", List.of(debit("6000", "300.00"), credit("1000", "300.00"))));

        TrialBalanceReport after = ledgerService.trialBalance(asOf);

        assertThat(after.getBalanced()).isTrue();
        assertThat(after.getTotalDebit()).isEqualTo(after.getTotalCredit());
        assertThat(new BigDecimal(after.getTotalDebit()).subtract(new BigDecimal(before.getTotalDebit())))
                .isEqualByComparingTo("2975.00"); // 2140 + 535 + 300
        assertThat(balanceDelta(before, after, "1000")).isEqualByComparingTo("2375.00"); // 2140 + 535 - 300
        assertThat(balanceDelta(before, after, "4000")).isEqualByComparingTo("2000.00");
        assertThat(balanceDelta(before, after, "4200")).isEqualByComparingTo("500.00");
        assertThat(balanceDelta(before, after, "2100")).isEqualByComparingTo("175.00");
        assertThat(balanceDelta(before, after, "6000")).isEqualByComparingTo("300.00");
        assertThat(after.getRows()).extracting(TrialBalanceRow::getAccountCode).isSorted().contains("1000", "1100", "2100", "3000", "4000", "4100", "4200", "6000");

        // Entries dated after asOf are excluded.
        TrialBalanceReport yesterday = ledgerService.trialBalance(today().minusDays(1).toString());
        assertThat(balanceDelta(yesterday, after, "6000")).isGreaterThanOrEqualTo(new BigDecimal("300.00"));
    }

    private static BigDecimal balanceDelta(TrialBalanceReport before, TrialBalanceReport after, String account) {
        return balance(after, account).subtract(balance(before, account));
    }

    private static BigDecimal balance(TrialBalanceReport report, String account) {
        return report.getRows().stream().filter(r -> r.getAccountCode().equals(account)).map(r -> new BigDecimal(r.getBalance())).findFirst().orElseThrow();
    }
}
