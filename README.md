# Asynchroniczny system analizy plików tekstowych

Spring Boot REST API do równoległej analizy zbioru plików `.txt`.

## Dostępne analizy

| Typ zadania    | Opis                              |
|----------------|-----------------------------------|
| `TOTAL_WORDS`  | Łączna liczba słów                |
| `UNIQUE_WORDS` | Liczba unikalnych słów            |
| `TOP_WORDS`    | Top 10 najczęściej występujących  |

Każda analiza wykonywana jest w wersji sekwencyjnej i równoległej z pomiarem wydajności (S(N), E(N)) dla 1/2/4/8 workerów.

---

## Uruchomienie lokalne

**Wymagania:** Java 21, Maven

```bash
# 1. Sklonuj / przejdź do katalogu projektu
cd pliki-api

# 2. Zbuduj
mvn clean package -DskipTests

# 3. Uruchom
java -jar target/pliki-api-1.0-SNAPSHOT.jar
```

Aplikacja startuje na `http://localhost:8080`.  
Przy pierwszym uruchomieniu automatycznie generuje 50 plików testowych w katalogu `texts/`.

---

## Uruchomienie w Dockerze

**Wymagania:** Docker, Docker Compose

```bash
docker-compose up --build
```

Aplikacja dostępna pod `http://localhost:8080`.  
Katalog `texts/` jest montowany jako wolumen — pliki zachowują się między restartami.

Zatrzymanie:
```bash
docker-compose down
```

---

## REST API

### POST /tasks
Tworzy nowe zadanie. Zwraca `202 Accepted`.

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"taskType":"TOTAL_WORDS","inputPath":"texts/","workers":4,"runBenchmark":true}'
```

```json
{
  "taskId": "a1b2c3d4-...",
  "status": "QUEUED",
  "taskType": "TOTAL_WORDS"
}
```

### GET /tasks/{id}
Zwraca stan zadania.

```bash
curl http://localhost:8080/tasks/a1b2c3d4-...
```

```json
{
  "taskId": "a1b2c3d4-...",
  "status": "DONE",
  "result": 184231,
  "benchmarkResult": {
    "sequentialTimeMs": 20,
    "parallelTimesMs": {"1":20,"2":10,"4":6,"8":8},
    "speedup":         {"1":1.0,"2":2.0,"4":3.33,"8":2.5},
    "efficiency":      {"1":1.0,"2":1.0,"4":0.83,"8":0.31}
  }
}
```

### GET /tasks
Lista wszystkich zadań.

---

## Statusy zadania

| Status    | Opis                        |
|-----------|-----------------------------|
| `QUEUED`  | Przyjęte, czeka na wykonanie|
| `RUNNING` | W trakcie przetwarzania     |
| `DONE`    | Zakończone sukcesem         |
| `FAILED`  | Błąd — patrz `errorMessage` |

---

## Struktura projektu

```
src/main/java/com/event/plikiapi/
├── analyzer/
│   ├── TextAnalyzer.java          # interfejs analizatora
│   ├── TotalWordsAnalyzer.java    # łączna liczba słów
│   ├── UniqueWordsAnalyzer.java   # liczba unikalnych słów
│   └── TopWordsAnalyzer.java      # najczęstsze słowa
├── controller/
│   └── TaskController.java        # endpoints REST
├── service/
│   └── TaskService.java           # logika, kolejka, benchmark
├── model/
│   └── Task.java, TaskStatus...   # modele danych
└── config/
    └── DataInitializer.java       # generowanie plików testowych

src/main/resources/static/
└── index.html                     # GUI klienta
```
