#!/bin/bash

echo $1
echo $2
java -cp ./restfb-1.10.1.jar:./c.jar:. GetID $1 $2
