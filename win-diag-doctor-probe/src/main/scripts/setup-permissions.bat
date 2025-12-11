@echo off
chcp 65001 >nul
echo ========================================================
echo   WinDiagDoctor Probe 权限配置向导
echo   WinDiagDoctor Probe Permission Setup Wizard
echo ========================================================
echo.
echo [INFO] 正在将当前用户 "%USERNAME%" 添加到 "Event Log Readers" 组...
echo [INFO] Adding user "%USERNAME%" to "Event Log Readers" group...
echo.

net localgroup "Event Log Readers" "%USERNAME%" /add

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] 配置成功！
    echo [IMPORTANT] 请务必【注销并重新登录】Windows，权限才会生效。
    echo [IMPORTANT] You MUST Log off and Log back in for changes to take effect.
) else (
    echo.
    echo [ERROR] 权限不足或发生错误。
    echo [TIP] 请右键点击本文件，选择【以管理员身份运行】。
    echo [TIP] Please right-click this file and select "Run as administrator".
)

pause