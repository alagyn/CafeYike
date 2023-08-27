import os

from fastapi import FastAPI, Request, APIRouter
from fastapi.responses import RedirectResponse, HTMLResponse, FileResponse
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware

# Force this since windows is dumb and registry can be borked
import mimetypes
mimetypes.add_type('text/javascript', '.js', True)

import yikemng.api.admin as admin
import yikemng.api.bot_control as bot_control
import yikemng.config_manager as YMConfig

app = FastAPI()

FRONTEND_DIR = YMConfig.frontendDir

page_router = APIRouter(tags=['Pages'])

@page_router.get("/", response_class=RedirectResponse)
async def get_root(request: Request):
    return RedirectResponse("/index.html")

##### Subrouters

app.include_router(page_router)
app.include_router(admin.router)
app.include_router(bot_control.router)

# This goes last so that other api's take precendence
app.mount("/assets", StaticFiles(directory=os.path.join(FRONTEND_DIR, "assets")), name='root')


# Hardcode because mounting as root breaks things
@app.get("/index.html")
async def get_index() -> HTMLResponse:
    with open(os.path.join(FRONTEND_DIR, "index.html")) as f:
        return HTMLResponse(
            content=f.read(),
            status_code=200
        )

@app.get("/YikeBotLogoMk2.ico")
async def get_ico() -> FileResponse:
    return FileResponse(os.path.join(FRONTEND_DIR, "YikeBotLogoMk2.ico"))