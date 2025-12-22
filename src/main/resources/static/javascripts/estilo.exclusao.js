/**
 * @deprecated
 * Este arquivo está deprecado em favor de exclusao.generica.js
 * CODE QUALITY FIX: Phase 12 - Medium Priority Issue #6
 *
 * Para migrar, atualize o HTML para usar data attributes:
 * - data-action="excluir"
 * - data-url="/estilos/{codigo}"
 * - data-codigo="{codigo}"
 * - data-nome="{nome}"
 * - data-tipo="Estilo"
 *
 * E inclua: <script src="/javascripts/exclusao.generica.js"></script>
 *
 * Este arquivo será removido em versão futura.
 */
var Brewer = Brewer || {};

Brewer.ExclusaoEstilo = (function() {

	function ExclusaoEstilo() {
		this.exclusaoBtns = $('.js-exclusao-estilo-btn');
	}

	ExclusaoEstilo.prototype.iniciar = function() {
		this.exclusaoBtns.on('click', onExclusaoClick.bind(this));
	}

	function onExclusaoClick(evento) {
		evento.preventDefault();
		var botaoClicado = $(evento.currentTarget);
		var codigo = botaoClicado.data('codigo');
		var nome = botaoClicado.data('nome');

		swal({
			title: 'Tem certeza?',
			text: 'Excluir "' + nome + '"? Você não poderá recuperar depois.',
			showCancelButton: true,
			confirmButtonColor: '#DD6B55',
			confirmButtonText: 'Sim, exclua agora!',
			cancelButtonText: 'Cancelar',
			closeOnConfirm: false
		}, onExclusaoConfirmada.bind(this, codigo));
	}

	function onExclusaoConfirmada(codigo) {
		var resposta = $.ajax({
			url: '/estilos/' + codigo,
			method: 'DELETE'
		});

		resposta.done(onExcluidoSucesso.bind(this));
		resposta.fail(onErroExclusao.bind(this));
	}

	function onExcluidoSucesso() {
		swal({
			title: 'Pronto!',
			text: 'Estilo excluído com sucesso!',
			type: 'success',
			confirmButtonText: 'Ok'
		}, function() {
			window.location.reload();
		});
	}

	function onErroExclusao(e) {
		console.log('erro exclusao', e);
		swal('Oops!', e.responseText, 'error');
	}

	return ExclusaoEstilo;

}());

$(function() {
	var exclusaoEstilo = new Brewer.ExclusaoEstilo();
	exclusaoEstilo.iniciar();
});
