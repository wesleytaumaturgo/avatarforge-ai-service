# AvatarForge AI Service

Plataforma backend para geração assíncrona de fotos de perfil com IA (Stable Diffusion), armazenamento S3 e acompanhamento em múltiplos canais (REST polling/long-polling, SSE, webhook e gRPC streaming). Construída em Quarkus para baixa latência, escalabilidade e DX de APIs modernas.

## Visão geral do fluxo
1) **Upload non-blocking** – `POST /customers/{id}` recebe o arquivo (multipart) e responde `202 Accepted` com `Location` para consulta; nenhum processamento IA ocorre na requisição.
2) **Fila/execução assíncrona** – `ProfilePhotoAsyncProcessor` roda em pool de threads, chama Stable Diffusion, salva originais/gerados em S3, persiste metadados e estados em MariaDB.
3) **Acompanhamento** – status disponível via:
   - REST polling (`GET .../status`) ou long-poll (`waitSeconds`).
   - SSE (`GET .../photos/stream`) para push em tempo real.
   - Webhook opcional (callback POST JSON ao finalizar ou falhar).
   - gRPC (`GetStatus` unário + `StreamStatus` server-streaming).
4) **Estados de job** – `PENDING → PROCESSING → DONE/FAILED`, com mensagem de erro opcional.

## Arquitetura e tecnologias
- **Runtime:** Quarkus 3 (RESTEasy Reactive, Mutiny, gRPC)
- **Persistência:** MariaDB + Hibernate ORM; migrations em `src/main/resources/db/migration`
- **Armazenamento de objetos:** Amazon S3 (quarkus-amazon-s3)
- **IA/geração:** Stable Diffusion via REST client reativo
- **Observabilidade:** Micrometer + Prometheus
- **Testes:** JUnit 5

## Execução e build
```bash
# Dev mode (live reload, devservices para MariaDB/S3)
./mvnw quarkus:dev

# Build jar
./mvnw package
```
Configuração base em `src/main/resources/application.properties`. Ajuste:
- Datasource MariaDB (`quarkus.datasource.*`)
- S3 (`quarkus.s3.*`)
- Endpoint Stable Diffusion (`quarkus.rest-client.stable-diffusion-api.url`)

## REST API
- `POST /customers/{id}`  
  Multipart: `photo` (arquivo), opcional `callbackUrl` (webhook). Retorna `202` e header `Location: /customers/{id}/photos/{jobId}/status`.
- `GET /customers/{id}/photos/{jobId}/status?waitSeconds={n}`  
  Retorna `ProfilePhotoJobStatus`. Com `waitSeconds>0`, faz long-poll até estado terminal ou timeout.
- `GET /customers/{id}/photos/stream` (SSE)  
  Stream JSON de updates de jobs do cliente.
- `GET /customers` / `GET /customers/{id}`  
  Consulta de clientes.

### Webhook
Se `callbackUrl` for informado no upload, o serviço envia `POST` com `ProfilePhotoJobStatus` (JSON) ao concluir ou falhar.

## gRPC API
Proto: `src/main/proto/profile_photo_job.proto`
- `GetStatus(JobStatusRequest)` – unário, suporta `wait_seconds` para long-poll.
- `StreamStatus(StatusStreamRequest)` – server-streaming filtrado por `customer_id` (opcional `job_id`).
Stubs Mutiny gerados em `target/generated-sources/grpc`.

## Modelo de domínio
- `ProfilePhotoJob`: id, customerId, status, URLs original/gerada, callbackUrl, erro, timestamps.
- `ProcessingStatus`: `PENDING | PROCESSING | DONE | FAILED` (método `isTerminal()`).
- Eventos internos via `BroadcastProcessor` abastecem SSE e gRPC streaming.

## Persistência e storage
- Migrations em `db/migration` criam tabelas de fotos e jobs, incluindo `callback_url`.
- Arquivos temporários de upload são limpos pelo processador após o envio a S3.

## Observabilidade e operação
- Métricas Micrometer/Prometheus já habilitadas (`quarkus.micrometer.*` defaults).
- Logs SQL (`quarkus.hibernate-orm.log.sql=true`) configuráveis em `application.properties`.

## Testes
- `src/test/java/com/taumaturgo/infrastructure/async/ProfilePhotoAsyncProcessorTest.java` cobre fluxo assíncrono (status DONE, cleanup, callback).
