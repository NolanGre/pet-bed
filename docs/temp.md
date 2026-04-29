### Lost algorithm
🔴 Critical Issues (обов'язково виправити)
1. @Async + @Transactional - Небезпечна комбінація в MatchingService
   - Проблема: Spring може створити проксі неправильно, транзакції працюватимуть некоректно
   - Рішення: Розділити на @Async метод + приватний @Transactional метод
2. PostGIS Point формат - Point.toString() ненадійний
   - Проблема: Формат може варіюватись, потрібен WKT
   - Рішення: Використовувати String.format("SRID=4326;POINT(%f %f)", x, y)
3. N+1 Query в findActiveLostRequestsWithinRadius()
   - Проблема: Спочатку отримуємо IDs, потім окремі запити findById() для кожного
   - Рішення: Використати findAllById(ids) для batch loading
4. Загальний catch Exception в MatchingEventListener
   - Проблема: Може приховати programming errors
   - Рішення: Ловити конкретні винятки (DataAccessException, PetBedException)
---
🟡 Important Issues (бажано виправити)
5. Null handling - orElse(null) анти-патерн
6. Lazy loading risk - MatchResult record тримає entity references
7. Missing validation - Немає перевірки null inputs в calculateScore
8. Log levels - debug замість info для подій
9. Відстань не зберігається - Розраховується, але не передається в MatchQueueService