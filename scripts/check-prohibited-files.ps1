[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repoRoot = (& git rev-parse --show-toplevel 2>$null)
if ($LASTEXITCODE -ne 0 -or -not $repoRoot) {
    throw 'Run this script inside the Harvester Git repository.'
}

$repoRoot = $repoRoot.Trim()
$candidateFiles = @(& git -C $repoRoot ls-files --cached --others --exclude-standard)
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to enumerate tracked files.'
}

$violations = [System.Collections.Generic.List[string]]::new()
$allowedJar = 'gradle/wrapper/gradle-wrapper.jar'

foreach ($relativePath in $candidateFiles) {
    $normalized = $relativePath.Replace('\', '/')
    $leaf = [System.IO.Path]::GetFileName($normalized)

    if ($normalized -match '(?i)(^|/)(mcp\d*|decompiled|instances|saves|logs|crash-reports)(/|$)') {
        $violations.Add("prohibited path: $normalized")
    }

    if ($normalized -match '(?i)(^|/)reference/(modloader|timber)(/|$)') {
        $violations.Add("third-party reference source: $normalized")
    }

    if ($leaf -match '(?i)^(PlayerControllerSP\.java|MLProp\.java|minecraft\.jar|client\.jar|server\.jar|accounts\.json)$') {
        $violations.Add("prohibited file: $normalized")
    }

    $extension = [System.IO.Path]::GetExtension($leaf)
    if ($extension -match '(?i)^\.(class|zip|7z|rar)$') {
        $violations.Add("prohibited binary/archive: $normalized")
    }
    if ($extension -ieq '.jar' -and $normalized -ne $allowedJar) {
        $violations.Add("unapproved JAR: $normalized")
    }

    $fullPath = Join-Path $repoRoot $relativePath
    if (Test-Path -LiteralPath $fullPath -PathType Leaf) {
        $item = Get-Item -LiteralPath $fullPath
        if ($item.Length -le 2MB -and $extension -match '(?i)^\.(java|kt|groovy)$') {
            $content = Get-Content -Raw -LiteralPath $fullPath -ErrorAction SilentlyContinue
            if ($content -match '(?i)Decompiled by Jad|package\s+net\.minecraft\.src\s*;') {
                $violations.Add("decompiled or MCP-era source marker: $normalized")
            }
        }
    }
}

if ($violations.Count -gt 0) {
    $violations | Sort-Object -Unique | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Prohibited-content check passed for $($candidateFiles.Count) candidate files."
