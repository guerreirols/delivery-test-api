---
name: java-clean-code
description: Implementar ou refatorar código Java deste projeto com Clean Code, responsabilidades coesas e mudanças pequenas, preservando comportamento e convenções locais.
---

# Clean Code em Java

- Leia o fluxo afetado e um componente equivalente antes de editar. Siga `AGENTS.md` na raiz.
- Nomeie pelo significado no domínio; evite nomes genéricos como manager, processor ou util quando houver uma responsabilidade concreta.
- Separe orquestração, regra de negócio, conversão e acesso a dados conforme os componentes existentes. Extraia métodos quando revelarem intenção ou eliminarem duplicação relevante; não fragmente por limite arbitrário de linhas.
- Use injeção por construtor e dependências finais. Preserve o estilo Lombok existente sem adicionar anotações que exponham estado ou relacionamentos JPA inadvertidamente.
- Prefira DTOs imutáveis e encapsule mudanças de estado nas entidades. Use `BigDecimal` para valores monetários e preserve a política de escala e arredondamento do domínio.
- Aplique SOLID quando resolver um problema concreto. Evite interfaces de implementação única, classes base e padrões de projeto sem benefício verificável.
- Não misture refatorações amplas com uma correção pontual. Comprove preservação do comportamento com testes relevantes quando houver mudança executável.
- Declare os tipos explicitamente em Java, inclusive em testes; não use var.
- Não adicione comentários no corpo do código. Use nomes e métodos claros para expressar intenção e documente decisões no README quando necessário.
- Revise nomes, ramificações, efeitos colaterais e código morto.


## Revisão de responsabilidades

- Métodos públicos de caso de uso devem expressar a sequência do negócio em um único nível de abstração. Separe validação de negócio, construção do objeto, autenticação e persistência quando misturados ocultarem essa sequência.
- Extraia métodos privados com nomes de intenção para etapas coesas. Mova detalhes de infraestrutura, como classificação de constraints SQL, para um componente de persistência da funcionalidade; não deixe mensagens de driver no service de autenticação.
- Não use apenas quantidade de linhas como critério: verifique se o método mistura motivos de mudança, níveis de abstração ou tratamento técnico com orquestração. Não crie abstrações vazias apenas para encurtar o método.
- Não aplique @Transactional por padrão. Identifique a unidade de trabalho, a atomicidade necessária e dependências de contexto JPA. Uma gravação isolada pode usar a transação do repository; múltiplas operações que precisam confirmar ou reverter juntas exigem fronteira explícita no caso de uso.
- Não mantenha transações abertas durante hash de senha ou emissão de tokens sem necessidade. readOnly é uma indicação de otimização, não garantia de ausência de escrita.
- Uma consulta prévia de existência não garante exclusividade concorrente. Preserve a constraint do banco e traduza somente a violação conhecida, sem consultar novamente uma transação que falhou.
