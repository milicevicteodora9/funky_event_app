from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "images" / "funky-event-app-dijagram-klasa.png"

W, H = 4200, 3400
BG = "#F7F9FC"
NAVY = "#17324D"
LINE = "#6E7C89"
WHITE = "#FFFFFF"
GROUPS = {
    "core": ("#2E6F95", "#EAF4FA"),
    "org": ("#3A7D6C", "#EAF6F1"),
    "money": ("#B86B2B", "#FFF2E8"),
    "team": ("#76538B", "#F3EDF7"),
    "doc": ("#5F6B76", "#F1F4F6"),
}

FONT = r"C:\Windows\Fonts\arial.ttf"
BOLD = r"C:\Windows\Fonts\arialbd.ttf"
title_font = ImageFont.truetype(BOLD, 58)
subtitle_font = ImageFont.truetype(FONT, 28)
class_font = ImageFont.truetype(BOLD, 29)
field_font = ImageFont.truetype(FONT, 22)
label_font = ImageFont.truetype(FONT, 19)
section_font = ImageFont.truetype(BOLD, 24)
enum_title_font = ImageFont.truetype(BOLD, 22)
enum_value_font = ImageFont.truetype(FONT, 19)

CLASSES = {
    "Client": ["String id", "String name", "String taxId", "String contactPerson", "String email", "String phone"],
    "Event": ["String id", "String name", "EventType type", "LocalDate startDate / endDate", "String location", "EventStatus status", "String clientId", "String paymentTerms"],
    "EventAssignment": ["String id", "String eventId", "String userId", "String roleOnEvent", "boolean owner"],
    "User": ["String id", "String firstName / lastName", "String email", "UserRole role", "boolean active"],
    "Cashbox": ["String id", "String userId", "Currency displayCurrency"],
    "Invoice": ["String id", "String eventId / clientId", "String invoiceNumber", "LocalDate issueDate / dueDate", "BigDecimal amount", "InvoiceStatus status"],
    "Budget": ["String id", "String eventId", "boolean includeVat", "BigDecimal discountPercentage"],
    "BudgetItem": ["String id", "BudgetType budgetType", "String categoryId", "String description", "BigDecimal quantity / days", "BigDecimal dailyRate", "+ getTotal()"],
    "BudgetCategory": ["String id", "String name"],
    "CashboxTransaction": ["String id", "String cashboxId / eventId", "BigDecimal amount", "Currency currency", "TransactionType transactionType", "ExpensePurpose purpose"],
    "TeamFee": ["String id", "String teamMemberId / eventId", "String description", "BigDecimal amount", "Currency currency", "LocalDate date"],
    "TeamMember": ["String id", "String fullName", "String phone / email", "String bankAccount", "boolean active"],
    "TeamPayment": ["String id", "String teamMemberId", "BigDecimal amount", "Currency currency", "LocalDate paymentDate", "PaymentMethod method"],
    "Receipt": ["String id", "String receiptNumber", "String seller", "LocalDate issueDate", "BigDecimal totalAmount", "ReceiptProcessingStatus status"],
    "ScannedDocument": ["String id", "String fileName / fileUri", "String mimeType", "DocumentSource source", "LocalDateTime addedAt"],
}

ENUMS = {
    "EventType": ["EVENT", "CAMPAIGN"],
    "EventStatus": ["CURRENT", "COMPLETED"],
    "UserRole": ["ADMIN", "MANAGER", "COORDINATOR"],
    "BudgetType": ["EXTERNAL", "INTERNAL", "ACTUAL"],
    "BudgetItemSource": ["MANUAL", "CASHBOX"],
    "Currency": ["EUR", "RSD", "AED", "USD"],
    "TransactionType": ["INCOME", "EXPENSE"],
    "ExpensePurpose": ["GENERAL", "EVENT"],
    "InvoiceStatus": ["DRAFT", "ISSUED", "PAID", "OVERDUE", "CANCELLED"],
    "PaymentMethod": ["CASH", "CARD", "BANK_TRANSFER", "OTHER"],
    "ReceiptProcessingStatus": ["NEW", "PROCESSED", "CONFIRMED", "ERROR"],
    "DocumentSource": ["CAMERA", "GALLERY", "PDF"],
}

# name: (x, y, width, group)
LAYOUT = {
    "Client": (70, 330, 650, "core"),
    "Event": (860, 300, 650, "core"),
    "EventAssignment": (1650, 340, 650, "org"),
    "User": (2440, 340, 650, "org"),
    "Cashbox": (3370, 380, 650, "org"),
    "Invoice": (70, 1100, 650, "money"),
    "Budget": (860, 1120, 650, "money"),
    "BudgetItem": (1650, 1070, 650, "money"),
    "BudgetCategory": (2440, 1170, 650, "money"),
    "CashboxTransaction": (3370, 1080, 650, "money"),
    "TeamFee": (70, 1900, 650, "team"),
    "TeamMember": (860, 1920, 650, "team"),
    "TeamPayment": (1650, 1900, 650, "team"),
    "Receipt": (2440, 1880, 650, "team"),
    "ScannedDocument": (3370, 1900, 650, "doc"),
}


def box_height(name):
    return 78 + len(CLASSES[name]) * 38 + 24


def anchor(box, side):
    x, y, w, h = box
    return {
        "left": (x, y + h // 2), "right": (x + w, y + h // 2),
        "top": (x + w // 2, y), "bottom": (x + w // 2, y + h),
    }[side]


def line_with_label(draw, a, sa, b, sb, label, ma="1", mb="0..*", via=None):
    ax, ay = anchor(a, sa); bx, by = anchor(b, sb)
    if via:
        points = [(ax, ay), *via, (bx, by)]
    elif sa in ("left", "right"):
        mx = (ax + bx) // 2
        points = [(ax, ay), (mx, ay), (mx, by), (bx, by)]
    else:
        my = (ay + by) // 2
        points = [(ax, ay), (ax, my), (bx, my), (bx, by)]
    draw.line(points, fill=LINE, width=4, joint="curve")
    draw.text((ax + 8, ay - 29), ma, font=label_font, fill=NAVY)
    tw = draw.textbbox((0, 0), mb, font=label_font)[2]
    draw.text((bx - tw - 8, by - 29), mb, font=label_font, fill=NAVY)
    mid = points[len(points)//2]
    bbox = draw.textbbox((0, 0), label, font=label_font)
    lw, lh = bbox[2] - bbox[0] + 18, bbox[3] - bbox[1] + 12
    lx, ly = mid[0] - lw//2, mid[1] - lh//2
    draw.rounded_rectangle((lx, ly, lx+lw, ly+lh), radius=8, fill=WHITE)
    draw.text((lx+9, ly+4), label, font=label_font, fill=LINE)


def draw_box(draw, name):
    x, y, w, group = LAYOUT[name]
    h = box_height(name)
    accent, fill = GROUPS[group]
    draw.rounded_rectangle((x, y, x+w, y+h), radius=18, fill=fill, outline=accent, width=4)
    draw.rounded_rectangle((x, y, x+w, y+68), radius=18, fill=accent)
    draw.rectangle((x, y+48, x+w, y+70), fill=accent)
    title = draw.textbbox((0,0), name, font=class_font)
    tw = title[2] - title[0]
    draw.text((x+(w-tw)//2, y+18), name, font=class_font, fill=WHITE)
    draw.line((x, y+70, x+w, y+70), fill=accent, width=3)
    ty = y + 88
    for field in CLASSES[name]:
        prefix = field if field.startswith("+") else f"- {field}"
        draw.text((x+22, ty), prefix, font=field_font, fill=NAVY)
        ty += 38
    return (x, y, w, h)


def draw_enum(draw, name, values, x, y, w=620):
    h = 70 + len(values) * 31 + 18
    draw.rounded_rectangle((x, y, x+w, y+h), radius=16, fill="#F1F4F6", outline=LINE, width=3)
    draw.text((x+18, y+15), f"<<enumeration>> {name}", font=enum_title_font, fill=NAVY)
    ty = y + 58
    for value in values:
        draw.text((x+22, ty), value, font=enum_value_font, fill=LINE)
        ty += 31


def main():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, 190), fill=NAVY)
    d.text((70, 38), "Funky Event App - UML dijagram klasa", font=title_font, fill=WHITE)
    d.text((72, 112), "Klijent direktno naručuje događaj; agencija organizuje realizaciju i prati tim, budžet, troškove i fakturisanje.", font=subtitle_font, fill=WHITE)
    d.text((70, 240), "OSNOVNI DOMEN I ORGANIZACIJA", font=section_font, fill=GROUPS["core"][0])
    d.text((70, 1010), "FINANSIJE DOGAĐAJA", font=section_font, fill=GROUPS["money"][0])
    d.text((70, 1810), "TIM I DOKUMENTACIJA", font=section_font, fill=GROUPS["team"][0])
    d.text((70, 2450), "ENUMERACIJE", font=section_font, fill=LINE)

    boxes = {name: (x, y, w, box_height(name)) for name, (x, y, w, _) in LAYOUT.items()}

    # Veze su nacrtane pre klasa, tako da nikad ne prelaze preko njihovog sadržaja.
    line_with_label(d, boxes["Client"], "right", boxes["Event"], "left", "naručuje")
    line_with_label(d, boxes["Event"], "right", boxes["EventAssignment"], "left", "organizuje tim", "1", "1..*")
    line_with_label(d, boxes["EventAssignment"], "right", boxes["User"], "left", "zadužen", "0..*", "1")
    line_with_label(d, boxes["User"], "right", boxes["Cashbox"], "left", "vodi", "1", "1")
    line_with_label(d, boxes["Client"], "bottom", boxes["Invoice"], "top", "prima fakture")
    line_with_label(d, boxes["Event"], "bottom", boxes["Budget"], "top", "ima budžet", "1", "1")
    line_with_label(d, boxes["Budget"], "right", boxes["BudgetItem"], "left", "sadrži", "1", "1..*")
    line_with_label(d, boxes["BudgetItem"], "right", boxes["BudgetCategory"], "left", "kategorija", "0..*", "1")
    line_with_label(d, boxes["Cashbox"], "bottom", boxes["CashboxTransaction"], "top", "evidentira")
    line_with_label(d, boxes["CashboxTransaction"], "bottom", boxes["BudgetItem"], "bottom", "kreira ACTUAL stavku", "1", "0..1", via=[(3695, 1640), (1975, 1640)])
    line_with_label(d, boxes["Event"], "left", boxes["Invoice"], "right", "fakturiše se")
    line_with_label(d, boxes["Event"], "bottom", boxes["TeamFee"], "top", "angažovanje", "1", "0..*", via=[(1185, 1780), (395, 1780)])
    line_with_label(d, boxes["TeamFee"], "right", boxes["TeamMember"], "left", "ostvaruje honorar", "0..*", "1")
    line_with_label(d, boxes["TeamMember"], "right", boxes["TeamPayment"], "left", "prima isplate")
    line_with_label(d, boxes["CashboxTransaction"], "bottom", boxes["Receipt"], "top", "dokaz troška", "0..1", "0..1", via=[(3695, 1760), (2765, 1760)])
    line_with_label(d, boxes["Receipt"], "right", boxes["ScannedDocument"], "left", "nastaje iz", "1", "1")

    for name in LAYOUT:
        draw_box(d, name)

    enum_names = list(ENUMS.items())
    enum_x = [70, 750, 1430, 2110, 2790, 3470]
    for i, (name, values) in enumerate(enum_names):
        row, col = divmod(i, 6)
        draw_enum(d, name, values, enum_x[col], 2520 + row * 390)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT, "PNG", optimize=True, dpi=(180, 180))
    print(OUT)


if __name__ == "__main__":
    main()
