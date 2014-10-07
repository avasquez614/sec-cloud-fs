#!/bin/sh

./randfiles.py 25 0 1000000 $1
./randfiles.py 15 1000000 10000000 $1
./randfiles.py 7 10000000 50000000 $1
./randfiles.py 3 50000000 100000000 $1
