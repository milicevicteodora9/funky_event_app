package com.example.funkyeventapp.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.TeamFee;
import com.example.funkyeventapp.models.TeamMember;
import com.example.funkyeventapp.models.TeamPayment;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Firestore access for non-authenticated team collaborators and their finances. */
public final class TeamRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception error);
    }

    public static final class TeamOverview {
        private final List<TeamMember> members;
        private final Map<String, BigDecimal> debtByMember;
        private final BigDecimal totalDebt;

        TeamOverview(List<TeamMember> members, Map<String, BigDecimal> debtByMember,
                     BigDecimal totalDebt) {
            this.members = Collections.unmodifiableList(new ArrayList<>(members));
            this.debtByMember = Collections.unmodifiableMap(new HashMap<>(debtByMember));
            this.totalDebt = totalDebt;
        }

        public List<TeamMember> getMembers() { return members; }
        public Map<String, BigDecimal> getDebtByMember() { return debtByMember; }
        public BigDecimal getTotalDebt() { return totalDebt; }
    }

    public static final class MemberDetails {
        private final TeamMember member;
        private final List<TeamFee> fees;
        private final List<TeamPayment> payments;
        private final BigDecimal totalFees;
        private final BigDecimal totalPayments;

        MemberDetails(TeamMember member, List<TeamFee> fees, List<TeamPayment> payments) {
            this.member = member;
            this.fees = Collections.unmodifiableList(new ArrayList<>(fees));
            this.payments = Collections.unmodifiableList(new ArrayList<>(payments));
            this.totalFees = sumFees(fees);
            this.totalPayments = sumPayments(payments);
        }

        public TeamMember getMember() { return member; }
        public List<TeamFee> getFees() { return fees; }
        public List<TeamPayment> getPayments() { return payments; }
        public BigDecimal getTotalFees() { return totalFees; }
        public BigDecimal getTotalPayments() { return totalPayments; }
        public BigDecimal getDebt() { return totalFees.subtract(totalPayments); }
    }

    private static final class TeamFinance {
        private final String memberId;
        private final BigDecimal debt;
        TeamFinance(String memberId, BigDecimal debt) {
            this.memberId = memberId;
            this.debt = debt;
        }
    }

    private static final TeamRepository INSTANCE = new TeamRepository();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private TeamRepository() { }
    public static TeamRepository getInstance() { return INSTANCE; }

    public void getTeamOverview(@NonNull Callback<TeamOverview> callback) {
        firestore.collection("teamMembers").get()
                .addOnSuccessListener(snapshot -> {
                    try {
                        List<TeamMember> members = mapMembers(snapshot);
                        List<Task<TeamFinance>> financeTasks = new ArrayList<>();
                        for (TeamMember member : members) {
                            financeTasks.add(loadFinance(member.getId()));
                        }
                        Tasks.whenAllSuccess(financeTasks)
                                .addOnSuccessListener(results -> {
                                    Map<String, BigDecimal> debtByMember = new HashMap<>();
                                    BigDecimal totalDebt = BigDecimal.ZERO;
                                    for (Object value : results) {
                                        TeamFinance finance = (TeamFinance) value;
                                        debtByMember.put(finance.memberId, finance.debt);
                                        if (finance.debt.signum() > 0) {
                                            totalDebt = totalDebt.add(finance.debt);
                                        }
                                    }
                                    callback.onSuccess(new TeamOverview(members, debtByMember, totalDebt));
                                })
                                .addOnFailureListener(callback::onError);
                    } catch (Exception error) {
                        callback.onError(error);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void getMemberDetails(@NonNull String memberId,
                                 @NonNull Callback<MemberDetails> callback) {
        DocumentReference member = firestore.collection("teamMembers").document(memberId);
        Task<DocumentSnapshot> memberTask = member.get();
        Task<QuerySnapshot> feesTask = member.collection("fees").get();
        Task<QuerySnapshot> paymentsTask = member.collection("payments").get();
        Tasks.whenAllSuccess(memberTask, feesTask, paymentsTask)
                .addOnSuccessListener(results -> {
                    try {
                        DocumentSnapshot memberDocument = (DocumentSnapshot) results.get(0);
                        if (!memberDocument.exists()) {
                            callback.onSuccess(new MemberDetails(null,
                                    Collections.emptyList(), Collections.emptyList()));
                            return;
                        }
                        callback.onSuccess(new MemberDetails(mapMember(memberDocument),
                                mapFees((QuerySnapshot) results.get(1), memberId),
                                mapPayments((QuerySnapshot) results.get(2), memberId)));
                    } catch (Exception error) {
                        callback.onError(error);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public void addTeamMember(@NonNull TeamMember member, @NonNull Callback<TeamMember> callback) {
        if (!validMember(member)) {
            callback.onError(new IllegalArgumentException("Team member name is required"));
            return;
        }
        DocumentReference document = firestore.collection("teamMembers").document();
        member.setId(document.getId());
        Map<String, Object> data = memberData(member);
        data.put("createdAt", FieldValue.serverTimestamp());
        document.set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(member))
                .addOnFailureListener(error -> {
                    member.setId(null);
                    callback.onError(error);
                });
    }

    public void updateTeamMember(@NonNull TeamMember member,
                                 @NonNull Callback<TeamMember> callback) {
        if (!validMember(member) || empty(member.getId())) {
            callback.onError(new IllegalArgumentException("Existing team member is required"));
            return;
        }
        firestore.collection("teamMembers").document(member.getId()).update(memberData(member))
                .addOnSuccessListener(unused -> callback.onSuccess(member))
                .addOnFailureListener(callback::onError);
    }

    public void addTeamFee(@NonNull TeamFee fee, @NonNull Callback<TeamFee> callback) {
        if (!validFee(fee)) {
            callback.onError(new IllegalArgumentException("Valid fee is required"));
            return;
        }
        DocumentReference document = firestore.collection("teamMembers")
                .document(fee.getTeamMemberId()).collection("fees").document();
        fee.setId(document.getId());
        Map<String, Object> data = feeData(fee);
        data.put("createdAt", FieldValue.serverTimestamp());
        document.set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(fee))
                .addOnFailureListener(error -> {
                    fee.setId(null);
                    callback.onError(error);
                });
    }

    public void updateTeamFee(@NonNull TeamFee fee, @NonNull Callback<TeamFee> callback) {
        if (!validFee(fee) || empty(fee.getId())) {
            callback.onError(new IllegalArgumentException("Existing fee is required"));
            return;
        }
        firestore.collection("teamMembers").document(fee.getTeamMemberId())
                .collection("fees").document(fee.getId()).update(feeData(fee))
                .addOnSuccessListener(unused -> callback.onSuccess(fee))
                .addOnFailureListener(callback::onError);
    }

    public void deleteTeamFee(@NonNull TeamFee fee, @NonNull Callback<Void> callback) {
        if (empty(fee.getTeamMemberId()) || empty(fee.getId())) {
            callback.onError(new IllegalArgumentException("Existing fee is required"));
            return;
        }
        firestore.collection("teamMembers").document(fee.getTeamMemberId())
                .collection("fees").document(fee.getId()).delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void addTeamPayment(@NonNull TeamPayment payment,
                               @NonNull Callback<TeamPayment> callback) {
        if (!validPayment(payment)) {
            callback.onError(new IllegalArgumentException("Valid payment is required"));
            return;
        }
        DocumentReference document = firestore.collection("teamMembers")
                .document(payment.getTeamMemberId()).collection("payments").document();
        payment.setId(document.getId());
        Map<String, Object> data = paymentData(payment);
        data.put("createdAt", FieldValue.serverTimestamp());
        document.set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(payment))
                .addOnFailureListener(error -> {
                    payment.setId(null);
                    callback.onError(error);
                });
    }

    public void updateTeamPayment(@NonNull TeamPayment payment,
                                   @NonNull Callback<TeamPayment> callback) {
        if (!validPayment(payment) || empty(payment.getId())) {
            callback.onError(new IllegalArgumentException("Existing payment is required"));
            return;
        }
        firestore.collection("teamMembers").document(payment.getTeamMemberId())
                .collection("payments").document(payment.getId()).update(paymentData(payment))
                .addOnSuccessListener(unused -> callback.onSuccess(payment))
                .addOnFailureListener(callback::onError);
    }

    public void deleteTeamPayment(@NonNull TeamPayment payment,
                                   @NonNull Callback<Void> callback) {
        if (empty(payment.getTeamMemberId()) || empty(payment.getId())) {
            callback.onError(new IllegalArgumentException("Existing payment is required"));
            return;
        }
        firestore.collection("teamMembers").document(payment.getTeamMemberId())
                .collection("payments").document(payment.getId()).delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    private Task<TeamFinance> loadFinance(String memberId) {
        DocumentReference member = firestore.collection("teamMembers").document(memberId);
        Task<QuerySnapshot> fees = member.collection("fees").get();
        Task<QuerySnapshot> payments = member.collection("payments").get();
        return Tasks.whenAllSuccess(fees, payments).continueWith(result -> {
            BigDecimal totalFees = sumFees(mapFees((QuerySnapshot) result.getResult().get(0), memberId));
            BigDecimal totalPayments = sumPayments(mapPayments(
                    (QuerySnapshot) result.getResult().get(1), memberId));
            return new TeamFinance(memberId, totalFees.subtract(totalPayments));
        });
    }

    private List<TeamMember> mapMembers(QuerySnapshot snapshot) {
        List<TeamMember> members = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) members.add(mapMember(document));
        members.sort(Comparator.comparing(TeamMember::getFullName, String.CASE_INSENSITIVE_ORDER));
        return members;
    }

    private TeamMember mapMember(DocumentSnapshot document) {
        String firstName = optionalString(document, "firstName");
        String lastName = optionalString(document, "lastName");
        TeamMember member;
        if (firstName.isEmpty() && lastName.isEmpty()) {
            member = new TeamMember(document.getId(), optionalString(document, "fullName"),
                    optionalString(document, "phone"), optionalString(document, "email"),
                    optionalString(document, "city"), optionalString(document, "bankAccount"),
                    optionalString(document, "notes"), optionalBoolean(document, "active", true));
        } else {
            member = new TeamMember(document.getId(), firstName, lastName,
                    optionalString(document, "phone"), optionalString(document, "email"),
                    optionalString(document, "city"), optionalString(document, "bankAccount"),
                    optionalString(document, "notes"), optionalBoolean(document, "active", true));
        }
        return member;
    }

    private List<TeamFee> mapFees(QuerySnapshot snapshot, String memberId) {
        List<TeamFee> fees = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            TeamFee fee = new TeamFee(document.getId(), memberId,
                    nullableString(document, "eventId"), optionalString(document, "description"),
                    requiredDecimal(document, "amount"), optionalCurrency(document),
                    requiredDate(document, "date"), optionalString(document, "notes"));
            fee.setCreatedAt(optionalDateTime(document, "createdAt"));
            fees.add(fee);
        }
        fees.sort((first, second) -> second.getDate().compareTo(first.getDate()));
        return fees;
    }

    private List<TeamPayment> mapPayments(QuerySnapshot snapshot, String memberId) {
        List<TeamPayment> payments = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            TeamPayment payment = new TeamPayment(document.getId(), memberId,
                    optionalString(document, "description"), requiredDecimal(document, "amount"),
                    optionalCurrency(document), requiredDate(document, "date"),
                    optionalString(document, "paymentMethod"), optionalString(document, "notes"));
            payment.setCreatedAt(optionalDateTime(document, "createdAt"));
            payments.add(payment);
        }
        payments.sort((first, second) -> second.getPaymentDate().compareTo(first.getPaymentDate()));
        return payments;
    }

    private Map<String, Object> memberData(TeamMember member) {
        Map<String, Object> data = new HashMap<>();
        data.put("firstName", clean(member.getFirstName()));
        data.put("lastName", clean(member.getLastName()));
        data.put("phone", clean(member.getPhone()));
        data.put("email", clean(member.getEmail()));
        data.put("city", clean(member.getCity()));
        data.put("bankAccount", clean(member.getBankAccount()));
        data.put("notes", clean(member.getNotes()));
        data.put("active", member.isActive());
        return data;
    }

    private Map<String, Object> feeData(TeamFee fee) {
        Map<String, Object> data = new HashMap<>();
        data.put("teamMemberId", fee.getTeamMemberId());
        data.put("amount", fee.getAmount().doubleValue());
        data.put("description", clean(fee.getDescription()));
        data.put("date", toTimestamp(fee.getDate()));
        data.put("eventId", empty(fee.getEventId()) ? null : fee.getEventId());
        data.put("currency", fee.getCurrency().name());
        data.put("notes", clean(fee.getNotes()));
        return data;
    }

    private Map<String, Object> paymentData(TeamPayment payment) {
        Map<String, Object> data = new HashMap<>();
        data.put("teamMemberId", payment.getTeamMemberId());
        data.put("amount", payment.getAmount().doubleValue());
        data.put("description", clean(payment.getDescription()));
        data.put("date", toTimestamp(payment.getPaymentDate()));
        data.put("currency", payment.getCurrency().name());
        data.put("paymentMethod", clean(payment.getPaymentMethod()));
        data.put("notes", clean(payment.getNotes()));
        return data;
    }

    private boolean validMember(TeamMember member) {
        return !empty(member.getFirstName()) || !empty(member.getLastName());
    }

    private boolean validFee(TeamFee fee) {
        return !empty(fee.getTeamMemberId()) && fee.getAmount() != null
                && fee.getAmount().signum() > 0 && !empty(fee.getDescription())
                && fee.getDate() != null && fee.getCurrency() != null;
    }

    private boolean validPayment(TeamPayment payment) {
        return !empty(payment.getTeamMemberId()) && payment.getAmount() != null
                && payment.getAmount().signum() > 0 && payment.getPaymentDate() != null
                && payment.getCurrency() != null;
    }

    private static BigDecimal sumFees(List<TeamFee> fees) {
        BigDecimal total = BigDecimal.ZERO;
        for (TeamFee fee : fees) total = total.add(fee.getAmount());
        return total;
    }

    private static BigDecimal sumPayments(List<TeamPayment> payments) {
        BigDecimal total = BigDecimal.ZERO;
        for (TeamPayment payment : payments) total = total.add(payment.getAmount());
        return total;
    }

    private Timestamp toTimestamp(LocalDate date) {
        return new Timestamp(Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant()));
    }

    private LocalDate requiredDate(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value instanceof Timestamp) return toLocalDate(((Timestamp) value).toDate());
        if (value instanceof Date) return toLocalDate((Date) value);
        if (value instanceof String) return LocalDate.parse((String) value);
        throw new IllegalStateException("Missing or invalid " + field);
    }

    private LocalDateTime optionalDateTime(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value == null) return null;
        if (value instanceof Timestamp) return toLocalDateTime(((Timestamp) value).toDate());
        if (value instanceof Date) return toLocalDateTime((Date) value);
        if (value instanceof String) return LocalDateTime.parse((String) value);
        throw new IllegalStateException("Invalid " + field);
    }

    private BigDecimal requiredDecimal(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (!(value instanceof Number) && !(value instanceof String)) {
            throw new IllegalStateException("Missing or invalid " + field);
        }
        return new BigDecimal(value.toString());
    }

    private Currency optionalCurrency(DocumentSnapshot document) {
        String value = optionalString(document, "currency");
        return value.isEmpty() ? Currency.EUR : Currency.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private String optionalString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        return value == null ? "" : value.trim();
    }

    private String nullableString(DocumentSnapshot document, String field) {
        String value = optionalString(document, field);
        return value.isEmpty() ? null : value;
    }

    private boolean optionalBoolean(DocumentSnapshot document, String field, boolean fallback) {
        Boolean value = document.getBoolean(field);
        return value == null ? fallback : value;
    }

    private LocalDate toLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private boolean empty(String value) { return value == null || value.trim().isEmpty(); }
}
