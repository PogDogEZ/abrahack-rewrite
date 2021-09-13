#!/usr/bin/env python3

import platform
import marshal
import os
import sys

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Invalid usage.")
        print("Use compiler.py <file>!")
        exit()

    file = sys.argv[1]

    print("Compiling '%s' for platform '%s' version '%s'" % (file, sys.platform, platform.python_version()))
    filestream = open(file, "r")
    data = filestream.read()
    filestream.close()

    compiled = compile(data, file, "exec")
    marshal.dump([sys.platform, platform.python_version(), compiled], open(os.path.splitext(file)[0] + ".plg", "wb"))