var Brewer = Brewer || {};

Brewer.EmitirVenda = (function() {

	function EmitirVenda() {
		this.emitirBtn = $('.js-emitir-venda-btn');
	}

	EmitirVenda.prototype.iniciar = function() {
		this.emitirBtn.on('click', onEmitirClick.bind(this));
	}

	function onEmitirClick(event) {
		event.preventDefault();
		var botaoClicado = $(event.currentTarget);
		var codigo = botaoClicado.data('codigo');
		var nomeCliente = botaoClicado.data('nome-cliente');

		swal({
			title: 'Tem certeza?',
			text: 'Deseja emitir a venda #' + codigo + ' para ' + nomeCliente + '?',
			showCancelButton: true,
			confirmButtonColor: '#DD6B55',
			confirmButtonText: 'Sim, emitir!',
			cancelButtonText: 'Cancelar',
			closeOnConfirm: false
		}, onEmitirConfirmado.bind(this, botaoClicado));
	}

	function onEmitirConfirmado(botaoClicado) {
		var codigo = botaoClicado.data('codigo');
		var url = botaoClicado.attr('href') || '/vendas/' + codigo + '/emitir';

		$.ajax({
			url: url,
			method: 'POST',
			success: onEmitirSuccess.bind(this),
			error: onErroEmitir.bind(this)
		});
	}

	function onEmitirSuccess() {
		swal({
			title: 'Emitida!',
			text: 'Venda emitida com sucesso!',
			type: 'success',
			confirmButtonText: 'OK'
		}, function() {
			window.location.reload();
		});
	}

	function onErroEmitir(xhr) {
		console.error('Erro ao emitir venda', xhr);

		var mensagem = 'Não foi possível emitir a venda. Tente novamente.';

		if (xhr.status === 403) {
			mensagem = 'Você não tem permissão para emitir esta venda.';
		} else if (xhr.status === 400 && xhr.responseText) {
			mensagem = xhr.responseText;
		} else if (xhr.status === 404) {
			mensagem = 'Venda não encontrada.';
		}

		swal('Erro!', mensagem, 'error');
	}

	return EmitirVenda;

}());

$(function() {
	var emitirVenda = new Brewer.EmitirVenda();
	emitirVenda.iniciar();
});
