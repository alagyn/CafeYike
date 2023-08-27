from enum import Enum
from typing import List
import os


from fastapi import Response, status, HTTPException, APIRouter
from pydantic import BaseModel

from yikemng.bot_manager import BotStatusPackage, manager
from yikemng.config_manager import botFile

class BotCmd(Enum):
    START = 'start'
    STOP = 'stop'


router = APIRouter(prefix='/bot', tags=['Bot'])

@router.post("/{cmd}", status_code=status.HTTP_202_ACCEPTED)
async def bot_cmd(cmd: BotCmd, response: Response) -> BotStatusPackage:
    if cmd == BotCmd.START:
        if manager.isOnline():
            response.status_code = status.HTTP_304_NOT_MODIFIED
            return manager.getStatusPack()
        
        # TODO error handling?
        manager.startup()

    elif cmd == BotCmd.STOP:
        if not manager.isOnline():
            response.status_code = status.HTTP_304_NOT_MODIFIED
            return manager.getStatusPack()
        
        # TODO error handling
        manager.shutdown()

    return manager.getStatusPack()


@router.get("/")
async def bot_status() -> BotStatusPackage:
    return manager.getStatusPack()

class LogEntry (BaseModel):
    lines: List[str]
    num: int

@router.get("/logs", status_code=200)
async def bot_logs(response: Response, force: bool = False, lastLine: int = 0) -> LogEntry:
    if force or lastLine != manager.log_num:
        return LogEntry(
            lines=list(manager.log_queue),
            num = manager.log_num
            )
    else:
        response.status_code = status.HTTP_304_NOT_MODIFIED
        return LogEntry(lines=[], num=lastLine)


@router.get("/exec", status_code=200)
async def get_bot_exec() -> str:
    return os.path.split(botFile)[1]