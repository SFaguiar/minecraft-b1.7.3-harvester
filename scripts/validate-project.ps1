[CmdletBinding()]
param([string]$ArtifactDirectory = (Join-Path $PSScriptRoot '..\build\libs'))

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$metadata = Get-Content (Join-Path $root 'src\main\resources\fabric.mod.json') -Raw
$expanded = $metadata.Replace('${version}', 'validation-version') | ConvertFrom-Json
$mixins = Get-Content (Join-Path $root 'src\main\resources\harvester.mixins.json') -Raw | ConvertFrom-Json
if ($expanded.id -ne 'harvester') { throw 'Unexpected mod id' }
if ($expanded.environment -ne '*') { throw 'Mod must support both sides' }
if (-not $expanded.entrypoints.client -or -not $expanded.entrypoints.server) { throw 'Side entrypoints missing' }
if ($mixins.required -ne $true -or $mixins.package -ne 'io.github.sfaguiar.harvester.mixin') { throw 'Mixin configuration invalid' }

& (Join-Path $root 'scripts\check-prohibited-files.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$artifacts = Get-ChildItem -LiteralPath $ArtifactDirectory -Filter '*.jar' -File | Where-Object Name -NotLike '*-sources.jar'
if ($artifacts.Count -ne 1) { throw "Expected one release artifact, found $($artifacts.Count)" }
$artifacts | Get-FileHash -Algorithm SHA256 | ForEach-Object { "$($_.Hash)  $([IO.Path]::GetFileName($_.Path))" }
