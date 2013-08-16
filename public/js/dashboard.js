function convertToDygraph(data) {
	var dygraph = {};
	dygraph['datapoints'] = [];
	if (data.length == 0) {
		return [];
	}

	for (var i = 0; i < data[0]['d'].length; i++) {
		var datapoint = [new Date(data[0]['d'][i]['d'] * 1000)];
		for (var j = 0; j < data.length; j++) {
			if (data[j]['d'][i] != undefined) {
				datapoint.push(data[j]['d'][i]['v']);
			}
			else if (i > 0) {
				datapoint.push(dygraph['datapoints'][i - 1][j]);
			}
			else {
				datapoint.push(0);
			}
		}
		dygraph['datapoints'].push(datapoint);
	}

	dygraph['labels'] = ['Date'];
	for (var i = 0; i < data.length; i++) {
		dygraph['labels'].push(data[i]['t']);
	}

	return dygraph;
}

function plotLineGraph(element, data, graphOptions) {
	if (!graphOptions) { graphOptions = {}; }
	return plotGraph(element, data, graphOptions);
}

function plotStackedGraph(element, data, graphOptions) {
	if (!graphOptions) { graphOptions = {}; }
	graphOptions['stackedGraph'] = true;
	return plotGraph(element, data, graphOptions);
}

function plotGraph(element, data, graphOptions) {
	var dygraph = convertToDygraph(data);
	if (dygraph.length == 0) {
		return false;
	}

	graphOptions['labels'] = dygraph['labels'];
	graphOptions['labelsSeparateLines'] = true;
	graphOptions['labelsDiv'] = document.getElementById('legend');

	$('#' + element.split(' ')[0]).html('');
	return new Dygraph(
		document.getElementById(element.split(' ')[0]),
		dygraph['datapoints'],
		graphOptions
	);
}

$(document).mousemove(function(event) {
	var legend = $('#legend');
	var xOffset = 20;
	if (event.pageX + legend.width() + 40 > $(window).width()){
		xOffset = -40 - legend.width();
	}
	var padding = legend.is(':empty') ? 0 : 10;
	legend.css({
		left: event.pageX + xOffset,
		top: event.pageY - legend.height() / 2,
		padding: padding
	});
});
