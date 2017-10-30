#!/bin/bash
rsync -vaz --rsh="ssh -l root" src/* medserve.online:~/ROOT/
