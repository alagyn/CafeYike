import subprocess as sp
from enum import Enum
import time
from collections import deque
from typing import Optional
import logging
import threading as thr
import signal
import os

from pydantic import BaseModel

from yikemng.errors import YMError
from yikemng.config_manager import YMConfig

class BotStatus(str, Enum):
    """
    Enumeration of Possible Bot Status States
    """
    ONLINE = "Online"
    OFFLINE = "Offline"

class BotStatusPackage(BaseModel):
    """
    Object containing bot status data
    """
    status: BotStatus
    bot_uptime: int
    server_uptime: int


class _BotManager:
    def __init__(self) -> None:
        self.status = BotStatus.OFFLINE
        self.server_startTime: float = time.time()
        self.bot_startTime: float = time.time()
        self.bot_proc: Optional[sp.Popen] = None
        self.log_queue: deque[str] = deque(maxlen=YMConfig.maxLogLines)
        self.log_thr: Optional[thr.Thread] = None


    def startup(self):
        if self.status != BotStatus.OFFLINE:
            raise YMError("BotManager.startup: Cannot Start bot, bot is not offline")

        if not os.path.exists(YMConfig.tempDir):
            os.makedirs(YMConfig.tempDir, exist_ok=True)

        env = os.environ.copy()

        env["YM_SYS_CFG"] = os.path.abspath(YMConfig.configFile)
        env['CafeYikeDB'] = os.path.abspath(YMConfig.dbFile)

        self.bot_startTime = time.time()
        self.bot_proc = sp.Popen(
            ["java", '-Djava.io.tmpdir=temp', "-cp", YMConfig.botFile, 'org.bdd.cafeyike.CafeYike'],
            env=env,
            stdout=sp.PIPE,
            universal_newlines=True,
            #creationflags=sp.CREATE_NO_WINDOW | sp.DETACHED_PROCESS | sp.CREATE_NEW_PROCESS_GROUP
        )

        if self.log_thr is not None:
            self.log_thr.join()

        logging.info("Starting log thread")
        self.log_thr = thr.Thread(target=self._consoleLogThread, daemon=True)
        self.log_thr.start()

        self.status = BotStatus.ONLINE

    def shutdown(self):
        if self.status != BotStatus.ONLINE:
            raise YMError("BotManager.shutdown: Cannot stop bot, bot is not online")

        self.status = BotStatus.OFFLINE
        if self.bot_proc is not None:
            try:
                print("Shutting down bot")
                self.bot_proc.send_signal(signal.CTRL_C_EVENT)
                print("Bot shutdown")
            except KeyboardInterrupt:
                pass
            self.bot_proc = None

        # TODO delete temp dir contents?

    def isOnline(self) -> bool:
        return self.status == BotStatus.ONLINE

    def getStatusPack(self) -> BotStatusPackage:
        now = time.time()
        if self.isOnline():
            bot_uptime = int(now - self.bot_startTime)
        else:
            bot_uptime = 0

        server_uptime = int(now - self.server_startTime)

        return BotStatusPackage(
            status=self.status,
            bot_uptime=bot_uptime,
            server_uptime=server_uptime
            )

    def _consoleLogThread(self) -> None:
        logging.info("In log thread")
        if self.bot_proc is not None and self.bot_proc.stdout is not None:
            for line in iter(self.bot_proc.stdout.readline, ''):
                self.log_queue.append(line)
                print(line, end='')

            # Should only get here when stdout returns EOF
            if self.status != BotStatus.OFFLINE:
                logging.warning("STDOUT Closed Unexpectedly")
                self.bot_proc = None
                self.shutdown()


manager = _BotManager()