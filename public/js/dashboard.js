var dateFormat = "mm/dd/yy";
var timeFormat = "HH:mmz";

var localStorageStartDateKeys = ["dashboard-" + dashboard.id + "-custom-start", "dashboard-default-custom-start"];
var localStorageEndDateKeys = ["dashboard-" + dashboard.id + "-custom-end", "dashboard-default-custom-end"];
var localStorageColumnCountKeys = ["dashboard-" + dashboard.id + "-columns", "dashboard-default-columns"];
var localStorageRefreshRateKeys = ["dashboard-" + dashboard.id + "-refresh", "dashboard-default-refresh"];
var localStorageDateRangeKeys = ["dashboard-" + dashboard.id + "-daterange", "dashboard-default-daterange"];
var localStorageOptionsPanelKeys = ["dashboard-" + dashboard.id + "-optionspanel", "dashboard-default-optionspanel"];
var localStorageSummarizerKeys = ["dashboard-" + dashboard.id + "-summarizer", "dashboard-default-summarizer"];

var allGraphIds = [];
$.map(graphs, function(graph) {
	allGraphIds.push(graph.id);
});

var zoomingToSelection = false;
var lineGraphs = {};
var initialXRange = null;
var currentXRange = null;
var zoomedIn = false;
function redrawGraph(id) {
	if (graphData[id]) {
		var data = graphData[id];
		var element = "graph-" + id + " svg";
		var graph = $.grep(graphs, function(graph) {
			return graph["id"] == id;
		})[0];

		var graphOptions = {
			drawCallback: function(me, initial) {
				if (initial) {
					initialXRange = me.xAxisRange();
					$('.zoom-update').hide(250);
				}
				if (zoomingToSelection || initial) { return; }
				zoomingToSelection = true;
				currentXRange = me.xAxisRange();

				zoomedIn = !($(currentXRange).not(initialXRange).length == 0 && $(initialXRange).not(currentXRange).length == 0);
				if (zoomedIn) {
					$('.zoom-update').show(250);
				}
				else {
					$('.zoom-update').hide(250);
				}

				$.map(graphs, function(g) {
					if (g.id == graph.id) { return; }
					if (lineGraphs.hasOwnProperty(g.id)) {
						lineGraphs[g.id].updateOptions({
							dateWindow: currentXRange
						});
					}
				});
				zoomingToSelection = false;
			},
			highlightCircleSize: 2,
			strokeWidth: 1,
			highlightSeriesOpts: {
				strokeWidth: 3,
				strokeBorderWidth: 1,
				highlightCircleSize: 5
			}
		};

		switch (graph["axisLabel"]) {
			case 'Full':
				graphOptions["maxNumberWidth"] = 999;
				break;
			case 'Power of 2':
				graphOptions["labelsKMG2"] = true;
				break;
			case 'Power of 10':
				graphOptions["labelsKMB"] = true;
				break;
			case 'Auto':
			default:
				break;
		}

		switch (graph["type"]) {
			case "Stacked":
				lineGraphs[graph["id"]] = plotStackedGraph(element, data, graphOptions);
				break;
			case "Normal":
			default:
				lineGraphs[graph["id"]] = plotLineGraph(element, data, graphOptions);
				break;
		}

		//show that the graph is done loading
		$('#loading-' + id).hide(250);
	}
}

var redrawGraphs = {};
function queueRedrawGraph(id) {
	redrawGraphs[id] = true;

	setTimeout(function() {
		if (redrawGraphs[id]) {
			redrawGraphs[id] = false;
			redrawGraph(id);
		}
	}, 1);
}

function queueRedrawAllGraphs() {
	$.map(graphs, function(graph) {
		queueRedrawGraph(graph["id"]);
	});
}


function showGraphError(id, error) {
	$("#graph-" + id + "-wrapper .graph-error").text(error);
}

function removeGraphError(id) {
	$("#graph-" + id + "-wrapper .graph-error").text("");
}

function regexIndexOf(string, regex, startpos) {
	var indexOf = string.substring(startpos || 0).search(regex);
	return (indexOf >= 0) ? (indexOf + (startpos || 0)) : indexOf;
}

function getUpdatedTargets() {
	var variables = [];
	$('#variables-group input').each(function() {
		variables.push({'key': $(this).attr('key'), 'value': $(this).val()});
	});
	$('#variables-group select').each(function() {
		variables.push({'key': $(this).attr('key'), 'value': $(this).val()});
	});

	var updatedTargets = {};
	$.map(targets, function(target) {
		var parts = target['target'].split("%");
		for (var i = 1; i < parts.length; i += 2) {
			parts[i] = parts[i].split(/=|\|/)[0];
		}

		$.map(variables, function(variable) {
			for (var i = 1; i < parts.length; i += 2) {
				if (parts[i] == variable['key']) {
					parts[i] = variable['value'];
				}
			}
		});

		updatedTargets[target['id']] = parts.join("");
	});

	return updatedTargets;
}

function getAffectedGraphs(changedVariable) {
	var graphIds = [];
	$.map(targets, function(target) {
		if (target['target'].indexOf('%' + changedVariable) != -1) {
			$.map(graphs, function(graph) {
				if (graph['id'] == target['graphId']) {
					graphIds.push(graph['id']);
				}
			});
		}
	});

	return graphIds;
}

var needRefreshData = false;
var graphsToRefresh = [];
function queueRefreshData(graphIds) {
	graphIds = typeof graphIds !== 'undefined' ? graphIds : allGraphIds; 
	
	$.map(graphIds, function(graphId) {
		if ($.inArray(graphId, graphsToRefresh) == -1) {
			graphsToRefresh.push(graphId);
		}
	});
	needRefreshData = true;
	setTimeout(function() {
		if (needRefreshData) {
			needRefreshData = false;
			refreshData(graphsToRefresh);
			graphsToRefresh = [];
		}
	}, 1);
}

var dataRequests = {};
var graphData = {};
function refreshData(graphIds) {
	var updatedTargets = getUpdatedTargets();

	$.map(graphs, function(graph) {
		if ($.inArray(graph['id'], graphIds) == -1) {
			return;
		}

		var data = {};

		//reset the graphite link under the graph
		var graphUrl = graphiteBaseUrl;
		graphUrl += '/render?title=' + graph['name'] + '&width=650&height=400&';

		var graphSeconds = null;
		if (dateRange["type"] == "custom") {
			data["from"] = Math.floor(dateRange["start"].getTime() / 1000);
			data["to"] = Math.floor(dateRange["end"].getTime() / 1000);
			graphSeconds = Math.abs(data["to"] - data["from"]);
			graphUrl += 'from=' + data['from'] + '&to=' + data['to'];
		}
		else {
			data["seconds"] = dateRange["seconds"];
			graphSeconds = data["seconds"];
			graphUrl += 'from=-' + data['seconds'] + 's';
		}

		var intervalString = null;
		if (summarizer == "auto") {
			// the number of pixels isn't exact, because of axis labels and graph padding, but it will be close enough.
			var pixels = getGraphWidth();

			// calculate number of data points based on interval, pixels, and the magic number.
			var intervalSeconds = graphSeconds / (autoIntervalPointsPerPixel * pixels);

			// adjust to a multiple of granularitySeconds
			intervalSeconds = intervalSeconds + (granularitySeconds - intervalSeconds % granularitySeconds);

			// require at least granularitySeconds seconds
			intervalSeconds = Math.max(intervalSeconds, granularitySeconds);

			if (intervalSeconds > granularitySeconds) {
				intervalString = intervalSeconds + "s";
			}
		}
		else if (summarizer == "all") {
			// do nothing, get all data points.
			// summarization function will not be applied below.
		}
		else {
			intervalString = summarizer + "s";
		}

		var count = 0;
		$.map(targets, function(target) {
			if (target["graphId"] == graph["id"]) {
				var updatedTarget = updatedTargets[target['id']];

				var summarizedTarget = updatedTarget;
				if (intervalString) {
					summarizedTarget = "summarize(" + updatedTarget + ",'" + intervalString + "','" + target['summarizer'] + "')";
				}

				data['target[' + (count++) + ']'] = summarizedTarget;
				graphUrl += encodeURIComponent('&target=' + summarizedTarget);
			}
		});

		if (dataRequests[graph["id"]]) {
			dataRequests[graph["id"]].abort();
		}

		$('#graph-link-' + graph['id']).attr('href', graphUrl);

		//show that the graph is reloading
		$('#loading-' + graph['id']).show(250);

		dataRequests[graph["id"]] = $.ajax({
			"type": "GET",
			"url": "/graphite/datapoints",
			"data": data,
			"dataType": "json",
			"timeout": 300000,
			"complete": function(xhr, status) {
				dataRequests[graph["id"]] = false;
			},
			"success": function(data) {
				if(!data || data.length == 0) {
					showGraphError(graph["id"], "No data available for this graph over the selected time period.");
				} else {
					graphData[graph["id"]] = data;
					queueRedrawGraph(graph["id"]);
					removeGraphError(graph["id"]);
				}
			},
			"failure": function(data) {
				showGraphError(graph["id"], "Failed to retrieve graphite data: " + data);
				console.log("Failed to retrieve graphite data", data);
			}
		});
	});
}

function getGraphWidth() {
	return $("#allgraphs .graph-wrapper").width();
}

function adjustGraphHeights() {
	// the 20 pixels is for the caption
	$("#allgraphs .graph-container").css('height', Math.floor((getGraphWidth() - 20) * 0.66) + "px");
	queueRedrawAllGraphs();

	if (summarizer == "auto") {
		queueRefreshData();
	}
}

function setOptionsPanel(visibility) {
	if(visibility == "hidden") {
		$("#options-container").slideUp();
	} else {
		$("#options-container").slideDown();
	}
}

var currentColumnCount = null;
function setColumnCount(count) {
	if (currentColumnCount != count) {
		currentColumnCount = count;
		$("#column-group button.active").removeClass("active");
		$("#column-group button[data='" + count + "']").addClass("active");
		$("#allgraphs").removeClass("g1 g2 g3 g4 g5 g6").addClass("g" + count);
		adjustGraphHeights();
	}
}

var refreshRateSeconds = null;
var refreshTimer = null;
function refreshTimerTick() {
	if (refreshTimer) {
		clearTimeout(refreshTimer);
	}

	if (refreshRateSeconds != "disable") {
		queueRefreshData();
		refreshTimer = setTimeout(function(event) {
			refreshTimer = null;
			refreshTimerTick();
		}, refreshRateSeconds * 1000);
	}
}

function setRefreshRate(seconds) {
	if (seconds != refreshRateSeconds) {
		$("#refreshrate-group button.active").removeClass("active");
		$("#refreshrate-group button[data='" + seconds + "']").addClass("active");

		// updating a graph for a static time period seems like a waste... change it to 1h
		if (seconds != "disable" && dateRange["type"] == "custom") {
			setDateRange(3600);
		}

		refreshRateSeconds = seconds;
		refreshTimerTick();
	}
}

var summarizer = null;
function setSummarizer(seconds) {
	if (seconds != summarizer) {
		$("#summarizer-group button.active").removeClass("active");
		$("#summarizer-group button[data='" + seconds + "']").addClass("active");
		summarizer = seconds;
		queueRefreshData();
	}
}

var dateRange = { "type": null };
function setDateRange(seconds) {
	if ((seconds == "custom" && dateRange['type'] != "custom") || (seconds != "custom" && seconds != dateRange['seconds'])) {
		$("#daterange-group button.active").removeClass("active");
		$("#daterange-group button[data='" + seconds + "']").addClass("active");

		if (seconds == "custom") {
			$("#custom-daterange-group").slideDown();
			if (dateRange["type"] != "custom") {
				dateRange = {
					"type": "custom",
					"start": null,
					"end": null
				};

				updateCustomDates();

				// updating a graph for a static time period seems like a waste... disable it.
				if (refreshRateSeconds != "disable") {
					setRefreshRate("disable");
				}
			}
		}
		else {
			$("#custom-daterange-group").slideUp();
			dateRange = {
				"type": "recent",
				"seconds": seconds
			};
		}

		queueRefreshData();
	}
}

function updateCustomDates() {
	var newStart = $("#custom-daterange-start-input").datepicker("getDate");
	var newEnd   = $("#custom-daterange-end-input").datepicker("getDate");

	if (newStart != dateRange["start"]) {
		dateRange["start"] = newStart;
		queueRefreshData();
	}
	if (newEnd != dateRange["end"]) {
		dateRange["end"] = newEnd;
		queueRefreshData();
	}
}

function setLocalStorage(keys, value) {
	if (window.localStorage) {
		for (var i = 0; i < keys.length; i++) {
			var key = keys[i];
			window.localStorage[key] = value;
		}
	}
}

function fromLocalStorage(keys, defaultValue) {
	if (window.localStorage) {
		for (var i = 0; i < keys.length; i++) {
			var key = keys[i];
			if (window.localStorage.hasOwnProperty(key)) {
				return window.localStorage[key];
			}
		}
	}

	return defaultValue;
}

function fromLocalStorageInt(keys, defaultValue) {
	return parseInt(fromLocalStorage(keys, defaultValue), 10);
}

function mapTargetsByType(selectCallback, textCallback) {
	$.map(targets, function(target) {
		if (target['target'].indexOf('%') != -1) {
			var targetPieces = target['target'].split('%');
			for (var i = 1; i < targetPieces.length; i = i + 2) {
				if (targetPieces[i].indexOf('|') != -1) {
					variablePieces = targetPieces[i].split('|');
					var nonEmptyOptions = variablePieces.slice(1).filter(function(n){return n});
					selectCallback(variablePieces[0], nonEmptyOptions);
				}
				else {
					var variablePieces = targetPieces[i].split('=', 2);
					textCallback(variablePieces[0], variablePieces.slice(1).join("="));
				}
			}
		}
	});
}

$(document).resize(function() {
	adjustGraphHeights();
});


$(document).ready(function() {
	$("#optiontoggler").click(function(event) {
		var visibility = fromLocalStorage(localStorageOptionsPanelKeys, "visible");
	
		if(visibility == "visible") {
			visibility = "hidden";
		} else {
			visibility = "visible";
		}

		setLocalStorage(localStorageOptionsPanelKeys, visibility);
		setOptionsPanel(visibility);
	});

	$("#infotoggler").click(function(event) {
		$("#info-container").slideToggle();
	});

	$("#refreshnow").click(function(event) {
		queueRefreshData();
	});

	$("#column-group button").click(function(event) {
		var count = parseInt($(event.target).attr("data"), 10);
		setLocalStorage(localStorageColumnCountKeys, count);
		setColumnCount(count);
	});

	$("#refreshrate-group button").click(function(event) {
		var seconds = $(event.target).attr("data");
		if (seconds != "disable") {
			seconds = parseInt(seconds, 10);
		}
		setLocalStorage(localStorageRefreshRateKeys, seconds);
		setRefreshRate(seconds);
	});

	$("#summarizer-group button").click(function(event) {
		var seconds = $(event.target).attr("data");
		if (seconds != "auto" && seconds != "all") {
			seconds = parseInt(seconds, 10);
		}
		setLocalStorage(localStorageSummarizerKeys, seconds);
		setSummarizer(seconds);
	});

	$("#daterange-group button").click(function(event) {
		var seconds = $(event.target).attr("data");
		if (seconds != "custom") {
			seconds = parseInt(seconds, 10);
		}
		setLocalStorage(localStorageDateRangeKeys, seconds);
		setDateRange(seconds);
	});

	$("#custom-daterange-start-input").datetimepicker({
		"dateFormat": dateFormat,
		"timeFormat": timeFormat,
		"onSelect": function(datetime, target) {
			var date = $("#custom-daterange-start-input").datepicker("getDate");
			setLocalStorage(localStorageStartDateKeys, date.getTime());
			updateCustomDates();
		}
	});

	$("#custom-daterange-end-input").datetimepicker({
		"dateFormat": dateFormat,
		"timeFormat": timeFormat,
		"onSelect": function(datetime, target) {
			var date = $("#custom-daterange-end-input").datepicker("getDate");
			setLocalStorage(localStorageEndDateKeys, date.getTime());
			updateCustomDates();
		}
	});

	// date picker keeps showing up under the active option buttons
	$("#ui-datepicker-div").css("z-index", 5);

	// also toggle options slider
	var initialStart = fromLocalStorageInt(localStorageStartDateKeys, new Date().getTime() - 86400000);
	$("#custom-daterange-start-input").datepicker("setDate", new Date(initialStart));
	
	var initialEnd = fromLocalStorageInt(localStorageEndDateKeys, new Date().getTime());
	$("#custom-daterange-end-input").datepicker("setDate", new Date(initialEnd));
	updateCustomDates();

	setColumnCount(fromLocalStorageInt(localStorageColumnCountKeys, 3));
	setRefreshRate(fromLocalStorage(localStorageRefreshRateKeys, 60));
	setDateRange(fromLocalStorage(localStorageDateRangeKeys, 3600));
	setOptionsPanel(fromLocalStorage(localStorageOptionsPanelKeys, "visible"));
	setSummarizer(fromLocalStorage(localStorageSummarizerKeys, "auto"));

	var selectTargetOptions = {};
	var textTargetDefaults = {};
	mapTargetsByType(
		// select targets
		function(name, options) {
			if (!selectTargetOptions[name]) {
				selectTargetOptions[name] = [];
			}

			$.map(options, function(option) {
				if (-1 == $.inArray(option, selectTargetOptions[name])) {
					selectTargetOptions[name].push(option);
				}
			});
		},
		// text targets
		function(name, value) {
			if (!textTargetDefaults[name]) {
				textTargetDefaults[name] = value;
			}
		}
	);

	var sortedInputNames = [];
	$.map(selectTargetOptions, function(value, name) { sortedInputNames.push(name); });
	$.map(textTargetDefaults, function(value, name) { sortedInputNames.push(name); });
	sortedInputNames.sort();

	var inputGroup = $('#variables-group');
	$.map(sortedInputNames, function(inputName) {
		if (selectTargetOptions.hasOwnProperty(inputName)) {
			var options = selectTargetOptions[inputName];
			inputGroup.append('<div class="row field-container"><label rel="tooltip" title="' + inputName + '" for="' + inputName + '"><strong>' + inputName + '</strong></label><select id="input-' + inputName + '" name="' + inputName + '" key="' + inputName + '"></select></div>');
			var inputElement = $('#input-' + inputName);

			$.map(options, function(option) {
				inputElement.append('<option value="' + option + '">' + option + '</option>');
			});

			inputElement.change(function(event) {
				setLocalStorage([dashKey + inputName], inputElement.val());
				var graphIds = getAffectedGraphs(inputName);
				queueRefreshData(graphIds);
			});

			var defaultValue = fromLocalStorage([dashKey + inputName], options[0]);
			$('#input-' + inputName + ' option[value="' + defaultValue + '"]').attr('selected', 'selected');
		}
		else if (textTargetDefaults.hasOwnProperty(inputName)) {
			var defaultValue = fromLocalStorage([dashKey + inputName], textTargetDefaults[inputName]);
			inputGroup.append('<div class="row field-container"><label rel="tooltip" title="' + inputName + '" for="' + inputName + '"><strong>' + inputName +'</strong></label><input id="input-' + inputName + '" name="' + inputName + '" type="text" class="span9 input-large" key="' + inputName + '" value="' + defaultValue + '" /></div>');

			var inputElement = $('#input-' + inputName);
			var lastValue = defaultValue;
			function updateInput() {
				var newValue = inputElement.val();
				if (newValue != lastValue) {
					lastValue = newValue;
					setLocalStorage([dashKey + inputElement.attr('key')], newValue);
					var graphIds = getAffectedGraphs(inputElement.attr('key'));
					queueRefreshData(graphIds);
				}
			}

			inputElement.keypress(function(e) {
				if(e.which == 13) {
					updateInput();
				}
			});
			inputElement.focusout(function(e) {
				updateInput();
			});
		}

		$('.zoom-update').click(function(event) {
			if (zoomedIn) {
				var start = new Date(currentXRange[0]);
				$("#custom-daterange-start-input").datepicker("setDate", start);
				setLocalStorage(localStorageStartDateKeys, start.getTime());

				var end = new Date(currentXRange[1]);
				$("#custom-daterange-end-input").datepicker("setDate", end);
				setLocalStorage(localStorageEndDateKeys, end.getTime());

				setLocalStorage(localStorageDateRangeKeys, "custom");
				setDateRange("custom");

				updateCustomDates();
			}
		});
	});
});
