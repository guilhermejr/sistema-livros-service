package net.guilhermejr.sistema.livrosservice.api.controller;

import lombok.RequiredArgsConstructor;
import net.guilhermejr.sistema.livrosservice.domain.entity.Capa;
import net.guilhermejr.sistema.livrosservice.service.CapaService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/**
 * A imagem da capa, sem token: o frontend a usa num {@code <img>}, que não manda o
 * cabeçalho Authorization. Liberado em {@code SegurancaCapasConfig}. Uma capa trocada
 * ganha outro id, então a resposta pode ficar em cache para sempre.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/capas")
public class CapaController {

    private final CapaService capaService;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> retornar(@PathVariable UUID id) {

        Capa capa = capaService.retornar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(capa.getTipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(capa.getConteudo());

    }

}
