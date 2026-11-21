package net.guilhermejr.sistema.livrosservice.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.guilhermejr.sistema.livrosservice.api.request.LivroRequest;
import net.guilhermejr.sistema.livrosservice.api.response.DadosIsbnResponse;
import net.guilhermejr.sistema.livrosservice.api.response.LivroResponse;
import net.guilhermejr.sistema.livrosservice.api.response.OpcoesResponse;
import net.guilhermejr.sistema.livrosservice.service.CapaService;
import net.guilhermejr.sistema.livrosservice.service.LivroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@RestController
@PreAuthorize("hasAnyRole('LIVROS')")
@RequestMapping("/livros")
public class LivroController {

    private final LivroService livroService;
    private final CapaService capaService;

    @GetMapping
    public ResponseEntity<List<LivroResponse>> listar() {

        log.info("Listando livros");
        return ResponseEntity.status(HttpStatus.OK).body(livroService.listar());

    }

    @GetMapping("/opcoes")
    public ResponseEntity<OpcoesResponse> opcoes() {

        log.info("Listando opções do formulário de livro");
        return ResponseEntity.status(HttpStatus.OK).body(livroService.opcoes());

    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<DadosIsbnResponse> dadosPorIsbn(@PathVariable String isbn) {

        log.info("Buscando dados do ISBN {}", isbn);
        return ResponseEntity.status(HttpStatus.OK).body(livroService.dadosPorIsbn(isbn));

    }

    @GetMapping("/{id}")
    public ResponseEntity<LivroResponse> detalhar(@PathVariable Long id) {

        log.info("Detalhando livro {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(livroService.detalhar(id));

    }

    @PostMapping
    public ResponseEntity<LivroResponse> inserir(@Valid @RequestBody LivroRequest livroRequest) {

        log.info("Inserindo livro");
        return ResponseEntity.status(HttpStatus.CREATED).body(livroService.inserir(livroRequest));

    }

    @PutMapping("/{id}")
    public ResponseEntity<LivroResponse> atualizar(@PathVariable Long id, @Valid @RequestBody LivroRequest livroRequest) {

        log.info("Atualizando livro {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(livroService.atualizar(id, livroRequest));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable Long id) {

        log.info("Apagando livro {}", id);
        livroService.apagar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

    @PostMapping("/{id}/capa/buscar")
    public ResponseEntity<LivroResponse> buscarCapa(@PathVariable Long id) {

        log.info("Buscando capa do livro {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(capaService.buscar(id));

    }

    @PutMapping(value = "/{id}/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LivroResponse> enviarCapa(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo) {

        log.info("Enviando capa do livro {}: {}", id, arquivo.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.OK).body(capaService.enviar(id, arquivo));

    }

    @DeleteMapping("/{id}/capa")
    public ResponseEntity<LivroResponse> removerCapa(@PathVariable Long id) {

        log.info("Removendo capa do livro {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(capaService.remover(id));

    }

}
