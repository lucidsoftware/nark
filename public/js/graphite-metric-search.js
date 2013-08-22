
$(document).ready(function() {
	var currentAutocompleteRequest = null;
	$('#target-input').autocomplete({
		autoFocus: false,
		delay: 500,
		minLength: 0,
		select: function(event, selected) {
			$(event.target).focus();

			setTimeout(function() {
				if (!selected.item.leaf) {
					$(event.target).autocomplete("search", selected.item.value);
				}
				else {
					graphPreview();
				}
			}, 1);
		},
		source: function(request, response) {
			graphPreview();

			if (currentAutocompleteRequest) {
				currentAutocompleteRequest.abort();
			}

			if (request.term.indexOf("%") >= 0) {
				response([]);
				return;
			}

			currentAutocompleteRequest = $.ajax({
				cache: false,
				url: '/graphite/metrics',
				data: {
					"target": request.term
				},
				timeout: 30000,
				complete: function(xhr, status) {
					currentAutocompleteRequest = null;
				},
				success: function(data, status, xhr) {
					var options = $.map(data, function(element) {
						return {"label": element['p'], "value": element['p'], "leaf": element['l']};
					});
					response(options);
				},
				error: function(xhr, status, error) {
					response([]);
				}
			});
		}
	});

	//try to render a preview if the user leaves the text box
	$('#target-input').focusout(function() {
		graphPreview();
	});

	var currentPreviewRequest = null;
	function graphPreview() {
		var target = $('#target-input').val();
		if (target.length == 0 || target[target.length - 1] == "." || target.indexOf("%") >= 0) {
			return;
		}

		var data = {
			"seconds": 3600,
			"target[0]": target
		};

		if (currentPreviewRequest) {
			currentPreviewRequest.abort();
		}

		currentPreviewRequest = $.ajax({
			"type": "GET",
			"url": "/graphite/datapoints",
			"data": data,
			"dataType": "json",
			"timeout": 300000,
			"complete": function(xhr, status) {
				currentPreviewRequest = false;
			},
			"success": function(data) {
				if (data.length > 0) {
					$('#target-graph').show();
					plotLineGraph('target-graph svg', data);
				}
			},
			"failure": function(data) {
				console.log("Failed to retrieve graphite data", data);
			}
		});
	}
});
