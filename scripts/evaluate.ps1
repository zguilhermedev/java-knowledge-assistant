param([string]$BaseUrl = 'http://localhost:8082')
$ErrorActionPreference = 'Stop'

function Invoke-KnowledgeJson {
    param([string]$Uri, [string]$Body)
    $taskResponse = Invoke-WebRequest -UseBasicParsing -Uri $Uri -Method Post `
        -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($Body))
    $taskResponse.RawContentStream.Position = 0
    $taskReader = [System.IO.StreamReader]::new($taskResponse.RawContentStream, [System.Text.Encoding]::UTF8)
    try {
        $taskReader.ReadToEnd() | ConvertFrom-Json
    } finally {
        $taskReader.Dispose()
    }
}

$taskRoot = Split-Path -Parent $PSScriptRoot
$taskQueries = Get-Content -LiteralPath (Join-Path $taskRoot 'docs/evaluation/queries.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$taskResults = foreach ($taskQuery in $taskQueries) {
    $taskWatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $taskSearchBody = @{query=$taskQuery.question; maxResults=3; minScore=0.6} | ConvertTo-Json
        $taskSearch = Invoke-KnowledgeJson -Uri "$BaseUrl/documents/search" -Body $taskSearchBody
        $taskBody = @{question=$taskQuery.question} | ConvertTo-Json
        $taskAnswer = Invoke-KnowledgeJson -Uri "$BaseUrl/questions/rag" -Body $taskBody
        [pscustomobject]@{
            question=$taskQuery.question
            expectedDocumentId=$taskQuery.expectedDocumentId
            retrievedDocumentIds=(@($taskSearch | ForEach-Object { $_.documentId }) -join ',')
            retrievalHit=if ($taskQuery.expectedDocumentId) { @($taskSearch.documentId) -contains $taskQuery.expectedDocumentId } else { $null }
            expectedKind=$taskQuery.expectedKind
            actualKind=$taskAnswer.kind
            answer=$taskAnswer.answer
            citedIds=(@($taskAnswer.sources | ForEach-Object { $_.chunkId }) -join ',')
            elapsedMs=$taskWatch.ElapsedMilliseconds
            error=$null
        }
    } catch {
        [pscustomobject]@{question=$taskQuery.question; error=$_.Exception.Message; elapsedMs=$taskWatch.ElapsedMilliseconds}
    }
}
$taskOutput = Join-Path $taskRoot 'target/evaluation'
New-Item -ItemType Directory -Path $taskOutput -Force | Out-Null
$taskResults | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $taskOutput 'results.json') -Encoding UTF8
$taskResults | Format-Table question, retrievalHit, actualKind, elapsedMs, error -AutoSize
Write-Output "Relatório: $taskOutput/results.json"
