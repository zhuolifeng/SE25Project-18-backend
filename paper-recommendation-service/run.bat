@echo off
REM ================================================
REM  run.bat  —  start paper‑recommender locally
REM ================================================
setlocal
cd /d "%~dp0"

REM ---------- virtualenv ----------
if not exist ".venv\Scripts\python.exe" (
    echo [SETUP] Creating venv...
    python -m venv .venv || goto :error
)

set PY=.venv\Scripts\python.exe

REM ---------- install deps (first run only) ----------
if not exist ".venv\.deps_ok" (
    echo [PKGS ] Installing requirements...
    "%PY%" -m pip install -r requirements.txt || goto :error
    echo ok> ".venv\.deps_ok"
) else (
    echo [PKGS ] Dependencies ok, skipping pip install.
)

REM ---------- build index once ----------
if not exist "faiss.index" (
    echo [INDEX] Building vector index...
    "%PY%" embeddings\build_index.py || goto :error
)

REM ---------- launch service ----------
echo [RUN  ] Starting Uvicorn on http://localhost:8010 ...
set UVICORN_WORKERS=1
set TORCH_NUM_THREADS=1
"%PY%" -m uvicorn app.main:app --host 0.0.0.0 --port 8010 --workers 1
goto :eof

:error
echo.
echo ********  SCRIPT FAILED  (errorlevel %errorlevel%) ********
pause
endlocal
