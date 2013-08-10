function convertToDygraph(data) {
	var dygraph = {};
	dygraph['datapoints'] = [];
	if (data.length == 0) {
		return[];
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

function plotLineGraph(element, data) {
	plotGraph(element, data, {});
}

function plotStackedGraph(element, data) {
	plotGraph(element, data, {'stackedGraph':true});
}

function plotGraph(element, data, preferences) {
	var dygraph = convertToDygraph(data);
	if (dygraph.length == 0) {
		return;
	}

	preferences['labels'] = dygraph['labels'];
	preferences['labelsSeparateLines'] = true;
	preferences['labelsDiv'] = document.getElementById('legend');

	$(element.split(' ')[0]).html('');
	new Dygraph(
		document.getElementById(element.split(' ')[0]),
		dygraph['datapoints'],
		preferences
	);
}