# 生成员工培训证明
# 用法：.\生成员工培训证明.ps1 [-BaseUrl http://localhost:8082]
# 依赖：logistics-server（8082）与 auth-server（8081）已启动
# 说明：只调接口，不直接读写数据库；岗位与网点均由签发方显式选择，没有默认值。

param(
    [string]$BaseUrl = "http://localhost:8082"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$BaseUrl = $BaseUrl.TrimEnd('/')

function Read-Phone {
    while ($true) {
        $phone = (Read-Host "请输入员工手机号").Trim()
        if ($phone -match '^1[3-9]\d{9}$') {
            return $phone
        }
        Write-Host "手机号格式不正确，请重新输入" -ForegroundColor Yellow
    }
}

function Read-Role {
    while ($true) {
        $role = (Read-Host "请选择岗位（1快递员 2客服 3司机）").Trim()
        if ($role -match '^[123]$') {
            return [int]$role
        }
        Write-Host "岗位只能是 1、2、3" -ForegroundColor Yellow
    }
}

function Read-Site {
    try {
        $sites = @(Invoke-RestMethod -Method Get -Uri "$BaseUrl/logistics/internal/sites")
    } catch {
        throw "拉取网点列表失败，请确认 logistics-server 已启动：$($_.Exception.Message)"
    }
    if ($sites.Count -eq 0) {
        throw "网点列表为空，请先执行 logistics 的种子迁移"
    }

    Write-Host "请选择归属网点："
    for ($i = 0; $i -lt $sites.Count; $i++) {
        $site = $sites[$i]
        $flag = ""
        if ($site.enabled -ne 1) { $flag = "（停用）" }
        Write-Host ("  {0}) {1} ({2}){3}" -f ($i + 1), $site.siteName, $site.siteCode, $flag)
    }

    while ($true) {
        $index = (Read-Host "请输入序号").Trim()
        if ($index -match '^\d+$') {
            $number = [int]$index
            if ($number -ge 1 -and $number -le $sites.Count) {
                return $sites[$number - 1]
            }
        }
        Write-Host "序号超出范围，请重新输入" -ForegroundColor Yellow
    }
}

Write-Host "==== 生成员工培训证明 ====" -ForegroundColor Cyan
$phone = Read-Phone
$staffRole = Read-Role
$site = Read-Site

$payload = @{
    phone     = $phone
    staffRole = $staffRole
    siteCode  = $site.siteCode
} | ConvertTo-Json -Depth 3

try {
    $response = Invoke-RestMethod -Method Post `
        -Uri "$BaseUrl/logistics/internal/staff-trainings" `
        -ContentType "application/json; charset=utf-8" `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($payload))
} catch {
    Write-Host ""
    Write-Host "发证失败" -ForegroundColor Red
    if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message
    } else {
        Write-Host $_.Exception.Message
    }
    exit 1
}

Write-Host ""
Write-Host "培训证明已生成" -ForegroundColor Green
Write-Host ("  凭证码    : {0}" -f $response.voucherCode)
Write-Host ("  学员      : {0}" -f $response.traineeName)
Write-Host ("  岗位      : {0}" -f $response.staffRole)
Write-Host ("  归属网点  : {0} ({1})" -f $response.siteName, $response.siteCode)
Write-Host ("  有效期至  : {0}" -f $response.expireAt)
Write-Host ("  凭证 ID   : {0}" -f $response.staffTrainingId)
Write-Host ""
Write-Host "下一步：在 App「我的 → 成为员工」输入凭证码（入职接口待实现）" -ForegroundColor DarkGray
