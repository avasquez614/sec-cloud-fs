#!/usr/bin/env python
import os
import random
import sys
import string
import uuid

def generateFile(size, outputDir):
    name = str(uuid.uuid1())
    file = open(outputDir + '/' + name, 'w')
    
    print 'Generating file "{}" of size {}'.format(name, size)
    
    for i in xrange(size):
        file.write(random.choice(string.printable))
        
    file.close()      

totalFiles = int(sys.argv[1])
minSize = int(sys.argv[2])
maxSize = int(sys.argv[3])
outputDir = sys.argv[4]

if not os.path.exists(outputDir):
    os.makedirs(outputDir)

for i in xrange(totalFiles):
    randSize = random.randrange(minSize, maxSize)
    generateFile(randSize, outputDir)
        
     