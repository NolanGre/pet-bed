# План покращення алгоритму matching (Групове порівняння)

## Мета
Підвищити точність matching через групове порівняння характеристик окремими запитами

## Архітектура

Замість одного `search_text` → окремі колонки для кожної групи:
- `breed_text` — порода
- `color_text` — колір + окрас
- `size_text` — розмір (малий/середній/великий)
- `sex_text` — стать (він/вона)

**Важливо:** Старий функціонал з `search_text` повністю замінюється новим груповим порівнянням.

> **Примітка:** В ці колонки додаємо **тільки те, що використовується в алгоритмі matching**. Поля, які не беруть участь у порівнянні (наприклад, кличка тварини), **не додаємо** — вони безкорисні для алгоритму. Повну інформацію про тварину завжди можна отримати з таблиці `pets` за `pet_id`.

---

## Фаза 1: Міграції БД + Сутності

### 1.1 Liquibase міграції

**Таблиця `lost_requests`:**
```sql
ALTER TABLE lost_requests 
  ADD COLUMN breed_text VARCHAR(100),
  ADD COLUMN color_text VARCHAR(100),
  ADD COLUMN size_text VARCHAR(20),
  ADD COLUMN sex_text VARCHAR(20);

-- Індекси для швидкого порівняння
CREATE INDEX idx_lost_breed ON lost_requests(breed_text);
CREATE INDEX idx_lost_color ON lost_requests(color_text);
CREATE INDEX idx_lost_size ON lost_requests(size_text);
CREATE INDEX idx_lost_sex ON lost_requests(sex_text);
```

**Таблиця `found_requests`:**
```sql
ALTER TABLE found_requests 
  ADD COLUMN breed_text VARCHAR(100),
  ADD COLUMN color_text VARCHAR(100),
  ADD COLUMN size_text VARCHAR(20),
  ADD COLUMN sex_text VARCHAR(20);

CREATE INDEX idx_found_breed ON found_requests(breed_text);
CREATE INDEX idx_found_color ON found_requests(color_text);
CREATE INDEX idx_found_size ON found_requests(size_text);
CREATE INDEX idx_found_sex ON found_requests(sex_text);
```

**Примітка:** Видалення старої колонки `search_text` — в кінці, після перевірки роботи.

### 1.2 Оновлення Entity

**LostRequest.java:**
- Додати поля: `breedText`, `colorText`, `sizeText`, `sexText` (всі `@Nullable`)
- Оновити `generateSearchText()` → `generateGroupTexts()` який повертає Map<String, String>
- Прибрати ім'я з будь-якого тексту

**FoundRequest.java:**
- Додати поля: `breedText`, `colorText`, `sizeText`, `sexText` (всі `@Nullable`)
- Оновити формування текстів при створенні

---

## Фаза 2: Оновлення MatchingAlgorithm

### 2.1 Новий алгоритм порівняння

**Ваги груп:**
```java
private static final double BREED_WEIGHT = 0.35;   // Порода — найважливіше
private static final double COLOR_WEIGHT = 0.30;   // Колір
private static final double SIZE_WEIGHT = 0.20;    // Розмір
private static final double SEX_WEIGHT = 0.15;     // Стать
```

**Логіка порівняння:**
```java
public BigDecimal calculateScore(LostRequest lost, FoundRequest found) {
    double totalWeight = 0;
    double weightedScore = 0;
    
    // Порода
    if (lost.getBreedText() != null && found.getBreedText() != null) {
        double breedScore = calculateTextSimilarity(lost.getBreedText(), found.getBreedText());
        weightedScore += breedScore * BREED_WEIGHT;
        totalWeight += BREED_WEIGHT;
    }
    
    // Колір
    if (lost.getColorText() != null && found.getColorText() != null) {
        double colorScore = calculateTextSimilarity(lost.getColorText(), found.getColorText());
        weightedScore += colorScore * COLOR_WEIGHT;
        totalWeight += COLOR_WEIGHT;
    }
    
    // Розмір (точне співпадіння або similarity)
    if (lost.getSizeText() != null && found.getSizeText() != null) {
        double sizeScore = lost.getSizeText().equalsIgnoreCase(found.getSizeText()) ? 1.0 : 0.0;
        weightedScore += sizeScore * SIZE_WEIGHT;
        totalWeight += SIZE_WEIGHT;
    }
    
    // Стать (точне співпадіння)
    if (lost.getSexText() != null && found.getSexText() != null) {
        double sexScore = lost.getSexText().equalsIgnoreCase(found.getSexText()) ? 1.0 : 0.0;
        weightedScore += sexScore * SEX_WEIGHT;
        totalWeight += SEX_WEIGHT;
    }
    
    // Нормалізація: якщо не всі групи заповнені — пропорційно збільшуємо вагу
    if (totalWeight == 0) {
        return BigDecimal.ZERO;
    }
    
    double normalizedScore = weightedScore / totalWeight;
    return BigDecimal.valueOf(normalizedScore).setScale(4, RoundingMode.HALF_UP);
}
```

**Правила обробки null:**
- Якщо `lost.X_text = null` → група не порівнюється, вага цієї групи не додається
- Якщо `found.X_text = null` → група не порівнюється, вага цієї групи не додається
- Якщо обидва null → група пропускається
- Якщо всі групи null → score = 0

### 2.2 SQL для порівняння (опціонально, для оптимізації)

```sql
SELECT 
  l.id as lost_id,
  f.id as found_id,
  -- Порода (35%)
  CASE 
    WHEN l.breed_text IS NOT NULL AND f.breed_text IS NOT NULL 
    THEN similarity(l.breed_text, f.breed_text) * 0.35 
    ELSE 0 
  END +
  -- Колір (30%)
  CASE 
    WHEN l.color_text IS NOT NULL AND f.color_text IS NOT NULL 
    THEN similarity(l.color_text, f.color_text) * 0.30 
    ELSE 0 
  END +
  -- Розмір (20%): 1 якщо співпадає, 0 якщо ні
  CASE 
    WHEN l.size_text IS NOT NULL AND f.size_text IS NOT NULL 
         AND l.size_text = f.size_text
    THEN 0.20 
    WHEN l.size_text IS NOT NULL AND f.size_text IS NOT NULL 
    THEN 0 
    ELSE 0 
  END +
  -- Стать (15%)
  CASE 
    WHEN l.sex_text IS NOT NULL AND f.sex_text IS NOT NULL 
         AND l.sex_text = f.sex_text
    THEN 0.15 
    WHEN l.sex_text IS NOT NULL AND f.sex_text IS NOT NULL 
    THEN 0 
    ELSE 0 
  END as weighted_score,
  -- Нормалізація (сума ваг тільки для не-null пар)
  CASE WHEN l.breed_text IS NOT NULL AND f.breed_text IS NOT NULL THEN 0.35 ELSE 0 END +
  CASE WHEN l.color_text IS NOT NULL AND f.color_text IS NOT NULL THEN 0.30 ELSE 0 END +
  CASE WHEN l.size_text IS NOT NULL AND f.size_text IS NOT NULL THEN 0.20 ELSE 0 END +
  CASE WHEN l.sex_text IS NOT NULL AND f.sex_text IS NOT NULL THEN 0.15 ELSE 0 END as total_weight
FROM lost_requests l
CROSS JOIN found_requests f
WHERE l.status = 'ACTIVE'
  AND l.pet_type = f.pet_type
  AND ST_DWithin(l.last_seen_location, f.location, 50000)
HAVING total_weight > 0
```

---

## Фаза 3: Інші зміни

### 3.1 Оновлення LostRequestService

- `generateGroupTexts(PetDTO pet)` — повертає Map з текстами для кожної групи
- Прибрати ім'я з breed_text
- Переклад size: SMALL→"малий", MEDIUM→"середній", LARGE→"великий"
- Переклад sex: MALE→"він", FEMALE→"вона"

### 3.2 Оновлення FoundRequestService

- При створенні: парсити description та розбивати на групи
- Або: окремі поля у формі для кожної групи (краще!)

### 3.3 Cleanup

- Видалити стару колонку `search_text` з обох таблиць
- Видалити старі індекси `idx_lost_search_text`, `idx_found_description_trgm`
- Оновити документацію

---

## Приклад роботи

**Lost (заповнені всі поля):**
```
breed_text: "овчар"
color_text: "чорний світлий живіт"
size_text: "середній"
sex_text: "вона"
```

**Found (заповнені всі поля):**
```
breed_text: "овчар"
color_text: "чорний однокольоровий"
size_text: "середній"
sex_text: "вона"
```

**Розрахунок:**
- Порода: similarity("овчар", "овчар") = 1.0 × 0.35 = 0.35
- Колір: similarity("чорний світлий живіт", "чорний однокольоровий") ≈ 0.4 × 0.30 = 0.12
- Розмір: "середній" = "середній" → 1.0 × 0.20 = 0.20
- Стать: "вона" = "вона" → 1.0 × 0.15 = 0.15
- **Total:** (0.35 + 0.12 + 0.20 + 0.15) / 1.0 = **0.82**

**Found (без розміру):**
```
breed_text: "овчар"
color_text: "чорний однокольоровий"
size_text: NULL
sex_text: "вона"
```

**Розрахунок:**
- Порода: 1.0 × 0.35 = 0.35
- Колір: 0.4 × 0.30 = 0.12
- Розмір: пропускається (null)
- Стать: 1.0 × 0.15 = 0.15
- **Total:** (0.35 + 0.12 + 0.15) / 0.80 = **0.775**
(Нормалізація: сума ваг заповнених груп = 0.35+0.30+0.15 = 0.80)

---

## Порядок виконання

1. **Фаза 1:** Міграції БД + Entity
2. **Фаза 2:** MatchingAlgorithm + Сервіси
3. **Фаза 3:** Cleanup

**Важливо:** Не видаляти старі колонки до повної перевірки роботи нового алгоритму!
