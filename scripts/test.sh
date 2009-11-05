#!/bin/bash
cd $(dirname $0)
python test.py
xml_pp test.log > test.xml
rm test.log
echo "test.xml produced"
