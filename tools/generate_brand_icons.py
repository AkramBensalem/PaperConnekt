#!/usr/bin/env python3
"""Export platform icons from branding/paperconnekt.svg (Pillow + CairoSVG)."""

from copy import deepcopy
from io import BytesIO
from pathlib import Path
import xml.etree.ElementTree as ET

import cairosvg
from PIL import Image, ImageDraw


RESAMPLING = getattr(Image, "Resampling", Image)
ROOT = Path(__file__).resolve().parents[1]
SVG = "{http://www.w3.org/2000/svg}"
ET.register_namespace("", SVG[1:-1])
SOURCE = ROOT / "branding/paperconnekt.svg"
MASTER = ET.parse(SOURCE).getroot()
MARK = MASTER.find(f"{SVG}g[@id='mark']")
ANDROID = ROOT / "app/android/src/main/res"
DESKTOP = ROOT / "app/desktop/src/main/resources/icons"
IDEA = ROOT / "plugin/native-plugins/idea/src/main/resources"
VSCODE = ROOT / "plugin/native-plugins/vscode/src/main/resources/icons"


def write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(content, bytes):
        path.write_bytes(content)
    else:
        path.write_text(content, encoding="utf-8")


def raster(svg, size):
    return Image.open(BytesIO(cairosvg.svg2png(
        bytestring=svg, output_width=size, output_height=size,
    ))).convert("RGBA")


def png(path, image):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG")


def mark_svg(size, color):
    icon = ET.Element(f"{SVG}svg", {
        "width": str(size), "height": str(size), "viewBox": "0 0 24 24",
    })
    mark = deepcopy(MARK)
    mark.attrib["transform"] = "translate(-2 0)"
    for path in mark:
        path.attrib["fill"] = color
    icon.append(mark)
    return ET.tostring(icon) + b"\n"


def android_vector(monochrome=False):
    paths = "".join(
        f'            <path android:fillColor="{("#FFFFFF" if monochrome else path.attrib["fill"])}" '
        f'android:fillType="{("evenOdd" if path.get("fill-rule") == "evenodd" else "nonZero")}" '
        f'android:pathData="{path.attrib["d"]}"/>\n'
        for path in MARK
    )
    return (
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp" android:height="108dp"\n'
        '    android:viewportWidth="108" android:viewportHeight="108">\n'
        '    <group android:translateX="22" android:translateY="26">\n'
        '        <group android:scaleX="2.3" android:scaleY="2.3">\n'
        f'{paths}'
        '        </group>\n'
        '    </group>\n'
        '</vector>\n'
    )


def export():
    source = SOURCE.read_bytes()
    full_bleed = deepcopy(MASTER)
    tile = full_bleed.find(f"{SVG}rect")
    tile.attrib.update(x="0", y="0", width="1024", height="1024", rx="0")
    full_bleed_svg = ET.tostring(full_bleed)

    for density, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                          ("xxhdpi", 144), ("xxxhdpi", 192)):
        png(ANDROID / f"mipmap-{density}/ic_launcher.png", raster(source, size))
        round_icon = raster(full_bleed_svg, size)
        mask = Image.new("L", (size * 4, size * 4))
        ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
        round_icon.putalpha(mask.resize((size, size), RESAMPLING.LANCZOS))
        png(ANDROID / f"mipmap-{density}/ic_launcher_round.png", round_icon)

    write(ANDROID / "drawable/ic_launcher_foreground.xml", android_vector())
    write(ANDROID / "drawable/ic_launcher_monochrome.xml", android_vector(monochrome=True))
    write(ANDROID / "drawable/ic_launcher_background.xml", '''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M0,0H108V108H0Z">
        <aapt:attr name="android:fillColor">
            <gradient android:type="linear" android:startX="0" android:startY="0"
                android:endX="108" android:endY="108">
                <item android:offset="0" android:color="#3B82F6"/>
                <item android:offset="0.55" android:color="#2457A6"/>
                <item android:offset="1" android:color="#173D80"/>
            </gradient>
        </aapt:attr>
    </path>
</vector>
''')
    for qualifier, monochrome in (("v26", ""), ("v33", '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n')):
        adaptive = (
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@drawable/ic_launcher_background"/>\n'
            '    <foreground android:drawable="@drawable/ic_launcher_foreground"/>\n'
            f'{monochrome}</adaptive-icon>\n'
        )
        for name in ("ic_launcher", "ic_launcher_round"):
            write(ANDROID / f"mipmap-anydpi-{qualifier}/{name}.xml", adaptive)

    desktop_icon = raster(source, 1024)
    png(DESKTOP / "paperconnekt.png", desktop_icon.resize((512, 512), RESAMPLING.LANCZOS))
    desktop_icon.save(DESKTOP / "paperconnekt.ico", sizes=[
        (16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256),
    ])
    desktop_icon.save(DESKTOP / "paperconnekt.icns", format="ICNS")
    png(ROOT / "app/composeApp/src/commonMain/composeResources/drawable/paperconnekt_brand.png",
        raster(source, 192))

    for name in ("pluginIcon.svg", "pluginIcon_dark.svg"):
        icon = deepcopy(MASTER)
        icon.attrib.update(width="40", height="40")
        write(IDEA / f"META-INF/{name}", ET.tostring(icon) + b"\n")
    write(IDEA / "icons/paperconnekt.svg", mark_svg(16, "#2457A6"))
    write(IDEA / "icons/paperconnekt_dark.svg", mark_svg(16, "#A9C7FF"))
    png(VSCODE / "paperconnekt.png", raster(source, 128))
    write(VSCODE / "paperconnekt-activity.svg", mark_svg(24, "#FFFFFF"))


if __name__ == "__main__":
    export()
