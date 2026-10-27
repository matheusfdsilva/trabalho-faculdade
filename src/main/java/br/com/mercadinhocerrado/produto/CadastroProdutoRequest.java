package br.com.mercadinhocerrado.produto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record CadastroProdutoRequest(
        @NotBlank(message = "Informe o nome do produto.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        @Pattern(regexp = "[^<>\\p{Cntrl}]*", message = "O nome não deve conter marcações ou caracteres de controle.")
        String nome,

        @NotBlank(message = "Informe a categoria.")
        @Size(max = 80, message = "A categoria deve ter no máximo 80 caracteres.")
        @Pattern(regexp = "[^<>\\p{Cntrl}]*", message = "A categoria não deve conter marcações ou caracteres de controle.")
        String categoria,

        @NotNull(message = "Informe o preço.")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter até 8 dígitos inteiros e 2 casas decimais.")
        BigDecimal preco,

        @NotNull(message = "Informe a quantidade em estoque.")
        @PositiveOrZero(message = "A quantidade em estoque não pode ser negativa.")
        Integer quantidadeEstoque,

        @NotNull(message = "Informe o estoque mínimo.")
        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo.")
        Integer estoqueMinimo) {
}
