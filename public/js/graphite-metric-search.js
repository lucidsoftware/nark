
$(document).ready(function() {
	$('#target-input').autocomplete({
		autoFocus: true,
		delay: 500,
		minLength: 0,
		select: function(event, selected) {
			$(event.target).focus();

			if (!selected.item.leaf) {
				setTimeout(function() {
					$(event.target).autocomplete("search", selected.item.value);
				}, 1);
			}
			else {
				graphPreview([selected.item]);
			}
		},
		source: function(request, response) {
			$.ajax({
				cache: false,
				url: '/graphite/metrics',
				data: {
					"target": request.term
				},
				timeout: 30000,
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

	setTimeout(function() {
		$('#target-input').focus();
		$('#target-input').autocomplete("search", "");
	}, 1);

	//try to render a preview if the user leaves the text box
	$('#target-input').focusout(function() {
		getPreview();
	});

	//try to render a preview if the user stops typing
	var typingTimer;
	$('#target-input').keyup(function() {
		clearTimeout(typingTimer);
		typingTimer = setTimeout(getPreview, 500);
	});

	function getPreview() {
		$.ajax({
			cache: false,
			url: '/graphite/metrics',
			data: {
				'target': $('#target-input').val()
			},
			timeout: 30000,
			success: function(data, status, xhr) {
				var leaves = [];
				for (var i = 0; i < data.length; i++) {
					if (data[i]['l'] == true) {
						leaves.push({"value": data[i]['p']});
					}
				}
				graphPreview(leaves);
			}
		});
	}

	function graphPreview(targets) {
		if (targets.length == 0) {
			$('#target-graph').hide();
			return;
		}

		var start = moment().subtract("hours", 1).format('X');
		var end = moment().format('X');
		var query = '';
		for (var i = 0; i < targets.length; i++) {
			query += (i == 0) ? '' : '&';
			query += 'target[]=' + targets[i]['value'];
		}

		updateGraphHelper("/graphite/datapoints?" + query, true, "target-graph svg", start, end, function() {
			$('#target-graph').show();
		});
	}
});
