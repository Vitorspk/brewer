var Brewer = Brewer || {};

Brewer.ExclusaoCerveja = (function() {

	function ExclusaoCerveja() {
		this.exclusaoBtns = $('.js-exclusao-btn');
	}

	ExclusaoCerveja.prototype.iniciar = function() {
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
			url: '/cervejas/' + codigo,
			method: 'DELETE'
		});

		resposta.done(onExcluidoSucesso.bind(this));
		resposta.fail(onErroExclusao.bind(this));
	}

	function onExcluidoSucesso() {
		swal({
			title: 'Pronto!',
			text: 'Cerveja excluída com sucesso!',
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

	return ExclusaoCerveja;

}());

$(function() {
	var exclusaoCerveja = new Brewer.ExclusaoCerveja();
	exclusaoCerveja.iniciar();
});
