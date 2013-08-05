function d3_format_linear_data(data) {
	return data.map(function(item) {
		return {x: item["d"], y: item["v"]}
	})
}

function d3_format_stacked_data(data) {
	return data.map(function(item) {
		return [item["d"], item["v"]]
	})
}

function plotLineGraph(element, data) {

	nv.addGraph(function() {
		var chart = nv.models.lineWithFocusChart();

		chart.xAxis
		.tickFormat(function(d) { return d3.time.format('%x')(new Date(d * 1000)) });

		chart.x2Axis
		.tickFormat(function(d) { return d3.time.format('%x')(new Date(d * 1000)) });

		chart.yAxis
		.tickFormat(d3.format(',.2f'));

		chart.y2Axis
		.tickFormat(d3.format(',.2f'));

		d3.select('#'+element)
		.datum(data.map( function(item) {
			return {
				key: 'Target: ' + item["t"],
				values: d3_format_linear_data(item["d"])
			}
		}))
		.transition().duration(500)
		.call(chart);

		nv.utils.windowResize(chart.update);

		return chart;
	});
}

function plotStackedGraph(element, data) {
	nv.addGraph(function() {
		var chart = nv.models.stackedAreaChart()
			.x(function(d) { return d[0] })
			.y(function(d) { return d[1] })
			.clipEdge(true);

		chart.xAxis
			.showMaxMin(false)
			.tickFormat(function(d) { return d3.time.format('%x')(new Date(d * 1000)) });

		chart.yAxis
			.tickFormat(d3.format(',.2f'));

		d3.select('#' + element)
			.datum(data.map( function(item) {
				return {
					key: 'Target: ' + item["t"],
					values: d3_format_stacked_data(item["d"])
				}
			}))
			.transition().duration(500).call(chart);

		nv.utils.windowResize(chart.update);

		return chart;
	});
}