package com.sptech.school.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sptech.school.model.RelatorioFinanceiro;
import com.sptech.school.util.Formatador;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

public class PdfService {
    public byte[] gerarPdf(RelatorioFinanceiro relatorio) throws Exception {
        String html = montarHtml(relatorio);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }

    private String montarHtml(RelatorioFinanceiro relatorio) {
        BigDecimal custoSobreReceita = Formatador.calcularPercentual(relatorio.custo(), relatorio.receita());
        String roiClasse = relatorio.roi().compareTo(BigDecimal.ZERO) >= 0 ? "td-pos" : "td-neg";

        return template()
                .replace("{{generatedAt}}", Formatador.escaparHtml(relatorio.generatedAt()))
                .replace("{{month}}", Formatador.escaparHtml(relatorio.month()))
                .replace("{{roi}}", Formatador.formatarPercentual(relatorio.roi()))
                .replace("{{custo}}", Formatador.formatarMoeda(relatorio.custo()))
                .replace("{{receita}}", Formatador.formatarMoeda(relatorio.receita()))
                .replace("{{margem}}", Formatador.formatarMoeda(relatorio.margem()))
                .replace("{{roiClasse}}", roiClasse)
                .replace("{{custoSobreReceita}}", Formatador.formatarPercentual(custoSobreReceita));
    }

    private String template() {
        return """
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    @page { size: A4; margin: 0; }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      font-family: Arial, Helvetica, sans-serif;
                      color: #08244a;
                      background: #ffffff;
                      font-size: 12px;
                    }
                    .pagina-relatorio {
                      width: 210mm;
                      min-height: 297mm;
                      padding: 18mm 16mm 16mm;
                      position: relative;
                    }
                    .capa-relatorio { border-bottom: 2px solid #2c83d4; padding-bottom: 12mm; }
                    .cabecalho-capa-relatorio {
                      display: table;
                      width: 100%;
                      margin-bottom: 12mm;
                    }
                    .logo-capa-relatorio, .badge-capa-relatorio { display: table-cell; vertical-align: middle; }
                    .logo-capa-relatorio { width: 70%; }
                    .marca-capa-relatorio {
                      font-size: 28px;
                      font-weight: 700;
                      letter-spacing: 0;
                      color: #0a1f44;
                    }
                    .marca-capa-relatorio span { color: #1119ff; }
                    .sub-logo-capa-relatorio {
                      color: #58708f;
                      font-size: 10px;
                      margin-top: 5px;
                    }
                    .badge-capa-relatorio {
                      width: 30%;
                      text-align: center;
                      border: 1px solid #9fcaf2;
                      background: #eef7ff;
                      color: #2c5d86;
                      padding: 10px 14px;
                    }
                    .rotulo-badge-capa-relatorio {
                      text-transform: uppercase;
                      font-size: 8px;
                      color: #6c8bab;
                      font-weight: 700;
                    }
                    .val-badge-capa-relatorio { font-weight: 700; margin-top: 3px; }
                    .titulo-capa-relatorio h1 {
                      font-size: 23px;
                      font-weight: 500;
                      margin: 0 0 6px;
                      color: #081f43;
                    }
                    .titulo-capa-relatorio p {
                      margin: 0;
                      color: #60748f;
                    }
                    .tags-capa-relatorio { margin-top: 10px; }
                    .tag-relatorio {
                      display: inline-block;
                      min-width: 72px;
                      padding: 5px 12px;
                      margin-right: 8px;
                      font-size: 8px;
                      color: #1d5d8f;
                    }
                    .tag-relatorio.blue { background: #e5f4ff; }
                    .tag-relatorio.green { background: #e4f5e9; color: #087044; }
                    .tag-relatorio.gold { background: #fbf1c9; color: #8a6a00; }
                    .secao-relatorio { margin-top: 12mm; }
                    .titulo-secao-relatorio {
                      color: #1168a9;
                      font-size: 10px;
                      text-transform: uppercase;
                      border-left: 2px solid #1168a9;
                      padding-left: 10px;
                      margin-bottom: 10px;
                    }
                    .grade-kpi-relatorio {
                      display: table;
                      width: 100%;
                      border-collapse: separate;
                      border-spacing: 10px 0;
                    }
                    .kpi-relatorio {
                      display: table-cell;
                      width: 25%;
                      border: 1px solid #cbd9e8;
                      padding: 13px;
                      min-height: 78px;
                    }
                    .rotulo-kpi-relatorio {
                      color: #6e82a0;
                      font-size: 8px;
                      text-transform: uppercase;
                      margin-bottom: 9px;
                    }
                    .val-kpi-relatorio {
                      color: #0b2348;
                      font-size: 17px;
                      font-weight: 700;
                      margin-bottom: 8px;
                    }
                    .delta-kpi-relatorio {
                      font-size: 9px;
                      font-weight: 700;
                    }
                    .up-good { color: #008752; }
                    .up-bad { color: #d34b3f; }
                    .tabela-relatorio {
                      width: 100%;
                      border-collapse: collapse;
                      font-size: 10px;
                    }
                    .tabela-relatorio th {
                      background: #eaf4fb;
                      color: #53708c;
                      text-align: left;
                      padding: 10px 14px;
                      text-transform: uppercase;
                      font-size: 8px;
                      font-weight: 700;
                    }
                    .tabela-relatorio td {
                      border: 1px solid #d6e0ea;
                      padding: 12px 14px;
                      color: #0b2348;
                    }
                    .td-num { text-align: right; font-weight: 700; }
                    .td-pos { text-align: right; font-weight: 700; color: #008752 !important; }
                    .td-neg { text-align: right; font-weight: 700; color: #c83232 !important; }
                    .td-muted { color: #647892 !important; font-size: 9px; }
                    .rodape-relatorio {
                      position: absolute;
                      left: 16mm;
                      right: 16mm;
                      bottom: 15mm;
                      border-top: 1px solid #cbd9e8;
                      padding-top: 10px;
                      display: table;
                      width: 178mm;
                      color: #0b2348;
                    }
                    .esq-rodape-relatorio, .dir-rodape-relatorio {
                      display: table-cell;
                      width: 50%;
                      line-height: 1.7;
                    }
                    .dir-rodape-relatorio { text-align: right; }
                    .rodape-relatorio small { color: #627994; }
                  </style>
                </head>
                <body>
                  <main class="pagina-relatorio">
                    <section class="capa-relatorio">
                      <div class="cabecalho-capa-relatorio">
                        <div class="logo-capa-relatorio">
                          <div class="marca-capa-relatorio">smart<span>data</span></div>
                          <div class="sub-logo-capa-relatorio">Infraestrutura STEAM</div>
                        </div>
                        <div class="badge-capa-relatorio">
                          <div class="rotulo-badge-capa-relatorio">Gerado em</div>
                          <div class="val-badge-capa-relatorio">{{generatedAt}}</div>
                        </div>
                      </div>
                      <div class="titulo-capa-relatorio">
                        <h1>Relatório Mensal - {{month}}</h1>
                        <p>Resumo financeiro consolidado de custos, receitas, margem e ROI.</p>
                      </div>
                      <div class="tags-capa-relatorio">
                        <span class="tag-relatorio blue">Financeiro</span>
                        <span class="tag-relatorio green">ROI {{roi}}</span>
                        <span class="tag-relatorio gold">Fechamento mensal</span>
                      </div>
                    </section>

                    <section class="secao-relatorio">
                      <div class="titulo-secao-relatorio">Indicadores principais</div>
                      <div class="grade-kpi-relatorio">
                        <div class="kpi-relatorio">
                          <div class="rotulo-kpi-relatorio">Custo total</div>
                          <div class="val-kpi-relatorio">{{custo}}</div>
                          <div class="delta-kpi-relatorio up-bad">Infraestrutura</div>
                        </div>
                        <div class="kpi-relatorio">
                          <div class="rotulo-kpi-relatorio">Receita total</div>
                          <div class="val-kpi-relatorio">{{receita}}</div>
                          <div class="delta-kpi-relatorio up-good">Faturamento</div>
                        </div>
                        <div class="kpi-relatorio">
                          <div class="rotulo-kpi-relatorio">Margem</div>
                          <div class="val-kpi-relatorio">{{margem}}</div>
                          <div class="delta-kpi-relatorio up-good">Receita - custo</div>
                        </div>
                        <div class="kpi-relatorio">
                          <div class="rotulo-kpi-relatorio">ROI</div>
                          <div class="val-kpi-relatorio">{{roi}}</div>
                          <div class="delta-kpi-relatorio up-good">Retorno estimado</div>
                        </div>
                      </div>
                    </section>

                    <section class="secao-relatorio">
                      <div class="titulo-secao-relatorio">Resumo financeiro</div>
                      <table class="tabela-relatorio">
                        <thead>
                          <tr><th>Métrica</th><th>Valor</th><th>Leitura</th></tr>
                        </thead>
                        <tbody>
                          <tr><td>Custo operacional</td><td class="td-num">{{custo}}</td><td class="td-muted">Energia, rede, hardware e operação</td></tr>
                          <tr><td>Receita estimada</td><td class="td-num">{{receita}}</td><td class="td-muted">Receita gerada pela infraestrutura</td></tr>
                          <tr><td>Margem líquida</td><td class="td-pos">{{margem}}</td><td class="td-muted">Diferença entre receita e custo</td></tr>
                          <tr><td>ROI mensal</td><td class="{{roiClasse}}">{{roi}}</td><td class="td-muted">Rentabilidade estimada no período</td></tr>
                          <tr><td>Custo sobre receita</td><td class="td-num">{{custoSobreReceita}}</td><td class="td-muted">Quanto da receita foi consumida por custos</td></tr>
                        </tbody>
                      </table>
                    </section>

                    <footer class="rodape-relatorio">
                      <div class="esq-rodape-relatorio">Smart Data<br /><small>Relatório gerado automaticamente pelo dashboard financeiro.</small></div>
                      <div class="dir-rodape-relatorio">{{month}}<br /><small>{{generatedAt}}</small></div>
                    </footer>
                  </main>
                </body>
                </html>
                """;
    }
}
