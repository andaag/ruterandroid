#!/bin/bash

fixlang() {
    echo "fixlang $1"
    rm -rf res/values-$1
    cp -r res/values-nb res/values-$1
}
fixlang no
fixlang sv



fixlangRegion() {
    echo "fixlang $1 region $2"
    rm -rf res/values-$1-r$2
    cp -r res/values-nb res/values-$1-r$2
}
fixlangRegion en NO
fixlangRegion en SE