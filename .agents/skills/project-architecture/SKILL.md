---
name: project-architecture
description: Integrar funcionalidades na arquitetura existente da API de delivery, preservando pacotes por funcionalidade, camadas e contratos REST.
---

# Arquitetura existente

Leia AGENTS.md, pom.xml e um fluxo equivalente antes de editar. Confirme a estrutura atual em com.deliverytest.api.

- Preserve Package by Feature com camadas internas: order, auth e user contêm subpacotes controller, service, repository, entity, dto, mapper e exception conforme as classes existentes. Não crie pastas vazias. Exceções específicas ficam no subpacote exception da funcionalidade; exception na raiz contém apenas GlobalExceptionHandler e ErrorResponse. Evite reorganizar o projeto em camadas globais.
- Controllers representam HTTP e validam entrada; services coordenam casos de uso; repositories expressam persistência; entidades protegem invariantes, como Order.updateStatus.
- Siga DTOs record, mappers explícitos e injeção por construtor. Não exponha entidades JPA nem coloque regras de negócio em mappers.
- Defina transações no limite do caso de uso quando houver necessidade de atomicidade. Examine lazy loading, cascatas e relacionamentos antes de converter respostas.
- Preserve paginação, ordenação, ULIDs e contratos públicos no fluxo afetado. Explique alterações intencionais e atualize sua documentação.
- Não introduza arquitetura hexagonal, CQRS, mensageria ou dependências sem requisito concreto. Escolha a menor evolução coerente com o teste técnico.

- Em auth, use controller e service para os respectivos componentes; serviços de tokens ficam em service, sua persistência em repository, entidades em entity e SessionTokens em dto. Configuração, filtros e RefreshCookie ficam em security. Use AccessTokenService para emissão do JWT de acesso.


