cd ..
cd wetty
node app.js -p 4000 > /dev/null 2> /dev/null&
cd ../sd2016

cd code1
sbt clean
cd ..

cd code2
sbt clean
cd ..

cd code3
sbt fullOptJS
cd ..

cd code4
sbt fullOptJS
cd ..

cd code5
sbt "demoJVM/run"&
sbt "demoJS/run"&
cd ..

cd code6
sbt fullOptJS
cd ..

python -m SimpleHTTPServer

##to be tested...
#cd ..
#cd stunserver
#./stunserver --mode basic --primaryinterface lo&
#cd ../sd2016
