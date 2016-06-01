python -m SimpleHTTPServer&

cd ..
cd wetty
./start.sh&
cd ../sd2016

cd code3
sbt "fastOptJS"&
sbt "fullOptJS"&
cd ..

cd code4
sbt "fastOptJS"&
sbt "fullOptJS"&
cd ..

cd code5
sbt "demoJVM/run"&
sbt "demoJS/run"&
cd ..

cd code6
sbt "fastOptJS"&
sbt "fullOptJS"&
cd ..

##to be tested...
#cd ..
#cd stunserver
#./stunserver --mode basic --primaryinterface lo&
#cd ../sd2016

#browser-sync start --config bs-config.js
