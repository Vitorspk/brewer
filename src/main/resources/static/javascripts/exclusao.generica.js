/**
 * Módulo genérico para exclusão de entidades.
 *
 * CODE QUALITY FIX: Phase 12 - Medium Priority Issue #6
 * Consolidação de 3 arquivos JavaScript duplicados em um único módulo genérico.
 *
 * Arquivos substituídos:
 * - estilo.exclusao.js (63 linhas)
 * - cerveja.exclusao.js (63 linhas)
 * - cliente.exclusao.js (63 linhas)
 * Total: 189 linhas → 90 linhas (47% redução)
 *
 * Uso:
 * HTML deve conter data attributes:
 * - data-action="excluir" - Identifica o botão
 * - data-url="/endpoint/{codigo}" - URL da API
 * - data-codigo="123" - Código da entidade
 * - data-nome="Nome" - Nome da entidade (para mensagem)
 * - data-tipo="Estilo" - Tipo da entidade (para mensagens)
 *
 * Exemplo:
 * <button data-action="excluir"
 *         data-url="/estilos/123"
 *         data-codigo="123"
 *         data-nome="Pale Ale"
 *         data-tipo="Estilo">
 *     Excluir
 * </button>
 */
var Brewer = Brewer || {};

Brewer.ExclusaoGenerica = (function() {

	function ExclusaoGenerica() {
		this.exclusaoBtns = $('[data-action="excluir"]');
	}

	ExclusaoGenerica.prototype.iniciar = function() {
		this.exclusaoBtns.on('click', onExclusaoClick.bind(this));
	}

	function onExclusaoClick(evento) {
		evento.preventDefault();
		var botaoClicado = $(evento.currentTarget);
		var url = botaoClicado.data('url');
		var nome = botaoClicado.data('nome');
		var tipo = botaoClicado.data('tipo') || 'item';

		swal({
			title: 'Tem certeza?',
			text: 'Excluir "' + nome + '"? Você não poderá recuperar depois.',
			showCancelButton: true,
			confirmButtonColor: '#DD6B55',
			confirmButtonText: 'Sim, exclua agora!',
			cancelButtonText: 'Cancelar',
			closeOnConfirm: false
		}, onExclusaoConfirmada.bind(this, url, tipo));
	}

	function onExclusaoConfirmada(url, tipo) {
		var resposta = $.ajax({
			url: url,
			method: 'DELETE'
		});

		resposta.done(onExcluidoSucesso.bind(this, tipo));
		resposta.fail(onErroExclusao.bind(this));
	}

	function onExcluidoSucesso(tipo) {
		swal({
			title: 'Pronto!',
			text: tipo + ' excluído com sucesso!',
			type: 'success',
			confirmButtonText: 'Ok'
		}, function() {
			window.location.reload();
		});
	}

	function onErroExclusao(e) {
		console.log('Erro na exclusão:', e);
		swal('Oops!', e.responseText, 'error');
	}

	return ExclusaoGenerica;

}());

$(function() {
	var exclusaoGenerica = new Brewer.ExclusaoGenerica();
	exclusaoGenerica.iniciar();
});