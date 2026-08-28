from pathlib import Path

from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A3, landscape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "funky-event-app-dijagram-klasa.pdf"

PAGE_W, PAGE_H = landscape(A3)
NAVY = HexColor("#17324D")
BLUE = HexColor("#2E6F95")
LIGHT_BLUE = HexColor("#EAF4FA")
GREEN = HexColor("#3A7D6C")
LIGHT_GREEN = HexColor("#EAF6F1")
ORANGE = HexColor("#B86B2B")
LIGHT_ORANGE = HexColor("#FFF2E8")
PURPLE = HexColor("#76538B")
LIGHT_PURPLE = HexColor("#F3EDF7")
GRAY = HexColor("#5F6B76")
LIGHT_GRAY = HexColor("#F5F7F9")
LINE = HexColor("#73808C")


pdfmetrics.registerFont(TTFont("Arial", r"C:\Windows\Fonts\arial.ttf"))
pdfmetrics.registerFont(TTFont("Arial-Bold", r"C:\Windows\Fonts\arialbd.ttf"))


CLASSES = {
    "Client": ["String id", "String name", "String taxId", "String contactPerson", "String email", "String phone"],
    "Event": ["String id", "String name", "EventType type", "LocalDate startDate / endDate", "String location", "EventStatus status", "String clientId", "String paymentTerms"],
    "EventAssignment": ["String id", "String eventId", "String userId", "String roleOnEvent", "boolean owner"],
    "User": ["String id", "String firstName / lastName", "String email", "UserRole role", "boolean active"],
    "Budget": ["String id", "String eventId", "boolean includeVat", "BigDecimal discountPercentage"],
    "BudgetItem": ["String id", "BudgetType budgetType", "String description", "BigDecimal quantity / days", "BigDecimal dailyRate", "+ getTotal()"],
    "BudgetCategory": ["String id", "String name"],
    "Invoice": ["String id", "String eventId / clientId", "String invoiceNumber", "LocalDate issueDate / dueDate", "BigDecimal amount", "InvoiceStatus status"],
    "Cashbox": ["String id", "String userId", "Currency displayCurrency"],
    "CashboxTransaction": ["String id", "String cashboxId / eventId", "BigDecimal amount", "Currency currency", "TransactionType transactionType", "ExpensePurpose purpose"],
    "Receipt": ["String id", "String receiptNumber", "String seller", "LocalDate issueDate", "BigDecimal totalAmount", "ReceiptProcessingStatus status"],
    "ScannedDocument": ["String id", "String fileName / fileUri", "String mimeType", "DocumentSource source", "LocalDateTime addedAt"],
    "TeamMember": ["String id", "String fullName", "String phone / email", "String bankAccount", "boolean active"],
    "TeamFee": ["String id", "String teamMemberId / eventId", "String description", "BigDecimal amount", "Currency currency", "LocalDate date"],
    "TeamPayment": ["String id", "String teamMemberId", "BigDecimal amount", "Currency currency", "LocalDate paymentDate", "PaymentMethod method"],
}


def text_width(value, font="Arial", size=8):
    return pdfmetrics.stringWidth(value, font, size)


def draw_header(c, page_no, subtitle):
    c.setFillColor(NAVY)
    c.rect(0, PAGE_H - 76, PAGE_W, 76, fill=1, stroke=0)
    c.setFillColor(white)
    c.setFont("Arial-Bold", 22)
    c.drawString(34, PAGE_H - 37, "Funky Event App - UML dijagram klasa")
    c.setFont("Arial", 10)
    c.drawString(35, PAGE_H - 57, subtitle)
    c.setFont("Arial", 8)
    c.drawRightString(PAGE_W - 34, 20, f"Strana {page_no}")


def box_height(fields):
    return 38 + len(fields) * 14


def draw_class(c, name, x, y, w, fill, accent):
    fields = CLASSES[name]
    h = box_height(fields)
    c.setFillColor(fill)
    c.setStrokeColor(accent)
    c.setLineWidth(1.2)
    c.roundRect(x, y, w, h, 7, fill=1, stroke=1)
    c.setFillColor(accent)
    c.roundRect(x, y + h - 27, w, 27, 7, fill=1, stroke=0)
    c.rect(x, y + h - 27, w, 8, fill=1, stroke=0)
    c.setFillColor(white)
    c.setFont("Arial-Bold", 10)
    c.drawCentredString(x + w / 2, y + h - 18, name)
    c.setStrokeColor(accent)
    c.line(x, y + h - 28, x + w, y + h - 28)
    c.setFillColor(NAVY)
    c.setFont("Arial", 7.6)
    ty = y + h - 41
    for field in fields:
        prefix = field if field.startswith("+") else f"- {field}"
        c.drawString(x + 8, ty, prefix)
        ty -= 14
    return (x, y, w, h)


def port(box, side):
    x, y, w, h = box
    return {
        "left": (x, y + h / 2),
        "right": (x + w, y + h / 2),
        "top": (x + w / 2, y + h),
        "bottom": (x + w / 2, y),
    }[side]


def relation(c, a, side_a, b, side_b, label, mult_a="1", mult_b="0..*", dashed=False):
    ax, ay = port(a, side_a)
    bx, by = port(b, side_b)
    c.saveState()
    c.setStrokeColor(LINE)
    c.setFillColor(GRAY)
    c.setLineWidth(1)
    if dashed:
        c.setDash(4, 3)
    if side_a in ("left", "right"):
        mid = (ax + bx) / 2
        points = [(ax, ay), (mid, ay), (mid, by), (bx, by)]
    else:
        mid = (ay + by) / 2
        points = [(ax, ay), (ax, mid), (bx, mid), (bx, by)]
    path = c.beginPath()
    path.moveTo(*points[0])
    for px, py in points[1:]:
        path.lineTo(px, py)
    c.drawPath(path, stroke=1, fill=0)
    c.setFont("Arial", 7)
    c.drawString(ax + 3, ay + 3, mult_a)
    c.drawRightString(bx - 3, by + 3, mult_b)
    lx, ly = points[len(points) // 2]
    label_w = text_width(label, size=7) + 8
    c.setFillColor(white)
    c.rect(lx - label_w / 2, ly - 5, label_w, 11, fill=1, stroke=0)
    c.setFillColor(GRAY)
    c.drawCentredString(lx, ly - 2, label)
    c.restoreState()


def draw_page_one(c):
    draw_header(c, 1, "Osnovni domen: klijent direktno naručuje događaj, a agencija vodi organizaciju, budžet i fakturisanje.")
    c.setFont("Arial-Bold", 9)
    c.setFillColor(BLUE)
    c.drawString(34, PAGE_H - 98, "KLIJENT I FAKTURISANJE")
    c.setFillColor(GREEN)
    c.drawString(316, PAGE_H - 98, "DOGAĐAJ I ORGANIZACIJA")
    c.setFillColor(ORANGE)
    c.drawString(878, PAGE_H - 98, "BUDŽET")

    boxes = {}
    w = 210
    boxes["Client"] = draw_class(c, "Client", 34, 545, w, LIGHT_BLUE, BLUE)
    boxes["Invoice"] = draw_class(c, "Invoice", 34, 260, w, LIGHT_ORANGE, ORANGE)
    boxes["Event"] = draw_class(c, "Event", 316, 520, w, LIGHT_BLUE, BLUE)
    boxes["EventAssignment"] = draw_class(c, "EventAssignment", 598, 540, w, LIGHT_GREEN, GREEN)
    boxes["User"] = draw_class(c, "User", 880, 548, w, LIGHT_GREEN, GREEN)
    boxes["Budget"] = draw_class(c, "Budget", 316, 260, w, LIGHT_ORANGE, ORANGE)
    boxes["BudgetItem"] = draw_class(c, "BudgetItem", 598, 230, w, LIGHT_ORANGE, ORANGE)
    boxes["BudgetCategory"] = draw_class(c, "BudgetCategory", 880, 275, w, LIGHT_ORANGE, ORANGE)

    relation(c, boxes["Client"], "right", boxes["Event"], "left", "naručuje", "1", "0..*")
    relation(c, boxes["Client"], "bottom", boxes["Invoice"], "top", "prima fakture", "1", "0..*")
    relation(c, boxes["Event"], "left", boxes["Invoice"], "right", "fakturiše se", "1", "0..*")
    relation(c, boxes["Event"], "right", boxes["EventAssignment"], "left", "organizuje tim", "1", "1..*")
    relation(c, boxes["EventAssignment"], "right", boxes["User"], "left", "zadužen", "0..*", "1")
    relation(c, boxes["Event"], "bottom", boxes["Budget"], "top", "ima plan", "1", "1")
    relation(c, boxes["Budget"], "right", boxes["BudgetItem"], "left", "sadrži", "1", "1..*")
    relation(c, boxes["BudgetItem"], "right", boxes["BudgetCategory"], "left", "kategorija", "0..*", "1")


def draw_page_two_operations(c):
    draw_header(c, 2, "Operativni domen: blagajna, dokumentacija troškova i angažovanje spoljnog tima.")
    c.setFont("Arial-Bold", 9)
    c.setFillColor(GREEN)
    c.drawString(34, PAGE_H - 98, "BLAGAJNA I TROŠKOVI")
    c.setFillColor(PURPLE)
    c.drawString(650, PAGE_H - 98, "DOKUMENTACIJA")
    c.drawString(34, 390, "ANGAŽOVANI TIM")

    boxes = {}
    w = 202
    boxes["User"] = draw_class(c, "User", 34, 555, w, LIGHT_GREEN, GREEN)
    boxes["Cashbox"] = draw_class(c, "Cashbox", 282, 575, w, LIGHT_GREEN, GREEN)
    boxes["CashboxTransaction"] = draw_class(c, "CashboxTransaction", 530, 535, w, LIGHT_ORANGE, ORANGE)
    boxes["Receipt"] = draw_class(c, "Receipt", 778, 535, w, LIGHT_PURPLE, PURPLE)
    boxes["ScannedDocument"] = draw_class(c, "ScannedDocument", 1026, 548, 130, LIGHT_GRAY, GRAY)

    boxes["Event"] = draw_class(c, "Event", 34, 105, w, LIGHT_BLUE, BLUE)
    boxes["TeamFee"] = draw_class(c, "TeamFee", 282, 130, w, LIGHT_PURPLE, PURPLE)
    boxes["TeamMember"] = draw_class(c, "TeamMember", 530, 140, w, LIGHT_PURPLE, PURPLE)
    boxes["TeamPayment"] = draw_class(c, "TeamPayment", 778, 125, w, LIGHT_PURPLE, PURPLE)

    relation(c, boxes["User"], "right", boxes["Cashbox"], "left", "vodi", "1", "1")
    relation(c, boxes["Cashbox"], "right", boxes["CashboxTransaction"], "left", "evidentira", "1", "0..*")
    relation(c, boxes["CashboxTransaction"], "right", boxes["Receipt"], "left", "dokaz troška", "0..1", "0..1")
    relation(c, boxes["Receipt"], "right", boxes["ScannedDocument"], "left", "nastaje iz", "1", "1")
    relation(c, boxes["Event"], "right", boxes["TeamFee"], "left", "angažovanje", "1", "0..*")
    relation(c, boxes["TeamFee"], "right", boxes["TeamMember"], "left", "ostvaruje honorar", "0..*", "1")
    relation(c, boxes["TeamMember"], "right", boxes["TeamPayment"], "left", "prima isplate", "1", "0..*")


def enum_box(c, title, values, x, y, w=210):
    h = 34 + len(values) * 16
    c.setFillColor(LIGHT_GRAY)
    c.setStrokeColor(LINE)
    c.roundRect(x, y, w, h, 7, fill=1, stroke=1)
    c.setFillColor(NAVY)
    c.setFont("Arial-Bold", 10)
    c.drawString(x + 10, y + h - 20, f"<<enumeration>> {title}")
    c.setFont("Arial", 8)
    ty = y + h - 38
    for value in values:
        c.drawString(x + 12, ty, value)
        ty -= 16


def draw_page_three(c):
    draw_header(c, 3, "Enumeracije i poslovna pravila koja dopunjuju glavni dijagram.")
    enum_box(c, "EventType", ["EVENT", "CAMPAIGN"], 34, 602)
    enum_box(c, "EventStatus", ["CURRENT", "COMPLETED"], 272, 602)
    enum_box(c, "UserRole", ["ADMIN", "MANAGER", "COORDINATOR"], 510, 586)
    enum_box(c, "BudgetType", ["EXTERNAL", "INTERNAL", "ACTUAL"], 748, 586)
    enum_box(c, "InvoiceStatus", ["DRAFT", "ISSUED", "PAID", "OVERDUE", "CANCELLED"], 986, 554, 170)

    enum_box(c, "Currency", ["EUR", "RSD", "AED", "USD"], 34, 410)
    enum_box(c, "TransactionType", ["INCOME", "EXPENSE"], 272, 442)
    enum_box(c, "ExpensePurpose", ["GENERAL", "EVENT"], 510, 442)
    enum_box(c, "ReceiptProcessingStatus", ["NEW", "PROCESSED", "CONFIRMED", "ERROR"], 748, 410)
    enum_box(c, "PaymentMethod", ["CASH", "CARD", "BANK_TRANSFER", "OTHER"], 986, 410, 170)

    c.setFillColor(NAVY)
    c.setFont("Arial-Bold", 14)
    c.drawString(34, 344, "Poslovna pravila")
    rules = [
        "1. Klijent može naručiti više događaja, a svaki događaj pripada tačno jednom klijentu.",
        "2. Svaki događaj ima jedan budžet i jednu ili više budžetskih stavki.",
        "3. Korisnici agencije se događaju dodeljuju preko klase EventAssignment.",
        "4. Trošak ili prihod se evidentira kroz CashboxTransaction i po potrebi vezuje za događaj.",
        "5. Račun predstavlja dokaz transakcije, a ScannedDocument čuva izvorni fajl dokumenta.",
        "6. Faktura povezuje događaj sa klijentom kome se usluga naplaćuje.",
        "7. Honorari članova tima vezani su za događaj; njihove isplate vode se odvojeno.",
    ]
    c.setFont("Arial", 10)
    c.setFillColor(GRAY)
    y = 316
    for rule in rules:
        c.drawString(44, y, rule)
        y -= 27

    c.setFillColor(LIGHT_BLUE)
    c.setStrokeColor(BLUE)
    c.roundRect(34, 72, PAGE_W - 68, 55, 7, fill=1, stroke=1)
    c.setFillColor(NAVY)
    c.setFont("Arial-Bold", 10)
    c.drawString(48, 105, "Napomena o usklađenosti")
    c.setFont("Arial", 9)
    c.drawString(48, 87, "Model je zasnovan na postojećim Java klasama i direktnoj vezi Client - Event preko polja Event.clientId.")


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=landscape(A3), pageCompression=1)
    c.setTitle("Funky Event App - UML dijagram klasa")
    c.setAuthor("Funky Event App")
    draw_page_one(c)
    c.showPage()
    draw_page_two_operations(c)
    c.showPage()
    draw_page_three(c)
    c.showPage()
    c.save()
    print(OUTPUT)


if __name__ == "__main__":
    main()
