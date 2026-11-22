param([Parameter(Mandatory = $true)][string]$Out)

# Renders the SmartTube Clean launcher art: dark gradient, soft colour glows, a glass disc with a play mark.
Add-Type -AssemblyName System.Drawing
$SS = 4   # supersampling factor

function New-Argb([int]$a, [int]$r, [int]$g, [int]$b) { [System.Drawing.Color]::FromArgb($a, $r, $g, $b) }

function New-RoundRect([single]$x, [single]$y, [single]$w, [single]$h, [single]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = 2 * $r
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return , $p
}

function Add-Glow($g, [single]$cx, [single]$cy, [single]$rx, [single]$ry, [System.Drawing.Color]$c) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $p.AddEllipse($cx - $rx, $cy - $ry, 2 * $rx, 2 * $ry)
    $b = New-Object System.Drawing.Drawing2D.PathGradientBrush($p)
    $b.CenterColor = $c
    $b.SurroundColors = [System.Drawing.Color[]]@((New-Argb 0 $c.R $c.G $c.B))
    $g.FillPath($b, $p)
    $b.Dispose(); $p.Dispose()
}

function Add-Background($g, [single]$x, [single]$y, [single]$w, [single]$h) {
    $rect = New-Object System.Drawing.RectangleF($x, $y, $w, $h)
    $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, (New-Argb 255 44 50 68), (New-Argb 255 5 5 6), [single]40)
    $blend = New-Object System.Drawing.Drawing2D.ColorBlend(3)
    $blend.Colors = [System.Drawing.Color[]]@((New-Argb 255 44 50 68), (New-Argb 255 17 19 26), (New-Argb 255 5 5 6))
    $blend.Positions = [single[]]@(0, 0.55, 1)
    $bg.InterpolationColors = $blend
    $g.FillRectangle($bg, $rect)
    $bg.Dispose()
    # colour behind the glass, like a blurred wallpaper
    Add-Glow $g ($x + $w * 0.16) ($y + $h * 0.20) ($w * 0.55) ($h * 0.75) (New-Argb 120 104 92 255)
    Add-Glow $g ($x + $w * 0.90) ($y + $h * 0.95) ($w * 0.55) ($h * 0.70) (New-Argb 95 38 196 190)
}

function New-RoundPolygon([System.Drawing.PointF[]]$pts, [single]$r) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $n = $pts.Length
    for ($i = 0; $i -lt $n; $i++) {
        $v = $pts[$i]; $p = $pts[($i + $n - 1) % $n]; $q = $pts[($i + 1) % $n]
        $l1 = [Math]::Sqrt(($p.X - $v.X) * ($p.X - $v.X) + ($p.Y - $v.Y) * ($p.Y - $v.Y))
        $l2 = [Math]::Sqrt(($q.X - $v.X) * ($q.X - $v.X) + ($q.Y - $v.Y) * ($q.Y - $v.Y))
        $u1x = ($p.X - $v.X) / $l1; $u1y = ($p.Y - $v.Y) / $l1
        $u2x = ($q.X - $v.X) / $l2; $u2y = ($q.Y - $v.Y) / $l2
        $half = [Math]::Acos($u1x * $u2x + $u1y * $u2y) / 2
        $t = $r / [Math]::Tan($half)
        $k = 0.45
        $t1 = New-Object System.Drawing.PointF(($v.X + $u1x * $t), ($v.Y + $u1y * $t))
        $t2 = New-Object System.Drawing.PointF(($v.X + $u2x * $t), ($v.Y + $u2y * $t))
        $c1 = New-Object System.Drawing.PointF(($v.X + $u1x * $t * $k), ($v.Y + $u1y * $t * $k))
        $c2 = New-Object System.Drawing.PointF(($v.X + $u2x * $t * $k), ($v.Y + $u2y * $t * $k))
        $path.AddBezier($t1, $c1, $c2, $t2)
    }
    $path.CloseFigure()
    return , $path
}

function New-PlayShape([single]$x, [single]$y, [single]$h) {
    $w = $h * 0.88
    $pts = [System.Drawing.PointF[]]@(
        (New-Object System.Drawing.PointF(($x - $w * 0.42), ($y - $h / 2))),
        (New-Object System.Drawing.PointF(($x + $w * 0.58), $y)),
        (New-Object System.Drawing.PointF(($x - $w * 0.42), ($y + $h / 2)))
    )
    return , (New-RoundPolygon $pts ($h * 0.11))
}

function Add-GlassShape($g, $shape, [int]$fillTop, [int]$fillBottom, [int]$edgeTop, [int]$edgeBottom, [single]$edgeWidth) {
    $b = $shape.GetBounds()
    $rect = New-Object System.Drawing.RectangleF($b.Left, ($b.Top - 2), $b.Width, ($b.Height + 4))
    $fill = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, (New-Argb $fillTop 255 255 255), (New-Argb $fillBottom 255 255 255), [single]90)
    $g.FillPath($fill, $shape)
    if ($edgeWidth -gt 0) {
        $edge = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, (New-Argb $edgeTop 255 255 255), (New-Argb $edgeBottom 255 255 255), [single]90)
        $pen = New-Object System.Drawing.Pen($edge, $edgeWidth)
        $g.DrawPath($pen, $shape)
        $edge.Dispose(); $pen.Dispose()
    }
    $fill.Dispose()
}

function Add-Sparkle($g, [single]$cx, [single]$cy, [single]$r, [int]$alpha) {
    $k = $r * 0.10
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $n = New-Object System.Drawing.PointF($cx, ($cy - $r)); $e = New-Object System.Drawing.PointF(($cx + $r), $cy)
    $s = New-Object System.Drawing.PointF($cx, ($cy + $r)); $w = New-Object System.Drawing.PointF(($cx - $r), $cy)
    $p.AddBezier($n, (New-Object System.Drawing.PointF(($cx + $k), ($cy - $k))), (New-Object System.Drawing.PointF(($cx + $k), ($cy - $k))), $e)
    $p.AddBezier($e, (New-Object System.Drawing.PointF(($cx + $k), ($cy + $k))), (New-Object System.Drawing.PointF(($cx + $k), ($cy + $k))), $s)
    $p.AddBezier($s, (New-Object System.Drawing.PointF(($cx - $k), ($cy + $k))), (New-Object System.Drawing.PointF(($cx - $k), ($cy + $k))), $w)
    $p.AddBezier($w, (New-Object System.Drawing.PointF(($cx - $k), ($cy - $k))), (New-Object System.Drawing.PointF(($cx - $k), ($cy - $k))), $n)
    $p.CloseFigure()
    $b = New-Object System.Drawing.SolidBrush((New-Argb $alpha 255 255 255))
    $g.FillPath($b, $p)
    $b.Dispose(); $p.Dispose()
}

# Three sheets of glass in the shape of a play mark, each one a step ahead of the last: playback that skips forward.
function Add-Mark($g, [single]$cx, [single]$cy, [single]$size) {
    $h = $size * 0.64; $step = $size * 0.17; $front = $cx + $size * 0.12
    $back = New-PlayShape ($front - 2 * $step) $cy $h
    $mid = New-PlayShape ($front - $step) $cy $h
    $top = New-PlayShape $front $cy $h
    Add-GlassShape $g $back 46 14 120 30 ($size * 0.009)
    Add-GlassShape $g $mid 120 52 210 70 ($size * 0.009)
    Add-GlassShape $g $top 255 255 0 0 0
    Add-Sparkle $g ($cx + $size * 0.37) ($cy - $size * 0.33) ($size * 0.115) 245
    Add-Sparkle $g ($cx + $size * 0.47) ($cy - $size * 0.17) ($size * 0.045) 170
    $back.Dispose(); $mid.Dispose(); $top.Dispose()
}

function New-TextPath([string]$text, [string]$family, [single]$em, [single]$tracking) {
    try { $ff = New-Object System.Drawing.FontFamily($family) } catch { $ff = New-Object System.Drawing.FontFamily('Segoe UI') }
    $fmt = [System.Drawing.StringFormat]::GenericTypographic
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    if ($tracking -le 0) {
        $path.AddString($text, $ff, 0, $em, (New-Object System.Drawing.PointF(0, 0)), $fmt)
        return , $path
    }
    $x = 0.0
    foreach ($ch in $text.ToCharArray()) {
        $one = New-Object System.Drawing.Drawing2D.GraphicsPath
        $one.AddString([string]$ch, $ff, 0, $em, (New-Object System.Drawing.PointF(0, 0)), $fmt)
        $b = $one.GetBounds()
        $m = New-Object System.Drawing.Drawing2D.Matrix
        $m.Translate([single]($x - $b.Left), 0)
        $one.Transform($m)
        $path.AddPath($one, $false)
        $x += $b.Width + $em * $tracking
        $one.Dispose(); $m.Dispose()
    }
    return , $path
}

function Move-Path($path, [single]$left, [single]$top) {
    $b = $path.GetBounds()
    $m = New-Object System.Drawing.Drawing2D.Matrix
    $m.Translate([single]($left - $b.Left), [single]($top - $b.Top))
    $path.Transform($m)
    $m.Dispose()
}

function New-Canvas([int]$w, [int]$h) {
    $bmp = New-Object System.Drawing.Bitmap(($w * $SS), ($h * $SS), [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.ScaleTransform($SS, $SS)
    return @{ Bitmap = $bmp; Graphics = $g }
}

function Save-Canvas($canvas, [int]$w, [int]$h, [string]$file) {
    $canvas.Graphics.Dispose()
    $final = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($final)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $attr = New-Object System.Drawing.Imaging.ImageAttributes
    $attr.SetWrapMode([System.Drawing.Drawing2D.WrapMode]::TileFlipXY)
    $g.DrawImage($canvas.Bitmap, (New-Object System.Drawing.Rectangle(0, 0, $w, $h)), 0, 0, $canvas.Bitmap.Width, $canvas.Bitmap.Height, [System.Drawing.GraphicsUnit]::Pixel, $attr)
    $g.Dispose()
    $final.Save($file, [System.Drawing.Imaging.ImageFormat]::Png)
    $final.Dispose(); $canvas.Bitmap.Dispose()
}

function Write-Banner([string]$file) {
    $w = 640; $h = 360
    $c = New-Canvas $w $h; $g = $c.Graphics
    Add-Background $g 0 0 $w $h

    $d = 196; $gap = 22
    $title = New-TextPath 'SmartTube' 'Segoe UI Semibold' 66 0
    $sub = New-TextPath 'CLEAN' 'Segoe UI' 23 0.42
    $tw = [Math]::Max($title.GetBounds().Width, $sub.GetBounds().Width)
    $left = ($w - ($d + $gap + $tw)) / 2
    Add-Mark $g ($left + $d / 2) ($h / 2) $d

    $th = $title.GetBounds().Height; $sh = $sub.GetBounds().Height; $lineGap = 20
    $top = ($h - ($th + $lineGap + $sh)) / 2
    Move-Path $title ($left + $d + $gap) $top
    Move-Path $sub ($left + $d + $gap + 3) ($top + $th + $lineGap)
    $b1 = New-Object System.Drawing.SolidBrush((New-Argb 255 255 255 255))
    $b2 = New-Object System.Drawing.SolidBrush((New-Argb 175 255 255 255))
    $g.FillPath($b1, $title); $g.FillPath($b2, $sub)
    $b1.Dispose(); $b2.Dispose(); $title.Dispose(); $sub.Dispose()
    Save-Canvas $c $w $h $file
}

function Write-Icon([string]$file, [int]$size, [single]$marginFraction) {
    $c = New-Canvas $size $size; $g = $c.Graphics
    $m = $size * $marginFraction; $s = $size - 2 * $m
    $tile = New-RoundRect $m $m $s $s ($s * 0.235)
    $state = $g.Save()
    $g.SetClip($tile, [System.Drawing.Drawing2D.CombineMode]::Replace)
    Add-Background $g $m $m $s $s
    $g.Restore($state)
    $rect = New-Object System.Drawing.RectangleF($m, $m, $s, $s)
    $edge = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, (New-Argb 90 255 255 255), (New-Argb 20 255 255 255), [single]90)
    $pen = New-Object System.Drawing.Pen($edge, [single]($s * 0.008))
    $g.DrawPath($pen, $tile)
    Add-Mark $g ($size / 2) ($size * 0.52) ($s * 0.70)
    $edge.Dispose(); $pen.Dispose(); $tile.Dispose()
    Save-Canvas $c $size $size $file
}

New-Item -ItemType Directory -Force $Out | Out-Null
Write-Banner (Join-Path $Out 'app_banner.png')
Write-Icon (Join-Path $Out 'app_icon.png') 320 0.02
Write-Icon (Join-Path $Out 'app_icon_v30.png') 320 0.10
Write-Icon (Join-Path $Out 'app_logo.png') 180 0.02
Write-Output "done: $Out"
