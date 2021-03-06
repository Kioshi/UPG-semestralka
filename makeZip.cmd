RD /s /q .\zip

MD zip
CD zip
MD bin
CD bin
MD semestralka
MD terrens
MD images
MD lib
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
COPY .\images\*.* .\zip\bin\images\*.*
COPY .\lib\*.* .\zip\bin\lib\*.*
COPY .\lib\*.* .\zip\src\semestralka\*.*

ECHO javac -cp semestralka\jcommon-1.0.17.jar;semestralka\jfreechart-1.0.14.jar semestralka\*.java > .\zip\src\Build.cmd
ECHO java -cp lib/jcommon-1.0.17.jar;lib/jfreechart-1.0.14.jar; semestralka.Main %%* > .\zip\bin\Run.cmd