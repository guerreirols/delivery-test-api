# Delivery API

API backend para gerenciamento de pedidos de delivery. Centraliza o ciclo de vida dos pedidos, a autenticação de usuários e a persistência de dados, oferecendo uma interface HTTP para integração com aplicações web.

## Tecnologias

| Tecnologia | Responsabilidade |
| --- | --- |
| Java | Linguagem da aplicação |
| Spring Boot e Spring MVC | Inicialização, configuração e camada HTTP |
| Spring Security | Autenticação e proteção de recursos |
| Spring Data JPA e Hibernate | Acesso a dados e mapeamento objeto-relacional |
| SQLite | Persistência local |
| Jakarta Validation | Validação dos dados de entrada |
| Lombok e SLF4J | Redução de código repetitivo e logging |
| Springdoc OpenAPI | Documentação dos contratos da API |
| JUnit, Mockito e AssertJ | Testes automatizados |
| Maven Wrapper | Build e gerenciamento de dependências |

As versões das dependências e a versão Java utilizada no build estão definidas no `pom.xml`.

## Arquitetura

A aplicação adota uma arquitetura em camadas, com separação entre apresentação,
aplicação, domínio e persistência. A organização por funcionalidade mantém as
responsabilidades relacionadas ao mesmo contexto próximas, facilitando a evolução
dos fluxos de negócio.

A camada de apresentação recebe e valida as requisições HTTP e traduz os resultados
em respostas. A camada de aplicação coordena os casos de uso, aciona as regras de
domínio e estabelece os limites transacionais necessários. O domínio protege as
invariantes e as transições de estado. A persistência é acessada por repositórios,
que concentram a interação com o banco de dados.

Os contratos de entrada e saída utilizam DTOs com conversão explícita, permitindo
que a representação HTTP evolua sem expor diretamente as entidades persistidas.
Autenticação, tratamento de erros e configuração são responsabilidades transversais,
centralizadas para manter um comportamento consistente entre os fluxos.

O fluxo principal parte da requisição HTTP, passa pela coordenação do caso de uso
e pelas regras de negócio e chega à persistência quando necessário. A implementação
utiliza os recursos do Spring e do JPA, preservando essa separação de responsabilidades
sem introduzir abstrações adicionais para cada componente.

## Segurança

A autenticação utiliza JWT para acesso aos recursos protegidos e refresh tokens para renovação da sessão. As senhas são armazenadas com hash PBKDF2 e salt aleatório.

O refresh token é opaco e enviado em cookie `HttpOnly`; somente seu hash é persistido. A renovação realiza rotação atômica, impedindo a reutilização do token anterior. O encerramento da sessão revoga o refresh token correspondente.

A integração com navegadores utiliza CORS com origens explícitas e proteção adicional nas operações que usam o cookie de renovação. As configurações de `Secure` e `SameSite` devem acompanhar o ambiente de execução.

Access tokens já emitidos permanecem válidos até sua expiração. A revogação de refresh tokens não representa revogação imediata dos JWTs de acesso.

## Pré-requisitos

- JDK 25, com `JAVA_HOME` configurado.
- Acesso aos repositórios de dependências na primeira execução.
- Permissão de escrita no diretório utilizado pelo banco local.

O Maven Wrapper está incluído no repositório; não é necessário instalar o Maven separadamente.

## Execução local

Execute os comandos a partir da raiz do projeto.

### Windows — PowerShell

```powershell
$env:REFRESH_COOKIE_SECURE = "false"
.\mvnw.cmd spring-boot:run
```

### Linux e macOS

```bash
REFRESH_COOKIE_SECURE=false sh ./mvnw spring-boot:run
```

A desativação de `Secure` nesses comandos se destina ao desenvolvimento local com HTTP. Em ambientes com HTTPS, mantenha essa proteção habilitada.

O SQLite utiliza um arquivo local, dispensando um servidor de banco separado. O caminho e as demais configurações ficam em `src/main/resources/application.yaml`.

## Configuração

As configurações podem ser fornecidas por variáveis de ambiente. Consulte `application.yaml` para os valores padrão vigentes.

| Variável | Finalidade |
| --- | --- |
| `JWT_SECRET` | Chave de assinatura em Base64, com pelo menos 32 bytes aleatórios |
| `JWT_LIFETIME` | Prazo de validade do access token em duração ISO-8601 |
| `REFRESH_TOKEN_LIFETIME` | Prazo de validade do refresh token em duração ISO-8601 |
| `REFRESH_COOKIE_SECURE` | Envio do cookie de renovação apenas por HTTPS |
| `REFRESH_COOKIE_SAME_SITE` | Política de envio do cookie entre sites |
| `CORS_ALLOWED_ORIGINS` | Origens autorizadas, separadas por vírgulas |
| `APP_LOG_LEVEL` | Nível de logging dos componentes da aplicação |

Sem `JWT_SECRET`, a aplicação gera uma chave temporária por inicialização. Para preservar a validação dos access tokens entre reinicializações, configure uma chave persistente fora do código-fonte. Instâncias que compartilhem os mesmos tokens precisam utilizar a mesma chave.

## Testes e build

A suíte combina testes unitários, testes de contratos HTTP com MockMvc e testes de integração. Os testes isolados verificam regras de domínio, serviços, conversões e validações. Os de integração exercitam segurança, persistência e comportamento transacional.

Os testes de persistência utilizam SQLite em memória e configuração própria em `src/test/resources/application.yaml`, sem acessar o banco local de desenvolvimento.

| Objetivo | Windows | Linux e macOS |
| --- | --- | --- |
| Executar a suíte | `.\mvnw.cmd test` | `sh ./mvnw test` |
| Validar e empacotar | `.\mvnw.cmd clean verify` | `sh ./mvnw clean verify` |

Os artefatos de build e os relatórios de testes são gerados em `target/`.

Novas funcionalidades e correções de comportamento devem incluir testes novos ou atualizados na mesma entrega, cobrindo os cenários relevantes de sucesso, rejeição e limite.

## Tratamento de erros e logs

O tratamento de exceções é centralizado e utiliza um formato consistente de resposta. As mensagens públicas são resolvidas por `MessageSource`, mantendo detalhes internos fora dos contratos da API.

Os logs utilizam Lombok `@Slf4j`, com `INFO` como nível padrão. Eventos rotineiros e rejeições esperadas ficam em `DEBUG`; configurações que exigem atenção geram `WARN`; falhas inesperadas são registradas em `ERROR` no ponto de tratamento.

Senhas, tokens, cookies, hashes de credenciais e dados pessoais não devem ser incluídos nos logs. O diagnóstico de falhas inesperadas preserva tipos e pilhas sem reproduzir as mensagens brutas das exceções.

## Documentação da API

Os contratos HTTP são documentados com OpenAPI e disponibilizados pelo Swagger UI durante a execução. Essa documentação acompanha as anotações do código e deve ser consultada para conhecer as operações, os modelos e os requisitos de autenticação vigentes.


As convenções de desenvolvimento estão registradas no `AGENTS.md` e nas skills locais do repositório.
