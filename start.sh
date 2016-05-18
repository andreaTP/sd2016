python -m SimpleHTTPServer&

cd ..
cd wetty
./start.sh&
cd ../akka.js_talk

cd code3
sbt "~fastOptJS"&
cd ..

cd code4
sbt "~fastOptJS"&
cd ..

cd code5
sbt "demoJVM/run"&
sbt "demoJS/run"&
cd ..

browser-sync start --config bs-config.js
