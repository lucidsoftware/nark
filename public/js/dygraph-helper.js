
function convertToDygraph(data) {
	// this sort makes sure that the graph colors are always the same.
	data.sort(function(a,b) { 
		if(a['t'] < b['t'])
			return 1;
		else if (a['t'] > b['t'])
			return -1;
		else 
			return 0;
	});

	var dygraph = {};
	dygraph['datapoints'] = [];
	dygraph['labels'] = ['Date'];
	if (data.length == 0) {
		return [];
	}

	var dataByTime = {};
	var dataLength = data.length;
	for (var dataIndex = 0; dataIndex < dataLength; dataIndex++) {
		var target = data[dataIndex];
		var targetLength = target['d'].length;

		dygraph['labels'].push(target['t']);

		for (var targetIndex = 0; targetIndex < targetLength; targetIndex++) {
			var point = target['d'][targetIndex];
			var date = point['d'];
			var value = point['v'];

			if (!dataByTime[date]) {
				dataByTime[date] = {};
			}

			if (value != null && value != undefined) {
				dataByTime[date][dataIndex] = value;
			}
		}
	}

	var times = Object.keys(dataByTime);
	var timeLength = times.length;
	times.sort();

	for (var timeIndex = 0; timeIndex < timeLength; timeIndex++) {
		var time = times[timeIndex];
		var timeData = dataByTime[time];

		var datapoint = [];
		datapoint[0] = new Date(time * 1000);
		for (var dataIndex = 0; dataIndex < dataLength; dataIndex++) {
			// [dataIndex+1] because [0] is the date
			datapoint[dataIndex + 1] = timeData[dataIndex];
		}

		dygraph['datapoints'].push(datapoint);
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
	var g = new Dygraph(
		document.getElementById(element.split(' ')[0]),
		dygraph['datapoints'],
		graphOptions
	);
	var onclick = function(ev) {
		if (g.isSeriesLocked()) {
			g.clearSelection();
		} 
		else {
			g.setSelection(g.getSelection(), g.getHighlightSeries(), true);
		}
	};
	g.updateOptions({clickCallback: onclick}, true);
	return g;
}

$(document).ready(function(event) {
	$('#legend').css({
		display: 'none'
	});

	$('.graph-container').mouseover(function() {
		$('#legend').css({
			display: 'inherit',
			padding: 5
		});
		if (typeof currentColumnCount != 'undefined' && currentColumnCount > 1) {
			var g = $(this);
			var showLegend = function() {
				var left = g.parent().offset().left;
				if (g.parent().offset().left + $('#legend').width() > $(window).width()) {
					left = $(window).width() - $('#legend').width() - 50;
				}
				$('#legend').css({
					left: left,
					top: g.parent().offset().top - $('#legend').height()
				});
			};

			setTimeout(showLegend, 50);
		}
		else {
			mouseLegend();
		}
	});

	$('.graph-container').mouseout(function() {
		$(document).unbind('mousemove');
		$('#legend').css({
			display: 'none',
			padding: 0
		});
	});
})

function mouseLegend() {
	$(document).mousemove(function(event) {
		var legend = $('#legend');

		var xOffset = 20;
		if (event.pageX + legend.width() + 40 > $(window).width()){
			xOffset = -40 - legend.width();
		}
		var padding = legend.is(':empty') ? 0 : 10;
		legend.css({
			left: event.pageX + xOffset,
			top: event.pageY + 40,
			padding: padding
		});
	});
}
