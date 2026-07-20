# DreamJEmu — Um Emulador de Dreamcast em Java

*[Read this in English](../../README.md)*

> A documentação completa e a comunicação de contribuições (issues, PRs, commits) são mantidas em **Inglês**, que é o idioma principal do projeto. Este documento é um resumo em Português; a versão em inglês (`README.md`, na raiz do repositório) é a fonte oficial e mais completa.

## O que é este projeto

O DreamJEmu é um emulador de Sega Dreamcast, escrito em **Java**, com interface em **JavaFX** e renderização baseada em **Vulkan** (sem suporte a OpenGL, nem planos de o ter). Não requer BIOS nem qualquer ficheiro original extraído de uma consola real.

## Princípios do projeto

- **O projeto nunca terá qualquer intenção de angariar dinheiro.** Sem doações, patrocínios, anúncios ou versões pagas.
- **O projeto não apoia a pirataria.** Não distribui nem liga a jogos, BIOS ou material protegido por direitos de autor.
- **Tudo é público e livre.** Código, documentação e builds vivem neste repositório público, sob licença GPLv3 (`LICENSE`).
- **Contribuições de IA são bem-vindas**, desde que sigam as mesmas regras que as contribuições humanas (`CONTRIBUTING.md`). A IA foi extensivamente usada para criar a estrutura e documentação inicial deste projeto.
- **Não são necessários ficheiros originais da consola** (BIOS, flash ROM, etc.).

## Prioridades do projeto

1. **Estrutura e precisão do emulador** em primeiro lugar.
2. **Compatibilidade e performance** em segundo lugar.
3. **Melhorias gráficas modernas** (Vulkan, up-scaling como o FSR, resoluções superiores) como prioridade central desde o início — não um extra tardio.
4. **Suporte multiplataforma**: Windows, Linux, macOS e Android. Suporte total a macOS/arm64 com Metal está planeado, mas não é prioridade inicial — apenas a estrutura base para esse suporte é uma meta inicial.

## Requisitos mínimos

Ver `docs/MINIMUM_REQUIREMENTS.md` (em inglês). Em resumo: sistema operativo de 64 bits, e uma GPU/driver com **suporte a Vulkan 1.2+** — este é um requisito rígido, sem alternativa a OpenGL. Reports de problemas em hardware que não cumpra este requisito são inválidos e serão fechados.

## Como contribuir

Todas as regras detalhadas estão em `CONTRIBUTING.md` (Inglês). Resumo:

- Todas as contribuições (código, issues, PRs, commits) devem ser escritas em **Inglês**.
- Todo PR deve explicar: **o que faz**, **como o faz**, **porque é necessário**, e **qual foi o teste/jogo usado para validar**.
- Contribuições devem focar-se preferencialmente em áreas assinaladas em `docs/STATUS.md` e `docs/ROADMAP.md` como precisando de trabalho.
- Contribuições que não cumpram estes critérios serão fechadas.
- Contribuições de IA são bem-vindas nas mesmas condições — ver `docs/AI_CONTRIBUTIONS.md`.

## Reports

- **Report de compatibilidade** (comportamento de um jogo específico): deve incluir o jogo testado, o log, a versão do emulador, e uma descrição do comportamento atual.
- **Report de problema** (bug geral): deve incluir a versão do emulador, um log (se necessário), a descrição do problema, e instruções de reprodução.
- Reports sobre hardware não suportado (ex.: GPU sem Vulkan) são inválidos.

## Continuar este projeto noutra conversa de IA

Ver `docs/AI_CONTINUATION.md` (Inglês) — é um resumo autocontido pensado para ser colado numa nova conversa com qualquer assistente de IA para retomar o contexto do projeto.

## Licença

GNU General Public License v3.0 — ver `LICENSE`.
