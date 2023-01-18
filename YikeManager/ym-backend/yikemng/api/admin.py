from fastapi import APIRouter, Depends

from yikemng.bot_manager import manager

router = APIRouter(prefix='/system', tags=['System'])

@router.post('/reconfigure')
async def reconfigure():
    # TODO reload config file
    pass

