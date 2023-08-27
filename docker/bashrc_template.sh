#!/bin/bash

if [ -f /etc/bashrc ]
then
    . /etc/bashrc
fi

export PY=/home/cafeyike/venv/bin/python
export YM_SYS_CFG=/home/cafeyike/dat/system.conf
export CafeYikeDB=/home/cafeyike/dat/cafe.db
export FrontendDir=/home/cafeyike/frontend
export export CafeYikeJar=/home/cafeyike/@@yikeJar