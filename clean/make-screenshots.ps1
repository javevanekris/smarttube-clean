param([string]$In, [string]$Out)
# Turns raw TV captures into README screenshots: account picture replaced by a neutral icon, scaled to 1280 wide, saved as JPEG.
Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Force $Out | Out-Null

function Save-Jpeg($bmp, [string]$file, [int]$width) {
    $h = [int]($bmp.Height * $width / $bmp.Width)
    $small = New-Object System.Drawing.Bitmap($width, $h)
    $g = [System.Drawing.Graphics]::FromImage($small)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($bmp, 0, 0, $width, $h)
    $g.Dispose()
    $codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
    $p = New-Object System.Drawing.Imaging.EncoderParameters(1)
    $p.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter([System.Drawing.Imaging.Encoder]::Quality, [long]90)
    $small.Save($file, $codec, $p)
    $small.Dispose()
}

function Hide-Avatar($bmp) {
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $bg = $bmp.GetPixel(430, 106)
    $orb = $bmp.GetPixel(130, 112)
    $b1 = New-Object System.Drawing.SolidBrush($bg)
    $b2 = New-Object System.Drawing.SolidBrush($orb)
    $white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(235, 255, 255, 255))
    $g.FillEllipse($b1, 248, 46, 120, 120)
    $g.FillEllipse($b2, 257, 55, 102, 102)
    $g.FillEllipse($white, 296, 80, 24, 24)
    $g.FillPie($white, 284, 110, 48, 40, 180, 180)
    $g.Dispose(); $b1.Dispose(); $b2.Dispose(); $white.Dispose()
}

function Convert-Shot([string]$name, [string]$target, [bool]$avatar) {
    $src = [System.Drawing.Bitmap]::FromFile((Join-Path $In $name))
    $bmp = New-Object System.Drawing.Bitmap($src)
    $src.Dispose()
    if ($avatar) { Hide-Avatar $bmp }
    Save-Jpeg $bmp (Join-Path $Out $target) 1280
    $bmp.Dispose()
}

Convert-Shot '13-browse-games.png' 'browse.jpg' $true
Convert-Shot '12-sidebar-games.png' 'sidebar.jpg' $false
Convert-Shot '08-settings-tiles.png' 'settings.jpg' $true

# Player: only the controls bar, so that the video and its title stay out of the picture
$src = [System.Drawing.Bitmap]::FromFile((Join-Path $In '01-player.png'))
$strip = $src.Clone((New-Object System.Drawing.Rectangle(40, 778, 1840, 222)), $src.PixelFormat)
$src.Dispose()
Save-Jpeg $strip (Join-Path $Out 'player-controls.jpg') 1280
$strip.Dispose()
Get-ChildItem $Out | ForEach-Object { '{0} {1}' -f $_.Name, $_.Length }
