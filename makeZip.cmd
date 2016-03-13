RD /s /q .\zip

MD zip
CD zip
MD bin
CD bin
MD semestralka
MD terrens
CD ..
MD src
CD src
MD semestralka
CD ..
MD doc
CD ..

COPY .\out\production\semestralka\semestralka\*.* .\zip\bin\semestralka\*.*
COPY .\src\semestralka\*.* .\zip\src\semestralka\*.*
COPY .\terrens\*.* .\zip\bin\terrens\*.*
COPY .\doc\*.* .\zip\doc\*.*

ECHO javac semestralka\*.java > .\zip\src\Build.cmd
ECHO java semestralka.Main %%* > .\zip\bin\Run.cmd