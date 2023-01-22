##### INIT API

from yikemng.api.api_setup import app

##### INIT MANAGER

import yikemng.bot_manager

##### Start server

from uvicorn.config import Config
from uvicorn.server import Server

if __name__ == '__main__':
    config = Config(
        'yikemng.main:app', 
        port=8000,
        log_level='debug',
        reload=True,
        host='0.0.0.0',
        forwarded_allow_ips='*'
        )
    server = Server(config)
    server.run()

