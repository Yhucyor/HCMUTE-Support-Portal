# ==============================================================================
# Script tu dong Add, Commit va Push len GitHub danh cho Developer & AI Agent
# ==============================================================================
# Cach su dung:
#   .\scripts\git-auto-sync.ps1 -Message "Tach Thymeleaf fragments navbar footer" -Type "feat" -Scope "ui"
#   .\scripts\git-auto-sync.ps1 -Message "Cap nhat tai lieu tien do" -Type "docs"
# ==============================================================================

param (
    [Parameter(Mandatory=$true, HelpMessage="Noi dung commit")]
    [string]$Message,

    [ValidateSet("feat", "fix", "docs", "style", "refactor", "test", "chore", "ci")]
    [string]$Type = "feat",

    [string]$Scope = ""
)

Write-Host ">>> [1/4] Kiem tra thay doi trong git repository..." -ForegroundColor Cyan
$status = git status --porcelain
if (-not $status) {
    Write-Host "Khong co thay doi nao can commit!" -ForegroundColor Yellow
    exit 0
}

# 1. Dinh dang commit message theo chuan Conventional Commits
$commitMsg = ""
if ($Scope -and $Scope.Trim() -ne "") {
    $commitMsg = "${Type}(${Scope}): ${Message}"
} else {
    $commitMsg = "${Type}: ${Message}"
}

# 2. Lay ten nhanh hien tai
$currentBranch = (git branch --show-current).Trim()
if (-not $currentBranch) {
    Write-Host "Loi: Khong xac dinh duoc nhanh Git hien tai!" -ForegroundColor Red
    exit 1
}

Write-Host ">>> [2/4] Stage toan bo file thay doi (git add .)..." -ForegroundColor Cyan
git add .

Write-Host ">>> [3/4] Commit voi message: '$commitMsg'..." -ForegroundColor Cyan
git commit -m "$commitMsg"

if ($LASTEXITCODE -ne 0) {
    Write-Host "Loi trong qua trinh commit!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ">>> [4/4] Push len GitHub (origin/$currentBranch)..." -ForegroundColor Cyan
git push origin $currentBranch

if ($LASTEXITCODE -eq 0) {
    $commitHash = (git rev-parse --short HEAD).Trim()
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host " DA PUSH THANH CONG LEN GITHUB!" -ForegroundColor Green
    Write-Host " Nhanh: $currentBranch" -ForegroundColor Green
    Write-Host " Hash : $commitHash" -ForegroundColor Green
    Write-Host " Msg  : $commitMsg" -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green
} else {
    Write-Host "Loi khi push len GitHub. Vui long kiem tra SSH/token hoac xung dot mang." -ForegroundColor Red
    exit $LASTEXITCODE
}
