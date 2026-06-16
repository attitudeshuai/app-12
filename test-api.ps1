Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ToolShare API 接口测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8082"
$passCount = 0
$failCount = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        [object]$Body = $null,
        [int]$ExpectedStatus = 200,
        [bool]$ExpectCode200 = $true
    )

    Write-Host "测试: $Name" -ForegroundColor Yellow
    Write-Host "  $Method $Url" -ForegroundColor Gray

    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $Headers
            ErrorAction = "Stop"
        }

        if ($Body -ne $null) {
            $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
            $params["ContentType"] = "application/json"
        }

        $response = Invoke-RestMethod @params
        
        if ($ExpectCode200 -and $response.code -eq 200) {
            Write-Host "  ✓ 通过" -ForegroundColor Green
            $script:passCount++
        } elseif (-not $ExpectCode200 -and $response.code -ne 200) {
            Write-Host "  ✓ 通过 (预期失败: $($response.message))" -ForegroundColor Green
            $script:passCount++
        } else {
            Write-Host "  ✗ 失败: 期望code=200, 实际code=$($response.code)" -ForegroundColor Red
            Write-Host "    消息: $($response.message)" -ForegroundColor Red
            $script:failCount++
        }

        return $response
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq $ExpectedStatus -or (-not $ExpectCode200)) {
            Write-Host "  ✓ 通过 (HTTP $statusCode)" -ForegroundColor Green
            $script:passCount++
        } else {
            Write-Host "  ✗ 失败: HTTP $statusCode" -ForegroundColor Red
            Write-Host "    $($_.Exception.Message)" -ForegroundColor Red
            $script:failCount++
        }
        return $null
    }
}

Write-Host ""
Write-Host "--- 1. 认证模块 ---" -ForegroundColor Cyan
Write-Host ""

# 1.1 用户注册
$registerBody = @{
    username = "testuser"
    email = "test@example.com"
    password = "123456"
}
$registerResp = Test-Endpoint -Name "用户注册" -Method "POST" -Url "$baseUrl/api/auth/register" -Body $registerBody

# 1.2 管理员登录
$adminLoginBody = @{
    username = "admin"
    password = "admin123"
}
$adminLoginResp = Test-Endpoint -Name "管理员登录" -Method "POST" -Url "$baseUrl/api/auth/login" -Body $adminLoginBody
$adminToken = $adminLoginResp.data.token

# 1.3 普通用户登录
$userLoginBody = @{
    username = "zhangsan"
    password = "123456"
}
$userLoginResp = Test-Endpoint -Name "普通用户登录" -Method "POST" -Url "$baseUrl/api/auth/login" -Body $userLoginBody
$userToken = $userLoginResp.data.token

# 1.4 获取当前用户信息
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Test-Endpoint -Name "获取当前用户信息" -Method "GET" -Url "$baseUrl/api/auth/me" -Headers $adminHeaders

# 1.5 更新个人信息
$updateBody = @{
    avatar = "https://example.com/new-avatar.jpg"
}
Test-Endpoint -Name "更新个人信息" -Method "PUT" -Url "$baseUrl/api/auth/me" -Headers $adminHeaders -Body $updateBody

# 1.6 未登录访问（应返回401）
Test-Endpoint -Name "未登录访问受限资源" -Method "GET" -Url "$baseUrl/api/auth/me" -ExpectedStatus 401 -ExpectCode200 $false

Write-Host ""
Write-Host "--- 2. 工具箱管理 ---" -ForegroundColor Cyan
Write-Host ""

# 2.1 获取工具箱列表
Test-Endpoint -Name "获取工具箱列表" -Method "GET" -Url "$baseUrl/api/toolboxes?page=0&size=10" -Headers $adminHeaders

# 2.2 创建工具箱
$createBoxBody = @{
    name = "测试工具箱"
    location = "测试位置"
    code = "TEST001"
}
$createBoxResp = Test-Endpoint -Name "创建工具箱" -Method "POST" -Url "$baseUrl/api/toolboxes" -Headers $adminHeaders -Body $createBoxBody
$boxId = $createBoxResp.data.id

# 2.3 获取工具箱详情
Test-Endpoint -Name "获取工具箱详情" -Method "GET" -Url "$baseUrl/api/toolboxes/1" -Headers $adminHeaders

# 2.4 更新工具箱
$updateBoxBody = @{
    name = "更新后的工具箱"
    isActive = $true
}
Test-Endpoint -Name "更新工具箱" -Method "PUT" -Url "$baseUrl/api/toolboxes/1" -Headers $adminHeaders -Body $updateBoxBody

Write-Host ""
Write-Host "--- 3. 工具管理 ---" -ForegroundColor Cyan
Write-Host ""

# 3.1 获取工具列表
Test-Endpoint -Name "获取工具列表" -Method "GET" -Url "$baseUrl/api/tools?page=0&size=10" -Headers $adminHeaders

# 3.2 获取可用工具列表（筛选）
Test-Endpoint -Name "按状态筛选工具(可用)" -Method "GET" -Url "$baseUrl/api/tools?status=AVAILABLE" -Headers $adminHeaders

# 3.3 创建工具
$createToolBody = @{
    boxId = 1
    name = "测试工具"
    category = "测试分类"
    description = "这是一个测试工具"
    purchaseDate = "2024-01-01"
}
$createToolResp = Test-Endpoint -Name "创建工具" -Method "POST" -Url "$baseUrl/api/tools" -Headers $adminHeaders -Body $createToolBody
$toolId = $createToolResp.data.id

# 3.4 获取工具详情
Test-Endpoint -Name "获取工具详情" -Method "GET" -Url "$baseUrl/api/tools/1" -Headers $adminHeaders

# 3.5 获取我发布的工具
Test-Endpoint -Name "获取我发布的工具" -Method "GET" -Url "$baseUrl/api/tools/mine" -Headers $adminHeaders

# 3.6 修改工具状态
$updateStatusBody = @{
    status = "MAINTENANCE"
}
Test-Endpoint -Name "修改工具状态" -Method "PATCH" -Url "$baseUrl/api/tools/1/status" -Headers $adminHeaders -Body $updateStatusBody

# 恢复状态
$restoreStatusBody = @{ status = "AVAILABLE" }
Test-Endpoint -Name "恢复工具状态为可用" -Method "PATCH" -Url "$baseUrl/api/tools/1/status" -Headers $adminHeaders -Body $restoreStatusBody

Write-Host ""
Write-Host "--- 4. 借用申请管理 ---" -ForegroundColor Cyan
Write-Host ""

# 4.1 获取借用申请列表
Test-Endpoint -Name "获取借用申请列表" -Method "GET" -Url "$baseUrl/api/borrowrequests?page=0&size=10" -Headers $adminHeaders

# 4.2 创建借用申请（普通用户）
$userHeaders = @{ Authorization = "Bearer $userToken" }
$createBorrowBody = @{
    toolId = 2
    startDate = "2024-06-20"
    expectedReturnDate = "2024-06-25"
    remark = "测试借用申请"
}
$createBorrowResp = Test-Endpoint -Name "创建借用申请" -Method "POST" -Url "$baseUrl/api/borrowrequests" -Headers $userHeaders -Body $createBorrowBody
$borrowId = $createBorrowResp.data.id

# 4.3 获取我的借用申请
Test-Endpoint -Name "获取我的借用申请" -Method "GET" -Url "$baseUrl/api/borrowrequests/mine" -Headers $userHeaders

# 4.4 获取借用申请详情
Test-Endpoint -Name "获取借用申请详情" -Method "GET" -Url "$baseUrl/api/borrowrequests/1" -Headers $adminHeaders

# 4.5 批准借用申请（工具所有者）
$approveBody = @{
    status = "APPROVED"
}
Test-Endpoint -Name "批准借用申请" -Method "PATCH" -Url "$baseUrl/api/borrowrequests/2/status" -Headers $adminHeaders -Body $approveBody

# 4.6 确认归还
$returnBody = @{
    status = "RETURNED"
}
Test-Endpoint -Name "确认工具归还" -Method "PATCH" -Url "$baseUrl/api/borrowrequests/3/status" -Headers $adminHeaders -Body $returnBody

Write-Host ""
Write-Host "--- 5. 使用日志管理 ---" -ForegroundColor Cyan
Write-Host ""

# 5.1 获取使用日志列表
Test-Endpoint -Name "获取使用日志列表" -Method "GET" -Url "$baseUrl/api/toollogs?page=0&size=10" -Headers $adminHeaders

# 5.2 创建使用日志
$createLogBody = @{
    toolId = 1
    action = "REPAIR"
    description = "测试维修记录"
}
Test-Endpoint -Name "创建使用日志" -Method "POST" -Url "$baseUrl/api/toollogs" -Headers $adminHeaders -Body $createLogBody

# 5.3 获取我的使用日志
Test-Endpoint -Name "获取我的使用日志" -Method "GET" -Url "$baseUrl/api/toollogs/mine" -Headers $adminHeaders

# 5.4 获取使用日志详情
Test-Endpoint -Name "获取使用日志详情" -Method "GET" -Url "$baseUrl/api/toollogs/1" -Headers $adminHeaders

# 5.5 按操作类型筛选
Test-Endpoint -Name "按操作类型筛选日志" -Method "GET" -Url "$baseUrl/api/toollogs?action=BORROW" -Headers $adminHeaders

Write-Host ""
Write-Host "--- 6. 统计模块 ---" -ForegroundColor Cyan
Write-Host ""

# 6.1 总览统计
Test-Endpoint -Name "总览统计" -Method "GET" -Url "$baseUrl/api/stats/overview" -Headers $adminHeaders

# 6.2 趋势统计
Test-Endpoint -Name "趋势统计" -Method "GET" -Url "$baseUrl/api/stats/trend" -Headers $adminHeaders

Write-Host ""
Write-Host "--- 7. 搜索与筛选 ---" -ForegroundColor Cyan
Write-Host ""

# 7.1 搜索工具
Test-Endpoint -Name "搜索工具(按名称)" -Method "GET" -Url "$baseUrl/api/tools?keyword=电钻" -Headers $adminHeaders

# 7.2 搜索工具箱
Test-Endpoint -Name "搜索工具箱" -Method "GET" -Url "$baseUrl/api/toolboxes?keyword=1号楼" -Headers $adminHeaders

# 7.3 按分类筛选工具
Test-Endpoint -Name "按分类筛选工具" -Method "GET" -Url "$baseUrl/api/tools?category=电动工具" -Headers $adminHeaders

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  通过: $passCount" -ForegroundColor Green
Write-Host "  失败: $failCount" -ForegroundColor Red
Write-Host ""
if ($failCount -eq 0) {
    Write-Host "  ✓ 所有测试通过！" -ForegroundColor Green
} else {
    Write-Host "  ✗ 有 $failCount 个测试失败" -ForegroundColor Red
}
Write-Host ""
