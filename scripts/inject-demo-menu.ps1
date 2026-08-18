[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [Alias("HostApk")]
    [string]$HostPath,

    [string]$MenuSource,
    [string]$ProviderClass,
    [string]$OutputApk,
    [string]$ToolkitRoot,
    [string]$JavaHome,
    [string]$WorkRoot,

    [switch]$SkipBuild,
    [switch]$ValidateOnly,
    [switch]$IHavePermission
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$AndroidNamespace = "http://schemas.android.com/apk/res/android"
$RepoRoot = Split-Path -Parent $PSScriptRoot

function Assert-FileExists {
    param([string]$Path, [string]$Label)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "$Label not found: $Path"
    }
}

function Trim-DroppedPath {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return ""
    }
    return $Path.Trim().Trim('"')
}

function Resolve-HostApkFromInput {
    param([string]$InputPath)

    $InputPath = Trim-DroppedPath -Path $InputPath
    if (-not (Test-Path -LiteralPath $InputPath)) {
        throw "Dropped APK/folder not found: $InputPath"
    }

    $item = Get-Item -LiteralPath $InputPath
    if (-not $item.PSIsContainer) {
        if ($item.Extension -ne ".apk") {
            throw "Host file must have the .apk extension: $($item.FullName)"
        }
        return $item.FullName
    }

    $candidates = @(Get-ChildItem -LiteralPath $item.FullName -Filter "*.apk" `
        -File -Recurse | Where-Object {
            $_.Name -notlike "*-onyx-demo-test-signed.apk" -and
            $_.Name -notlike "*-onyx-menu-test-signed.apk"
        } | Sort-Object LastWriteTime -Descending)

    if ($candidates.Count -eq 0) {
        throw "No APK found under folder: $($item.FullName)"
    }
    if ($candidates.Count -eq 1) {
        Write-Host "Selected APK: $($candidates[0].FullName)" -ForegroundColor Green
        return $candidates[0].FullName
    }

    Write-Host "Multiple APKs found. Choose one:" -ForegroundColor Yellow
    for ($index = 0; $index -lt $candidates.Count; ++$index) {
        Write-Host ("  [{0}] {1}  ({2:N1} MB)" -f `
            ($index + 1),
            $candidates[$index].FullName,
            ($candidates[$index].Length / 1MB))
    }

    while ($true) {
        $answer = Read-Host "APK number"
        [int]$selected = 0
        if ([int]::TryParse($answer, [ref]$selected) -and
                $selected -ge 1 -and $selected -le $candidates.Count) {
            return $candidates[$selected - 1].FullName
        }
        Write-Host "Invalid selection." -ForegroundColor Red
    }
}

function Resolve-MenuDonorApk {
    param([string]$InputPath)

    $InputPath = Trim-DroppedPath -Path $InputPath
    if (-not (Test-Path -LiteralPath $InputPath)) {
        throw "Menu donor APK/folder not found: $InputPath"
    }

    $item = Get-Item -LiteralPath $InputPath
    if (-not $item.PSIsContainer) {
        if ($item.Extension -ne ".apk") {
            throw "Menu donor file must have the .apk extension: $($item.FullName)"
        }
        return $item.FullName
    }

    $preferredFolders = @(
        (Join-Path $item.FullName "build\outputs\apk\debug"),
        (Join-Path $item.FullName "app\build\outputs\apk\debug")
    )
    foreach ($preferredFolder in $preferredFolders) {
        if (-not (Test-Path -LiteralPath $preferredFolder -PathType Container)) {
            continue
        }
        $preferred = @(Get-ChildItem -LiteralPath $preferredFolder -Filter "*.apk" `
            -File | Sort-Object LastWriteTime -Descending)
        if ($preferred.Count -gt 0) {
            Write-Host "Selected menu donor: $($preferred[0].FullName)" -ForegroundColor Green
            return $preferred[0].FullName
        }
    }

    $candidates = @(Get-ChildItem -LiteralPath $item.FullName -Filter "*.apk" `
        -File -Recurse | Sort-Object LastWriteTime -Descending)
    if ($candidates.Count -eq 0) {
        throw "No donor APK found under menu folder: $($item.FullName)"
    }
    if ($candidates.Count -eq 1) {
        Write-Host "Selected menu donor: $($candidates[0].FullName)" -ForegroundColor Green
        return $candidates[0].FullName
    }

    Write-Host "Multiple donor APKs found. Choose one:" -ForegroundColor Yellow
    for ($index = 0; $index -lt $candidates.Count; ++$index) {
        Write-Host ("  [{0}] {1}" -f ($index + 1), $candidates[$index].FullName)
    }
    while ($true) {
        $answer = Read-Host "Donor APK number"
        [int]$selected = 0
        if ([int]::TryParse($answer, [ref]$selected) -and
                $selected -ge 1 -and $selected -le $candidates.Count) {
            return $candidates[$selected - 1].FullName
        }
        Write-Host "Invalid selection." -ForegroundColor Red
    }
}

function Resolve-JavaHomePath {
    param([string]$RequestedPath)

    $candidates = @()
    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        $candidates += (Trim-DroppedPath -Path $RequestedPath)
    }
    $candidates += @(
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Android\Android Studio\jbr"
    )
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidates += $env:JAVA_HOME
    }

    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($null -ne $javaCommand) {
        $candidates += (Split-Path -Parent (Split-Path -Parent $javaCommand.Source))
    }

    foreach ($candidate in $candidates) {
        if ([string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }
        $candidate = Trim-DroppedPath -Path $candidate
        if (Test-Path -LiteralPath (Join-Path $candidate "bin\java.exe") -PathType Leaf) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
        if ((Split-Path -Leaf $candidate) -eq "bin" -and
                (Test-Path -LiteralPath (Join-Path $candidate "java.exe") -PathType Leaf)) {
            return (Resolve-Path -LiteralPath (Split-Path -Parent $candidate)).Path
        }
    }

    $dropped = Trim-DroppedPath -Path (Read-Host "Drag JDK home folder here")
    if (Test-Path -LiteralPath (Join-Path $dropped "bin\java.exe") -PathType Leaf) {
        return (Resolve-Path -LiteralPath $dropped).Path
    }
    throw "Could not find a JDK containing bin\java.exe"
}

function Test-ToolkitResourceRoot {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        return $false
    }
    $required = @(
        "apktool.jar",
        "zipalign.exe",
        "apksigner.jar",
        "ApkToolkit_Key.pk8",
        "ApkToolkit_Certificate.pem"
    )
    foreach ($file in $required) {
        if (-not (Test-Path -LiteralPath (Join-Path $Path $file) -PathType Leaf)) {
            return $false
        }
    }
    return $true
}

function Resolve-ToolkitResourceRoot {
    param([string]$RequestedPath)

    $candidates = @()
    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        $candidates += (Trim-DroppedPath -Path $RequestedPath)
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ONYX_APK_TOOLKIT)) {
        $candidates += $env:ONYX_APK_TOOLKIT
    }
    $candidates += "D:\APK_Toolkit_by_0xd00d"

    foreach ($candidate in $candidates) {
        if ([string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }
        $candidate = Trim-DroppedPath -Path $candidate
        if (Test-ToolkitResourceRoot -Path $candidate) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
        $resourceChild = Join-Path $candidate "6 - Resources"
        if (Test-ToolkitResourceRoot -Path $resourceChild) {
            return (Resolve-Path -LiteralPath $resourceChild).Path
        }
    }

    $dropped = Trim-DroppedPath -Path (Read-Host `
        "Drag toolkit root or '6 - Resources' folder here")
    if (Test-ToolkitResourceRoot -Path $dropped) {
        return (Resolve-Path -LiteralPath $dropped).Path
    }
    $resourceChild = Join-Path $dropped "6 - Resources"
    if (Test-ToolkitResourceRoot -Path $resourceChild) {
        return (Resolve-Path -LiteralPath $resourceChild).Path
    }
    throw "Toolkit folder is missing apktool/zipalign/apksigner/test-key files"
}

function Invoke-Checked {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$Description
    )

    Write-Host "`n==> $Description" -ForegroundColor Cyan
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE"
    }
}

function Get-AndroidAttribute {
    param(
        [System.Xml.XmlElement]$Node,
        [string]$LocalName
    )

    return $Node.GetAttribute($LocalName, $AndroidNamespace)
}

function Set-AndroidAttribute {
    param(
        [System.Xml.XmlDocument]$Document,
        [System.Xml.XmlElement]$Node,
        [string]$LocalName,
        [string]$Value
    )

    $attribute = $Node.Attributes.GetNamedItem($LocalName, $AndroidNamespace)
    if ($null -eq $attribute) {
        $attribute = $Document.CreateAttribute("android", $LocalName, $AndroidNamespace)
        [void]$Node.Attributes.Append($attribute)
    }
    $attribute.Value = $Value
}

function Resolve-AndroidClassName {
    param(
        [string]$PackageName,
        [string]$ClassName
    )

    if ($ClassName.StartsWith(".")) {
        return "$PackageName$ClassName"
    }
    if ($ClassName.Contains(".")) {
        return $ClassName
    }
    return "$PackageName.$ClassName"
}

function Add-ManifestEntries {
    param(
        [string]$ManifestPath,
        [string]$SelectedProviderClass
    )

    [xml]$document = Get-Content -LiteralPath $ManifestPath -Raw
    [System.Xml.XmlElement]$manifest = $document.DocumentElement
    if ($null -eq $manifest -or $manifest.LocalName -ne "manifest") {
        throw "Invalid decoded AndroidManifest.xml: $ManifestPath"
    }

    [System.Xml.XmlElement]$application = $manifest.SelectSingleNode("application")
    if ($null -eq $application) {
        throw "Manifest has no application element"
    }

    $permissions = @(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
        "android.permission.POST_NOTIFICATIONS"
    )

    foreach ($permission in $permissions) {
        $exists = $false
        foreach ($node in @($manifest.SelectNodes("uses-permission"))) {
            if ((Get-AndroidAttribute -Node $node -LocalName "name") -eq $permission) {
                $exists = $true
                break
            }
        }

        if (-not $exists) {
            [System.Xml.XmlElement]$permissionNode = $document.CreateElement("uses-permission")
            Set-AndroidAttribute -Document $document -Node $permissionNode -LocalName "name" -Value $permission
            [void]$manifest.InsertBefore($permissionNode, $application)
        }
    }

    $providerKey = "com.nguyen.onyxmenu.MENU_PROVIDER"
    [System.Xml.XmlElement]$providerMetadata = $null
    foreach ($node in @($application.SelectNodes("meta-data"))) {
        if ((Get-AndroidAttribute -Node $node -LocalName "name") -eq $providerKey) {
            $providerMetadata = $node
            break
        }
    }
    if ($null -eq $providerMetadata) {
        $providerMetadata = $document.CreateElement("meta-data")
        [void]$application.AppendChild($providerMetadata)
    }
    Set-AndroidAttribute -Document $document -Node $providerMetadata -LocalName "name" -Value $providerKey
    Set-AndroidAttribute -Document $document -Node $providerMetadata -LocalName "value" `
        -Value $SelectedProviderClass

    $serviceClass = "com.nguyen.onyxmenu.overlay.MenuOverlayService"
    [System.Xml.XmlElement]$service = $null
    foreach ($node in @($application.SelectNodes("service"))) {
        if ((Get-AndroidAttribute -Node $node -LocalName "name") -eq $serviceClass) {
            $service = $node
            break
        }
    }
    if ($null -eq $service) {
        $service = $document.CreateElement("service")
        [void]$application.AppendChild($service)
    }
    Set-AndroidAttribute -Document $document -Node $service -LocalName "name" -Value $serviceClass
    Set-AndroidAttribute -Document $document -Node $service -LocalName "exported" -Value "false"
    Set-AndroidAttribute -Document $document -Node $service -LocalName "foregroundServiceType" -Value "specialUse"
    Set-AndroidAttribute -Document $document -Node $service -LocalName "stopWithTask" -Value "true"

    [System.Xml.XmlElement]$property = $null
    foreach ($node in @($service.SelectNodes("property"))) {
        if ((Get-AndroidAttribute -Node $node -LocalName "name") -eq
                "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE") {
            $property = $node
            break
        }
    }
    if ($null -eq $property) {
        $property = $document.CreateElement("property")
        [void]$service.AppendChild($property)
    }
    Set-AndroidAttribute -Document $document -Node $property -LocalName "name" `
        -Value "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
    Set-AndroidAttribute -Document $document -Node $property -LocalName "value" `
        -Value "User-initiated overlay for an authorized test app"

    $settings = New-Object System.Xml.XmlWriterSettings
    $settings.Indent = $true
    $settings.Encoding = New-Object System.Text.UTF8Encoding($false)
    $writer = [System.Xml.XmlWriter]::Create($ManifestPath, $settings)
    try {
        $document.Save($writer)
    } finally {
        $writer.Close()
    }

    return $document
}

function Find-LauncherClass {
    param([System.Xml.XmlDocument]$ManifestDocument)

    [System.Xml.XmlElement]$manifest = $ManifestDocument.DocumentElement
    [System.Xml.XmlElement]$application = $manifest.SelectSingleNode("application")
    $packageName = $manifest.GetAttribute("package")

    $candidates = @($application.SelectNodes("activity")) +
        @($application.SelectNodes("activity-alias"))

    foreach ($candidate in $candidates) {
        foreach ($intentFilter in @($candidate.SelectNodes("intent-filter"))) {
            $hasMain = $false
            $hasLauncher = $false

            foreach ($action in @($intentFilter.SelectNodes("action"))) {
                if ((Get-AndroidAttribute -Node $action -LocalName "name") -eq
                        "android.intent.action.MAIN") {
                    $hasMain = $true
                }
            }
            foreach ($category in @($intentFilter.SelectNodes("category"))) {
                if ((Get-AndroidAttribute -Node $category -LocalName "name") -eq
                        "android.intent.category.LAUNCHER") {
                    $hasLauncher = $true
                }
            }

            if ($hasMain -and $hasLauncher) {
                $className = Get-AndroidAttribute -Node $candidate -LocalName "name"
                if ($candidate.LocalName -eq "activity-alias") {
                    $target = Get-AndroidAttribute -Node $candidate -LocalName "targetActivity"
                    if (-not [string]::IsNullOrWhiteSpace($target)) {
                        $className = $target
                    }
                }
                return Resolve-AndroidClassName -PackageName $packageName -ClassName $className
            }
        }
    }

    throw "Could not find a MAIN/LAUNCHER activity in the decoded manifest"
}

function Find-SmaliClassFile {
    param(
        [string]$DecodedRoot,
        [string]$ClassName
    )

    $relativePath = $ClassName.Replace(".", [IO.Path]::DirectorySeparatorChar) + ".smali"
    foreach ($smaliRoot in Get-ChildItem -LiteralPath $DecodedRoot -Directory |
            Where-Object { $_.Name -like "smali*" }) {
        $candidate = Join-Path $smaliRoot.FullName $relativePath
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }

    throw "Launcher smali not found for $ClassName"
}

function Patch-LauncherOnCreate {
    param([string]$SmaliPath)

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    $content = [IO.File]::ReadAllText($SmaliPath)
    if ($content.Contains("Lcom/nguyen/onyxmenu/overlay/MenuOverlayService;")) {
        Write-Host "Launcher already references MenuOverlayService; skipping patch" -ForegroundColor Yellow
        return
    }

    $methodPattern = "(?ms)^\.method[^\r\n]*\sonCreate\(Landroid/os/Bundle;\)V[\r\n]+(?<body>.*?)^\.end method"
    $methodMatch = [regex]::Match($content, $methodPattern)
    if (-not $methodMatch.Success) {
        throw "Launcher has no onCreate(Bundle) method; patch it manually: $SmaliPath"
    }

    $body = $methodMatch.Groups["body"].Value
    $localsMatch = [regex]::Match(
        $body,
        "(?m)^(?<indent>[ \t]*)\.locals[ \t]+(?<count>\d+)[ \t]*(?=\r?$)"
    )
    if (-not $localsMatch.Success) {
        throw "Launcher uses .registers or an unsupported method layout; patch manually: $SmaliPath"
    }

    $oldLocals = [int]$localsMatch.Groups["count"].Value
    if ($oldLocals -gt 13) {
        throw "Launcher already uses more than 13 locals; automatic invoke register encoding is unsafe"
    }

    $newLocals = $oldLocals + 2
    $localsLine = $localsMatch.Groups["indent"].Value + ".locals " + $newLocals
    $body = $body.Remove($localsMatch.Index, $localsMatch.Length).Insert(
        $localsMatch.Index,
        $localsLine
    )

    $superPattern = "(?m)^[ \t]*invoke-super(?:/range)?[^\r\n]*->onCreate\(Landroid/os/Bundle;\)V[ \t]*(?=\r?$)"
    $superMatch = [regex]::Match($body, $superPattern)
    if (-not $superMatch.Success) {
        throw "Could not find invoke-super ... onCreate(Bundle) in launcher"
    }

    $lineEnding = "`n"
    if ($content.Contains("`r`n")) {
        $lineEnding = "`r`n"
    }

    $intentRegister = "v$oldLocals"
    $classRegister = "v$($oldLocals + 1)"
    $snippet = @(
        "",
        "    # Onyx menu overlay bootstrap (authorized test APK)",
        "    new-instance $intentRegister, Landroid/content/Intent;",
        "",
        "    const-class $classRegister, Lcom/nguyen/onyxmenu/overlay/MenuOverlayService;",
        "",
        "    invoke-direct {$intentRegister, p0, $classRegister}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V",
        "",
        "    invoke-virtual {p0, $intentRegister}, Landroid/content/Context;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;"
    ) -join $lineEnding

    $insertAt = $superMatch.Index + $superMatch.Length
    $body = $body.Insert($insertAt, $lineEnding + $snippet)

    $newMethod = $methodMatch.Value.Substring(
        0,
        $methodMatch.Groups["body"].Index - $methodMatch.Index
    ) + $body + ".end method"
    $content = $content.Remove($methodMatch.Index, $methodMatch.Length).Insert(
        $methodMatch.Index,
        $newMethod
    )
    [IO.File]::WriteAllText($SmaliPath, $content, $utf8NoBom)
}

function Get-NextSmaliDirectory {
    param([string]$DecodedRoot)

    $maximum = 0
    foreach ($directory in Get-ChildItem -LiteralPath $DecodedRoot -Directory |
            Where-Object { $_.Name -like "smali*" }) {
        if ($directory.Name -eq "smali") {
            $maximum = [Math]::Max($maximum, 1)
        } elseif ($directory.Name -match "^smali_classes(?<index>\d+)$") {
            $maximum = [Math]::Max($maximum, [int]$Matches["index"])
        }
    }

    return Join-Path $DecodedRoot ("smali_classes" + ($maximum + 1))
}

function Find-OnyxMenuProviders {
    param([string]$DonorDecoded)

    $providers = @()
    foreach ($smaliRoot in Get-ChildItem -LiteralPath $DonorDecoded -Directory |
            Where-Object { $_.Name -like "smali*" }) {
        foreach ($file in Get-ChildItem -LiteralPath $smaliRoot.FullName -Filter "*.smali" `
                -File -Recurse) {
            $content = Get-Content -LiteralPath $file.FullName -Raw
            if (-not $content.Contains(
                    ".implements Lcom/nguyen/onyxmenu/engine/MenuProvider;")) {
                continue
            }

            $classMatch = [regex]::Match(
                $content,
                "(?m)^\.class[^\r\n]* L(?<class>[^;]+);"
            )
            if (-not $classMatch.Success) {
                continue
            }

            $className = $classMatch.Groups["class"].Value.Replace("/", ".")
            if ($className.StartsWith("com.nguyen.onyxmenu.engine.")) {
                continue
            }
            $providers += [PSCustomObject]@{
                ClassName = $className
                FilePath = $file.FullName
            }
        }
    }

    return @($providers | Sort-Object ClassName -Unique)
}

function Select-OnyxMenuProvider {
    param(
        [object[]]$Providers,
        [string]$RequestedClass
    )

    if ($Providers.Count -eq 0) {
        throw "Donor APK contains no selectable implementation of Onyx MenuProvider"
    }

    if (-not [string]::IsNullOrWhiteSpace($RequestedClass)) {
        foreach ($provider in $Providers) {
            if ($provider.ClassName -eq $RequestedClass) {
                return $provider.ClassName
            }
        }
        throw "Requested ProviderClass not found in donor: $RequestedClass"
    }

    if ($Providers.Count -eq 1) {
        Write-Host "Selected menu provider: $($Providers[0].ClassName)" -ForegroundColor Green
        return $Providers[0].ClassName
    }

    Write-Host "Multiple Onyx menus found. Choose one:" -ForegroundColor Yellow
    for ($index = 0; $index -lt $Providers.Count; ++$index) {
        Write-Host ("  [{0}] {1}" -f ($index + 1), $Providers[$index].ClassName)
    }
    while ($true) {
        $answer = Read-Host "Menu number"
        [int]$selected = 0
        if ([int]::TryParse($answer, [ref]$selected) -and
                $selected -ge 1 -and $selected -le $Providers.Count) {
            return $Providers[$selected - 1].ClassName
        }
        Write-Host "Invalid selection." -ForegroundColor Red
    }
}

function Copy-OnyxMenuSmali {
    param(
        [string]$DonorDecoded,
        [string]$Destination,
        [string]$SelectedProviderClass
    )

    $providerPath = $SelectedProviderClass.Replace(".", "\")
    $providerPackage = Split-Path -Parent $providerPath
    $packages = @(
        "com\nguyen\onyxmenu\bridge",
        "com\nguyen\onyxmenu\engine",
        "com\nguyen\onyxmenu\model",
        "com\nguyen\onyxmenu\overlay",
        "com\nguyen\onyxmenu\storage",
        "com\nguyen\onyxmenu\ui",
        "com\nguyen\onyxmenu\nativebridge",
        $providerPackage
    ) | Select-Object -Unique

    New-Item -ItemType Directory -Path $Destination | Out-Null
    $copied = 0
    foreach ($smaliRoot in Get-ChildItem -LiteralPath $DonorDecoded -Directory |
            Where-Object { $_.Name -like "smali*" }) {
        foreach ($package in $packages) {
            $source = Join-Path $smaliRoot.FullName $package
            if (-not (Test-Path -LiteralPath $source -PathType Container)) {
                continue
            }

            $destinationPackage = Join-Path $Destination $package
            New-Item -ItemType Directory -Path $destinationPackage -Force | Out-Null
            Copy-Item -Path (Join-Path $source "*") -Destination $destinationPackage `
                -Recurse -Force
            ++$copied
        }
    }

    if ($copied -eq 0) {
        throw "No Onyx smali packages were found in the menu donor APK"
    }

    $providerFile = Join-Path $Destination ($providerPath + ".smali")
    if (-not (Test-Path -LiteralPath $providerFile -PathType Leaf)) {
        throw "Selected provider smali was not copied: $SelectedProviderClass"
    }
}

function Copy-OnyxNativeLibraries {
    param(
        [string]$DonorDecoded,
        [string]$HostDecoded,
        [string]$RequiredLibrary
    )

    $donorLibRoot = Join-Path $DonorDecoded "lib"
    $donorLibraries = @()
    if (Test-Path -LiteralPath $donorLibRoot -PathType Container) {
        $donorLibraries = @(Get-ChildItem -LiteralPath $donorLibRoot `
            -Filter "libonyx_*.so" -File -Recurse)
    }
    if ($donorLibraries.Count -eq 0) {
        if (-not [string]::IsNullOrWhiteSpace($RequiredLibrary)) {
            throw "Selected provider requires $RequiredLibrary, but donor has no Onyx native libraries"
        }
        Write-Host "Menu donor has no Onyx native library; continuing UI-only" `
            -ForegroundColor Yellow
        return
    }

    $hostLibRoot = Join-Path $HostDecoded "lib"
    $abis = @()
    if (Test-Path -LiteralPath $hostLibRoot -PathType Container) {
        $abis = @(Get-ChildItem -LiteralPath $hostLibRoot -Directory | Select-Object -ExpandProperty Name)
    }
    if ($abis.Count -eq 0) {
        New-Item -ItemType Directory -Path $hostLibRoot -Force | Out-Null
        $abis = @(Get-ChildItem -LiteralPath $donorLibRoot -Directory | Select-Object -ExpandProperty Name)
    }

    $copied = 0
    $requiredCopied = 0
    foreach ($abi in $abis) {
        $abiSource = Join-Path $donorLibRoot $abi
        if (-not (Test-Path -LiteralPath $abiSource -PathType Container)) {
            continue
        }
        $destination = Join-Path $hostLibRoot $abi
        New-Item -ItemType Directory -Path $destination -Force | Out-Null
        foreach ($source in Get-ChildItem -LiteralPath $abiSource `
                -Filter "libonyx_*.so" -File) {
            Copy-Item -LiteralPath $source.FullName -Destination $destination -Force
            ++$copied
            if ($source.Name -eq $RequiredLibrary) {
                ++$requiredCopied
            }
        }
    }

    if ($copied -eq 0) {
        throw "Host and donor APKs have no compatible ABI for their Onyx native libraries"
    }
    if (-not [string]::IsNullOrWhiteSpace($RequiredLibrary) -and $requiredCopied -eq 0) {
        throw "Host and donor APKs have no compatible ABI for $RequiredLibrary"
    }

    Write-Host "Copied $copied Onyx native library file(s)" -ForegroundColor Green
}

if (-not $IHavePermission) {
    throw "Refusing to modify an APK without -IHavePermission. Use only APKs you own or may test."
}

$HostApk = Resolve-HostApkFromInput -InputPath $HostPath
Assert-FileExists -Path $HostApk -Label "Host APK"

$JavaHome = Resolve-JavaHomePath -RequestedPath $JavaHome
$toolsRoot = Resolve-ToolkitResourceRoot -RequestedPath $ToolkitRoot
$java = Join-Path $JavaHome "bin\java.exe"
$gradle = Join-Path $RepoRoot "gradlew.bat"
$apktool = Join-Path $toolsRoot "apktool.jar"
$zipalign = Join-Path $toolsRoot "zipalign.exe"
$apksigner = Join-Path $toolsRoot "apksigner.jar"
$key = Join-Path $toolsRoot "ApkToolkit_Key.pk8"
$certificate = Join-Path $toolsRoot "ApkToolkit_Certificate.pem"
$builtInDemoApk = Join-Path $RepoRoot "app\build\outputs\apk\debug\app-debug.apk"

Assert-FileExists -Path $java -Label "Java"
Assert-FileExists -Path $gradle -Label "Gradle wrapper"
Assert-FileExists -Path $apktool -Label "Apktool"
Assert-FileExists -Path $zipalign -Label "zipalign"
Assert-FileExists -Path $apksigner -Label "apksigner"
Assert-FileExists -Path $key -Label "Test signing key"
Assert-FileExists -Path $certificate -Label "Test signing certificate"

if ([string]::IsNullOrWhiteSpace($MenuSource) -and -not $SkipBuild) {
    $oldJavaHome = $env:JAVA_HOME
    $env:JAVA_HOME = $JavaHome
    Push-Location $RepoRoot
    try {
        Invoke-Checked -FilePath $gradle -Arguments @(":app:assembleDebug") `
            -Description "Build safe Onyx demo donor APK"
    } finally {
        Pop-Location
        $env:JAVA_HOME = $oldJavaHome
    }
}
if ([string]::IsNullOrWhiteSpace($MenuSource)) {
    $donorApk = $builtInDemoApk
    Assert-FileExists -Path $donorApk -Label "Built-in demo donor APK"
} else {
    $donorApk = Resolve-MenuDonorApk -InputPath $MenuSource
    Assert-FileExists -Path $donorApk -Label "Menu donor APK"
}

if ($ValidateOnly) {
    Write-Host "Validation successful." -ForegroundColor Green
    Write-Host "Host APK : $HostApk"
    Write-Host "Donor APK: $donorApk"
    Write-Host "Java home: $JavaHome"
    Write-Host "Tools     : $toolsRoot"
    Write-Host "No APK was decoded or modified."
    return
}

$hostDirectory = Split-Path -Parent $HostApk
$hostName = [IO.Path]::GetFileNameWithoutExtension($HostApk)
if ([string]::IsNullOrWhiteSpace($OutputApk)) {
    $OutputApk = Join-Path $hostDirectory "$hostName-onyx-menu-test-signed.apk"
}
$outputDirectory = Split-Path -Parent $OutputApk
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
if (Test-Path -LiteralPath $OutputApk) {
    throw "Output already exists; choose a new -OutputApk path: $OutputApk"
}

if ([string]::IsNullOrWhiteSpace($WorkRoot)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $WorkRoot = Join-Path $hostDirectory "onyx-menu-inject-work-$timestamp"
}
if (Test-Path -LiteralPath $WorkRoot) {
    throw "WorkRoot already exists; choose a new directory: $WorkRoot"
}

$hostDecoded = Join-Path $WorkRoot "host-decoded"
$donorDecoded = Join-Path $WorkRoot "menu-donor-decoded"
$unsignedApk = Join-Path $WorkRoot "host-onyx-unsigned.apk"
$alignedApk = Join-Path $WorkRoot "host-onyx-aligned.apk"
New-Item -ItemType Directory -Path $WorkRoot | Out-Null

Invoke-Checked -FilePath $java -Arguments @(
    "-Xmx8g", "-jar", $apktool, "d", $HostApk, "-o", $hostDecoded
) -Description "Decode host APK"

Invoke-Checked -FilePath $java -Arguments @(
    "-Xmx4g", "-jar", $apktool, "d", $donorApk, "-o", $donorDecoded
) -Description "Decode selected Onyx menu donor APK"

$providers = @(Find-OnyxMenuProviders -DonorDecoded $donorDecoded)
$selectedProvider = Select-OnyxMenuProvider -Providers $providers `
    -RequestedClass $ProviderClass
$payloadSmali = Get-NextSmaliDirectory -DecodedRoot $hostDecoded
Copy-OnyxMenuSmali -DonorDecoded $donorDecoded -Destination $payloadSmali `
    -SelectedProviderClass $selectedProvider
$requiredNativeLibrary = ""
if ($selectedProvider -eq "com.nguyen.onyxmenu.demo.profile.DemoMenuProvider") {
    $requiredNativeLibrary = "libonyx_demo_native.so"
} elseif ($selectedProvider -eq "com.nguyen.onyxpayload.MenuFreeFireProvider") {
    $requiredNativeLibrary = "libonyx_menufreefire.so"
}
Copy-OnyxNativeLibraries -DonorDecoded $donorDecoded -HostDecoded $hostDecoded `
    -RequiredLibrary $requiredNativeLibrary

$manifestPath = Join-Path $hostDecoded "AndroidManifest.xml"
$manifestDocument = Add-ManifestEntries -ManifestPath $manifestPath `
    -SelectedProviderClass $selectedProvider
$launcherClass = Find-LauncherClass -ManifestDocument $manifestDocument
$launcherSmali = Find-SmaliClassFile -DecodedRoot $hostDecoded -ClassName $launcherClass
Patch-LauncherOnCreate -SmaliPath $launcherSmali

Write-Host "Launcher patched: $launcherClass" -ForegroundColor Green
Write-Host "Menu provider   : $selectedProvider" -ForegroundColor Green
Write-Host "Payload smali: $payloadSmali" -ForegroundColor Green

Invoke-Checked -FilePath $java -Arguments @(
    "-Xmx8g", "-jar", $apktool, "b", $hostDecoded, "-o", $unsignedApk
) -Description "Rebuild modified APK"

Invoke-Checked -FilePath $zipalign -Arguments @(
    "-f", "-p", "4", $unsignedApk, $alignedApk
) -Description "Zipalign rebuilt APK"

Invoke-Checked -FilePath $zipalign -Arguments @(
    "-c", "4", $alignedApk
) -Description "Verify zip alignment"

Invoke-Checked -FilePath $java -Arguments @(
    "-jar", $apksigner, "sign",
    "--key", $key,
    "--cert", $certificate,
    "--out", $OutputApk,
    $alignedApk
) -Description "Sign APK with toolkit test certificate"

Invoke-Checked -FilePath $java -Arguments @(
    "-jar", $apksigner, "verify", "--verbose", "--print-certs", $OutputApk
) -Description "Verify signed APK"

Write-Host "`nDone." -ForegroundColor Green
Write-Host "Signed APK : $OutputApk"
Write-Host "Work folder: $WorkRoot"
Write-Host "Grant Display over other apps manually, then reopen the app."
Write-Host "The APK signature is changed; this script does not bypass integrity checks."
