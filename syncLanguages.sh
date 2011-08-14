#!/bin/bash

fixlang() {
    rm -rf res/values-$1
    cp res/values-no res/values-$1
}
fixlang no



fixlangRegion() {
    rm -rf res/values-$1-r$2
    cp res/values-no res/values-$1-r$2
}
fixlangRegion en NO