# UIMS
## 教务中心

### 由于2017年12月教务系统的一次更新，使得Cookie信息需要手动添加部分数据
```java
addHeader("Cookie", "loginPage=userLogin.jsp; alu=" + 教学号+ "; pwdStrength=1;")
```

### 登陆时，表单向```http://uims.jlu.edu.cn/ntms/j_spring_security_check```提交的数据
| 数据 | 解释 |
|------|-------|
| j_username | 教学号 |
| j_password | （UIMS+密码+教学号）所生成的MD5 |
| mousePath | 滑动验证数据 |

#### 数据分析
进行登录验证的js文件有userLogin.js、utilSeldom.js、SlideVerify.js。在userLogin.js文件中进行了一些初始判断，以及最终数据的传递，在utilSeldom.js文件中提供了j_password的生成过程。在SlideVerify.js文件中定义了mousePath的生成过程，较为繁琐，但是在其数据的生成过程中没有用到时间（时间仅作为是否滑动过程超时）、Cookie等可作为验证的数据，因此猜想该数据的验证只在于数据的正确与否，经过验证该数据可以通过一次截包后重复使用。

### 数据截图
<table>
<tr>
	<td><img  src="img/捕获.JPG?raw=true"></td>
	<td><img  src="img/捕获1.JPG?raw=true"></td>

</tr>
<tr>
	<td><img  src="img/捕获2.JPG?raw=true"></td>
	<td><img  src="img/捕获3.JPG?raw=true"></td>
</tr>  
</table>


```javaScript
var form = dojo.byId("loginForm");

var userName=form.j_username.value;
var pwds = form.pwdPlain.value;
var index=userName.indexOf('@');
var userName1="";//正常我们输入时是不带@mails.jlu.edu.cn

			if(index>0){
				userName.substr(0, index);
				userName1 = userName.substr(index+1,userName.length);
			}else{//So Here !
				userName1= userName;
				pwdStrength = this.checkPwdStrength(pwds, userName);
				setUserInfoCookie( 'pwdStrength', pwdStrength);
			}

checkPwdStrength:function(s, username){//s:密码 username:教学号
			if(s.length < 4)//小于4位密码
				return 0;
			if (s==username)//密码与教学号相同
				return 0;
			if (s=='000000')//密码纯零
				return 0;

			var ls = 0;//  ig 全局不区分大小写
			if (s.match(/[a-z]/ig)) //含有英文字母
				ls++;
			if (s.match(/[0-9]/ig)) //含有数字
				ls++;
			if (s.match(/(.[^a-z0-9])/ig))//含有除英文字母，数字外的其他
				ls++;
			if (s.length < 6 && ls > 0)//...
				ls--;
			return ls;
		},

form.j_password.value = ntms.util.makeTransferPwd(userName1, pwds);
this.makeTransferPwd=function(b,d){//b:教学号  d:密码
    dojo.require("dojox.encoding.digests.MD5");
    var a=dojox.encoding.digests;
    var c="UIMS"+b+d;
    var e=a.MD5(c,a.outputTypes.Hex);//摘要以16进制表示
    return e
    };


var verifyStr = "";
var wdSlideVerify = dijit.byId("slideVerify");
if (!wdSlideVerify.isSuccess() ){
	if (userName1.length == 8 && false) {
			alert("请拖动下方滑块验证");
			return false;
	}
}else{
	verifyStr = wdSlideVerify.getZipPath();
}
form.mousePath.value = verifyStr;


dojo.provide("skyw.widget.SlideVerify");
dojo.require("dojox.encoding.digests._base");
dojo.declare(
	"skyw.widget.SlideVerify",
	[dijit._Widget, dijit._Templated],
	{
		widgetsInTemplate: true,
		templatePath: dojo.moduleUrl("skyw", "widget/templates/SlideVerify.html"),
		_startTime:0,
		_startX:0,
		_startY:0,
		_moveLen: 230,

		constructor: function () {
			this._paint = false;
			this._success = false;
			this._clickPath = [];
		},
		startup: function () {
			this.inherited(arguments);
			this._clickPath = [];
			var block = this.wdLabel;
			this.connect(block, "onmousedown", "onStart");
			this.connect(block, "onmousemove", "onMove");
			this.connect(block, "onmouseup", "onEnd");
			this.connect(block, "onmouseleave", "onEnd");

			this.connect(block, "ontouchstart", "onStart");
			this.connect(block, "ontouchmove", "onMove");
			this.connect(block, "ontouchend", "onEnd");

			// array of 32-bit
			var s1 = dojox.encoding.digests.wordToHex([12]);//"0c000000"
			var s2 = dojox.encoding.digests.wordToBase64([12]);//"DAAAAA=="

			var str = this._word30ToBase64([0x01020304]);//"BAgME"

		},
		onStart: function(e){
			this._startTime = (new Date()).getTime();
			this._paint = true;
			var ee= e.touches ? e.touches[0] : e;
			this._startX = ee.clientX;//

			var pos= dojo.position(this.wdLabel, false);
			this._startY = Math.round(pos.y);//

			//第一个值的x是起始click的相对位置
			this._addClick(this._startX - Math.round(pos.x), ee.clientY - this._startY, 0);
		},
		onMove: function(e){
			if (!this._paint) return;
			var ee= e.touches ? e.touches[0] : e;
			var posx= ee.clientX - this._startX;//
			var posy= ee.clientY - this._startY;//
			this._addClick(posx, posy, 1);
			this.updateView(posx);
			this.checkSuccess(posx);
		},
		onEnd: function(e){
			if (!this._success)
				this.reset();
		},
		_changeStyle:function(/*boolean success or reset */suc){
			//this._success = suc; //为了明显起见，不放到这里了
			this._paint = false;
			var span = this.id + "_prompt";

			dojo.attr(span, "innerHTML", suc? "验证成功" : "拖动滑块验证");
			dojo.toggleClass(span, "success", suc);
			dojo.attr(this.wdLabel, 'innerHTML', suc ? "OK" : "&gt;&gt;");

			this.updateView(suc ? this._moveLen : 0);
		},
		checkSuccess:function(x){
			//can bu connect
			if (x < this._moveLen) {
				return false;
			}
			this._success = true;
			this._changeStyle(true);
			this.onClientSuccess();
			return true;
		},
		onClientSuccess:function(){
			//for connect
		},
		onTimerOut:function(){
			alert("超时");
		},
		updateView: function(layerX){
			layerX+="px";
			dojo.style(this.wdLabel, 'left', layerX);
			dojo.style(this.id + "_bgSlider", 'width', layerX);
		},
		reset:function(){
			//console.log("reset");
			this._clickPath = [];
			this._success = false;
			this._changeStyle(false);
		},
		_addClick: function (ax, ay, dragging){
			var time =0;
			if (dragging != 0) {
				time = (new Date()).getTime();
				time -= this._startTime;
			}
			if (time > 30000){ //MAX_TIME
				this.onTimerOut();
				this.reset();
				return;
			}
			var x= {x:ax, y:ay, t:time}
			this._clickPath.push(x);
			return x;
		},
		_base64Tab:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",
		_word30ToBase64: function(/* word[] */wa){
			//	summary: 每一个word是高位在前
			var s=[], tab= this._base64Tab;
			for(var i=0; i<wa.length; ++i){
				var t= wa[i];
				for(var j=0; j<5; ++j){
					s.push(tab.charAt((t>>6*(4-j)) & 0x3F));
				}
			}
			return s.join("");	//	string
		},
		isSuccess:function(){
			return this._success;
		},
		getZipPath:function(){
			var VERSION = 1;
			if (!this._success) return "";
			//console.log(this._clickPath);
			var len = this._clickPath.length;
			var x=[], checksum=0;
			for(var i=0; i<len; ++i){
				var p= this._clickPath[i];
				if (i==0) p.t= VERSION; //用来标记版本
				//最后一个的x可能会超界
				if ((i==len-1) && (p.x>255)) p.x = 255;
				if (p.y > 65535 || p.x >255 || p.y >128 || p.y<0) {
					console.log(p);
					alert("数据超界");
					return "";
				}
				var y =(p.y << 24) | (p.x << 16) | p.t;
				x.push( y) ;
				checksum = checksum ^ y;
			}
			x.push(checksum);
			var str = this._word30ToBase64(x);
			return str;
		},
		_tail:null
	}
);


 ```

一次跳转-> Location:```http://uims.jlu.edu.cn/ntms/index.do```
```java
String login_suss = response.getFirstHeader("location").getValue().trim().split(";")[0];
System.out.println("中间跳转 ---> " + login_suss);

HttpGet get = new HttpGet(login_suss);
get.addHeader("Upgrade-Insecure-Requests", "1");
get.addHeader("Cookie", "loginPage=userLogin.jsp; alu=" + username+ "; pwdStrength=1; ");

get.addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36");
response = client.execute(get);
System.out.println("跳转登陆:\t" + response.getStatusLine());
```

### 学生信息
POST ```http://uims.jlu.edu.cn/ntms/action/getCurrentUserInfo.do```


### POST  ```http://uims.jlu.edu.cn/ntms/service/res.do```
#### 数据示例

##### 评教：
  查看未评记录：```"{\"tag\":\"student@evalItem\",\"branch\":\"self\",\"params\":{\"blank\":\"Y\"}}"```

  评教数据：```"{\"evalItemId\":\"" + evalItemId+ "\",\"answers\":{\"prob11\":\"A\",\"prob12\":\"A\",\"prob13\":\"D\",\"prob14\":\"A\",\"prob15\":\"D\",\"prob21\":\"A\",\"prob22\":\"A\",\"prob23\":\"A\",\"prob31\":\"A\",\"prob32\":\"A\",\"prob41\":\"A\",\"prob42\":\"A\",\"prob43\":\"C\",\"prob51\":\"A\",\"prob52\":\"A\",\"sat6\":\"A\",\"mulsel71\":\"L\",\"advice8\":\"\"}}"```

  查看已评记录：```"{\"tag\":\"student@evalItem\",\"branch\":\"self\",\"params\":{\"done\":\"Y\"}}"```

##### 成绩：
  查看最新成绩：```"{\"tag\":\"archiveScore@queryCourseScore\",\"branch\":\"latest\",\"params\":{},\"rowLimit\":"+ num + "}"```

  查看某一科比例：```"{\"asId\":\"" + asId + "\"}"```

##### 课表：
  查看某一学期：```"{\"tag\":\"teachClassStud@schedule\",\"branch\":\"default\",\"params\":{\"termId\":" + term+ ",\"studId\":" + stuID + "}}"```

##### 课程：
  查看某一课程分数：```"{\"tag\":\"termScore@inqueryTermScore\",\"branch\":\"default\",\"params\":{\"termId\":"+ term + ",\"studId\":" + stuID + "}}"```

  查看某一学期分数：```"{\"tag\":\"archiveScore@queryCourseScore\",\"branch\":\"byTerm\",\"params\":{\"studId\":"+ stuID + ",\"termId\":" + term + "},\"orderBy\":\"teachingTerm.termId, course.courName\"}"```
