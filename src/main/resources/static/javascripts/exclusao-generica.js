var Brewer = Brewer || {};

/**
 * Módulo genérico para exclusão de entidades com confirmação SweetAlert.
 *
 * Uso:
 * No HTML, adicione os seguintes data attributes ao botão de exclusão:
 * - data-entity-type: tipo da entidade (ex: "cliente", "cerveja", "estilo")
 * - data-entity-id: código/ID da entidade
 * - data-entity-name: nome da entidade para exibição
 * - data-url-base: URL base do endpoint (ex: "/clientes", "/cervejas")
 *
 * Exemplo:
 * <a class="js-exclusao-btn"
 *    data-entity-type="cliente"
 *    data-entity-id="1"
 *    data-entity-name="João Silva"
 *    data-url-base="/clientes">
 *   <i class="glyphicon glyphicon-remove"></i>
 * </a>
 */
Brewer.ExclusaoGenerica = (function() {

	function ExclusaoGenerica() {
		this.exclusaoBtns = $('.js-exclusao-btn');
	}

	ExclusaoGenerica.prototype.iniciar = function() {
		this.exclusaoBtns.on('click', onExclusaoClick.bind(this));
	}

	function onExclusaoClick(evento) {
		evento.preventDefault();
		var botaoClicado = $(evento.currentTarget);

		// Lê os data attributes do botão
		var entityId = botaoClicado.data('entity-id') || botaoClicado.data('codigo');
		var entityName = botaoClicado.data('entity-name') || botaoClicado.data('nome');
		var urlBase = botaoClicado.data('url-base');
		var entityType = botaoClicado.data('entity-type') || 'registro';

		// Validação
		if (!entityId || !urlBase) {
			console.error('Erro: data-entity-id e data-url-base são obrigatórios');
			return;
		}

		// Exibe confirmação
		swal({
			title: 'Tem certeza?',
			text: 'Excluir "' + entityName + '"? Você não poderá recuperar depois.',
			showCancelButton: true,
			confirmButtonColor: '#DD6B55',
			confirmButtonText: 'Sim, exclua agora!',
			cancelButtonText: 'Cancelar',
			closeOnConfirm: false
		}, function() {
			onExclusaoConfirmada(entityId, urlBase, entityType);
		});
	}

	function onExclusaoConfirmada(entityId, urlBase, entityType) {
		var resposta = $.ajax({
			url: urlBase + '/' + entityId,
			method: 'DELETE'
		});

		resposta.done(function() {
			onExcluidoSucesso(entityType);
		});
		resposta.fail(onErroExclusao);
	}

	function onExcluidoSucesso(entityType) {
		// Capitaliza o tipo da entidade para exibição
		var entityTypeCapitalized = entityType.charAt(0).toUpperCase() + entityType.slice(1);

		swal({
			title: 'Pronto!',
			text: entityTypeCapitalized + ' excluído(a) com sucesso!',
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

// Inicialização automática quando o documento estiver pronto
$(function() {
	var exclusaoGenerica = new Brewer.ExclusaoGenerica();
	exclusaoGenerica.iniciar();
});
