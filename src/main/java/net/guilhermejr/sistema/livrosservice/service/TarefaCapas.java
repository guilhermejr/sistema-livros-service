package net.guilhermejr.sistema.livrosservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.guilhermejr.sistema.livrosservice.domain.repository.LivroRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Procura, aos poucos, a capa dos livros que ainda não tiveram busca: os que vieram
 * na migração e os que foram cadastrados ou tiveram o ISBN trocado. Cada livro é
 * tentado uma vez; quem não achou fica para o botão "Buscar capa" ou o envio à mão.
 *
 * <p>O lote pequeno espaça as consultas: a cota grátis do Google Books é de 1.000
 * por dia. Quando ela acaba (429), ou uma fonte não responde, o lote para sem
 * marcar o livro e continua na próxima rodada.</p>
 *
 * <p>Uma fonte pode falhar sempre para um mesmo livro (a Open Library devolve 503
 * para uma capa com o arquivo quebrado). Como a fila anda por id, esse livro abriria
 * todo lote e travaria os outros. Depois de {@link #FALHAS_SEGUIDAS} rodadas seguidas
 * falhando nele, ele é registrado como tentado, sem capa, e o lote segue.</p>
 *
 * <p>Desligada no perfil dev ({@code livros.capas.tarefa-ativa: false}): o banco local
 * é uma cópia dos mesmos livros, e buscar as capas dos dois lados gastaria duas vezes
 * a cota diária da mesma chave. No local, a capa só vem pelo botão "Buscar capa".</p>
 */
@Log4j2
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "livros.capas.tarefa-ativa", havingValue = "true", matchIfMissing = true)
public class TarefaCapas {

    private static final int LOTE = 10;
    static final int FALHAS_SEGUIDAS = 3;

    private final LivroRepository livroRepository;
    private final BuscadorCapas buscadorCapas;
    private final CapaService capaService;

    /** Rodadas seguidas em que a busca do livro falhou. Some ao reiniciar, e tudo bem. */
    private final Map<Long, Integer> falhas = new HashMap<>();

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT5M")
    public void buscarPendentes() {

        // Sem a chave, a busca só na Open Library marcaria como tentados livros que o
        // Google acharia depois; melhor esperar a chave.
        if (!buscadorCapas.temChaveGoogle()) {
            return;
        }

        List<Long> ids = livroRepository.idsSemCapaBuscada(Limit.of(LOTE));
        if (ids.isEmpty()) {
            return;
        }

        int achadas = 0;
        for (Long id : ids) {
            BuscadorCapas.Consulta consulta = capaService.consulta(id).orElse(null);
            if (consulta == null) {
                continue;
            }
            try {
                BuscadorCapas.Imagem imagem = buscadorCapas.buscar(consulta).orElse(null);
                falhas.remove(id);
                capaService.gravarBusca(id, imagem);
                if (imagem != null) {
                    achadas++;
                }
            } catch (BuscadorCapas.CotaEsgotadaException e) {
                log.warn("Busca de capas interrompida, continua na próxima rodada: {}", e.getMessage());
                break;
            } catch (BuscadorCapas.BuscaIndisponivelException e) {
                int vezes = falhas.merge(id, 1, Integer::sum);
                if (vezes < FALHAS_SEGUIDAS) {
                    log.warn("Busca de capas interrompida, continua na próxima rodada: {}", e.getMessage());
                    break;
                }
                falhas.remove(id);
                log.warn("Busca da capa do livro {} falhou {} rodadas seguidas; fica sem capa, para o botão \"Buscar capa\": {}",
                        id, vezes, e.getMessage());
                capaService.gravarBusca(id, null);
            }
        }
        log.info("Busca de capas: {} de {} livros com capa nova", achadas, ids.size());

    }

}
