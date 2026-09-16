# AgriSense — Smart Farm Monitoring (real-time)

Dashboard giám sát nông trại thời gian thực: sensor data → Redis Streams → WebSocket → biểu đồ live + cảnh báo + AI (Gemini) phân tích.

Stack: Spring Boot 3 (Java 17) · PostgreSQL · Redis Streams · React 18 + TypeScript (Vite).

## Chạy dev

```bash
# 1. Infra (Postgres + Redis)
docker compose up -d

# 2. Backend  (http://localhost:8080)
cd backend
./mvnw spring-boot:run

# 3. Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

## Trạng thái — Tuần 1 (nền)

- [x] Infra: docker-compose (Postgres + Redis)
- [x] Backend skeleton + JWT auth (register / login)
- [x] Schema DB (users, farms, sensors, readings, alerts) — Flyway
- [x] Farm CRUD tối thiểu (list / create, bảo vệ bằng token)
- [x] Frontend: login → farm list

## Trạng thái — Tuần 2 (streaming pipeline ⭐)

- [x] Sensor CRUD (`/farms/{id}/sensors`) + ownership check (FR-2.2, FR-3)
- [x] Redis Streams pipeline: publisher → consumer group `agrisense-workers` → Postgres (at-least-once + ack)
- [x] Replay CSV vào stream: mode `historical` (giữ timestamp) + `live` (nhịp thật)
- [x] Query lịch sử readings theo range 1h/24h/7d (`/sensors/{id}/readings`)
- [x] Seed CSV 24h × 5 sensor (soil giảm dần dưới ngưỡng để test alert Tuần 3)

Chạy thử pipeline:
```bash
# sau khi login, tạo farm + sensor:
curl -X POST localhost:8080/farms/1/replay?mode=historical -H "Authorization: Bearer $TOKEN"
# → readings chảy stream → consumer → Postgres; query:
curl localhost:8080/sensors/1/readings?range=7d -H "Authorization: Bearer $TOKEN"
```

## Trạng thái — Tuần 3–5 (realtime + AI ⭐)

- [x] Alert vượt ngưỡng: consumer check `thresholdMin/Max` khi lưu reading → tạo `Alert` (FR-6)
- [x] WebSocket/STOMP (`/ws`, SockJS): broker in-memory, JWT auth qua CONNECT header `token`, ownership per-topic
- [x] Consumer push realtime `reading` + `alert` tới `/topic/farm/{farmId}` → chart live, không reload
- [x] AlertController: list gần đây + resolve (FR-6.3)
- [x] AI (NFR-6): `AiService` interface + Gemini impl; **stub fallback** khi không có `GEMINI_KEY` → không bao giờ crash UI (FR-7.3)
  - `POST /farms/{id}/analyze` — phân tích sức khỏe 24h · `POST /alerts/{id}/explain` — giải thích 1 cảnh báo
- [x] Frontend `FarmDetail`: tab sensor · Recharts line (history + live append) · nút replay · phân tích AI · list alert (explain/resolve)
- [x] Demo seeder idempotent: `demo@agrisense.dev` / `demo1234`, farm Đà Lạt + 5 sensor
- [x] E2E smoke `smoke3.sh`: login → replay → 14 alert → analyze → explain → resolve · WS: 56 push xác nhận

### Kiến trúc realtime

```
CSV replay ─▶ ReadingPublisher ─▶ Redis Stream (stream:readings)
                                        │  consumer group: agrisense-workers
                                        ▼
                              ReadingConsumer  (at-least-once, manual ack)
                                 │ save reading (Postgres)
                                 │ AlertService.checkAndCreate (threshold)
                                 ▼
                    RealtimePublisher ─▶ STOMP /topic/farm/{id} ─▶ React chart + alert
```

AI (Gemini) tách sau interface `AiService` — có key thì gọi `gemini-1.5-flash`, không key thì stub tiếng Việt tóm tắt min/max/avg. Xem [../project_plan.md](../project_plan.md).

Demo nhanh:
```bash
docker compose up -d && (cd backend && ./mvnw spring-boot:run &)
bash smoke3.sh                       # E2E T3-5
# hoặc UI: cd frontend && npm run dev → login demo@agrisense.dev / demo1234
```

## Thử thách kỹ thuật & cách giải

Những quyết định đáng kể trong quá trình build (không phải chi tiết vụn):

**1. Không mất dữ liệu khi consumer chết — at-least-once + manual ack**
Nếu ack ngay khi nhận message rồi mới xử lý, consumer crash giữa chừng → reading mất vĩnh viễn.
Giải: dùng Redis **consumer group** (`agrisense-workers`) + **chỉ `XACK` sau khi** đã lưu Postgres, check ngưỡng và push realtime xong. Consumer restart → đọc lại pending entries (PEL) → reprocess. Đánh đổi: at-least-once nên xử lý phải idempotent-friendly; đủ cho time-series (ghi trùng 1 reading vô hại).

**2. Ownership: trả 404 thay vì 403**
Trả 403 "Forbidden" khi user A xem farm của user B sẽ **rò rỉ sự tồn tại** của resource (attacker biết ID nào có thật).
Giải: `Ownership.farm()/sensor()` throw **404** đồng nhất cho cả "không tồn tại" lẫn "không sở hữu" → attacker không phân biệt được.
Bug đi kèm: Spring forward request 404 sang `/error` (ERROR dispatch), security STATELESS coi đó là anonymous → nuốt thành **401 rỗng**. Fix bằng `.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` (fix chuẩn Spring Security 6).

**3. Chart live nhưng WebSocket callback đọc state cũ (stale closure)**
Callback STOMP đăng ký 1 lần trong `useEffect`, nhưng cần biết sensor **đang chọn** để lọc reading. Closure "đóng băng" giá trị `selected` lúc subscribe → đổi tab, chart không cập nhật đúng.
Giải: `selectedRef`/`sensorsRef` (useRef) — callback đọc `.current` (giá trị mới nhất) thay vì biến bị capture. Tránh phải re-subscribe mỗi lần đổi tab (đắt + rớt message).

**4. AI không được làm sập UI khi thiếu key / lỗi mạng**
Demo cho nhà tuyển dụng thường **không có** `GEMINI_KEY`.
Giải: `AiService` là **interface**, `GeminiAiService` check `hasKey()` → không key thì trả **stub tiếng Việt** tóm tắt min/max/avg từ chính readings; mọi exception khi gọi Gemini được catch → trả text fallback, không ném lên controller. Bấm "Phân tích AI" **luôn** ra kết quả.

**5. `SimpMessagingTemplate` bean không tồn tại → app fail start**
`RealtimePublisher` inject `SimpMessagingTemplate` nhưng `WebSocketConfig` thiếu `@EnableWebSocketMessageBroker` → không bean nào đăng ký → `UnsatisfiedDependencyException` lúc khởi động.
Giải: thêm annotation kích hoạt STOMP broker. Bài học: annotation `@Enable*` mới là thứ **đăng ký** hạ tầng messaging, không phải việc implement `WebSocketMessageBrokerConfigurer`.

## Cấu trúc

```
agrisense/
  docker-compose.yml   # Postgres + Redis
  backend/             # Spring Boot API
  frontend/            # React + Vite
```
