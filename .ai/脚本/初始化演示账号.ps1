# 初始化演示账号
# 用法：.\初始化演示账号.ps1 [-BaseUrl http://localhost:8081]
# 依赖：auth-server（8081）已启动
# 说明：调用内部建账号接口，不经过验证码与密码复杂度校验；重复执行幂等。

param(
    [string]$BaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$BaseUrl = $BaseUrl.TrimEnd('/')

$Password = "123456"

$Accounts = @(
    @{ phone = "15837000028"; name = "杨浚铠" },
    @{ phone = "13070606769"; name = "杨建国" },
    @{ phone = "16123395854"; name = "陈圣浩" },
    @{ phone = "18773448720"; name = "陈桂芬" },
    @{ phone = "17401035107"; name = $null },
    @{ phone = "19093910593"; name = $null },
    @{ phone = "16363841835"; name = $null },
    @{ phone = "14799249291"; name = $null },
    @{ phone = "15154442620"; name = $null },
    @{ phone = "13803304779"; name = $null },
    @{ phone = "17104743170"; name = $null },
    @{ phone = "14775749437"; name = $null },
    @{ phone = "19599441540"; name = $null },
    @{ phone = "13292197692"; name = $null }
)

Write-Host "==== 初始化演示账号 ====" -ForegroundColor Cyan
Write-Host ("目标服务：{0}" -f $BaseUrl)
Write-Host ("统一密码：{0}" -f $Password)
Write-Host ""

$success = 0
$failed = 0

foreach ($account in $Accounts) {
    $displayName = "—"
    if ($account.name) { $displayName = $account.name }

    $payload = @{
        phone    = $account.phone
        password = $Password
        realName = $account.name
    } | ConvertTo-Json -Depth 3

    try {
        $response = Invoke-RestMethod -Method Post `
            -Uri "$BaseUrl/auth/internal/accounts" `
            -ContentType "application/json; charset=utf-8" `
            -Body ([System.Text.Encoding]::UTF8.GetBytes($payload))
        $success++
        Write-Host ("{0}  {1}  accountId={2}" -f $account.phone, $displayName, $response.accountId)
    } catch {
        $failed++
        $detail = $_.ErrorDetails.Message
        if (-not $detail) { $detail = $_.Exception.Message }
        Write-Host ("{0}  {1}  失败：{2}" -f $account.phone, $displayName, $detail) -ForegroundColor Red
    }
}

Write-Host ""
Write-Host ("完成：成功 {0} 个，失败 {1} 个" -f $success, $failed) -ForegroundColor Green
if ($failed -gt 0) { exit 1 }
