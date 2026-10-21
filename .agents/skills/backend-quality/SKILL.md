---
name: backend-quality
description: Validar mudanças backend Spring com testes proporcionais ao risco e verificar efeitos relevantes em transações, persistência, segurança e contratos da API.
---

# Qualidade backend

Leia AGENTS.md, testes e configurações do fluxo alterado. Use as bibliotecas declaradas no Maven.

- Teste resultados observáveis e limites das regras. Para correções, reproduza a falha e cubra a regressão. Evite testes que apenas repetem a implementação ou verificam getters.
- Use testes unitários para regras isoladas, MVC para contratos HTTP e persistência ou integração quando o risco depender de JPA, transações ou configuração real. Não carregue o contexto completo sem necessidade.
- Isole dados e relógio quando o resultado depender deles. Nunca execute testes contra delivery.db; configure banco separado e preserve particularidades do SQLite quando relevantes.
- Em mudanças JPA, avalie atomicidade, cascatas, integridade das relações, concorrência e consultas adicionais conforme o fluxo. Não simule paginação carregando tudo em memória.
- Em rotas protegidas, verifique identidade, autorização sobre o recurso e cenários negados. Use Spring Security e codificação de senha adequada; não registre senhas ou tokens nem desative proteções para passar testes.
- Mantenha logs úteis e sem dados sensíveis; não acrescente infraestrutura de observabilidade sem necessidade da tarefa.
- Execute testes relevantes e verificação Maven conforme AGENTS.md. Informe comandos, resultados e bloqueios, distinguindo problemas prévios de regressões. Não declare testes não executados como aprovados.

## Testes obrigatórios por entrega

- Toda nova feature ou correção que altere comportamento deve incluir testes unitários novos ou atualizados na mesma entrega. Para correções, cubra a regressão reproduzindo o cenário defeituoso e verificando o comportamento esperado.
- Cubra sucesso, rejeições e limites relevantes das regras alteradas. Verifique resultados e ausência de efeitos indevidos nas falhas; não apenas chamadas a mocks.
- Testes unitários usam JUnit e Mockito sem SpringBootTest, contexto Spring ou banco. Espelhe os pacotes de produção em src/test/java; use tipos explícitos e não adicione comentários no código.
- Preserve testes de integração para contratos HTTP, segurança e transações reais: mocks não comprovam filtros, validação MVC, constraints ou rollback. Testes unitários e de integração são complementares.
- Não crie testes artificiais de getters, DTOs sem lógica ou código gerado para aumentar números. Para alterações exclusivamente documentais ou movimentação de pacotes sem mudança de comportamento, justifique a ausência de novos testes e execute os existentes aplicáveis.
- Antes de concluir, execute os testes afetados e a verificação Maven da entrega. Informe resultados observados e lacunas conhecidas; só declare percentual de cobertura se houver relatório de ferramenta que o comprove.

## Logs e diagnóstico

- Use Lombok @Slf4j e mensagens parametrizadas com {} nos componentes que precisam de diagnóstico. Não use System.out, printStackTrace ou concatenação para logs.
- DEBUG: sucesso de operações rotineiras, consultas e rejeições esperadas (validação, credenciais inválidas, recurso ausente, conflito de negócio e token expirado). Não imprima stack trace para esses casos.
- INFO: marcos operacionais relevantes e pouco frequentes. Não registre toda requisição nem entrada/saída de cada método em INFO.
- WARN: situação recuperável ou configuração que exige atenção, como chave JWT efêmera. Não transforme erro normal de entrada em WARN por padrão.
- ERROR: falha inesperada que impede a operação, registrada uma única vez no ponto de tratamento. Preserve tipo e localização da falha para diagnóstico sem imprimir mensagens brutas de exceções que possam conter credenciais, SQL ou dados do cliente.
- Não registre senhas, tokens, cookies, Authorization, hashes de credenciais, DTOs/entidades inteiros, e-mails, nomes, endereços, corpo de requisição ou valores rejeitados. Não registre URI/query string brutas. Prefira mensagens estáticas, códigos de evento e metadados técnicos seguros.
- Camadas que apenas propagam ou traduzem uma exceção não devem duplicar seu log. Registre a falha onde ela é efetivamente tratada; não acrescente catch apenas para logar e relançar.
- Use INFO como nível padrão e permita ativar DEBUG somente no pacote necessário para investigação. Não silencie erros do framework nem habilite SQL, parâmetros JDBC ou debug global por padrão.
- Preserve semântica HTTP e rollback ao adicionar tratamento de falhas. Teste o comportamento e as respostas de erro relevantes. Não crie testes específicos de logs, appenders ou níveis de logging, salvo solicitação explícita do usuário; logs não substituem testes nem tornam uma exceção tratada por si só.

