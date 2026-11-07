$ErrorActionPreference = 'Stop'
$archive = Join-Path $PSScriptRoot 'maven.zip'
Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile $archive
Expand-Archive -LiteralPath $archive -DestinationPath (Join-Path $PSScriptRoot '.tools') -Force
