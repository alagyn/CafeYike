import configparser
import os

try:
    _CONFIG = os.environ["YM_SYS_CFG"]
except KeyError:
    _CONFIG = 'system.cfg'

_configs = {}

if not os.path.exists(_CONFIG):
    print(f"Cannot find system config: \"{os.path.abspath(_CONFIG)}\".")
    print("Set YM_SYS_CFG env var")
    exit(0)

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
    tempDir = _configs['TempDir']
    maxLogLines = int(_configs['MaxLogLines'])
    
    botFile = os.environ['CafeYikeJar']
    frontendDir = os.environ['FrontendDir']
