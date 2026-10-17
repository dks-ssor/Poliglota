# Poliglota

Aplicativo Android para analisar recursos de texto de aplicativos compatíveis e preparar traduções para português brasileiro (pt-BR).

## Estado do projeto

Este repositório inicia a base Android do Poliglota. A primeira versão terá uma interface nativa para listar aplicativos visíveis ao usuário, pesquisar e separar jogos de outros aplicativos. A tradução e a geração de APKs modificados serão implementadas por etapas, pois dependem do formato do pacote, das assinaturas e das proteções de cada aplicativo.

## Tecnologias

- Kotlin
- Android SDK
- Gradle Kotlin DSL
- Jetpack Compose

## Abrir o projeto

1. Baixe ou clone este repositório.
2. Abra a pasta no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Execute o app em um dispositivo Android ou emulador.

## Limitações importantes

Nem todo APK pode ser traduzido automaticamente. Alguns textos ficam em recursos compilados, arquivos proprietários, imagens ou servidores externos. Um APK modificado normalmente precisa ser assinado novamente e pode não funcionar como atualização do original. O Poliglota deve informar limitações em vez de declarar sucesso sem verificar o resultado.

## Privacidade

Não coloque chaves de API, senhas ou certificados privados neste repositório.