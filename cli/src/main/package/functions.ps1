# This script should be dot-sourced from the PowerShell profile.
# It provides the PowerShell equivalent of the IDEasy bash "functions" script.

function _ide_get_options {
    if ([string]::IsNullOrWhiteSpace($env:IDE_OPTIONS)) {
        return
    }

    # This intentionally behaves similarly to the unquoted ${IDE_OPTIONS}
    # expansion used by the bash implementation.
    $env:IDE_OPTIONS -split '\s+' | Where-Object { $_ -ne '' }
}

function _ide_set_environment {
    $ideOptions = @(_ide_get_options)
    $ideEnv = @(& ideasy @ideOptions env)
    $returnCode = $LASTEXITCODE

    if ($returnCode -ne 0) {
        return $returnCode
    }

    foreach ($line in $ideEnv) {
        $line = [string]$line

        # Split only at the first "=" since values themselves may contain "=".
        $separator = $line.IndexOf('=')

        if ($separator -gt 0) {
            $name = $line.Substring(0, $separator)
            $value = $line.Substring($separator + 1)

            Set-Item -LiteralPath "Env:$name" -Value $value
        }
    }

    return 0
}

function _ide_create_project {
    $foundCreate = $false

    foreach ($argument in $args) {
        if ($argument -eq 'create') {
            $foundCreate = $true
            continue
        }

        if ($argument.StartsWith('-')) {
            continue
        }

        if ($foundCreate) {
            return $argument
        }
    }
}

function ide {
    $ideArguments = @($args)

    if ($ideArguments.Count -ne 0) {
        $ideOptions = @(_ide_get_options)

        & ideasy @ideOptions @ideArguments
        $returnCode = $LASTEXITCODE

        if ($returnCode -ne 0) {
            Write-Error "IDEasy failed with exit code $returnCode"
            return
        }

        $createProject = _ide_create_project @ideArguments

        if (-not [string]::IsNullOrEmpty($createProject) -and
            -not [string]::IsNullOrEmpty($env:IDE_ROOT)) {

            $projectPath = Join-Path -Path $env:IDE_ROOT -ChildPath $createProject

            if (Test-Path -LiteralPath $projectPath -PathType Container) {
                try {
                    Set-Location -LiteralPath $projectPath -ErrorAction Stop
                }
                catch {
                    Write-Error $_
                    return
                }
            }
        }
    }

    Remove-Item -LiteralPath 'Env:IDE_HOME' -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath 'Env:WORKSPACE' -ErrorAction SilentlyContinue

    $returnCode = _ide_set_environment

    if ($returnCode -eq 0) {
        if ($ideArguments.Count -eq 0 -and
            -not [string]::IsNullOrEmpty($env:IDE_HOME)) {

            Write-Host "IDE environment variables have been set for $env:IDE_HOME in workspace $env:WORKSPACE"
        }
    }
}

function _ide_get_current_project {
    if ([string]::IsNullOrEmpty($env:IDE_ROOT)) {
        return
    }

    $root = $env:IDE_ROOT.TrimEnd('\', '/')
    $currentDirectory = (Get-Location).Path

    if ($currentDirectory.StartsWith(
            $root + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase)) {

        $relativePath = $currentDirectory.Substring($root.Length)
        $relativePath = $relativePath.TrimStart('\', '/')

        if (-not [string]::IsNullOrEmpty($relativePath)) {
            return ($relativePath -split '[\\/]')[0]
        }
    }

    if (-not [string]::IsNullOrEmpty($env:IDE_HOME) -and
        $env:IDE_HOME.StartsWith(
            $root + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase)) {

        $relativePath = $env:IDE_HOME.Substring($root.Length)
        $relativePath = $relativePath.TrimStart('\', '/')

        if (-not [string]::IsNullOrEmpty($relativePath)) {
            return ($relativePath -split '[\\/]')[0]
        }
    }
}

function icd {
    $icdArguments = @($args)

    if ($icdArguments.Count -eq 1 -and
        -not $icdArguments[0].StartsWith('-')) {

        try {
            Set-Location -LiteralPath $icdArguments[0] -ErrorAction Stop
        }
        catch {
            Write-Error $_
            return
        }

        ide
        return
    }

    # icd
    if ($icdArguments.Count -eq 0) {
        if (-not [string]::IsNullOrEmpty($env:IDE_HOME)) {
            try {
                Set-Location -LiteralPath $env:IDE_HOME -ErrorAction Stop
            }
            catch {
                Write-Error $_
                return
            }

            ide
            return
        }

        if (-not [string]::IsNullOrEmpty($env:IDE_ROOT)) {
            try {
                Set-Location -LiteralPath $env:IDE_ROOT -ErrorAction Stop
            }
            catch {
                Write-Error $_
            }
        }

        return
    }

    $icdProject = $null
    $icdWorkspace = $null

    $index = 0

    while ($index -lt $icdArguments.Count) {
        $argument = $icdArguments[$index]

        switch ($argument) {
            { $_ -eq '-p' -or $_ -eq '--project' } {
                $index++

                if ($index -ge $icdArguments.Count) {
                    Write-Error 'Missing project name.'
                    return
                }

                $icdProject = $icdArguments[$index]
                $index++
                continue
            }

            { $_ -eq '-w' -or $_ -eq '--workspace' } {
                $index++

                if ($index -lt $icdArguments.Count -and
                    -not $icdArguments[$index].StartsWith('-')) {

                    $icdWorkspace = $icdArguments[$index]
                    $index++
                }
                else {
                    $icdWorkspace = 'main'
                }

                continue
            }

            { $_ -eq '-h' -or $_ -eq '--help' } {
                Write-Host 'USAGE: icd [-p «project»] [-w [«workspace»]]'
                Write-Host ''
                Write-Host 'Change directory and initialize IDEasy environment.'
                Write-Host 'The icd command can be used as an alternative to the regular cd command.'
                Write-Host 'As additional effect, it will automatically update your environment variables.'
                Write-Host 'Further, it allows shortcuts to quickly navigate to common directories of IDEasy.'
                Write-Host 'Without any arguments icd will navigate to your top-level project directory (IDE_HOME).'
                Write-Host ''
                Write-Host 'OPTIONS:'
                Write-Host '  -p | --project   «project»    The IDEasy project to cd to.'
                Write-Host '  -w | --workspace «workspace»  The workspace inside your IDEasy project to cd to. Defaults to main.'
                Write-Host '  -h | --help                   Print this help text.'
                return
            }

            default {
                Write-Error "Unknown option $argument"
                return
            }
        }
    }

    if ([string]::IsNullOrEmpty($env:IDE_ROOT)) {
        Write-Error 'IDE_ROOT is not defined.'
        return
    }

    $icdPath = $env:IDE_ROOT

    # Determine the project to use.
    if (-not [string]::IsNullOrEmpty($icdProject)) {
        $icdPath = Join-Path -Path $env:IDE_ROOT -ChildPath $icdProject
    }
    elseif (-not [string]::IsNullOrEmpty($icdWorkspace)) {
        $currentProject = _ide_get_current_project

        if (-not [string]::IsNullOrEmpty($currentProject)) {
            $icdPath = Join-Path -Path $env:IDE_ROOT -ChildPath $currentProject
        }
        elseif (-not [string]::IsNullOrEmpty($env:IDE_HOME)) {
            $icdPath = $env:IDE_HOME
        }
    }
    elseif (-not [string]::IsNullOrEmpty($env:IDE_HOME)) {
        $icdPath = $env:IDE_HOME
    }

    # Append workspace if requested.
    if (-not [string]::IsNullOrEmpty($icdWorkspace)) {
        $icdPath = Join-Path -Path $icdPath -ChildPath 'workspaces'
        $icdPath = Join-Path -Path $icdPath -ChildPath $icdWorkspace
    }

    try {
        Set-Location -LiteralPath $icdPath -ErrorAction Stop
    }
    catch {
        Write-Error $_
        return
    }

    ide
}

function claude {
    if (-not [string]::IsNullOrEmpty($env:IDE_HOME)) {
        ide claude @args
        return
    }

    # "claude" is now a PowerShell function, so explicitly search for
    # the external command to avoid recursively calling this function.
    $claudeCommand = Get-Command claude `
        -CommandType Application, ExternalScript `
        -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if ($null -eq $claudeCommand) {
        Write-Error "The command 'claude' could not be found."
        return
    }

    & $claudeCommand.Source @args
}

function _ide_completion {
    $registerCommand = Get-Command Register-ArgumentCompleter -ErrorAction SilentlyContinue

    if ($null -eq $registerCommand) {
        return
    }

    # Native-style argument completion is available in modern PowerShell.
    # Keep completion optional so it cannot prevent environment initialization
    # on older Windows PowerShell versions.
    if (-not $registerCommand.Parameters.ContainsKey('Native')) {
        return
    }

    Register-ArgumentCompleter -Native -CommandName ide -ScriptBlock {
        param(
            $wordToComplete,
            $commandAst,
            $cursorPosition
        )

        $completionArguments = @()

        $elements = @($commandAst.CommandElements)

        if ($elements.Count -gt 1) {
            for ($i = 1; $i -lt $elements.Count; $i++) {
                $element = $elements[$i]

                if ($element -is [System.Management.Automation.Language.StringConstantExpressionAst]) {
                    $completionArguments += $element.Value
                }
                else {
                    $completionArguments += $element.Extent.Text
                }
            }
        }

        # A trailing space means IDEasy expects another empty argument,
        # equivalent to the bash completion implementation.
        if ([string]::IsNullOrEmpty($wordToComplete)) {
            $completionArguments += ''
        }

        $replies = @(& ideasy -q complete @completionArguments)

        foreach ($reply in $replies) {
            if (-not [string]::IsNullOrEmpty($reply)) {
                New-Object System.Management.Automation.CompletionResult -ArgumentList @(
                    $reply,
                    $reply,
                    'ParameterValue',
                    $reply
                )
            }
        }
    }
}


# Ensure ideasy itself is available.
if ($null -eq (Get-Command ideasy -ErrorAction SilentlyContinue)) {
    if (-not [string]::IsNullOrEmpty($env:IDE_ROOT)) {
        $ideasyBin = Join-Path -Path $env:IDE_ROOT -ChildPath '_ide\installation\bin'

        if (Test-Path -LiteralPath $ideasyBin -PathType Container) {
            $pathSeparator = [System.IO.Path]::PathSeparator
            $pathEntries = @($env:PATH -split [regex]::Escape([string]$pathSeparator))

            if ($pathEntries -notcontains $ideasyBin) {
                if ([string]::IsNullOrEmpty($env:PATH)) {
                    $env:PATH = $ideasyBin
                }
                else {
                    $env:PATH = "$env:PATH$pathSeparator$ideasyBin"
                }
            }
        }
    }
}

_ide_completion

# Initialize the IDEasy environment immediately when this file is dot-sourced.
ide
