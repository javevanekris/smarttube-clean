# SmartTube Clean: pull in the latest upstream SmartTube, build, and optionally put it on the TV.
#
# Usage (from the repo root):
#   clean\update.ps1                         merge upstream and build
#   clean\update.ps1 -TvHost 192.168.2.11    the same, then install on the TV
#   clean\update.ps1 -SkipMerge -TvHost ...  only build and install what is checked out
#
# The TV step is refused while a video is playing, because installing closes the app. Use -Force to do it anyway.
# A merge conflict stops the script: resolve it, commit, and run again with -SkipMerge.
param(
    [string]$TvHost,
    [switch]$SkipMerge,
    [switch]$Force,
    [string]$JavaHome = "$env:USERPROFILE\.jdks\ms-jdk-17"
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

function Invoke-Git { & git @args; if ($LASTEXITCODE -ne 0) { throw "git $($args -join ' ') failed" } }

if (-not (Test-Path "$JavaHome\bin\java.exe")) { throw "JDK 17 not found at $JavaHome (pass -JavaHome)" }
if (-not (Test-Path 'keystore.properties')) { throw 'keystore.properties is missing in the repo root, so the APK would not be signed with your key' }

if (-not $SkipMerge) {
    if (git status --porcelain --untracked-files=no) { throw 'There are uncommitted changes. Commit or stash them first.' }
    if (-not (git remote | Select-String -SimpleMatch -Quiet 'upstream')) {
        Invoke-Git remote add upstream https://github.com/yuliskov/SmartTube.git
    }
    Invoke-Git fetch upstream
    $new = (git rev-list --count 'HEAD..upstream/master')
    if ($new -eq '0') {
        Write-Output 'Upstream has nothing new.'
    } else {
        Write-Output "Merging $new new upstream commits:"
        git log --oneline 'HEAD..upstream/master' | Select-Object -First 15
        & git merge --no-edit upstream/master
        if ($LASTEXITCODE -ne 0) {
            throw 'Merge conflict. Resolve the files git lists, commit, then run: clean\update.ps1 -SkipMerge'
        }
    }
    Invoke-Git submodule update --init --recursive
}

$env:JAVA_HOME = $JavaHome
& .\gradlew.bat :smarttubetv:assembleStfdroidRelease -x lintVitalStfdroidRelease --console=plain
$build = $LASTEXITCODE
& .\gradlew.bat --stop | Out-Null
if ($build -ne 0) { throw 'Build failed, see the output above.' }

$apk = Get-ChildItem 'smarttubetv\build\outputs\apk\stfdroid\release\SmartTube_Clean_*_armeabi-v7a.apk' |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) { throw 'Build finished but the APK was not found.' }
Write-Output "Built: $($apk.FullName)"

if ($TvHost) {
    $device = if ($TvHost -match ':') { $TvHost } else { "${TvHost}:5555" }
    & adb connect $device | Out-Null
    $playing = (& adb -s $device shell 'dumpsys media_session') -match 'state=PlaybackState \{state=3'
    $front = (& adb -s $device shell 'dumpsys activity activities') -match 'mResumedActivity.*app\.smarttube\.clean'
    if ($playing -and $front -and -not $Force) {
        throw 'SmartTube Clean is playing a video on the TV. Installing would close it. Run again later or pass -Force.'
    }
    & adb -s $device install -r $apk.FullName
    if ($LASTEXITCODE -ne 0) { throw 'Install failed.' }
    Write-Output 'Installed on the TV.'
}
