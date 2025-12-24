var Brewer = Brewer || {};

Brewer.GraficoVendasPorMes = (function() {

	function GraficoVendasPorMes() {
		this.ctx = $('#graficoVendasPorMes')[0].getContext('2d');
	}

	GraficoVendasPorMes.prototype.iniciar = function() {
		var url = '/dashboard/vendas/totalPorMes';

		$.ajax({
			url: url,
			method: 'GET',
			success: onDadosRecebidos.bind(this),
			error: function(xhr, status, error) {
				console.error('Erro ao carregar vendas por mês:', error);
			}
		});
	}

	function onDadosRecebidos(vendaMes) {
		var meses = [];
		var valores = [];

		vendaMes.forEach(function(obj) {
			meses.push(obj.mes);
			valores.push(obj.totalVendas);
		});

		var graficoVendasPorMes = new Chart(this.ctx, {
			type: 'line',
			data: {
				labels: meses,
				datasets: [{
					label: 'Vendas por mês',
					backgroundColor: 'rgba(26,179,148,0.5)',
					borderColor: 'rgba(26,179,148,0.7)',
					pointBorderColor: 'rgba(26,179,148,1)',
					pointBackgroundColor: '#fff',
					data: valores,
					fill: true,
					tension: 0.4
				}]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				plugins: {
					legend: {
						display: true,
						position: 'top'
					},
					title: {
						display: false
					}
				},
				scales: {
					y: {
						beginAtZero: true,
						ticks: {
							callback: function(value) {
								return Number.isInteger(value) ? value : null;
							}
						}
					}
				}
			}
		});
	}

	return GraficoVendasPorMes;

})();

Brewer.GraficoVendasPorOrigem = (function() {

	function GraficoVendasPorOrigem() {
		this.ctx = $('#graficoVendasPorOrigem')[0].getContext('2d');
	}

	GraficoVendasPorOrigem.prototype.iniciar = function() {
		var url = '/dashboard/vendas/totalPorOrigem';

		$.ajax({
			url: url,
			method: 'GET',
			success: onDadosRecebidos.bind(this),
			error: function(xhr, status, error) {
				console.error('Erro ao carregar vendas por origem:', error);
			}
		});
	}

	function onDadosRecebidos(vendaOrigem) {
		var origens = [];
		var valores = [];

		vendaOrigem.forEach(function(obj) {
			origens.push(obj.origem);
			valores.push(obj.totalVendas);
		});

		var graficoVendasPorOrigem = new Chart(this.ctx, {
			type: 'doughnut',
			data: {
				labels: origens,
				datasets: [{
					backgroundColor: [
						'rgba(26,179,148,0.7)',
						'rgba(220,220,220,0.7)'
					],
					borderColor: [
						'rgba(26,179,148,1)',
						'rgba(220,220,220,1)'
					],
					borderWidth: 1,
					data: valores
				}]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				plugins: {
					legend: {
						display: true,
						position: 'right'
					},
					title: {
						display: false
					}
				}
			}
		});
	}

	return GraficoVendasPorOrigem;

})();

$(function() {
	// Só inicializa os gráficos se os elementos canvas existirem na página
	if ($('#graficoVendasPorMes').length > 0) {
		var graficoVendasPorMes = new Brewer.GraficoVendasPorMes();
		graficoVendasPorMes.iniciar();
	}

	if ($('#graficoVendasPorOrigem').length > 0) {
		var graficoVendasPorOrigem = new Brewer.GraficoVendasPorOrigem();
		graficoVendasPorOrigem.iniciar();
	}
});