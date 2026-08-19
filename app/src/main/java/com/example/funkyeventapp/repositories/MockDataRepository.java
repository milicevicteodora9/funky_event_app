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

import java.time.LocalDate;
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
    private long mockIdCounter = 1000;

    private MockDataRepository() {
        seedClients();
        seedUsers();
        seedEvents();
        seedAssignments();
        seedBudgets();
        seedInvoices();
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
        users.add(new User("user_bojana", "Bojana", "Mumović", "bojana@funkybusiness.rs", UserRole.MANAGER, true));
        users.add(new User("user_valentina", "Valentina", "Gajić", "valentina@funkybusiness.rs", UserRole.COORDINATOR, true));
        users.add(new User("user_nikola", "Nikola", "Simić", "nikola@funkybusiness.rs", UserRole.COORDINATOR, true));
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

    public User getUserById(String id) {
        for (User user : users) if (user.getId().equals(id)) return user;
        return null;
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
