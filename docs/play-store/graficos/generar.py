# Genera los gráficos de Google Play (icono, feature graphic y capturas) con Chrome sin interfaz.
# Uso: python generar.py <carpeta de salida> <carpeta con capturas del emulador>
import base64, os, subprocess, sys

OUT = sys.argv[1]
SHOTS = sys.argv[2]
HERE = os.path.dirname(os.path.abspath(__file__))
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
os.makedirs(OUT, exist_ok=True)

FONTS = '<link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@500;600;700&family=IBM+Plex+Mono:wght@500;600&display=block" rel="stylesheet">'

CUBE = '''<svg viewBox="28 18 52 62" xmlns="http://www.w3.org/2000/svg">
<path fill="#FF8A4C" d="M54,31 L74,42.5 L54,54 L34,42.5 Z"/>
<path fill="#C2410C" d="M34,42.5 L54,54 L54,77 L34,65.5 Z"/>
<path fill="#E2561A" d="M54,54 L74,42.5 L74,65.5 L54,77 Z"/>
<path stroke="rgba(255,255,255,.22)" stroke-width="1.2" fill="none" d="M34,48.5 L54,60 L74,48.5 M34,54.5 L54,66 L74,54.5 M34,60.5 L54,72 L74,60.5"/>
<path fill="#FFFFFF" d="M50.5,20 L57.5,20 L57.5,25 L55.5,28.5 L52.5,28.5 L50.5,25 Z"/>
</svg>'''

# Capas FDM: franjas finas horizontales, el sello de la marca
LAYERS = "repeating-linear-gradient(180deg, rgba(194,65,12,.10) 0 3px, transparent 3px 14px)"

def img(name):
    with open(os.path.join(SHOTS, name), "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode()

def render(name, w, h, body, css=""):
    html = f"""<!doctype html><html><head><meta charset="utf-8">{FONTS}<style>
    *{{margin:0;padding:0;box-sizing:border-box}}
    html,body{{width:{w}px;height:{h}px;overflow:hidden}}
    body{{font-family:'IBM Plex Sans',sans-serif;-webkit-font-smoothing:antialiased}}
    {css}</style></head><body>{body}</body></html>"""
    path = os.path.join(HERE, name + ".html")
    open(path, "w", encoding="utf-8").write(html)
    out = os.path.join(OUT, name + ".png")
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
                    f"--window-size={w},{h}", "--virtual-time-budget=8000", f"--screenshot={out}", "file:///" + path.replace("\\", "/")],
                   check=True, capture_output=True)
    print("ok", out)

# ---------- Icono 512 ----------
render("icono-512", 512, 512, f'<div class="bg"><div class="cube">{CUBE}</div></div>', """
.bg{width:512px;height:512px;background:#1F252C;display:flex;align-items:center;justify-content:center}
.cube{width:300px;height:358px}
.cube svg{width:100%;height:100%}""")

# ---------- Feature graphic 1024x500 ----------
render("grafico-funciones", 1024, 500, f'''
<div class="fg">
  <div class="layers"></div>
  <div class="text">
    <div class="brand"><span class="mini">{CUBE}</span>3D Cost Manager</div>
    <h1>Sabe lo que cuesta<br>cada impresión 3D<br>y <em>cuánto cobrar</em></h1>
    <p>Material · luz · desgaste · tiempo · fallos</p>
  </div>
  <div class="ticket">
    <div class="row"><span>Material</span><b>2,40 €</b></div>
    <div class="row"><span>Luz y desgaste</span><b>0,58 €</b></div>
    <div class="row"><span>Tu tiempo</span><b>6,25 €</b></div>
    <div class="rule"></div>
    <div class="label">Precio de venta</div>
    <div class="price">16,20 €</div>
    <div class="keep">Te quedan 3,87 € limpios</div>
  </div>
</div>''', f"""
.fg{{position:relative;width:1024px;height:500px;background:#1F252C;color:#fff;display:flex;align-items:center;padding:0 64px;gap:48px;overflow:hidden}}
.layers{{position:absolute;inset:0;background:repeating-linear-gradient(180deg, rgba(255,138,76,.07) 0 3px, transparent 3px 16px);mask-image:linear-gradient(90deg,transparent 0,#000 70%)}}
.text{{position:relative;flex:1}}
.brand{{display:flex;align-items:center;gap:12px;font-weight:600;font-size:22px;color:#FFB38A;letter-spacing:.2px}}
.mini{{width:30px;height:36px;display:inline-block}} .mini svg{{width:100%;height:100%}}
h1{{font-size:44px;line-height:1.1;font-weight:700;margin:20px 0 18px;letter-spacing:-.8px}}
h1 em{{font-style:normal;color:#FF8A4C}}
p{{font-family:'IBM Plex Mono',monospace;font-size:16px;color:#A7B0BA;white-space:nowrap}}
.ticket{{position:relative;width:300px;background:#F4F5F7;color:#15191E;border-radius:18px;padding:26px 26px 24px;box-shadow:0 18px 40px rgba(0,0,0,.35)}}
.row{{display:flex;justify-content:space-between;font-size:17px;padding:5px 0;color:#56606B}}
.row b{{font-family:'IBM Plex Mono',monospace;color:#15191E;font-weight:600}}
.rule{{border-top:2px dashed #C9CFD6;margin:12px 0 14px}}
.label{{font-size:15px;color:#56606B;font-weight:600}}
.price{{font-family:'IBM Plex Mono',monospace;font-size:52px;font-weight:600;color:#C2410C;letter-spacing:-1.5px;line-height:1.1}}
.keep{{font-size:15px;color:#14593C;font-weight:600;margin-top:4px}}""")

# ---------- Capturas 1080x1920 ----------
SHOT_CSS = f"""
.s{{position:relative;width:1080px;height:1920px;background:#F4F5F7;overflow:hidden}}
.s.dark{{background:#1F252C}}
.layers{{position:absolute;left:0;right:0;bottom:0;height:900px;background:{LAYERS}}}
.dark .layers{{background:repeating-linear-gradient(180deg, rgba(255,138,76,.08) 0 3px, transparent 3px 14px)}}
.head{{position:relative;padding:110px 90px 0}}
.eyebrow{{display:inline-flex;align-items:center;gap:12px;font-family:'IBM Plex Mono',monospace;font-weight:600;font-size:28px;color:#C2410C;letter-spacing:2px;text-transform:uppercase}}
.eyebrow i{{width:34px;height:4px;background:#C2410C;display:inline-block}}
.dark .eyebrow{{color:#FF8A4C}} .dark .eyebrow i{{background:#FF8A4C}}
h1{{font-size:76px;line-height:1.06;font-weight:700;color:#15191E;margin-top:26px;letter-spacing:-1.5px}}
.dark h1{{color:#fff}}
h1 em{{font-style:normal;color:#C2410C}} .dark h1 em{{color:#FF8A4C}}
.phone{{position:absolute;left:50%;transform:translateX(-50%);top:440px;width:800px;border-radius:64px;background:#15191E;padding:18px;box-shadow:0 30px 70px rgba(21,25,30,.28)}}
.dark .phone{{background:#000;box-shadow:0 30px 80px rgba(0,0,0,.5)}}
.screen{{width:764px;height:1600px;border-radius:48px;overflow:hidden;background:#fff}}
.screen img{{width:100%;display:block}}"""

def shot(name, eyebrow, title, image, offset=0, dark=False):
    render(name, 1080, 1920, f'''
<div class="s{' dark' if dark else ''}"><div class="layers"></div>
  <div class="head"><div class="eyebrow"><i></i>{eyebrow}</div><h1>{title}</h1></div>
  <div class="phone"><div class="screen"><img src="{img(image)}" style="margin-top:-{offset}px"></div></div>
</div>''', SHOT_CSS)

SETS = {
 "es": [
  ("1", "Coste real", "Sabe lo que te cuesta<br><em>de verdad</em> cada pieza", "es_breakdown.png", 0, False),
  ("2", "Precio por canal", "Cobra lo justo en Etsy,<br>Wallapop <em>o tu tienda</em>", "n_channels2.png", 0, False),
  ("3", "Tu taller", "Presupuestos, ventas<br>y <em>beneficio</em> del mes", "n_chart.png", 0, True),
  ("4", "Importa y listo", "Abre el G-code o 3MF<br>y se <em>rellena solo</em>", "es_calc5.png", 0, False),
  ("5", "Temporizador", "Te avisa cuando<br><em>termina</em> de imprimir", "u_timer.png", 0, True),
 ],
}
for lang, items in SETS.items():
    for n, eb, t, im, off, dark in items:
        shot(f"captura-{lang}-{n}", eb, t, im, off, dark)
