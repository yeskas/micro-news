// Shows user name modal to user
function trigger_user_name_modal() {
	$('#modal-trigger').trigger("click");
}

$(document).ready(function(){
	// Enable all tooltips
	$('[data-toggle="tooltip"]').tooltip();

	// Handler to set user name and change greeting on response
	$("#user-name-save").click(function() {
		var user_name = $("#user-name-form").val();

		$.ajax({
			type: "POST",
			url: "/set_user_name",
			data: {
				user_name: user_name
			},
			async: true
		})
		.done(function(result) {
			if (result['is_valid'] === true) {
				$('#user-name').text(result['user_name']);
			}
		});
	});
});
