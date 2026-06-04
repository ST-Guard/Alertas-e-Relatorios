# GerarRelatorioJava

Aplicacao Java de console que le os dados financeiros de um JSON no S3, gera PDF mensal e envia o arquivo final para o S3.

## Estrutura

- `Main`: orquestra a execucao da aplicacao.
- `config/AppConfig`: centraliza bucket, chaves do S3 e regiao.
- `s3/S3StorageService`: baixa o JSON e envia o PDF para o S3.
- `service/RelatorioJsonService`: interpreta o JSON e monta os objetos de relatorio.
- `service/PdfService`: monta o HTML e gera o PDF.
- `model/RelatorioFinanceiro`: representa os dados do relatorio.
- `util/Formatador`: formata moeda, percentual, HTML e nomes de arquivo.

## Configuracao AWS

Crie um arquivo `.env` na raiz do projeto, usando o `.env.example` como base:

```text
AWS_ACCESS_KEY_ID=sua_access_key
AWS_SECRET_ACCESS_KEY=sua_secret_key
AWS_SESSION_TOKEN=seu_session_token
AWS_REGION=us-east-1

S3_BUCKET=smartdatabucket2
S3_JSON_KEY=client/dashFinanceira.json
S3_PDF_KEY=relatorios/relatorio-financeiro.pdf
```

O `.env` esta no `.gitignore`, entao suas credenciais nao devem ser versionadas.

Tambem e possivel configurar as variaveis de ambiente pelo PowerShell:

```powershell
$env:AWS_ACCESS_KEY_ID="sua_access_key"
$env:AWS_SECRET_ACCESS_KEY="sua_secret_key"
$env:AWS_SESSION_TOKEN="seu_session_token"
$env:AWS_REGION="us-east-1"
```

Valores padrao usados pelo projeto:

```powershell
$env:S3_BUCKET="smartdatabucket2"
$env:S3_JSON_KEY="client/dashFinanceira.json"
$env:S3_PDF_KEY="relatorios/relatorio-financeiro.pdf"
```

Se existir mais de um relatorio no JSON, o projeto cria um PDF por mes, por exemplo:

```text
relatorios/relatorio-financeiro-maio-2025.pdf
```

## Executar

```powershell
mvn compile exec:java
```

O JSON pode ser:

- um objeto unico;
- um array de relatorios;
- um objeto com uma lista em `relatorios`, `reports`, `meses` ou `months`.

Campos reconhecidos para os valores principais:

- custo: `custo`, `custoTotal`, `totalCusto`, `custoOperacional`, `cost`, `totalCost`, `operationalCost`
- receita: `receita`, `receitaTotal`, `totalReceita`, `receitaEstimada`, `revenue`, `totalRevenue`, `estimatedRevenue`
- margem: `margem`, `margemLiquida`, `lucro`, `profit`, `margin`
- ROI: `roi`, `ROI`, `roiMensal`, `monthlyRoi`
- mes: `month`, `mes`, `mesAno`, `periodo`, `competencia`
- gerado em: `gen`, `geradoEm`, `generatedAt`, `dataGeracao`
