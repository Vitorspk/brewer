var Brewer = Brewer || {};

Brewer.ExclusaoCliente = (function() {

	function ExclusaoCliente() {
		this.exclusaoBtns = $('.js-exclusao-btn');
	}

	ExclusaoCliente.prototype.iniciar = function() {
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
			url: '/clientes/' + codigo,
			method: 'DELETE'
		});

		resposta.done(onExcluidoSucesso.bind(this));
		resposta.fail(onErroExclusao.bind(this));
	}

	function onExcluidoSucesso() {
		swal({
			title: 'Pronto!',
			text: 'Cliente excluído com sucesso!',
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

	return ExclusaoCliente;

}());

$(function() {
	var exclusaoCliente = new Brewer.ExclusaoCliente();
	exclusaoCliente.iniciar();
});
