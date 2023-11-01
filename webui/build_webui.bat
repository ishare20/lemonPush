@ECHO off
call npm i

call npm run build

rd /s /q ..\desktop\dist\webui\ || echo "webui not exists"
mkdir ..\desktop\dist\webui\
mkdir ..\desktop\dist\webui\assets\

copy .\dist\index.html ..\desktop\dist\webui\

copy .\dist\assets\* ..\desktop\dist\webui\assets\


