package net.guilhermejr.sistema.livrosservice.exception;

import lombok.extern.log4j.Log4j2;
import net.guilhermejr.sistema.livrosservice.exception.dto.ErrorDefaultDTO;
import net.guilhermejr.sistema.livrosservice.exception.dto.ErrorRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

@Log4j2
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ExceptionDefault.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<ErrorDefaultDTO> handleErroDefault(ExceptionDefault ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO(ex.getMessage()));

    }

    @ExceptionHandler(ExceptionNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public List<ErrorDefaultDTO> handleErroNotFound(ExceptionNotFound ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO(ex.getMessage()));

    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<ErrorDefaultDTO> handleErroParametro(Exception ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO("Parâmetro inválido."));

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<ErrorRequestDTO> handleErroValidacao(MethodArgumentNotValidException ex) {

        log.error(ex.getMessage());
        return ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorRequestDTO(e.getField(), e.getField() + ": " + e.getDefaultMessage()))
                .toList();

    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<ErrorDefaultDTO> handleErroTamanho(MaxUploadSizeExceededException ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO("A imagem passa de 5 MB."));

    }

    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<ErrorDefaultDTO> handleErroArquivo(Exception ex) {

        log.error(ex.getMessage(), ex);
        return List.of(new ErrorDefaultDTO("Envie a imagem da capa."));

    }

}
