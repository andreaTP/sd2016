
/*
 |--------------------------------------------------------------------------
 | Browser-sync config file
 |--------------------------------------------------------------------------
 |
 | For up-to-date information about the options:
 |   http://www.browsersync.io/docs/options/
 |
 | There are more options than you see here, these are just the ones that are
 | set internally. See the website for more info.
 |
 |
 */
module.exports = {
    "ui": {
        "port": 3001,
        "weinre": {
            "port": 8080
        }
    },
    "files": ["slide_reveal/index.html",
        "code1/shared/src/main/scala/PingPong.scala",
        "code2/shared/src/main/scala/raft/Raft.scala",
        "code3/js/src/main/scala/visualization/ToDo.scala",
        "code3/js/target/scala-2.11/demo-fastopt.js",
        "code3/js/target/scala-2.11/demo-launcher.js",
        "code4/js/src/main/scala/visualization/ChatUI.scala",
        "code4/js/target/scala-2.11/demo-fastopt.js",
        "code4/js/target/scala-2.11/demo-launcher.js",
        "code5/shared/src/main/scala/chat/ChatServer.scala",
        "code5/jvm/src/main/scala/ChatServerAkkaHttp.scala",
        "code5/js/src/main/scala/ChatServerNode.scala"
        ],
    "watchOptions": {},
    "server": false,
    "proxy": "localhost:8000",
    "port": 3000,
    "middleware": false,
    "serveStatic": [],
    "ghostMode": {
        "clicks": true,
        "scroll": true,
        "forms": {
            "submit": true,
            "inputs": true,
            "toggles": true
        }
    },
    "logLevel": "info",
    "logPrefix": "BS",
    "logConnections": false,
    "logFileChanges": true,
    "logSnippet": true,
    "rewriteRules": false,
    "open": "local",
    "browser": "default",
    "xip": false,
    "hostnameSuffix": false,
    "reloadOnRestart": false,
    "notify": true,
    "scrollProportionally": true,
    "scrollThrottle": 0,
    "scrollRestoreTechnique": "window.name",
    "scrollElements": [],
    "scrollElementMapping": [],
    "reloadDelay": 0,
    "reloadDebounce": 0,
    "plugins": [],
    "injectChanges": true,
    "startPath": null,
    "minify": true,
    "host": null,
    "codeSync": true,
    "timestamps": true,
    "clientEvents": [
        "scroll",
        "scroll:element",
        "input:text",
        "input:toggles",
        "form:submit",
        "form:reset",
        "click"
    ],
    "socket": {
        "socketIoOptions": {
            "log": false
        },
        "socketIoClientConfig": {
            "reconnectionAttempts": 50
        },
        "path": "/browser-sync/socket.io",
        "clientPath": "/browser-sync",
        "namespace": "/browser-sync",
        "clients": {
            "heartbeatTimeout": 5000
        }
    },
    "tagNames": {
        "less": "link",
        "scss": "link",
        "css": "link",
        "jpg": "img",
        "jpeg": "img",
        "png": "img",
        "svg": "img",
        "gif": "img",
        "js": "script"
    },
    "reloadDelay": 5000,
    "reloadDebounce": 2000
};