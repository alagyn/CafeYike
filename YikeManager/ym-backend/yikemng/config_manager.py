import configparser
import os


_CONFIG = os.environ["YM_SYS_CFG"]
if _CONFIG is None or len(_CONFIG) == 0:
    _CONFIG = 'system.cfg'

_configs = {}

print("Loading Configs")
with open(_CONFIG, mode='r') as f:
    for line in f:
        line = line.strip()
        if len(line) == 0 or line.startswith('#'):
            continue
        key, val = line.split('=')
        _configs[key.strip()] = val.strip()

class YMConfig:
    configFile = _CONFIG
    botFile = _configs['CafeYikeJar']
    dbFile = _configs['CafeYikeDB']
    tempDir = _configs['TempDir']
    maxLogLines = int(_configs['MaxLogLines'])
