"""Regenerates the logo SVG and the legacy launcher PNGs (API 21-25).
Adaptive icon (API 26+) uses the vector drawables in res/drawable. Needs: pip install cairosvg
Run from the project root:  python3 branding/make_icons.py"""
import os, cairosvg

BG, TEAL, CORAL, GHOST = "#14172B", "#2DD4BF", "#FB7185", "#F5F7FF"
GHOST_PATH = ("M54 29 C44.6 29 37 36.6 37 46 L37 66 Q42.67 74 48.33 66 Q54 74 59.67 66 "
              "Q65.33 74 71 66 L71 46 C71 36.6 63.4 29 54 29 Z")

def art(scale=1.0):
    g = f'<g transform="translate(54 54) scale({scale}) translate(-54 -54)">'
    return f'''{g}
    <circle cx="54" cy="54" r="31" fill="none" stroke="{TEAL}" stroke-width="2.2"/>
    <circle cx="54" cy="54" r="25" fill="none" stroke="{TEAL}" stroke-width="1.4" stroke-opacity="0.35"/>
    <circle cx="73" cy="38" r="6" fill="none" stroke="{CORAL}" stroke-width="1.2" stroke-opacity="0.8"/>
    <circle cx="73" cy="38" r="3.2" fill="{CORAL}"/>
    <path d="{GHOST_PATH}" fill="{GHOST}"/>
    <circle cx="48" cy="47" r="3.4" fill="{BG}"/><circle cx="60" cy="47" r="3.4" fill="{BG}"/>
    <circle cx="54" cy="55.5" r="2.2" fill="{BG}"/>
  </g>'''

def svg(shape, scale):
    bg = {"rounded": f'<rect width="108" height="108" rx="24" fill="{BG}"/>',
          "circle": f'<circle cx="54" cy="54" r="54" fill="{BG}"/>'}[shape]
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512">\n  {bg}\n  {art(scale)}\n</svg>\n'

os.makedirs("branding", exist_ok=True)
open("branding/ghostguard_logo.svg", "w").write(svg("rounded", 1.15))
open("branding/ghostguard_logo_round.svg", "w").write(svg("circle", 1.15))
cairosvg.svg2png(bytestring=svg("rounded", 1.15).encode(), write_to="branding/ic_launcher_512.png", output_width=512, output_height=512)

for d, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    out = f"app/src/main/res/mipmap-{d}"; os.makedirs(out, exist_ok=True)
    cairosvg.svg2png(bytestring=svg("rounded", 1.15).encode(), write_to=f"{out}/ic_launcher.png", output_width=px, output_height=px)
    cairosvg.svg2png(bytestring=svg("circle", 1.15).encode(), write_to=f"{out}/ic_launcher_round.png", output_width=px, output_height=px)
print("done")
