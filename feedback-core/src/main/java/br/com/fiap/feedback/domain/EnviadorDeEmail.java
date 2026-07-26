package br.com.fiap.feedback.domain;

/**
 * Porta de saida para envio de e-mail aos administradores (RF-03 e RF-04).
 *
 * <p>Os enderecos de remetente e destinatarios sao configuracao de ambiente,
 * resolvidos pelo adaptador; o dominio decide apenas o conteudo.
 */
public interface EnviadorDeEmail {

    void enviarParaAdministradores(String assunto, String corpoHtml);
}
