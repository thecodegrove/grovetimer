#!/usr/bin/env python3
"""Generate Google Play listing metadata and promotional screenshots."""

from __future__ import annotations

import shutil
import textwrap
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
METADATA = ROOT / "fastlane" / "metadata" / "android"
ICON = ROOT / "app" / "src" / "main" / "ic_launcher-playstore.png"
FONT_DIR = ROOT / "app" / "src" / "main" / "res" / "font"

SCREENSHOTS = {
    "home": Path("/private/tmp/grovetimer-home-running.png"),
    "start": Path("/private/tmp/grovetimer-video-start.png"),
    "notification": Path("/private/tmp/grovetimer-notification-v3.png"),
    "settings": Path("/private/tmp/grovetimer-settings-screen.png"),
}

LOCALES = {
    "es-ES": {
        "title": "GroveTimer",
        "short": "Temporizador de sueño para pausar música, podcasts y vídeos.",
        "feature": "Duerme con tu contenido favorito. GroveTimer se encarga del final.",
        "screens": [
            ("Duerme con tu música", "Programa un temporizador para pausar la reproducción cuando quieras.", "home"),
            ("Elige la duración perfecta", "Ajusta minutos al instante y empieza con un toque.", "start"),
            ("Control siempre visible", "Consulta el tiempo restante y pausa o detén desde la notificación.", "notification"),
            ("Apagado más suave", "Activa el fade-out progresivo para bajar el volumen antes de parar.", "settings"),
            ("Simple y privado", "Sin cuentas, sin ruido: solo un temporizador claro para tus medios.", "home"),
        ],
        "description": """GroveTimer es un temporizador de sueño para música, podcasts, vídeos y cualquier reproducción multimedia del dispositivo.

Elige cuánto tiempo quieres seguir escuchando, inicia el temporizador y deja que la app pause la reproducción automáticamente al terminar. Es ideal para dormir con música, relajarte con un podcast o ver un vídeo sin preocuparte de que siga sonando toda la noche.

Funciones principales:
• Temporizador rápido para reproducción multimedia.
• Notificación persistente con tiempo restante y controles.
• Pausa automática de la reproducción al finalizar.
• Opción de fade-out progresivo para reducir el volumen antes de parar.
• Vibración opcional al terminar.
• Modo claro y oscuro.
• Diseño limpio, sin cuentas ni configuración innecesaria.

Permisos:
GroveTimer usa notificaciones para mostrar el temporizador activo y permitir pausarlo o detenerlo desde cualquier pantalla. También solicita acceso de control multimedia para poder detectar y pausar la reproducción cuando el temporizador termina.

GroveTimer está pensada para ser pequeña, práctica y respetuosa: configuras el tiempo, pulsas iniciar y listo.""",
    },
    "en-US": {
        "title": "GroveTimer",
        "short": "A sleep timer for pausing music, podcasts, and videos.",
        "feature": "Fall asleep to your favorite media. GroveTimer handles the ending.",
        "screens": [
            ("Sleep with your music", "Set a timer and pause playback automatically when time runs out.", "home"),
            ("Pick the perfect duration", "Adjust minutes quickly and start the timer with one tap.", "start"),
            ("Control stays visible", "Check remaining time and pause or stop from the notification.", "notification"),
            ("A gentler finish", "Enable progressive fade-out to lower volume before playback stops.", "settings"),
            ("Simple and private", "No accounts, no clutter: just a clear timer for your media.", "home"),
        ],
        "description": """GroveTimer is a sleep timer for music, podcasts, videos, and other media playback on your device.

Choose how long you want to keep listening, start the timer, and let the app pause playback automatically when time runs out. It is useful for falling asleep to music, relaxing with a podcast, or watching a video without leaving audio playing all night.

Key features:
• Fast playback sleep timer.
• Persistent notification with remaining time and controls.
• Automatic media pause when the timer ends.
• Optional progressive fade-out before stopping playback.
• Optional vibration when finished.
• Light and dark mode.
• Clean design with no accounts or unnecessary setup.

Permissions:
GroveTimer uses notifications to show the active timer and let you pause or stop it from any screen. It also requests media control access so it can detect and pause playback when the timer finishes.

GroveTimer is designed to be small, practical, and respectful: set the time, tap start, and you are done.""",
    },
    "ca": {
        "title": "GroveTimer",
        "short": "Temporitzador de son per pausar música, pòdcasts i vídeos.",
        "feature": "Adorm-te amb el teu contingut preferit. GroveTimer s’encarrega del final.",
        "screens": [
            ("Adorm-te amb música", "Programa un temporitzador per pausar la reproducció quan vulguis.", "home"),
            ("Tria la durada perfecta", "Ajusta els minuts ràpidament i comença amb un toc.", "start"),
            ("Control sempre visible", "Consulta el temps restant i pausa o atura des de la notificació.", "notification"),
            ("Un final més suau", "Activa el fade-out progressiu per baixar el volum abans d’aturar.", "settings"),
            ("Simple i privat", "Sense comptes ni soroll: només un temporitzador clar per als teus mitjans.", "home"),
        ],
        "description": """GroveTimer és un temporitzador de son per a música, pòdcasts, vídeos i qualsevol reproducció multimèdia del dispositiu.

Tria quant de temps vols continuar escoltant, inicia el temporitzador i deixa que l’app pausi la reproducció automàticament quan s’acabi el temps. És ideal per adormir-te amb música, relaxar-te amb un pòdcast o mirar un vídeo sense preocupar-te que continuï sonant tota la nit.

Funcions principals:
• Temporitzador ràpid per a reproducció multimèdia.
• Notificació persistent amb temps restant i controls.
• Pausa automàtica de la reproducció quan s’acaba el temps.
• Opció de fade-out progressiu per reduir el volum abans d’aturar.
• Vibració opcional en finalitzar.
• Mode clar i fosc.
• Disseny net, sense comptes ni configuració innecessària.

Permisos:
GroveTimer utilitza notificacions per mostrar el temporitzador actiu i permetre pausar-lo o aturar-lo des de qualsevol pantalla. També sol·licita accés de control multimèdia per detectar i pausar la reproducció quan el temporitzador finalitza.

GroveTimer està pensada per ser petita, pràctica i respectuosa: configures el temps, prems iniciar i ja està.""",
    },
}

PALETTE = {
    "cream": (247, 243, 236),
    "leaf": (79, 175, 84),
    "sprout": (201, 230, 201),
    "root": (93, 64, 55),
    "ink": (27, 30, 28),
    "sage": (227, 234, 224),
}


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT_DIR / name), size=size)


FONTS = {
    "title": font("dm_sans_bold.ttf", 76),
    "subtitle": font("dm_sans_medium.ttf", 34),
    "small": font("dm_sans_regular.ttf", 28),
    "brand": font("playfair_display_bold.ttf", 72),
    "feature": font("dm_sans_bold.ttf", 46),
}


def draw_wrapped(draw: ImageDraw.ImageDraw, text: str, xy: tuple[int, int], width: int, font_obj, fill, spacing=8):
    avg = max(font_obj.getlength("abcdefghijklmnopqrstuvwxyz") / 26, 1)
    lines = []
    for paragraph in text.splitlines():
        wrap_at = max(8, int(width / avg))
        lines.extend(textwrap.wrap(paragraph, width=wrap_at) or [""])

    x, y = xy
    for line in lines:
        draw.text((x, y), line, font=font_obj, fill=fill)
        bbox = draw.textbbox((x, y), line, font=font_obj)
        y += bbox[3] - bbox[1] + spacing
    return y


def rounded_image(img: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, img.width, img.height), radius=radius, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def fit_cover(img: Image.Image, size: tuple[int, int]) -> Image.Image:
    scale = max(size[0] / img.width, size[1] / img.height)
    resized = img.resize((int(img.width * scale), int(img.height * scale)), Image.Resampling.LANCZOS)
    left = (resized.width - size[0]) // 2
    top = (resized.height - size[1]) // 2
    return resized.crop((left, top, left + size[0], top + size[1]))


def paste_phone(canvas: Image.Image, source: Path, center_x: int, top: int, height: int):
    raw = Image.open(source).convert("RGB")
    width = int(height * 9 / 20)
    screen = fit_cover(raw, (width, height))
    phone = Image.new("RGBA", (width + 34, height + 34), (0, 0, 0, 0))
    pd = ImageDraw.Draw(phone)
    pd.rounded_rectangle((0, 0, phone.width, phone.height), radius=54, fill=(31, 32, 30))
    phone.alpha_composite(rounded_image(screen.convert("RGBA"), 42), (17, 17))

    shadow = Image.new("RGBA", phone.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle((0, 0, phone.width, phone.height), radius=54, fill=(0, 0, 0, 80))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))

    x = center_x - phone.width // 2
    canvas.alpha_composite(shadow, (x + 4, top + 10))
    canvas.alpha_composite(phone, (x, top))


def draw_leaf_pattern(draw: ImageDraw.ImageDraw, variant: int):
    colors = [(201, 230, 201, 120), (79, 175, 84, 95), (93, 64, 55, 55)]
    offsets = [(760, 1250), (-80, 1420), (800, 360), (-120, 520), (760, 1650)]
    for i, (x, y) in enumerate(offsets):
        c = colors[(i + variant) % len(colors)]
        draw.ellipse((x, y, x + 520, y + 260), fill=c)
    for i in range(8):
        x = 52 + ((i * 157 + variant * 41) % 940)
        y = 390 + ((i * 211 + variant * 89) % 1270)
        draw.line((x, y, x + 78, y + 38), fill=(93, 64, 55, 55), width=4)


def make_phone_screenshot(locale: str, index: int, title: str, body: str, source_key: str, out: Path):
    bg = PALETTE["leaf"] if index in (0, 2) else PALETTE["cream"]
    canvas = Image.new("RGBA", (1080, 1920), bg + (255,))
    draw = ImageDraw.Draw(canvas)
    draw_leaf_pattern(draw, index)

    text_color = (255, 255, 255) if index in (0, 2) else PALETTE["ink"]
    draw.text((78, 100), title, font=FONTS["title"], fill=text_color)
    draw_wrapped(draw, body, (82, 205), 900, FONTS["subtitle"], text_color)

    if index == 0:
        paste_phone(canvas, SCREENSHOTS[source_key], 540, 465, 1260)
    elif index == 1:
        paste_phone(canvas, SCREENSHOTS[source_key], 540, 505, 1210)
    elif index == 2:
        paste_phone(canvas, SCREENSHOTS[source_key], 540, 505, 1210)
    elif index == 3:
        paste_phone(canvas, SCREENSHOTS[source_key], 540, 505, 1210)
    else:
        paste_phone(canvas, SCREENSHOTS[source_key], 540, 505, 1210)

    badge = Image.open(ICON).convert("RGBA").resize((126, 126), Image.Resampling.LANCZOS)
    draw.rounded_rectangle((74, 1718, 1006, 1832), radius=36, fill=(255, 255, 255, 224))
    canvas.alpha_composite(badge, (104, 1712))
    draw.text((252, 1755), "GroveTimer", font=FONTS["subtitle"], fill=PALETTE["root"])
    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(out, quality=95)


def make_feature(locale_data: dict, out: Path):
    canvas = Image.new("RGBA", (1024, 500), PALETTE["cream"] + (255,))
    draw = ImageDraw.Draw(canvas)
    draw.ellipse((720, -120, 1120, 280), fill=PALETTE["sprout"])
    draw.ellipse((-110, 260, 300, 620), fill=(79, 175, 84, 105))
    icon = Image.open(ICON).convert("RGBA").resize((178, 178), Image.Resampling.LANCZOS)
    canvas.alpha_composite(icon, (72, 160))
    draw.text((292, 124), "GroveTimer", font=FONTS["brand"], fill=PALETTE["root"])
    draw_wrapped(draw, locale_data["feature"], (296, 232), 620, FONTS["feature"], PALETTE["ink"])
    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(out, quality=95)


def write_text(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


def main():
    missing = [str(path) for path in SCREENSHOTS.values() if not path.exists()]
    if missing:
        raise SystemExit("Missing screenshots: " + ", ".join(missing))
    if not ICON.exists():
        raise SystemExit(f"Missing Play Store icon: {ICON}")

    for locale, data in LOCALES.items():
        base = METADATA / locale
        images = base / "images"
        shots = images / "phoneScreenshots"

        write_text(base / "title.txt", data["title"])
        write_text(base / "short_description.txt", data["short"])
        write_text(base / "full_description.txt", data["description"])
        images.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ICON, images / "icon.png")
        make_feature(data, images / "featureGraphic.png")

        for idx, (title, body, source_key) in enumerate(data["screens"], start=1):
            make_phone_screenshot(locale, idx - 1, title, body, source_key, shots / f"{idx:02d}.png")

    print(f"Generated Google Play metadata in {METADATA}")


if __name__ == "__main__":
    main()
