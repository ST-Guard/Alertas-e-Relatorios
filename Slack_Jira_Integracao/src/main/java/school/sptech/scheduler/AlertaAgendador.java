package school.sptech.scheduler;

import school.sptech.app.AlertaProcessador;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import school.sptech.servicos.ServicoGeradorHistorico;


@Component
public class AlertaAgendador {

    private final AlertaProcessador alertaProcessador;
    private final ServicoGeradorHistorico servicoGeradorHistorico;

    public AlertaAgendador(AlertaProcessador alertaProcessador, ServicoGeradorHistorico servicoGeradorHistorico) {
        this.alertaProcessador = alertaProcessador;
        this.servicoGeradorHistorico = servicoGeradorHistorico;
    }

    @Scheduled(fixedDelay = 600000)
    public void verificarAlertas() {
       alertaProcessador.processar();
   }

    @Scheduled(fixedDelay = 300000)
    public void gerarHistorico() {
        servicoGeradorHistorico.gerar();
    }
}
