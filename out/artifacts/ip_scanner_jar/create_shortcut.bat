@echo off
setlocal EnableDelayedExpansion

:: Set the paths
set "JAR_PATH=%~dp0ip_scanner.jar"
set "LIB_PATH=%~dp0lib"
set "ICO_PATH=%~dp0icon\ipScannerLogo.ico"
set "DESKTOP_PATH=%USERPROFILE%\Desktop"
set "SHORTCUT_NAME=IP Scanner.lnk"

:: Build the arguments string with JavaFX modules
set "JAVAFX_MODULES=--module-path ""%LIB_PATH%"" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base"
set "JAR_COMMAND=-jar ""%JAR_PATH%"""

:: Create a temporary VBScript to generate the shortcut
(
    echo Set oWS = WScript.CreateObject("WScript.Shell"^)
    echo sLinkFile = "%DESKTOP_PATH%\%SHORTCUT_NAME%"
    echo Set oLink = oWS.CreateShortcut(sLinkFile^)
    echo oLink.TargetPath = "javaw.exe"
    echo oLink.Arguments = "%JAVAFX_MODULES% %JAR_COMMAND%"
    echo oLink.WorkingDirectory = "%~dp0"
    echo oLink.IconLocation = "%ICO_PATH%"
    echo oLink.Save
) > "%TEMP%\CreateShortcut.vbs"

:: Execute the VBScript and then delete it
cscript //nologo "%TEMP%\CreateShortcut.vbs"
del "%TEMP%\CreateShortcut.vbs"

:: Check if shortcut was created successfully
if exist "%DESKTOP_PATH%\%SHORTCUT_NAME%" (
    echo Shortcut created successfully on your desktop.
    echo Location: "%DESKTOP_PATH%\%SHORTCUT_NAME%"
) else (
    echo Failed to create shortcut.
)

pause