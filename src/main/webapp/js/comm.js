String.prototype.trim = function(){
    return this.replace(/(^\s*)|(\s*$)/g, "");
}

function onlyNum(event)
{
    var event = event || window.event;	
    var key = event.keyCode;

    if(!(key==46) && !(key==8) && !(key==37) && !(key==39))
    {
        if(!((key >=48 && key <= 57) || (key >= 96 && key <= 105)))
        {
            if (window.event) //IE    
            {
                event.returnValue = false;    
            }    
            else //Firefox    
            {            	
                event.preventDefault();  
                event.stopPropagation();//取消冒泡
            }    
        }
    }
}

function getRadioValue(name){
    var objArr = document.getElementsByName(name);
    for(var i=0;i<objArr.length;i++){
        if(objArr[i].checked){
            return objArr[i].value;
        }
    }
}

function checkEmail(str) {
    //alert(str);
    isEmail1 = /^\w+([\.\-]\w+)*\@\w+([\.\-]\w+)*\.\w+$/;
    isEmail2 = /^.*@[^_]*$/;
    //alert(isEmail1.test(str)&&isEmail2.test(str));
    return (isEmail1.test(str) && isEmail2.test(str));
}
function IsDigit(cCheck) {
    return (('0' <= cCheck) && (cCheck <= '9'));
}

function IsAlpha(cCheck) {
    return ((('a' <= cCheck) && (cCheck <= 'z')) || (('A' <= cCheck) && (cCheck <= 'Z')))
}

function IsaNull(cCheck) {
    return (cCheck != " ")
}

function isDigitORAlpha(s) {
    var patrn = /^\d{17}$|^[0-9a-zA-Z]{17}$/;
    if (!patrn.exec(s))
        return false;
    return true;
}

function WriteCookie(cookieName, cookieValue, expiry) {
    var expDate = new Date();
    var newCookieValue = escape(cookieValue).replace(/\+/g, '%2B');

    // 如果设置了cookie失效时间;
    if (expiry) {
        expDate.setTime(expDate.getTime() + expiry);
        document.cookie = cookieName + "=" + (newCookieValue) + "; expires="
        + expDate.toGMTString();
    } else {
        // escape不对+号编码，要手动替换。
        document.cookie = cookieName + "=" + (newCookieValue);
    }
}

function deleteCookie(name) {
    var expires = new Date();
    expires.setTime(expires.getTime() - 1);
    WriteCookie(name, "Delete Cookie", expires);
}

function getCookie(cookieName) {
    var cookiestring = document.cookie;
    var cookie = cookiestring.split(";");
    for ( var i = 0; i < cookie.length; i++) {
        var keyvalue = cookie[i].split("=");
        if (keyvalue[0].Trim() == cookieName) {
            return keyvalue[1];
        }
    }

}

/**处理键盘事件*/
function doKey(e){
    var ev = e || window.event;//获取event对象  
    var obj = ev.target || ev.srcElement;//获取事件源  
    var t = obj.type || obj.getAttribute('type');//获取事件源类型  
    if(ev.keyCode == 8 && t != "password" && t != "text" && t != "textarea"){
        return false;
    }
   
    if(ev.keyCode == 8 && (t=="password" || t=="text" || t=="textarea")){
        var vReadOnly = obj.getAttribute('readonly');
        var vEnabled = obj.getAttribute('enabled');
        if(vReadOnly){
            return false;
        }
        if(vEnabled){
            return true;
        }
    }
}
