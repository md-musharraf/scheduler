import math
import os
from PIL import Image, ImageDraw

def create_nothing_chronometer(size=512, is_foreground_only=False):
    # Render at 4x resolution for super-sampled anti-aliasing
    scale = 4
    canvas_size = size * scale
    center = canvas_size / 2.0
    
    if is_foreground_only:
        # Transparent background for adaptive icon foreground
        img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    else:
        # AMOLED black background for brand logo and standalone icon
        img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 255))
        
    draw = ImageDraw.Draw(img)
    
    # Safe radius for launcher icons: central 65% (radius ~ 32% of canvas_size)
    # For standalone logo, we can use slightly larger (~38% of canvas_size)
    dial_radius = canvas_size * (0.33 if is_foreground_only else 0.38)
    
    # Colors
    NOTHING_RED = (215, 25, 33, 255)       # #D71921 signature Nothing Red
    NOTHING_WHITE = (255, 255, 255, 255)
    NOTHING_GRAY = (145, 145, 145, 255)
    NOTHING_DARK_GRAY = (65, 65, 65, 255)
    NOTHING_SUBTLE = (35, 35, 35, 255)
    
    # 1. Subtle Outer Frame Ring
    outer_frame_r = dial_radius * 1.08
    draw.ellipse(
        [center - outer_frame_r, center - outer_frame_r, center + outer_frame_r, center + outer_frame_r],
        outline=NOTHING_SUBTLE,
        width=int(2.5 * scale)
    )
    
    # 2. Outer Segmented Interval Ring
    # Draw 60 precision tick dots / marks
    for i in range(60):
        angle_deg = i * 6 - 90
        angle_rad = math.radians(angle_deg)
        
        is_major = (i % 5 == 0)
        is_cardinal = (i % 15 == 0)
        is_top = (i == 0)
        
        if is_top:
            # 12 o'clock Nothing Red precision dot
            dot_r = dial_radius * 1.0
            x = center + dot_r * math.cos(angle_rad)
            y = center + dot_r * math.sin(angle_rad)
            r = 10.0 * scale
            draw.ellipse([x - r, y - r, x + r, y + r], fill=NOTHING_RED)
        elif is_major:
            # 5-min intervals: Clean dots
            dot_r = dial_radius * 1.0
            x = center + dot_r * math.cos(angle_rad)
            y = center + dot_r * math.sin(angle_rad)
            r = (6.0 if is_cardinal else 4.5) * scale
            col = NOTHING_WHITE if is_cardinal else NOTHING_GRAY
            draw.ellipse([x - r, y - r, x + r, y + r], fill=col)
        else:
            # Micro ticks
            r1 = dial_radius * 0.97
            r2 = dial_radius * 1.03
            x1 = center + r1 * math.cos(angle_rad)
            y1 = center + r1 * math.sin(angle_rad)
            x2 = center + r2 * math.cos(angle_rad)
            y2 = center + r2 * math.sin(angle_rad)
            draw.line([(x1, y1), (x2, y2)], fill=NOTHING_DARK_GRAY, width=int(2 * scale))

    # 3. Active Interval Arc (from 12 o'clock / -90 deg to ~45 deg / 3:30)
    # Clean Nothing White active sweep
    arc_r = dial_radius * 0.86
    arc_box = [center - arc_r, center - arc_r, center + arc_r, center + arc_r]
    # Track background
    draw.arc(arc_box, start=0, end=360, fill=NOTHING_SUBTLE, width=int(4 * scale))
    # Active progress segment
    draw.arc(arc_box, start=-90, end=45, fill=NOTHING_WHITE, width=int(5 * scale))
    
    # 4. Inner Concentric Dial Ring
    inner_r = dial_radius * 0.70
    draw.ellipse(
        [center - inner_r, center - inner_r, center + inner_r, center + inner_r],
        outline=NOTHING_DARK_GRAY,
        width=int(2 * scale)
    )
    
    # 5. Classic Minimalist Hands
    # Minute Hand (Pointing at ~ 45 degrees / 1:30 mark)
    min_angle = math.radians(45 - 90)
    min_length = dial_radius * 0.58
    min_x = center + min_length * math.cos(min_angle)
    min_y = center + min_length * math.sin(min_angle)
    draw.line([(center, center), (min_x, min_y)], fill=NOTHING_WHITE, width=int(7 * scale))
    
    # Chrono / Second Hand (Pointing precisely at 12 o'clock towards Nothing Red dot)
    sec_angle = math.radians(-90)
    sec_length = dial_radius * 0.92
    sec_x = center + sec_length * math.cos(sec_angle)
    sec_y = center + sec_length * math.sin(sec_angle)
    # Counterweight (extends backwards by 15%)
    cw_length = dial_radius * 0.18
    cw_x = center - cw_length * math.cos(sec_angle)
    cw_y = center - cw_length * math.sin(sec_angle)
    draw.line([(cw_x, cw_y), (sec_x, sec_y)], fill=NOTHING_RED, width=int(3.5 * scale))
    
    # 6. Central Pivot Hub
    hub_outer_r = 14.0 * scale
    draw.ellipse(
        [center - hub_outer_r, center - hub_outer_r, center + hub_outer_r, center + hub_outer_r],
        fill=NOTHING_RED
    )
    hub_inner_r = 6.0 * scale
    draw.ellipse(
        [center - hub_inner_r, center - hub_inner_r, center + hub_inner_r, center + hub_inner_r],
        fill=(0, 0, 0, 255)
    )

    # Downsample using high quality Lanczos filter
    final_img = img.resize((size, size), Image.Resampling.LANCZOS)
    return final_img

def main():
    base_res = "app/src/main/res"
    
    # 1. Update in-app Brand Logo: app_brand_logo.png (512x512)
    brand_logo = create_nothing_chronometer(512, is_foreground_only=False)
    brand_logo_path = os.path.join(base_res, "drawable", "app_brand_logo.png")
    brand_logo.save(brand_logo_path, "PNG")
    print(f"Updated: {brand_logo_path}")
    
    # 2. Update Adaptive Foreground: ic_launcher_foreground_img.png (512x512)
    fg_icon = create_nothing_chronometer(512, is_foreground_only=True)
    fg_path = os.path.join(base_res, "drawable-nodpi", "ic_launcher_foreground_img.png")
    fg_icon.save(fg_path, "PNG")
    print(f"Updated: {fg_path}")
    
    # 3. Update legacy mipmap icons (hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi)
    mipmap_sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    
    for folder, px in mipmap_sizes.items():
        folder_path = os.path.join(base_res, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Standard icon (with black background)
        icon_img = create_nothing_chronometer(px, is_foreground_only=False)
        icon_file = os.path.join(folder_path, "ic_launcher.webp")
        icon_img.save(icon_file, "WEBP", quality=100)
        
        # Round icon (circular crop with black background)
        round_mask = Image.new("L", (px, px), 0)
        mask_draw = ImageDraw.Draw(round_mask)
        mask_draw.ellipse([0, 0, px, px], fill=255)
        round_img = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        round_img.paste(icon_img, (0, 0), round_mask)
        round_file = os.path.join(folder_path, "ic_launcher_round.webp")
        round_img.save(round_file, "WEBP", quality=100)
        
        print(f"Updated {folder}: {px}x{px}")

if __name__ == "__main__":
    main()
