
var cfg = {iceServers: [
    {urls: "stun:23.21.150.121"},
    {urls: "stun:stun.l.google.com:19302"},
    {urls: "stun:stun.voxgratia.org"},
    {urls: "stun:stunserver.org"}
  ]}

var channelOptions = {
    offerToReceiveAudio: false,
    offerToReceiveVideo: false
  }

var pc = new RTCPeerConnection(cfg)

var channel = undefined

var writeTo = function(field, json) {
  var txt = JSON.stringify(json)
  document.getElementById(field).value = txt
}

var readFrom = function(field) {
  return JSON.parse(document.getElementById(field).value)
}

var create = function() {
  console.log('create')

  pc.onicecandidate = function (e) {
    //console.log('ICE candidate (pc)', e)
    if (e.candidate == null) {
      console.log('ICE candidate (pc) FOUND!', e)
      
      writeTo('createText', pc.localDescription)
    }
  }
  
  channel = pc.createDataChannel("mychannel1", channelOptions);
  
  pc.createOffer(function (desc) {
      pc.setLocalDescription(desc, function () {}, function () {})
      console.log('created local offer', desc)
    },
    function (err) { console.warn("Couldn't create offer "+err) },
    channelOptions)

  bindChannel()
}
window.create = create

var doCreate = function() {
  console.log('create going on from here')
  var answerDesc = new RTCSessionDescription(readFrom('createAnswerText'))
  pc.setRemoteDescription(answerDesc)
}
window.doCreate = doCreate

var join = function() {
  console.log('join')

  var offerDesc = new RTCSessionDescription(readFrom('joinText'))

  pc.onicecandidate = function (e) {
    //console.log('ICE candidate answare (pc)', e)
    if (e.candidate == null) {
      console.log('ICE candidate answare (pc) FOUND', e)

      writeTo('joinAnswerText', pc.localDescription)
    }
  }

  pc.ondatachannel = function (e) {
    console.log("on data channel!!!")
    channel = e.channel

    bindChannel()
  }

  pc.setRemoteDescription(offerDesc)
  pc.createAnswer(function (answerDesc) {
    console.log('Created local answer: ', answerDesc)
    pc.setLocalDescription(answerDesc)
  },
  function () { console.warn("Couldn't create offer") },
  channelOptions)
}
window.join = join

var bindChannel = function() {
  channel.onopen = function (e) {
    console.log('Channel connected!!!')
  }

  channel.onerror = function (err) {
    console.error("Channel Error:", err)
  }

  channel.onmessage = function (e) {
    console.log("Got message:", e.data)
    if (e.data == "ping")
      channel.send("pong")
  }
}

var ping = function() {
  channel.send("ping")
}
window.ping = ping
