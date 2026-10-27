package br.com.mercadinhocerrado.erro;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import br.com.mercadinhocerrado.produto.ProdutoNaoEncontradoException;
import br.com.mercadinhocerrado.produto.ProdutoComHistoricoException;
import br.com.mercadinhocerrado.movimentacao.MovimentacaoInvalidaException;
import org.springframework.dao.DataAccessException;

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
        return new ErroApi("Envie um JSON válido apenas com os campos aceitos e os tipos esperados. Quantidades devem ser números inteiros.", List.of());
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroApi tratarParametrosInvalidos() {
        return new ErroApi("Confira os parâmetros: identificador positivo, nome de até 150 caracteres, tipo ENTRADA ou SAIDA, reposicao true ou false, página de 0 a 1000000 e tamanho de 1 a 100.", List.of());
    }

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroApi tratarProdutoNaoEncontrado(ProdutoNaoEncontradoException erro) {
        return new ErroApi(erro.getMessage(), List.of());
    }

    @ExceptionHandler(MovimentacaoInvalidaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroApi tratarMovimentacaoInvalida(MovimentacaoInvalidaException erro) {
        return new ErroApi(erro.getMessage(), List.of());
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErroApi tratarFalhaDePersistencia() {
        return new ErroApi("Não foi possível concluir a operação no banco de dados. Tente novamente em instantes; em uma movimentação, reenvie a mesma tentativa.", List.of());
    }

    @ExceptionHandler(ProdutoComHistoricoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroApi tratarProdutoComHistorico(ProdutoComHistoricoException erro) {
        return new ErroApi(erro.getMessage(), List.of());
    }
}
