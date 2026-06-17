$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$migrationDir = Join-Path $root "gms-server\src\main\resources\db\migration"

if (-not (Test-Path $migrationDir)) {
    Write-Error "Flyway migration directory not found: $migrationDir"
    exit 1
}

$versions = @{}
Get-ChildItem -LiteralPath $migrationDir -File -Filter "V*__*.sql" | ForEach-Object {
    if ($_.Name -match '^V(.+)__.+\.sql$') {
        $version = $Matches[1]
        if (-not $versions.ContainsKey($version)) {
            $versions[$version] = New-Object System.Collections.Generic.List[string]
        }
        $versions[$version].Add($_.Name)
    }
}

$duplicates = $versions.GetEnumerator() | Where-Object { $_.Value.Count -gt 1 } | Sort-Object Name

if ($duplicates) {
    Write-Host "Flyway migration version conflict detected:" -ForegroundColor Red
    foreach ($entry in $duplicates) {
        Write-Host "  Version $($entry.Key):" -ForegroundColor Red
        foreach ($file in ($entry.Value | Sort-Object)) {
            Write-Host "    - $file" -ForegroundColor Red
        }
    }
    Write-Host ""
    Write-Host "Please give each migration a unique V*.sql version before building." -ForegroundColor Red
    exit 1
}

Write-Host "Flyway migration versions OK."
