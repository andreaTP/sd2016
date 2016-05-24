cd ../akka.js/akka-js-actor/
echo "Total:"
wc -l `find . -name \*.scala -print` | grep total
cd shared
echo "Shared:"
wc -l `find . -name \*.scala -print` | grep total
cd ../js
echo "JS:"
wc -l `find . -name \*.scala -print` | grep total
