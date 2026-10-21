---
name: spring-exceptions
description: Criar ou ajustar validações e tratamento de exceções Spring MVC mantendo ErrorResponse, status HTTP e mensagens internacionalizadas desta API.
---

# Exceções e validação

Leia AGENTS.md, o handler e ErrorResponse em src/main/java/com/deliverytest/api/exception, as exceções nos subpacotes exception de order, user e auth, os DTOs afetados e src/main/resources/messages.properties.

- Valide formato e obrigatoriedade com Jakarta Validation nos DTOs e @Valid na entrada, incluindo objetos aninhados. Proteja invariantes de negócio no domínio também fora de HTTP.
- Use exceções específicas para falhas esperadas, seguindo OrderNotFoundException e InvalidOrderStatusException: chave de mensagem e argumentos separados, traduzidos no handler.
- Centralize a tradução HTTP em GlobalExceptionHandler. Preserve ErrorResponse(status, message, fieldErrors), MessageSource e LocaleContextHolder. Adicione chaves aos bundles existentes quando necessário.
- Preserve os significados atuais: recurso ausente retorna 404; transição inválida, 409; validação e JSON ilegível, 400. Escolha outros status pelo significado e pelo contrato do endpoint.
- Não esconda falhas com null, sucesso ou catch genérico. Capture apenas para recuperar, acrescentar contexto útil ou traduzir na fronteira apropriada, preservando a causa técnica.
- Não exponha stack traces, SQL, caminhos ou mensagens brutas de infraestrutura. Se a tarefa exigir tratar falhas inesperadas, use mensagem pública genérica e diagnóstico interno sem dados sensíveis.
- Falhas de segurança podem ocorrer antes do MVC; examine Spring Security e seus handlers antes de depender de RestControllerAdvice.
- Ao mudar comportamento executável, verifique falhas relevantes, status, formato, campos e tradução, além de ausência de efeitos parciais em operações rejeitadas.

