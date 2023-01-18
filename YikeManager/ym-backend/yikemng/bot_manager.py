import subprocess as sp
from enum import Enum
import time

from pydantic import BaseModel

from yikemng.errors import YMError

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

    def startup(self):
        if self.status != BotStatus.OFFLINE:
            raise YMError("BotManager.startup: Cannot Start bot, bot is not offline")

        self.bot_startTime = time.time()
        # TODO start bot
        self.status = BotStatus.ONLINE

    def shutdown(self):
        if self.status != BotStatus.ONLINE:
            raise YMError("BotManager.shutdown: Cannot stop bot, bot is not online")

        self.status = BotStatus.OFFLINE


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

manager = _BotManager()