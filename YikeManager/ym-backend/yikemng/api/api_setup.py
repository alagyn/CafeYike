import os

from fastapi import FastAPI, Request, APIRouter
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware

# Force this since windows is dumb and registry can be borked
import mimetypes
mimetypes.add_type('text/javascript', '.js', True)

import yikemng.api.admin as admin
import yikemng.api.bot_control as bot_control
from yikemng.config_manager import YMConfig

app = FastAPI()

FRONTEND_DIR = '../ym-frontend'
TEMPLATE_DIR = os.path.join(FRONTEND_DIR, 'templates')

templates = Jinja2Templates(directory=os.path.join(FRONTEND_DIR, 'templates'))


def _addStatic(mount, folder=None):
    if folder is None:
        folder = mount
    app.mount(f'/{mount}',
        StaticFiles(directory=os.path.join(FRONTEND_DIR, folder)),
        name=f'_{mount}')

_addStatic('scripts')
_addStatic('assets')
_addStatic('node_modules')
_addStatic('style')

page_router = APIRouter(tags=['Pages'])

@page_router.get("/", response_class=HTMLResponse)
async def get_root(request: Request):
    return templates.TemplateResponse(
        'index.html',
        {
            'request': request,
            'bot_exec': os.path.split(YMConfig.botFile)[1]
        })

##### Subrouters

app.include_router(page_router)
app.include_router(admin.router)
app.include_router(bot_control.router)