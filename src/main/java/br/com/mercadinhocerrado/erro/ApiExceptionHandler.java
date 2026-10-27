package br.com.mercadinhocerrado.erro;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErroCampo(String campo, String mensagem) { }
    public record ErroApi(String mensagem, List<ErroCampo> campos) { }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroApi tratarValidacao(MethodArgumentNotValidException erro) {
        var campos = erro.getBindingResult().getFieldErrors().stream()
                .map(campo -> new ErroCampo(campo.getField(), campo.getDefaultMessage()))
                .toList();
        return new ErroApi("Confira os campos informados.", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroApi tratarJsonInvalido() {
        return new ErroApi("Envie um JSON válido com os tipos esperados. Quantidades devem ser números inteiros.", List.of());
    }
}
