# Timer Jitsu: respostas para o Play Console

Rascunho baseado no código da versão 0.1.0. Revise cada item antes de enviar: as declarações são suas, e as opções
do Console podem ter nomes diferentes dos usados aqui.

**Atualize este arquivo e a política de privacidade se o app ganhar internet, anúncios, login ou cobrança.**

## Dados do app

| Campo | Resposta |
| --- | --- |
| Nome | Timer Jitsu |
| Idioma padrão | Português (Brasil) |
| Tipo | App |
| Gratuito ou pago | Gratuito (depois de publicado como gratuito, não pode virar pago; o premium teria de ser compra dentro do app) |
| Categoria | Saúde e fitness |
| Política de privacidade | `https://viniciusrio.github.io/timerjitsu/privacy-policy.html` (só funciona depois de ativar o GitHub Pages) |

## Segurança dos dados

- **O app coleta ou compartilha dados de usuário?** Não coleta: o manifesto não pede `INTERNET`, e o app não tem
  SDKs de análise ou anúncios. O backup automático está desativado (`android:allowBackup="false"`).
- **Ponto a decidir: opção Voz.** Com a opção ligada (vem desligada), o nome do intervalo vai para o mecanismo de voz
  do aparelho. A página do Google diz que transferências para outro app no mesmo aparelho contam como compartilhamento,
  mas não trata desse caso. Sugestão: leia a seção "Compartilhamento" da página e decida se declara. A política de
  privacidade já descreve essa transferência.
- **Exclusão de dados:** não há conta nem dados no servidor.

## Apps de saúde

A página do Google não trata de timers. "Atividade e fitness" cobre apps que "monitoram e registram" exercícios, e
o Timer Jitsu não registra nenhum treino realizado.

- **Sugestão:** marcar "Atividade e fitness", por ser a opção mais conservadora para um app da categoria Saúde e
  fitness com modelos de treino.
- **Alternativa:** "Meu app não oferece recursos de saúde", também defensável.
- O aviso de que o app não é dispositivo médico já está na descrição e na política.

## Serviço em primeiro plano

**Tipo:** `mediaPlayback`. O Google descreve esse caso de uso como reprodução de áudio ou vídeo iniciada pelo usuário.
A lista de casos de uso não é exaustiva, e é possível digitar um personalizado.

**Descrição da função:**

> O usuário cria um timer de treino e toca em iniciar. O serviço reproduz os sinais sonoros sintetizados pelo app
> (sino, gongo, apito, bipes e contagem regressiva) e a voz opcional, e mantém a contagem dos intervalos enquanto a
> tela está apagada ou o app está em segundo plano, pois a pessoa está treinando longe do celular. Uma notificação
> permanente mostra o intervalo atual e permite pausar ou continuar. O serviço termina quando o treino acaba ou é
> encerrado pelo usuário.

**Impacto de uma interrupção:**

> Se o serviço não iniciar na hora ou for interrompido, os sinais sonoros deixam de tocar no momento certo e a
> contagem do treino para ou fica errada. O usuário perde os avisos de início e fim dos intervalos no meio do
> exercício, sem conseguir perceber isso, pois normalmente não está olhando para a tela.

**Roteiro do vídeo** (30 a 60 segundos, com áudio, publicado no YouTube como "não listado"):

1. Abrir o app e entrar em um timer, como "Rounds de 3 min" na aba Modelos.
2. Tocar em iniciar e deixar o som tocar.
3. Apagar a tela ou voltar à tela inicial, com os sons continuando.
4. Mostrar a notificação e pausar por ela.

## Classificação de conteúdo

- **Categoria do questionário:** utilitário/produtividade, ou a opção mais próxima.
- **Violência, sexo, linguagem, drogas, jogos de azar, conteúdo gerado por usuários compartilhado:** não. O texto
  digitado pelo usuário fica só no aparelho, e "luta" aparece apenas como nome de treino.
- **Interação entre usuários, compartilhamento de localização, compras digitais:** não.

## Outras declarações

| Declaração | Resposta |
| --- | --- |
| Anúncios | Não |
| Acesso ao app | Todas as funções sem login ou restrição |
| Público-alvo | 18 anos ou mais (recomendado, pois incluir menores traz regras extras) |
| App de notícias | Não |
| App governamental | Não |
| Recursos financeiros | Nenhum |
| ID de publicidade | Não usa |

## Imagens e textos

- `play-store/pt-BR/`: nome, descrição curta e descrição completa
- `play-store/images/icon-512.png`: ícone
- `play-store/images/feature-graphic-1024x500.png`: imagem de destaque
- `play-store/images/phoneScreenshots/`: capturas, na ordem de envio
