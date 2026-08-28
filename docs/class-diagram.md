# Funky Event App — dijagram klasa

Ovaj domenski model prikazuje ceo poslovni tok: klijent naručuje događaj, agencija ga formira, dodeljuje tim, planira budžet, evidentira troškove i račune, fakturiše klijentu i zatvara događaj.

```mermaid
classDiagram
direction LR

class Client {
  +String id
  +String name
  +String taxId
  +String address
  +String email
  +String phone
  +String contactPerson
}

class Event {
  +String id
  +String name
  +EventType type
  +LocalDate startDate
  +LocalDate endDate
  +String location
  +EventStatus status
  +String billingEntity
  +String poNumber
  +String paymentTerms
  +String notes
  +start()
  +complete()
  +cancel()
}

class User {
  +String id
  +String firstName
  +String lastName
  +String email
  +UserRole role
  +boolean active
}

class EventAssignment {
  +String id
  +String roleOnEvent
  +boolean owner
}

class TeamMember {
  +String id
  +String fullName
  +String phone
  +String email
  +String city
  +String bankAccount
  +boolean active
}

class TeamFee {
  +String id
  +String description
  +BigDecimal amount
  +Currency currency
  +LocalDate date
}

class TeamPayment {
  +String id
  +BigDecimal amount
  +Currency currency
  +LocalDate paymentDate
  +PaymentMethod paymentMethod
}

class Budget {
  +String id
  +boolean includeVat
  +BigDecimal discountPercentage
  +calculateTotal()
}

class BudgetItem {
  +String id
  +BudgetType budgetType
  +String description
  +BigDecimal quantity
  +BigDecimal days
  +BigDecimal dailyRate
  +BudgetItemSource sourceType
  +getTotal()
}

class BudgetCategory {
  +String id
  +String name
}

class Cashbox {
  +String id
  +Currency displayCurrency
  +getBalance()
}

class CashboxTransaction {
  +String id
  +String name
  +BigDecimal amount
  +Currency currency
  +BigDecimal exchangeRate
  +BigDecimal amountInEur
  +LocalDate date
  +TransactionType transactionType
  +ExpensePurpose expensePurpose
}

class Receipt {
  +String id
  +String receiptNumber
  +String seller
  +LocalDate issueDate
  +BigDecimal totalAmount
  +Currency currency
  +ReceiptProcessingStatus processingStatus
}

class ScannedDocument {
  +String id
  +String fileName
  +String fileUri
  +String mimeType
  +DocumentSource source
  +LocalDateTime addedAt
}

class Invoice {
  +String id
  +String invoiceNumber
  +LocalDate issueDate
  +LocalDate dueDate
  +BigDecimal amount
  +Currency currency
  +InvoiceStatus status
  +String pdfUri
  +issue()
  +markPaid()
  +cancel()
}

class EventType {
  <<enumeration>>
  EVENT
  CAMPAIGN
}
class EventStatus {
  <<enumeration>>
  PLANNED
  IN_PROGRESS
  COMPLETED
  CANCELLED
}
class UserRole {
  <<enumeration>>
  ADMIN
  MANAGER
  COORDINATOR
}
class BudgetType {
  <<enumeration>>
  EXTERNAL
  INTERNAL
  ACTUAL
}
class InvoiceStatus {
  <<enumeration>>
  DRAFT
  ISSUED
  PAID
  OVERDUE
  CANCELLED
}

Client "1" --> "0..*" Event : naručuje

Event "1" *-- "1" Budget : finansijski plan
Budget "1" *-- "1..*" BudgetItem : stavke
BudgetCategory "1" <-- "0..*" BudgetItem : kategorija

Event "1" *-- "1..*" EventAssignment : organizuje tim
User "1" --> "0..*" EventAssignment : zadužen
Event "1" --> "0..*" TeamFee : angažovanje
TeamMember "1" --> "0..*" TeamFee : ostvaruje honorar
TeamMember "1" --> "0..*" TeamPayment : prima isplatu

User "1" *-- "1" Cashbox : vodi
Cashbox "1" *-- "0..*" CashboxTransaction : evidentira
Event "0..1" <-- "0..*" CashboxTransaction : trošak ili prihod
BudgetItem "0..1" <-- "0..*" CashboxTransaction : realizuje stavku
CashboxTransaction "0..1" --> "0..1" Receipt : dokaz troška
Receipt "1" --> "1" ScannedDocument : nastaje iz

Event "1" --> "0..*" Invoice : fakturiše se
Client "1" --> "0..*" Invoice : primalac
```

## Poslovna pravila koja dijagram podrazumeva

1. Klijent može naručiti više događaja, ali svaki događaj pripada tačno jednom klijentu.
2. Svaki događaj ima jedan budžet i najmanje jednu budžetsku stavku.
3. Organizacija događaja se prati kroz zaduženja korisnika agencije.
4. Trošak događaja se evidentira transakcijom, može imati račun i može realizovati planiranu budžetsku stavku.
5. Faktura povezuje konkretan događaj i klijenta koji ga je naručio.
6. Događaj se zatvara kada je realizovan, finansije evidentirane i fakturisanje rešeno.

## Usklađenost sa trenutnim kodom

Već postoje klase `Client`, `Event`, `User`, `EventAssignment`, `TeamMember`, `TeamFee`, `TeamPayment`, `Budget`, `BudgetItem`, `BudgetCategory`, `Cashbox`, `CashboxTransaction`, `Receipt`, `ScannedDocument` i `Invoice`.

Veza klijenta i događaja već postoji kroz `Event.clientId`. Dijagram ne uvodi klase koje nisu potrebne aplikaciji; jedina preporučena izmena je detaljniji `EventStatus` ako se kasnije bude pratilo više faza realizacije.
