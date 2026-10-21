# Convenções do projeto

Este repositório contém uma API de delivery para um teste técnico do iFood. Priorize clareza, correção e decisões que o candidato consiga explicar na avaliação. Comunique-se em português; mantenha identificadores Java em inglês e as convenções de mensagens existentes.

## Regras permanentes

- Declare tipos explicitamente em Java, inclusive nos testes; não use `var`.
- Não adicione comentários no corpo do código. Expresse intenção com nomes e métodos claros; documente decisões no README quando necessário.

- Aplique Clean Code em toda alteração: responsabilidades coesas, nomes claros, dependências explícitas e a menor solução que atenda ao requisito. Não crie abstrações ou dependências sem necessidade demonstrada.
- Preserve a arquitetura por funcionalidade em `com.deliverytest.api`: `order`, `auth` e `user`; DTOs e mappers ficam dentro da funcionalidade; exceções específicas ficam em `exception` dentro da funcionalidade; o pacote raiz `exception` mantém apenas `GlobalExceptionHandler` e `ErrorResponse`. Dentro de cada funcionalidade, agrupe classes em `controller`, `service`, `repository`, `entity`, `dto`, `mapper` e `exception`, criando apenas os subpacotes necessários. Em `auth`, infraestrutura de segurança e cookies ficam em `security`; serviços de tokens ficam em `service`, persistência em `repository` e entidades em `entity`. Confirme a estrutura atual antes de implementar.
- Controllers cuidam do contrato HTTP; services coordenam casos de uso e transações; repositories cuidam da persistência; entidades protegem invariantes. Siga os DTOs record, mappers explícitos e injeção por construtor já utilizados.
- Preserve contratos públicos e regras de negócio, salvo quando a tarefa exigir alterá-los. Não transforme o projeto em outra arquitetura ou introduza infraestrutura empresarial apenas por preferência.
- Trate falhas esperadas com exceções específicas e o `GlobalExceptionHandler`, mantendo `ErrorResponse` e internacionalização em `messages.properties`.
- Use Lombok `@Slf4j` com níveis e sigilo definidos na skill backend-quality. Registre falhas inesperadas uma única vez no tratamento; mantenha eventos rotineiros em DEBUG e o padrão da aplicação em INFO.
- Não exponha entidades JPA, credenciais, dados pessoais ou detalhes internos em respostas ou logs. Verifique autorização ao acessar recursos protegidos.
- Preserve alterações existentes do usuário. Não modifique `delivery.db` em testes; use banco isolado quando precisar de persistência.

## Skills locais

Leia as skills aplicáveis antes de alterar código; as regras acima valem mesmo quando nenhuma skill for carregada.

- `.agents/skills/java-clean-code/SKILL.md`: implementação e refatoração Java.
- `.agents/skills/project-architecture/SKILL.md`: criação ou alteração de fluxos e componentes.
- `.agents/skills/spring-exceptions/SKILL.md`: validações, falhas e contratos de erro.
- `.agents/skills/backend-quality/SKILL.md`: testes, segurança e persistência afetados pela tarefa.

## Validação e entrega

Toda nova feature ou correção de comportamento deve incluir testes unitários novos ou atualizados na mesma entrega, conforme a skill backend-quality. Mantenha testes de integração quando o risco depender de HTTP, segurança ou transações reais.

Consulte `pom.xml` para a stack efetiva; atualmente declara Java 25, Spring Boot 4.1.1 e Maven Wrapper. Não atualize versões sem necessidade da tarefa.
Para mudanças Java, execute testes relevantes com `./mvnw.cmd test` no Windows ou `./mvnw test` em Unix, usando `-Dtest=NomeDoTeste` quando adequado. Para entrega de código, execute `./mvnw.cmd verify` quando viável. Informe comandos executados, resultados e bloqueios reais; nunca declare testes não executados como aprovados. Mudanças apenas de instruções exigem validação de estrutura e conteúdo, sem iniciar a aplicação.

## Especialista

O agente `spring_backend_senior` está definido em `.codex/agents/spring_backend_senior.toml`. Use-o quando o usuário solicitar esse especialista ou delegação backend. Sua existência não exige delegação automática em toda tarefa.
