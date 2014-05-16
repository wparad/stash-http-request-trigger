(function ($) 
{
    $(document).ready(function () 
    {
        var waiting = '<span class="aui-icon aui-icon-wait">Wait</span>';
        var button = $(".triggerBuild");
        var link = $('<textarea />').html(button.text()).text();
        
        button.html("Build");
		button.click(function() 
		{
		    var $this = $(this);
		    var text = $this.text();
		
		    $this.attr("disabled", "disabled").html(waiting + " " + text);
		    $.post(link, function() 
		    { 
		    	setTimeout(function() { $this.removeAttr("disabled").text(text).reload(); }, 500);  
		    });
		    //Set it back anyway after 1 second
		    setTimeout(function() { $this.removeAttr("disabled").text(text).reload(); }, 1000);
		    return false;
		});
	});
})(AJS.$);