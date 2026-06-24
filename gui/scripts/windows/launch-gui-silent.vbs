' Runs launch-gui.bat completely hidden (no console window), regardless of the
' user's terminal "close on exit" settings. The shortcut targets this file via
' wscript.exe instead of launch-gui.bat directly so no window ever appears.
Dim shell, fso, scriptDir, batPath

Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
batPath = fso.BuildPath(scriptDir, "launch-gui.bat")

shell.Run """" & batPath & """", 0, False
