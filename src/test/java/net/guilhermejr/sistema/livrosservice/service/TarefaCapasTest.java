package net.guilhermejr.sistema.livrosservice.service;

import net.guilhermejr.sistema.livrosservice.domain.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class TarefaCapasTest {

    private static final BuscadorCapas.Consulta TRAVADO = new BuscadorCapas.Consulta("9788533613379", "Travado", null, null);
    private static final BuscadorCapas.Consulta OUTRO = new BuscadorCapas.Consulta("9788535914849", "Outro", null, null);

    private LivroRepository livroRepository;
    private BuscadorCapas buscadorCapas;
    private CapaService capaService;
    private TarefaCapas tarefa;

    @BeforeEach
    void preparar() {

        livroRepository = mock(LivroRepository.class);
        buscadorCapas = mock(BuscadorCapas.class);
        capaService = mock(CapaService.class);
        tarefa = new TarefaCapas(livroRepository, buscadorCapas, capaService);

        when(buscadorCapas.temChaveGoogle()).thenReturn(true);
        when(livroRepository.idsSemCapaBuscada(any(Limit.class))).thenReturn(List.of(1L, 2L));
        when(capaService.consulta(1L)).thenReturn(Optional.of(TRAVADO));
        when(capaService.consulta(2L)).thenReturn(Optional.of(OUTRO));
        when(buscadorCapas.buscar(OUTRO)).thenReturn(Optional.empty());

    }

    // O caso da produção: a Open Library devolve 503 sempre para a mesma capa.
    @Test
    void livroQueFalhaSempreDeixaDeTravarAFila() {

        when(buscadorCapas.buscar(TRAVADO)).thenThrow(new BuscadorCapas.BuscaIndisponivelException("503"));

        for (int i = 1; i < TarefaCapas.FALHAS_SEGUIDAS; i++) {
            tarefa.buscarPendentes();
        }
        verify(capaService, never()).gravarBusca(anyLong(), any());

        tarefa.buscarPendentes();
        verify(capaService).gravarBusca(eq(1L), isNull());
        verify(capaService).gravarBusca(eq(2L), isNull());

    }

    @Test
    void falhaPassageiraNaoMarcaOLivro() {

        when(buscadorCapas.buscar(TRAVADO))
                .thenThrow(new BuscadorCapas.BuscaIndisponivelException("rede"))
                .thenThrow(new BuscadorCapas.BuscaIndisponivelException("rede"))
                .thenReturn(Optional.empty())
                .thenThrow(new BuscadorCapas.BuscaIndisponivelException("rede"));

        tarefa.buscarPendentes();
        tarefa.buscarPendentes();
        tarefa.buscarPendentes(); // respondeu: zera a contagem
        tarefa.buscarPendentes(); // volta a falhar, mas é a primeira vez

        verify(capaService, times(1)).gravarBusca(eq(1L), isNull());
        verify(capaService, times(1)).gravarBusca(eq(2L), isNull());

    }

    @Test
    void cotaEsgotadaNuncaMarcaOLivro() {

        when(buscadorCapas.buscar(TRAVADO)).thenThrow(new BuscadorCapas.CotaEsgotadaException());

        for (int i = 0; i < TarefaCapas.FALHAS_SEGUIDAS + 2; i++) {
            tarefa.buscarPendentes();
        }
        verify(capaService, never()).gravarBusca(anyLong(), any());

    }

}
