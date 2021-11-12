var releasedVersion = '2.0.0';
var releasedVersionAPK = 'https://cdn.jsdelivr.net/gh/zhuziwuming/TVRemoteIME@master/released/IMEService-release2.0.apk';
var feedbackUrl = 'http://www.cnblogs.com/kingthy/p/tvremoteime.html';
var rewardUrl = 'https://cdn.jsdelivr.net/gh/zhuziwuming/TVRemoteIME@master/released/reward.html';
$(function(){
	if(feedbackUrl.length && $('#feedbackLink').length == 0){
		$('.version').append('<a href="' + feedbackUrl + '" target="_blank" style="margin-left:10px;" id="feedbackLink">[问题反馈]</a>');
		$('.version').append('<a href="' + rewardUrl + '" target="_blank" style="margin-left:10px;color:#ff007f;" id="rewardLink">[赞赏一下呗]</a>');
	}
	$.get('/version', function(version){
		$('#curVer').html(version);
		$('#newVer').html(releasedVersion);
		$('#newVerUrl').attr('href', releasedVersionAPK);
		if(version != releasedVersion){
			$('#newVerUrl').show();
		}
	});
})
