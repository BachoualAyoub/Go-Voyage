param(
    [string]$PgBin = '',
    [ValidateRange(1024, 65535)][int]$Port = 55432,
    [switch]$Arreter
)

$ErrorActionPreference = 'Stop'
$racine = Split-Path -Parent $PSScriptRoot
$local = Join-Path $racine '.local'
$donnees = Join-Path $local 'postgres'
$journal = Join-Path $local 'postgres.log'

if (-not $PgBin) {
    $installation = Join-Path $env:ProgramFiles 'PostgreSQL'
    $version = Get-ChildItem -LiteralPath $installation -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^\d+$' -and (Test-Path (Join-Path $_.FullName 'bin\pg_ctl.exe')) } |
        Sort-Object { [int]$_.Name } -Descending |
        Select-Object -First 1
    if (-not $version) { throw 'PostgreSQL introuvable. Indiquez le dossier bin avec -PgBin.' }
    $PgBin = Join-Path $version.FullName 'bin'
}
$pgCtl = Join-Path $PgBin 'pg_ctl.exe'
if (-not (Test-Path -LiteralPath $pgCtl)) { throw "pg_ctl.exe introuvable dans $PgBin." }

if ($Arreter) {
    & $pgCtl -D $donnees -m fast -w stop
    if ($LASTEXITCODE -ne 0) { throw 'Arret de PostgreSQL en echec.' }
    return
}

New-Item -ItemType Directory -Force -Path $local | Out-Null
if (-not (Test-Path (Join-Path $donnees 'PG_VERSION'))) {
    $fichierMotDePasse = Join-Path $local 'postgres-init.pw'
    try {
        Set-Content -LiteralPath $fichierMotDePasse -Value 'govoyage' -Encoding ascii
        & (Join-Path $PgBin 'initdb.exe') -D $donnees -U govoyage -A scram-sha-256 --encoding=UTF8 --locale=C "--pwfile=$fichierMotDePasse"
        if ($LASTEXITCODE -ne 0) { throw 'Initialisation de PostgreSQL en echec.' }
    } finally {
        Remove-Item -LiteralPath $fichierMotDePasse -Force -ErrorAction SilentlyContinue
    }
}

& $pgCtl -D $donnees status | Out-Null
if ($LASTEXITCODE -eq 0) {
    $portActif = (Get-Content -LiteralPath (Join-Path $donnees 'postmaster.pid') -TotalCount 4)[3]
    if ([int]$portActif -ne $Port) {
        throw "Cette instance utilise deja le port $portActif. Relancez avec -Port $portActif."
    }
} else {
    & $pgCtl -D $donnees -l $journal -o "-h 127.0.0.1 -p $Port" -w start
    if ($LASTEXITCODE -ne 0) { throw "Demarrage de PostgreSQL en echec. Voir $journal." }
}

$ancienMotDePasse = $env:PGPASSWORD
try {
    $env:PGPASSWORD = 'govoyage'
    $existe = & (Join-Path $PgBin 'psql.exe') -X -w -h 127.0.0.1 -p $Port -U govoyage -d postgres -tA -c "SELECT 1 FROM pg_database WHERE datname = 'govoyage'"
    if ($LASTEXITCODE -ne 0) { throw 'Connexion PostgreSQL en echec.' }
    if ($existe -ne '1') {
        & (Join-Path $PgBin 'createdb.exe') -w -h 127.0.0.1 -p $Port -U govoyage govoyage
        if ($LASTEXITCODE -ne 0) { throw 'Creation de la base govoyage en echec.' }
    }
} finally {
    $env:PGPASSWORD = $ancienMotDePasse
}
Write-Host "Base govoyage disponible sur 127.0.0.1:$Port."
Write-Host "Dans le terminal du backend, definissez DB_PORT a $Port."
