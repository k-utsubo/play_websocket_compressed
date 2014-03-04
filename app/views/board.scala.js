@(code:String)(implicit r: RequestHeader)

$(function() {

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var socket = new WS("@routes.Application.data(code).webSocketURL()")

    
    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        var bs=base64decode(data.data)
        var dec=zip_inflate(bs)
        var p=JSON.parse(dec)
        $("#data").text(p.data);
       
    }

    socket.onmessage = receiveEvent

})


