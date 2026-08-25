package com.example.funkyeventapp.repositories;

import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.models.Budget;
import com.example.funkyeventapp.models.BudgetCategory;
import com.example.funkyeventapp.models.BudgetItem;
import com.example.funkyeventapp.models.BudgetItemSource;
import com.example.funkyeventapp.models.BudgetType;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventAssignment;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.models.EventType;
import com.example.funkyeventapp.models.Invoice;
import com.example.funkyeventapp.models.InvoiceStatus;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.models.UserRole;
import com.example.funkyeventapp.models.Cashbox;
import com.example.funkyeventapp.models.CashboxTransaction;
import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.ExpensePurpose;
import com.example.funkyeventapp.models.TransactionType;
import com.example.funkyeventapp.models.DocumentSource;
import com.example.funkyeventapp.models.Receipt;
import com.example.funkyeventapp.models.ReceiptProcessingStatus;
import com.example.funkyeventapp.models.ScannedDocument;
import com.example.funkyeventapp.models.TeamMember;
import com.example.funkyeventapp.models.TeamFee;
import com.example.funkyeventapp.models.TeamPayment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MockDataRepository {
    private static final MockDataRepository INSTANCE = new MockDataRepository();

    private final List<Event> events = new ArrayList<>();
    private final List<Client> clients = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<EventAssignment> assignments = new ArrayList<>();
    private final List<Budget> budgets = new ArrayList<>();
    private final List<BudgetCategory> budgetCategories = new ArrayList<>();
    private final List<BudgetItem> budgetItems = new ArrayList<>();
    private final List<Invoice> invoices = new ArrayList<>();
    private final List<Cashbox> cashboxes = new ArrayList<>();
    private final List<CashboxTransaction> cashboxTransactions = new ArrayList<>();
    private final List<ScannedDocument> scannedDocuments = new ArrayList<>();
    private final List<Receipt> receipts = new ArrayList<>();
    private final List<TeamMember> teamMembers = new ArrayList<>();
    private final List<TeamFee> teamFees = new ArrayList<>();
    private final List<TeamPayment> teamPayments = new ArrayList<>();
    private long mockIdCounter = 1000;

    private MockDataRepository() {
        seedClients();
        seedUsers();
        seedEvents();
        seedAssignments();
        seedBudgets();
        seedInvoices();
        seedCashbox();
        seedTeam();
    }

    public static MockDataRepository getInstance() { return INSTANCE; }

    private void seedClients() {
        clients.add(new Client("client_expo", "EXPO", "", "109998877", "Bulevar umetnosti 4, Beograd", "office@expo.rs", "+381 11 555 0101", "Ana Jovanović"));
        clients.add(new Client("client_addiko", "Addiko Bank", "", "100000123", "Bulevar Mihajla Pupina 6, Beograd", "neda.bogunovic@addiko.com", "+381 11 222 6000", "Neda Bogunović"));
        clients.add(new Client("client_tiger", "Flying Tiger Copenhagen", "", "112233445", "Galerija, Beograd", "store.rs@flyingtiger.com", "+381 11 400 2200", "Maja Ilić"));
        clients.add(new Client("client_intesa", "Banca Intesa", "", "100001159", "Milentija Popovića 7b, Beograd", "marketing@bancaintesa.rs", "+381 11 201 1200", "Marko Petrović"));
    }

    private void seedUsers() {
        users.add(new User("user_teodora", "Teodora", "Milićević", "teodora@funkybusiness.rs", UserRole.ADMIN, true));
        users.add(new User("user_bojana", "Bojana", "Mumović", "bojana@funkybusiness.rs", UserRole.ADMIN, true));
        users.add(new User("user_valentina", "Valentina", "Gajić", "valentina@funkybusiness.rs", UserRole.COORDINATOR, true));
        users.add(new User("user_vladica", "Vladica", "Veličkov", "vladica@funkybusiness.rs", UserRole.COORDINATOR, true));
        users.add(new User("user_nikola", "Nikola", "Simić", "nikola@funkybusiness.rs", UserRole.MANAGER, true));
    }

    private void seedEvents() {
        events.add(event("1", "Addiko Banka - Branding", EventType.CAMPAIGN, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 27), "Beograd", "client_addiko", "Addiko Bank a.d. Beograd", "PO-ADD-2026-081", "50% avansno, 50% nakon događaja", "Branding kampanja.", false));
        events.add(event("2", "Flying Tiger Store Opening", EventType.EVENT, LocalDate.of(2026, 9, 3), null, "Galerija, Beograd", "client_tiger", "Tiger Retail Serbia d.o.o.", "FT-OPEN-0903", "Plaćanje 15 dana nakon realizacije", "Otvaranje nove prodavnice.", false));
        events.add(event("3", "Funky Summer Activation", EventType.EVENT, LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 13), "Novi Sad", "client_expo", "EXPO 2027 d.o.o.", "EXPO-SA-1209", "30 dana", "Letnja aktivacija.", false));
        events.add(event("4", "Addiko Bank Promo Weekend", EventType.CAMPAIGN, LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 6), "Niš", "client_addiko", "Addiko Bank a.d. Beograd", "PO-ADD-2026-055", "30 dana", "Završena kampanja.", true));
        events.add(event("5", "Flying Tiger Spring Launch", EventType.EVENT, LocalDate.of(2026, 5, 18), null, "Beograd", "client_tiger", "Tiger Retail Serbia d.o.o.", "FT-SPRING-0518", "15 dana", "Završeno prolećno lansiranje.", true));
    }

    private Event event(String id, String name, EventType type, LocalDate start, LocalDate end,
                        String location, String clientId, String billingEntity, String poNumber,
                        String paymentTerms, String notes, boolean completed) {
        return new Event(id, name, type, start, end, location,
                completed ? EventStatus.COMPLETED : EventStatus.CURRENT, clientId,
                billingEntity, poNumber, paymentTerms, notes, completed);
    }

    private void seedAssignments() {
        assignments.add(new EventAssignment("a1", "user_teodora", "1", "Event owner", true));
        assignments.add(new EventAssignment("a2", "user_bojana", "1", "Project manager", false));
        assignments.add(new EventAssignment("a3", "user_valentina", "1", "Coordinator", false));
        assignments.add(new EventAssignment("a4", "user_bojana", "2", "Event owner", true));
        assignments.add(new EventAssignment("a5", "user_teodora", "2", "Manager", false));
        assignments.add(new EventAssignment("a6", "user_valentina", "3", "Event owner", true));
        assignments.add(new EventAssignment("a7", "user_nikola", "3", "Coordinator", false));
        assignments.add(new EventAssignment("a8", "user_teodora", "4", "Event owner", true));
        assignments.add(new EventAssignment("a9", "user_bojana", "5", "Event owner", true));
    }

    private void seedBudgets() {
        String[] categories = {"Staff", "Logistics", "Transport", "Accommodation", "Equipment", "Production", "Food", "Printing", "Decoration"};
        for (int i = 0; i < categories.length; i++)
            budgetCategories.add(new BudgetCategory("cat_" + (i + 1), categories[i]));
        for (Event event : events)
            budgets.add(new Budget("budget_" + event.getId(), event.getId(), false, BigDecimal.ZERO));
        budgetItems.add(item("bi_1", "1", BudgetType.EXTERNAL, "cat_2", "Dezinfekcija kostima", "1", "1", "70", ""));
        budgetItems.add(item("bi_2", "1", BudgetType.EXTERNAL, "cat_1", "Garderober-koordinator", "1", "1", "80", ""));
        budgetItems.add(item("bi_3", "1", BudgetType.EXTERNAL, "cat_1", "Animator u maskoti", "1", "1", "150", ""));
        budgetItems.add(item("bi_4", "1", BudgetType.INTERNAL, "cat_1", "Interni tim", "1", "1", "150", ""));
        budgetItems.add(item("bi_5", "2", BudgetType.EXTERNAL, "cat_6", "Store opening produkcija", "1", "1", "450", ""));
    }

    private BudgetItem item(String id, String eventId, BudgetType type, String categoryId,
                            String description, String quantity, String days, String rate, String notes) {
        return new BudgetItem(id, eventId, type, categoryId, description,
                new BigDecimal(quantity), new BigDecimal(days), new BigDecimal(rate), notes,
                BudgetItemSource.MANUAL, null, null);
    }

    public List<Event> getEvents(EventStatus status) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) if (event.getStatus() == status) result.add(event);
        return result;
    }

    public Event getEventById(String id) {
        for (Event event : events) if (event.getId().equals(id)) return event;
        return null;
    }

    public Client getClientById(String id) {
        for (Client client : clients) if (client.getId().equals(id)) return client;
        return null;
    }

    private void seedInvoices() {
        invoices.add(invoice("inv_1", "1", "client_addiko", "FB-2026-081", "2026-08-12", "2026-08-27", "4200.00", "EUR", InvoiceStatus.ISSUED, "Branding campaign advance."));
        invoices.add(invoice("inv_2", "4", "client_addiko", "FB-2026-056", "2026-07-07", "2026-08-06", "3150.00", "EUR", InvoiceStatus.PAID, "Promo weekend final invoice."));
        invoices.add(invoice("inv_3", "2", "client_tiger", "FB-2026-090", "2026-08-15", "2026-09-18", "5800.00", "EUR", InvoiceStatus.DRAFT, "Store opening production."));
        invoices.add(invoice("inv_4", "5", "client_tiger", "FB-2026-041", "2026-05-20", "2026-06-04", "2750.00", "EUR", InvoiceStatus.PAID, "Spring launch."));
        invoices.add(invoice("inv_5", "3", "client_expo", "FB-2026-094", "2026-08-18", "2026-09-17", "7600.00", "EUR", InvoiceStatus.ISSUED, "Summer activation advance."));
    }

    private Invoice invoice(String id, String eventId, String clientId, String number,
                            String issueDate, String dueDate, String amount, String currency,
                            InvoiceStatus status, String notes) {
        return new Invoice(id, eventId, clientId, number, LocalDate.parse(issueDate),
                LocalDate.parse(dueDate), new BigDecimal(amount), currency, status, "", notes);
    }

    private void seedCashbox() {
        cashboxes.add(new Cashbox("cashbox_teodora", "user_teodora", Currency.EUR));
        addCashboxTransaction(tx("ct_1", "Cash received", "Project advance", "20000", Currency.RSD, "117.20", "170.65", "2026-08-12", TransactionType.INCOME, ExpensePurpose.GENERAL, null));
        addCashboxTransaction(tx("ct_2", "Putarine", "Put do Niša", "1279.96", Currency.RSD, "117.20", "10.92", "2026-08-18", TransactionType.EXPENSE, ExpensePurpose.EVENT, "4"));
        addCashboxTransaction(tx("ct_3", "Taxi", "Kristina taxi", "2000", Currency.RSD, "117.20", "17.06", "2026-08-17", TransactionType.EXPENSE, ExpensePurpose.EVENT, "3"));
        addCashboxTransaction(tx("ct_4", "Gorivo", "Dostava opreme", "6000", Currency.RSD, "117.20", "51.19", "2026-08-14", TransactionType.EXPENSE, ExpensePurpose.GENERAL, null));
        addCashboxTransaction(tx("ct_5", "Smeštaj", "Tim na terenu", "145", Currency.EUR, "1", "145", "2026-08-13", TransactionType.EXPENSE, ExpensePurpose.EVENT, "1"));
        addCashboxTransaction(tx("ct_6", "Ručak", "Ručak za produkciju", "4200", Currency.RSD, "117.20", "35.84", "2026-08-12", TransactionType.EXPENSE, ExpensePurpose.EVENT, "2"));
    }

    private CashboxTransaction tx(String id, String name, String description, String amount, Currency currency,
            String exchangeRate, String amountInEur, String date, TransactionType type, ExpensePurpose purpose, String eventId) {
        return new CashboxTransaction(id, "cashbox_teodora", name, description, new BigDecimal(amount), currency,
                new BigDecimal(exchangeRate), new BigDecimal(amountInEur), LocalDate.parse(date), type, purpose, eventId, null);
    }

    public List<Event> getAllEvents() { return new ArrayList<>(events); }

    private void seedTeam() {
        teamMembers.add(member("tm_1", "Aleksandar Lazić", "+381 63 223960", "aleksandar@email.rs", "Beograd", "265-000000648353-89"));
        teamMembers.add(member("tm_2", "Aleksandra Facepaint", "+381 64 112233", "facepaint@email.rs", "Beograd", ""));
        teamMembers.add(member("tm_3", "Ana Marija", "+381 66 428834", "ana.marija@email.rs", "Beograd", ""));
        teamMembers.add(member("tm_4", "Anastasija Otić", "+381 63 1530120", "anastasija@email.rs", "Novi Sad", ""));
        teamMembers.add(member("tm_5", "Anđela Bojanić", "", "andjela@email.rs", "Bačko Dobro Polje", ""));
        teamMembers.add(member("tm_6", "Anja Atanasković", "+381 60 5588806", "", "Beograd", ""));
        teamMembers.add(member("tm_7", "Bojan Marković", "+381 64 230011", "bojan@email.rs", "Niš", ""));
        teamMembers.add(member("tm_8", "Jelena Petrović", "+381 65 882211", "jelena@email.rs", "Kragujevac", ""));
        teamMembers.add(member("tm_9", "Luka Nikolić", "", "luka@email.rs", "Novi Sad", ""));
        teamMembers.add(member("tm_10", "Marija Ilić", "+381 63 981122", "", "Beograd", ""));
        teamMembers.add(member("tm_11", "Miloš Stanković", "+381 64 776655", "milos@email.rs", "Pančevo", ""));
        teamMembers.add(member("tm_12", "Sara Jovanović", "+381 62 445566", "sara@email.rs", "Beograd", ""));
        teamFees.add(fee("tf_1", "tm_1", "1", "Moderator oba dana", "1600"));
        teamFees.add(fee("tf_2", "tm_1", "3", "EXPO karavan", "800"));
        teamFees.add(fee("tf_3", "tm_1", "4", "Glumac i kviz", "800"));
        teamPayments.add(payment("tp_1", "tm_1", "800")); teamPayments.add(payment("tp_2", "tm_1", "400")); teamPayments.add(payment("tp_3", "tm_1", "400"));
        teamFees.add(fee("tf_4", "tm_5", "3", "Hostesa", "520")); teamPayments.add(payment("tp_4", "tm_5", "400"));
        teamFees.add(fee("tf_5", "tm_3", "2", "Promoter", "300")); teamPayments.add(payment("tp_5", "tm_3", "300"));
        teamFees.add(fee("tf_6", "tm_7", "5", "Tehničar", "450")); teamPayments.add(payment("tp_6", "tm_7", "500"));
        teamFees.add(fee("tf_7", "tm_8", "1", "Animator", "250"));
    }

    private TeamMember member(String id,String name,String phone,String email,String city,String account){return new TeamMember(id,name,phone,email,city,account,"",true);}
    private TeamFee fee(String id,String memberId,String eventId,String description,String amount){return new TeamFee(id,memberId,eventId,description,new BigDecimal(amount),Currency.EUR,LocalDate.of(2026,8,20),"");}
    private TeamPayment payment(String id,String memberId,String amount){return new TeamPayment(id,memberId,new BigDecimal(amount),Currency.EUR,LocalDate.of(2026,8,20),"Bank transfer","");}

    public List<TeamMember> getTeamMembers(){return new ArrayList<>(teamMembers);}
    public TeamMember getTeamMemberById(String id){for(TeamMember member:teamMembers)if(member.getId().equals(id))return member;return null;}
    public TeamMember addTeamMember(TeamMember member){if(member.getId()==null||member.getId().trim().isEmpty())member.setId(nextId("team_member"));teamMembers.add(member);return member;}
    public boolean updateTeamMember(TeamMember updated){for(int i=0;i<teamMembers.size();i++)if(teamMembers.get(i).getId().equals(updated.getId())){teamMembers.set(i,updated);return true;}return false;}
    public List<TeamFee> getFeesForTeamMember(String id){List<TeamFee> result=new ArrayList<>();for(TeamFee fee:teamFees)if(fee.getTeamMemberId().equals(id))result.add(fee);return result;}
    public List<TeamPayment> getPaymentsForTeamMember(String id){List<TeamPayment> result=new ArrayList<>();for(TeamPayment payment:teamPayments)if(payment.getTeamMemberId().equals(id))result.add(payment);return result;}
    public TeamFee addTeamFee(TeamFee fee){if(fee.getId()==null||fee.getId().trim().isEmpty())fee.setId(nextId("team_fee"));teamFees.add(fee);return fee;}
    public boolean updateTeamFee(TeamFee updated){for(int i=0;i<teamFees.size();i++)if(teamFees.get(i).getId().equals(updated.getId())){teamFees.set(i,updated);return true;}return false;}
    public boolean deleteTeamFee(String id){for(int i=0;i<teamFees.size();i++)if(teamFees.get(i).getId().equals(id)){teamFees.remove(i);return true;}return false;}
    public TeamPayment addTeamPayment(TeamPayment payment){if(payment.getId()==null||payment.getId().trim().isEmpty())payment.setId(nextId("team_payment"));teamPayments.add(payment);return payment;}
    public boolean updateTeamPayment(TeamPayment updated){for(int i=0;i<teamPayments.size();i++)if(teamPayments.get(i).getId().equals(updated.getId())){teamPayments.set(i,updated);return true;}return false;}
    public boolean deleteTeamPayment(String id){for(int i=0;i<teamPayments.size();i++)if(teamPayments.get(i).getId().equals(id)){teamPayments.remove(i);return true;}return false;}
    public BigDecimal getTotalFeesForMember(String id){BigDecimal total=BigDecimal.ZERO;for(TeamFee fee:getFeesForTeamMember(id))total=total.add(fee.getAmount());return total;}
    public BigDecimal getTotalPaidForMember(String id){BigDecimal total=BigDecimal.ZERO;for(TeamPayment payment:getPaymentsForTeamMember(id))total=total.add(payment.getAmount());return total;}
    public BigDecimal getDebtForMember(String id){return getTotalFeesForMember(id).subtract(getTotalPaidForMember(id));}
    public BigDecimal getTotalTeamDebt(){BigDecimal total=BigDecimal.ZERO;for(TeamMember member:teamMembers){BigDecimal debt=getDebtForMember(member.getId());if(debt.signum()>0)total=total.add(debt);}return total;}

    public Cashbox getCashboxForUser(String userId) {
        for (Cashbox cashbox : cashboxes) if (cashbox.getUserId().equals(userId)) return cashbox;
        return null;
    }

    public List<CashboxTransaction> getCashboxTransactions(String cashboxId) {
        List<CashboxTransaction> result = new ArrayList<>();
        for (CashboxTransaction transaction : cashboxTransactions)
            if (transaction.getCashboxId().equals(cashboxId)) result.add(transaction);
        result.sort((first, second) -> second.getDate().compareTo(first.getDate()));
        return result;
    }

    public BigDecimal getCashboxTotal(String cashboxId, TransactionType type) {
        BigDecimal total = BigDecimal.ZERO;
        for (CashboxTransaction transaction : cashboxTransactions)
            if (transaction.getCashboxId().equals(cashboxId) && transaction.getTransactionType() == type)
                total = total.add(transaction.getAmountInEur());
        return total;
    }

    public BigDecimal getCashboxBalance(String cashboxId) {
        return getCashboxTotal(cashboxId, TransactionType.INCOME)
                .subtract(getCashboxTotal(cashboxId, TransactionType.EXPENSE));
    }

    public List<Event> getAssignedEventsForUser(String userId) {
        List<Event> result = new ArrayList<>();
        for (EventAssignment assignment : assignments) {
            if (!assignment.getUserId().equals(userId)) continue;
            Event event = getEventById(assignment.getEventId());
            if (event != null && !result.contains(event)) result.add(event);
        }
        return result;
    }

    public CashboxTransaction addCashboxTransaction(CashboxTransaction transaction) {
        if (transaction.getId() == null || transaction.getId().trim().isEmpty()) transaction.setId(nextId("cashbox_tx"));
        cashboxTransactions.add(transaction);
        syncActualBudgetItemForTransaction(transaction);
        return transaction;
    }

    public boolean updateCashboxTransaction(CashboxTransaction updated) {
        for (int i = 0; i < cashboxTransactions.size(); i++) {
            if (cashboxTransactions.get(i).getId().equals(updated.getId())) {
                cashboxTransactions.set(i, updated);
                syncActualBudgetItemForTransaction(updated);
                return true;
            }
        }
        return false;
    }

    public boolean deleteCashboxTransaction(String transactionId) {
        for (int i = 0; i < cashboxTransactions.size(); i++) {
            if (cashboxTransactions.get(i).getId().equals(transactionId)) {
                cashboxTransactions.remove(i);
                removeActualBudgetItemForTransaction(transactionId);
                return true;
            }
        }
        return false;
    }

    public void syncActualBudgetItemForTransaction(CashboxTransaction transaction) {
        removeActualBudgetItemForTransaction(transaction.getId());
        boolean eventExpense = transaction.getTransactionType() == TransactionType.EXPENSE
                && transaction.getExpensePurpose() == ExpensePurpose.EVENT && transaction.getEventId() != null;
        if (!eventExpense) return;
        String description = transaction.getName();
        if (transaction.getDescription() != null && !transaction.getDescription().trim().isEmpty())
            description += " · " + transaction.getDescription().trim();
        String categoryId = transaction.getCategoryId();
        if (categoryId == null || getBudgetCategoryById(categoryId) == null) categoryId = "cat_2";
        budgetItems.add(new BudgetItem(nextId("item"), transaction.getEventId(), BudgetType.ACTUAL,
                categoryId, description, BigDecimal.ONE, BigDecimal.ONE, transaction.getAmountInEur(), "",
                BudgetItemSource.CASHBOX, transaction.getId(), null));
    }

    public int removeActualBudgetItemForTransaction(String transactionId) {
        int removed = 0;
        for (int i = budgetItems.size() - 1; i >= 0; i--) {
            BudgetItem item = budgetItems.get(i);
            if (item.getBudgetType() == BudgetType.ACTUAL && item.getSourceType() == BudgetItemSource.CASHBOX
                    && transactionId.equals(item.getSourceTransactionId())) {
                budgetItems.remove(i);
                removed++;
            }
        }
        return removed;
    }

    public BudgetItem getActualBudgetItemForTransaction(String transactionId) {
        for (BudgetItem item : budgetItems)
            if (item.getBudgetType() == BudgetType.ACTUAL && item.getSourceType() == BudgetItemSource.CASHBOX
                    && transactionId.equals(item.getSourceTransactionId())) return item;
        return null;
    }

    public ScannedDocument createMockScannedDocument(DocumentSource source) {
        String extension = source == DocumentSource.PDF ? ".pdf" : ".jpg";
        String mimeType = source == DocumentSource.PDF ? "application/pdf" : "image/jpeg";
        return new ScannedDocument(null, "receipt_20260824" + extension, "mock://receipt" + extension,
                mimeType, source, LocalDateTime.now());
    }

    public Receipt createMockReceiptDraft() {
        return new Receipt(null, "001234", "NIS Petrol", "123456789", LocalDate.of(2026, 8, 24),
                new BigDecimal("5000"), Currency.RSD, "Mock recognized receipt text",
                ReceiptProcessingStatus.PROCESSED, null);
    }

    public CashboxTransaction saveConfirmedReceiptExpense(ScannedDocument document, Receipt receipt,
            CashboxTransaction transaction) {
        saveScannedDocument(document);
        if (receipt.getId() == null || receipt.getId().trim().isEmpty()) receipt.setId(nextId("receipt"));
        receipt.setScannedDocumentId(document.getId());
        receipt.setProcessingStatus(ReceiptProcessingStatus.CONFIRMED);
        receipts.add(receipt);
        transaction.setReceiptId(receipt.getId());
        return addCashboxTransaction(transaction);
    }

    public ScannedDocument saveScannedDocument(ScannedDocument document) {
        if (document.getId() == null || document.getId().trim().isEmpty()) document.setId(nextId("document"));
        if (getScannedDocumentById(document.getId()) == null) scannedDocuments.add(document);
        return document;
    }

    public Receipt getReceiptById(String id) {
        for (Receipt receipt : receipts) if (receipt.getId().equals(id)) return receipt;
        return null;
    }

    public ScannedDocument getScannedDocumentById(String id) {
        if (id == null) return null;
        for (ScannedDocument document : scannedDocuments) if (document.getId().equals(id)) return document;
        return null;
    }

    public List<Client> getClients() { return new ArrayList<>(clients); }

    public List<Event> getEventsForClient(String clientId) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) if (event.getClientId().equals(clientId)) result.add(event);
        return result;
    }

    public List<Invoice> getInvoicesForClient(String clientId) {
        List<Invoice> result = new ArrayList<>();
        for (Invoice invoice : invoices) if (invoice.getClientId().equals(clientId)) result.add(invoice);
        return result;
    }

    public Invoice addInvoice(Invoice invoice) {
        if (invoice.getId() == null || invoice.getId().trim().isEmpty()) invoice.setId(nextId("invoice"));
        invoices.add(invoice);
        return invoice;
    }

    public BigDecimal getInvoicedTotal(int year, String currency) {
        BigDecimal total = BigDecimal.ZERO;
        for (Invoice invoice : invoices) {
            boolean invoiced = invoice.getStatus() == InvoiceStatus.ISSUED
                    || invoice.getStatus() == InvoiceStatus.PAID
                    || invoice.getStatus() == InvoiceStatus.OVERDUE;
            if (invoiced && invoice.getIssueDate().getYear() == year
                    && currency.equalsIgnoreCase(invoice.getCurrency())) total = total.add(invoice.getAmount());
        }
        return total;
    }

    public BigDecimal getPaidInvoiceTotal(int year, String currency) {
        BigDecimal total = BigDecimal.ZERO;
        for (Invoice invoice : invoices)
            if (invoice.getStatus() == InvoiceStatus.PAID && invoice.getIssueDate().getYear() == year
                    && currency.equalsIgnoreCase(invoice.getCurrency())) total = total.add(invoice.getAmount());
        return total;
    }

    public BigDecimal getTotalForAllBudgets(BudgetType type) {
        BigDecimal total = BigDecimal.ZERO;
        for (BudgetItem item : budgetItems)
            if (item.getBudgetType() == type) total = total.add(item.getTotal());
        return total;
    }

    public User getUserById(String id) {
        for (User user : users) if (user.getId().equals(id)) return user;
        return null;
    }

    public List<User> getUsers() { return new ArrayList<>(users); }

    public User addUser(User user) {
        if (user.getId() == null || user.getId().trim().isEmpty()) user.setId(nextId("user"));
        users.add(user);
        if (getCashboxForUser(user.getId()) == null)
            cashboxes.add(new Cashbox(nextId("cashbox"), user.getId(), Currency.EUR));
        return user;
    }

    public boolean updateUser(User updated) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(updated.getId())) {
                users.set(i, updated);
                return true;
            }
        }
        return false;
    }

    public boolean setUserRole(String userId, UserRole role) {
        User user = getUserById(userId);
        if (user == null || (user.isActive() && user.getRole() == UserRole.ADMIN
                && role != UserRole.ADMIN && isLastActiveAdmin(userId))) return false;
        user.setRole(role);
        return true;
    }

    public boolean setUserActive(String userId, boolean active) {
        User user = getUserById(userId);
        if (user == null || (!active && user.isActive() && user.getRole() == UserRole.ADMIN
                && isLastActiveAdmin(userId))) return false;
        user.setActive(active);
        return true;
    }

    public boolean isLastActiveAdmin(String userId) {
        User target = getUserById(userId);
        if (target == null || !target.isActive() || target.getRole() != UserRole.ADMIN) return false;
        int activeAdmins = 0;
        for (User user : users)
            if (user.isActive() && user.getRole() == UserRole.ADMIN) activeAdmins++;
        return activeAdmins <= 1;
    }

    public boolean emailExists(String email) {
        if (email == null) return false;
        for (User user : users) if (email.trim().equalsIgnoreCase(user.getEmail())) return true;
        return false;
    }

    public List<EventAssignment> getAssignmentsForEvent(String eventId) {
        List<EventAssignment> result = new ArrayList<>();
        for (EventAssignment assignment : assignments)
            if (assignment.getEventId().equals(eventId)) result.add(assignment);
        return result;
    }

    public boolean removeAssignment(String assignmentId) {
        for (int i = 0; i < assignments.size(); i++) {
            EventAssignment assignment = assignments.get(i);
            if (assignment.getId().equals(assignmentId) && !assignment.isOwner()) {
                assignments.remove(i);
                return true;
            }
        }
        return false;
    }

    public List<User> getAvailableUsersForEvent(String eventId) {
        List<User> available = new ArrayList<>();
        for (User user : users) {
            if (!user.isActive()) continue;
            boolean assigned = false;
            for (EventAssignment assignment : assignments)
                if (assignment.getEventId().equals(eventId) && assignment.getUserId().equals(user.getId())) assigned = true;
            if (!assigned) available.add(user);
        }
        return available;
    }

    public EventAssignment addEventAssignment(String eventId, String userId, String roleOnEvent, boolean owner) {
        for (EventAssignment assignment : assignments)
            if (assignment.getEventId().equals(eventId) && assignment.getUserId().equals(userId)) return assignment;
        EventAssignment assignment = new EventAssignment(nextId("assignment"), userId, eventId, roleOnEvent, owner);
        assignments.add(assignment);
        return assignment;
    }

    public Budget getBudgetForEvent(String eventId) {
        for (Budget budget : budgets) if (budget.getEventId().equals(eventId)) return budget;
        Budget budget = new Budget(nextId("budget"), eventId, false, BigDecimal.ZERO);
        budgets.add(budget);
        return budget;
    }

    public List<BudgetCategory> getBudgetCategories() { return new ArrayList<>(budgetCategories); }

    public BudgetCategory getBudgetCategoryById(String id) {
        for (BudgetCategory category : budgetCategories) if (category.getId().equals(id)) return category;
        return null;
    }

    public BudgetCategory addBudgetCategory(String name) {
        for (BudgetCategory category : budgetCategories)
            if (category.getName().equalsIgnoreCase(name.trim())) return category;
        BudgetCategory category = new BudgetCategory(nextId("category"), name.trim());
        budgetCategories.add(category);
        return category;
    }

    public List<BudgetItem> getBudgetItems(String eventId, BudgetType type) {
        List<BudgetItem> result = new ArrayList<>();
        for (BudgetItem item : budgetItems)
            if (item.getEventId().equals(eventId) && item.getBudgetType() == type) result.add(item);
        return result;
    }

    public BudgetItem addBudgetItem(BudgetItem item) {
        if (item.getId() == null || item.getId().trim().isEmpty()) item.setId(nextId("item"));
        budgetItems.add(item);
        return item;
    }

    public void updateBudgetItem(BudgetItem updated) {
        for (int i = 0; i < budgetItems.size(); i++)
            if (budgetItems.get(i).getId().equals(updated.getId())) { budgetItems.set(i, updated); return; }
    }

    public boolean deleteBudgetItem(String itemId) {
        for (int i = 0; i < budgetItems.size(); i++)
            if (budgetItems.get(i).getId().equals(itemId)) { budgetItems.remove(i); return true; }
        return false;
    }

    public BigDecimal getBudgetTotal(String eventId, BudgetType type) {
        BigDecimal total = BigDecimal.ZERO;
        for (BudgetItem item : getBudgetItems(eventId, type)) total = total.add(item.getTotal());
        return total;
    }

    public boolean copyBudgetItemToInternal(String itemId) {
        BudgetItem source = null;
        for (BudgetItem item : budgetItems) if (item.getId().equals(itemId)) source = item;
        if (source == null || source.getBudgetType() != BudgetType.EXTERNAL) return false;
        for (BudgetItem item : getBudgetItems(source.getEventId(), BudgetType.INTERNAL))
            if (source.getId().equals(item.getSourceBudgetItemId())) return false;
        addBudgetItem(new BudgetItem(nextId("item"), source.getEventId(), BudgetType.INTERNAL,
                source.getCategoryId(), source.getDescription(), source.getQuantity(), source.getDays(),
                source.getDailyRate(), source.getNotes(), BudgetItemSource.MANUAL, null, source.getId()));
        return true;
    }

    public int copyAllExternalToInternal(String eventId) {
        int copied = 0;
        List<BudgetItem> external = getBudgetItems(eventId, BudgetType.EXTERNAL);
        for (BudgetItem item : external) if (copyBudgetItemToInternal(item.getId())) copied++;
        return copied;
    }

    private String nextId(String prefix) { return prefix + "_" + (++mockIdCounter); }
}
