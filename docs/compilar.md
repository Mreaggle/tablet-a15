# Compilar os APKs

O build dos APKs locais usa o SDK do Android, Java e `zip`. Os scripts foram verificados com a plataforma Android 28 e Build Tools 35.0.0. O APK do Chogan também usa Apktool, Python 3, `patch` e `sha256sum`. [Android Studio e SDK](https://developer.android.com/studio)

## Jogos, atalhos e utilitários

Use uma chave que você controle. Guarde os arquivos fora da árvore do repositório. O arquivo de senha deve conter a senha do keystore em uma única linha. Use a mesma chave ao publicar atualizações de um app para preservar a assinatura de instalação.

```bash
export ANDROID_HOME="/caminho/para/android-sdk"
export A15_KEYSTORE="/caminho/privado/a15.p12"
export A15_KEY_PASS_FILE="/caminho/privado/a15.pass"

for app in dollmaker princesas salao otimizador shorts icones; do
  ./scripts/build-android.sh "$app"
done
```

Os APKs saem em `dist/`. Use `A15_DIST` para escolher outro destino. Para instalações do SDK com nomes ou diretórios diferentes, use `A15_BUILD_TOOLS` e `A15_ANDROID_JAR`. É possível informar um JAR do compilador Eclipse em `A15_ECJ_JAR` para substituir o `javac`.

Para uma chave nova com JDK, use o comando abaixo com o diretório privado já criado. O `keytool` vai solicitar os dados da chave e da senha. Escolha um nome de certificado que você possa tornar público. A assinatura inclui o certificado público no APK.

```bash
keytool -genkeypair -keystore "$A15_KEYSTORE" -storetype PKCS12 \
  -alias a15 -keyalg RSA -keysize 2048 -validity 10000
```

Os APKs de Princesas e Salão usam os arquivos web em [apps/jogos-offline/web](../apps/jogos-offline/web). O patch, a revisão upstream e o HTML da adaptação de Princesas estão em [fontes/princesas](../apps/jogos-offline/fontes/princesas). O build Android usa as imagens reduzidas e o JavaScript compilado já incluídos no repositório.

Para regenerar os dois arquivos web do Salão a partir do projeto Scratch editável, use Node.js e npm no diretório do empacotador.

```bash
cd apps/jogos-offline/fontes/salao
npm ci
mkdir -p web
node pack.cjs
cp web/index.html web/polyfills.js ../../web/salao/
```

## Chogan com a correção para WebView 66

Baixe `io.github.choganhq.chogan-0.38.0.apk` e `apktool_3.0.3.jar`. O script verifica o SHA-256 do Chogan, aplica o patch aos seis jogos, muda o ID do pacote e gera uma versão assinada. [Chogan v0.38.0](https://github.com/choganhq/chogan/releases/tag/v0.38.0) · [Apktool v3.0.3](https://github.com/iBotPeaches/Apktool/releases/tag/v3.0.3)

```bash
./scripts/build-chogan.sh /caminho/io.github.choganhq.chogan-0.38.0.apk /caminho/apktool_3.0.3.jar
```

Os builds foram compilados e passaram na validação de assinatura. Os testes no A15 correspondem às versões e funcionalidades descritas na [cronologia](../README.md#como-o-projeto-evoluiu). O ícone público de vídeo foi compilado e ainda não passou por teste nessa unidade.
