$(function() {
	
	$('.js-submit-btn[data-action]').on('click', function(event) {
		event.preventDefault();
		
		var botaoClicado = $(event.currentTarget);
		var acao = botaoClicado.data('action');
		var form = botaoClicado.closest('form');
		var action = form.attr('action');
		
		form.attr('action', action + '?' + acao);
		form.submit();
	});
	
});
