package br.com.fiap.feedback.adapter.in.rest.dto;

import br.com.fiap.feedback.domain.Avaliacao;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Resultado da consulta de avaliacoes (SPEC 5.2).
 */
@Schema(name = "ListaDeAvaliacoes", description = "Avaliacoes encontradas no periodo")
public record ListaDeAvaliacoesResponse(List<AvaliacaoResponse> itens, int total) {

    public static ListaDeAvaliacoesResponse de(List<Avaliacao> avaliacoes) {
        List<AvaliacaoResponse> itens = avaliacoes.stream().map(AvaliacaoResponse::de).toList();
        return new ListaDeAvaliacoesResponse(itens, itens.size());
    }
}
