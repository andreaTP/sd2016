
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

var create = function() {
  console.log('create')

  pc.onicecandidate = function (e) {
    //console.log('ICE candidate (pc)', e)
    if (e.candidate == null) {
      console.log('ICE candidate (pc) FOUND!', e)
      document.getElementById('createText').value = JSON.stringify(pc.localDescription)
    }
  }
  
  channel = pc.createDataChannel("mychannel1", channelOptions);
  
  pc.createOffer(function (desc) {
      pc.setLocalDescription(desc, function () {}, function () {})
      console.log('created local offer', desc)
    },
    function (err) { console.warn("Couldn't create offer "+err) },
    channelOptions)

  channel.onopen = function (e) {
    console.log('data channel connected!!!')

    channel.send("We io ti ho creato!!!")
  }

  channel.onerror = function (err) {
    console.error("Channel Error:", err)
  }

  channel.onmessage = function (e) {
    console.log("Got message:", e.data)
  }
}

var doCreate = function() {
  console.log('create going on from here')
  var answerDesc = new RTCSessionDescription(JSON.parse(document.getElementById('createAnswerText').value))
  pc.setRemoteDescription(answerDesc)
}

var join = function() {
  console.log('join')

  var offerDesc = new RTCSessionDescription(JSON.parse(document.getElementById('joinText').value))

  pc.onicecandidate = function (e) {
    //console.log('ICE candidate answare (pc)', e)
    if (e.candidate == null) {
      console.log('ICE candidate answare (pc) FOUND', e)
      document.getElementById('joinAnswerText').value = JSON.stringify(pc.localDescription)
    }
  }

  pc.ondatachannel = function (e) {
    console.log("on data channel!!!")
    channel = e.channel

    channel.onopen = function (e) {
      console.log('data channel connected!!!')
    }

    channel.onerror = function (err) {
      console.error("Channel Error:", err)
    }

    channel.onmessage = function (e) {
      console.log("Got message:", e.data)
      channel.send("E allora ti rispondo!")
    }
  }

  pc.setRemoteDescription(offerDesc)
  pc.createAnswer(function (answerDesc) {
    console.log('Created local answer: ', answerDesc)
    pc.setLocalDescription(answerDesc)
  },
  function () { console.warn("Couldn't create offer") },
  channelOptions)
}
