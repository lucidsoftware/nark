
$(document).ready(function() {
	$('#host-input').autocomplete({
		delay: 500,
		minLength: 0,
		select: function(event, selected) {
			$(event.target).focus();

			if (!selected.item.leaf) {
				setTimeout(function() {
					$(event.target).autocomplete("search", selected.item.value);
				}, 1);
			}
		},
		source: function(request, response) {
			$.ajax({
				cache: false,
				url: '/graphite/hosts',
				data: {
					"search": request.term
				},
				timeout: 5000,
				success: function(data, status, xhr) {
					var options = $.map(data, function(element) {
						return {"label": element['n'], "value": element['n'], "state": element['s']};
					});
					response(options);
				},
				error: function(xhr, status, error) {
					response([]);
				}
			});
		}
	});
});
