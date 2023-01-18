from enum import Enum

from fastapi import Response, status, HTTPException, APIRouter

from yikemng.bot_manager import BotStatusPackage, manager

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