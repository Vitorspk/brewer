Brewer.TabelaItens = (function() {

	function TabelaItens(autocomplete) {
		this.autocomplete = autocomplete;
		this.tabelaItensContainer = $('.js-tabela-cervejas-container');
		this.emitter = $({});
		this.on = this.emitter.on.bind(this.emitter);
	}

	TabelaItens.prototype.iniciar = function() {
		this.autocomplete.on('item-selecionado', onItemSelecionado.bind(this));
		bindQuantidade.call(this);
		bindTabelaItem.call(this);
	}

	function onItemSelecionado(evento, item) {
		var resposta = $.ajax({
			url: 'item',
			method: 'POST',
			data: {
				codigoCerveja: item.codigo
			}
		});

		resposta.done(onItemAdicionadoOuAlterado.bind(this));
	}

	function onItemAdicionadoOuAlterado(html) {
		this.tabelaItensContainer.html(html);
		bindQuantidade.call(this);
		var tabelaItem = bindTabelaItem.call(this);

		this.emitter.trigger('tabela-itens-atualizada', tabelaItem.data('valor-total'));
	}

	function onQuantidadeItemAlterado(evento) {
		var input = $(evento.target);
		var quantidade = input.val();

		if (quantidade <= 0) {
			input.val(1);
			quantidade = 1;
		}

		var codigoCerveja = input.data('codigo-cerveja');

		var resposta = $.ajax({
			url: 'item/' + codigoCerveja,
			method: 'PUT',
			data: {
				quantidade: quantidade
			}
		});

		resposta.done(onItemAdicionadoOuAlterado.bind(this));
	}

	function onDoubleClick(evento) {
		$(this).removeAttr('readonly');
	}

	function bindQuantidade() {
		var quantidadeItemInput = $('.js-tabela-cerveja-quantidade-item');
		quantidadeItemInput.on('change', onQuantidadeItemAlterado.bind(this));
		quantidadeItemInput.on('dblclick', onDoubleClick);
	}

	function bindTabelaItem() {
		var tabelaItem = $('.js-tabela-item');
		tabelaItem.on('dblclick', onDoubleClick);
		$('.js-exclusao-item-btn').on('click', onExclusaoItemClick.bind(this));

		return tabelaItem;
	}

	function onExclusaoItemClick(evento) {
		var botaoClicado = $(evento.currentTarget);
		var codigoCerveja = botaoClicado.data('codigo-cerveja');

		var resposta = $.ajax({
			url: 'item/' + codigoCerveja,
			method: 'DELETE'
		});

		resposta.done(onItemAdicionadoOuAlterado.bind(this));
	}

	return TabelaItens;

}());

$(function() {

	var autocomplete = new Brewer.Autocomplete();
	autocomplete.iniciar();

	var tabelaItens = new Brewer.TabelaItens(autocomplete);
	tabelaItens.iniciar();

});