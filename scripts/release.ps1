# Usage: release.ps1 prepare (before the commit) | publish (after the push).
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('prepare', 'publish')]
    [string]$Step
)

$ErrorActionPreference = 'Stop'
$Keep = 10
$Root = Split-Path -Parent $PSScriptRoot
$Props = Join-Path $Root 'gradle.properties'
$Changelog = Join-Path $Root 'CHANGELOG.md'
$Releases = Join-Path $Root 'releases'
$Utf8 = New-Object System.Text.UTF8Encoding($false)

function Get-Prop([string]$name) {
    $line = [IO.File]::ReadAllLines($Props, $Utf8) | Where-Object { $_ -match "^$name=" } | Select-Object -First 1
    if (-not $line) { throw "$name missing in gradle.properties" }
    return $line.Substring($name.Length + 1).Trim()
}

# '0.1.9-alpha' -> number 19 and suffix '-alpha'
function Split-Version([string]$version) {
    if ($version -notmatch '^(\d)\.(\d)\.(\d)(-[0-9A-Za-z.]+)?$') {
        throw "Version '$version' is not X.Y.Z with digits 0-9 (optional -suffix)"
    }
    return @{ Number = [int]$Matches[1] * 100 + [int]$Matches[2] * 10 + [int]$Matches[3]; Suffix = [string]$Matches[4] }
}

function Join-Version([int]$number, [string]$suffix) {
    if ($number -gt 999) { throw 'Past 9.9.9: pick a new scheme first' }
    return '{0}.{1}.{2}{3}' -f [math]::Floor($number / 100), ([math]::Floor($number / 10) % 10), ($number % 10), $suffix
}

function Test-RemoteTag([string]$tag) {
    $out = git -C $Root ls-remote --tags origin "refs/tags/$tag"
    return [bool]$out
}

function Get-Notes([string]$version) {
    $lines = [IO.File]::ReadAllLines($Changelog, $Utf8)
    $start = -1
    for ($i = 0; $i -lt $lines.Length; $i++) {
        if ($lines[$i].StartsWith("## [$version]")) { $start = $i; break }
    }
    if ($start -lt 0) { throw "No '## [$version] - <date>' section in CHANGELOG.md" }
    $end = $lines.Length
    for ($i = $start + 1; $i -lt $lines.Length; $i++) {
        if ($lines[$i].StartsWith('## ')) { $end = $i; break }
    }
    $body = ($lines[($start + 1)..($end - 1)] -join "`n").Trim()
    return $body
}

$version = Get-Prop 'mod_version'
$fileName = Get-Prop 'mod_file_name'

if ($Step -eq 'prepare') {
    if (Test-RemoteTag "v$version") {
        $v = Split-Version $version
        $next = Join-Version ($v.Number + 1) $v.Suffix
        $text = [IO.File]::ReadAllText($Props, $Utf8)
        $text = $text -replace "(?m)^mod_version=.*$", "mod_version=$next"
        [IO.File]::WriteAllText($Props, $text, $Utf8)
        Write-Host "mod_version $version -> $next"
        $version = $next
    } else {
        Write-Host "mod_version $version has no release yet: kept"
    }
    Write-Host "CHANGELOG heading needed: ## [$version] - $(Get-Date -Format yyyy-MM-dd)"
    exit 0
}

if (Test-RemoteTag "v$version") { throw "v$version is already released: run 'prepare' before the commit" }
if (git -C $Root status --porcelain --untracked-files=no) { throw 'Uncommitted changes: commit and push first' }
git -C $Root fetch -q origin
$branch = git -C $Root rev-parse --abbrev-ref HEAD
$ahead = git -C $Root rev-list --count "origin/$branch..HEAD"
if ([int]$ahead -gt 0) { throw "$ahead commit(s) not pushed yet: push first" }
$notes = Get-Notes $version

Push-Location $Root
try {
    & .\gradlew.bat build --console=plain -q
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' }
} finally {
    Pop-Location
}
$jar = Join-Path $Root "build\libs\$fileName-$version.jar"
if (-not (Test-Path $jar)) { throw "Jar not found: $jar" }

New-Item -ItemType Directory -Force $Releases | Out-Null
Copy-Item $jar $Releases -Force
Write-Host "Saved releases\$fileName-$version.jar"

$notesFile = Join-Path ([IO.Path]::GetTempPath()) "release-notes-$version.md"
$body = $notes + "`n`n---`nMinecraft 1.21.1, NeoForge 21.1+. Put the jar in your ``mods`` folder."
[IO.File]::WriteAllText($notesFile, $body, $Utf8)
$sha = git -C $Root rev-parse HEAD
# No --prerelease, also for -alpha: only a Latest release gets the green badge.
gh release create "v$version" $jar --target $sha --title "v$version" --notes-file $notesFile --latest
if ($LASTEXITCODE -ne 0) { throw 'gh release create failed' }
Remove-Item $notesFile

$local = Get-ChildItem $Releases -Filter "$fileName-*.jar" | ForEach-Object {
    $v = $_.BaseName.Substring($fileName.Length + 1)
    try { [pscustomobject]@{ File = $_; Number = (Split-Version $v).Number } } catch { $null }
} | Where-Object { $_ } | Sort-Object Number -Descending
$local | Select-Object -Skip $Keep | ForEach-Object {
    Remove-Item $_.File.FullName
    Write-Host "Deleted old releases\$($_.File.Name)"
}

$online = gh release list --limit 200 --json tagName | ConvertFrom-Json | ForEach-Object {
    try { [pscustomobject]@{ Tag = $_.tagName; Number = (Split-Version $_.tagName.TrimStart('v')).Number } } catch { $null }
} | Where-Object { $_ } | Sort-Object Number -Descending
$online | Select-Object -Skip $Keep | ForEach-Object {
    gh release delete $_.Tag --cleanup-tag --yes
    Write-Host "Deleted old GitHub release $($_.Tag)"
}
Write-Host "Released v$version"
