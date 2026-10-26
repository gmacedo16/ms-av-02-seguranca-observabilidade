# MS-AV-02 — Segurança e Observabilidade

API REST de autenticação e administração de usuários com Spring Boot, Spring Security, `JdbcUserDetailsManager`, H2 e BCrypt. Os contadores são exportados para Prometheus e apresentados em um dashboard Grafana provisionado por JSON.

## Requisitos

- JDK 17 ou superior e Maven 3.9+
- Para o painel: Docker com Compose

## Executar localmente

Configure senhas próprias para os usuários iniciais (não use senhas reais de outros serviços):

```bash
APP_BOOTSTRAP_ADMIN_PASSWORD='DefinaUmaSenhaForte' \
APP_BOOTSTRAP_MAINTENANCE_PASSWORD='DefinaOutraSenhaForte' \
mvn spring-boot:run
```

O banco H2 local é em memória e reinicia com a aplicação. O bootstrap cria os usuários `admin` (`ROLE_ADMIN`) e `manutencao` (`ROLE_MANUTENCAO`) apenas se as variáveis de senha forem fornecidas e se esses usuários ainda não existirem. Não são criadas contas com senhas-padrão.

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Métricas Prometheus: <http://localhost:8080/actuator/prometheus>

## Endpoints

| Método | Caminho | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/login` | Público | Autentica o JSON `{"username":"...","senha":"..."}`; responde 200 ou 401. |
| `PUT` | `/trocarsenha/{userid}` | Público | Recebe `{"atual":"...","nova":"..."}`; confere a senha atual e grava novo hash BCrypt. |
| `POST` | `/` | `ROLE_MANUTENCAO` | Cadastra usuário com role inicial `ROLE_USER`. |
| `GET` | `/` | `ROLE_MANUTENCAO` | Lista usuário, roles e estados; nunca inclui senhas nem hashes. |
| `PUT` | `/unlock/{userid}` | `ROLE_ADMIN` | Habilita conta e zera tentativas de login. |

As rotas protegidas usam HTTP Basic. O endpoint `/login` valida credenciais, mas não emite token nem sessão; use Basic Auth nas rotas administrativas. Falhas consecutivas são registradas por usuário e a segunda bloqueia uma conta existente. Um login válido ou o desbloqueio zera a sequência.

Exemplo de login:

```bash
curl -i http://localhost:8080/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"manutencao","senha":"SENHA_CONFIGURADA"}'
```

Exemplo de cadastro:

```bash
curl -i -u manutencao:SENHA_CONFIGURADA \
  http://localhost:8080/ \
  -H 'Content-Type: application/json' \
  -d '{"username":"novo-usuario","senha":"SenhaNovaSegura123"}'
```

## Prometheus e Grafana

Defina as três senhas localmente e suba a stack:

```bash
export APP_BOOTSTRAP_ADMIN_PASSWORD='DefinaUmaSenhaForte'
export APP_BOOTSTRAP_MAINTENANCE_PASSWORD='DefinaOutraSenhaForte'
export GRAFANA_ADMIN_PASSWORD='DefinaUmaSenhaGrafana'
docker compose up --build
```

- Grafana: <http://localhost:3000> (usuário `admin`, senha definida em `GRAFANA_ADMIN_PASSWORD`)
- Prometheus: <http://localhost:9090>
- Dashboard provisionado: **MS-AV-02 — Segurança e Observabilidade**

O dashboard exibe os contadores `app_login_success_total`, `app_login_failure_total` e o gauge `app_locked_users`. Contadores de login são cumulativos desde a inicialização do processo; o total de bloqueados é consultado no banco.

Captura do dashboard com tráfego real: [`docs/grafana-dashboard.png`](./docs/grafana-dashboard.png); export do Grafana: [`docs/grafana-dashboard-export.json`](./docs/grafana-dashboard-export.json); Swagger: [`docs/swagger-ui.png`](./docs/swagger-ui.png). O dashboard provisionado fica em `observability/grafana/provisioning/dashboards/ms-av-02.json`.

## Banco de dados

O schema mantém `usuarios` como fonte de credenciais, com `senha` de tamanho suficiente para o BCrypt, tabela `authorities` para roles e `login_attempts` para bloqueios. O DDL corrige a vírgula excedente do enunciado. `JdbcUserDetailsManager` usa consultas customizadas para trabalhar com esse schema.

## Testes

```bash
mvn test
```

