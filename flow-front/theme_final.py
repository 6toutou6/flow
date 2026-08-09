import os, re

# All replacements: (pattern, replacement)
# Patterns are compiled with re.IGNORECASE so #a20513 == #A20513
REPLACEMENTS = [
    # ============ PRIMARY RED → SLATE ============
    (r'#a20513\b', '#334155'),
    (r'#c91827\b', '#334155'),
    (r'#8f1d1d\b', '#475569'),
    (r'#8a0410\b', '#1E293B'),
    (r'rgba\(162,\s*5,\s*19,', 'rgba(51, 65, 85,'),

    # ============ WARM GRAYS → COOL ============
    (r'#f6f3f2\b', '#F1F5F9'),
    (r'#fcf9f8\b', '#F8FAFC'),
    (r'#f5f1f0\b', '#F1F5F9'),
    (r'#f0e3e1\b', '#E2E8F0'),
    (r'#fdf9f9\b', '#F8FAFC'),
    (r'#fbf6f5\b', '#F8FAFC'),
    (r'#f1e7e5\b', '#E2E8F0'),
    (r'#f5f1f0\b', '#F1F5F9'),

    # ============ WARM TEXT/BORDER → SLATE ============
    (r'#8a6a66\b', '#64748B'),
    (r'#d0a6a0\b', '#94A3B8'),
    (r'#e4beba\b', '#CBD5E1'),
    (r'#c8b3b0\b', '#94A3B8'),
    (r'#b8a6a3\b', '#94A3B8'),

    # ============ CHART PINK → INDIGO ============
    (r'#b83280\b', '#6366F1'),

    # ============ GREEN SUCCESS (mute, keep semantic) ============
    (r'#166534\b', '#15803D'),
    (r'#266d00\b', '#15803D'),
    (r'#67c23a\b', '#22C55E'),
    (r'#2f855a\b', '#047857'),
    (r'#2e7d32\b', '#15803D'),
    (r'#5aad32\b', '#22C55E'),
    (r'#c8e6c9\b', '#DCFCE7'),
    (r'#e8f5e9\b', '#ECFDF5'),

    # Green rgba
    (r'rgba\(38,\s*109,\s*0,', 'rgba(21, 128, 61,'),

    # ============ WARNING (muted amber) ============
    (r'#b7791f\b', '#B45309'),
    (r'#e6a23c\b', '#D97706'),
    (r'#e65100\b', '#EA580C'),
    (r'#92400e\b', '#B45309'),

    # Warning/amber rgba
    (r'rgba\(183,\s*121,\s*31,', 'rgba(180, 83, 9,'),

    # ============ DANGER RED (modern muted) ============
    (r'#991b1b\b', '#B91C1C'),
    (r'#ba1a1a\b', '#DC2626'),

    # ============ WARM GOLD/BROWN TEXT → SLATE ============
    (r'#b78f5c\b', '#64748B'),
    (r'#7a5c2e\b', '#475569'),
    (r'#7a5a12\b', '#475569'),
    (r'#8c6d1f\b', '#334155'),
    (r'#a0690c\b', '#475569'),
    (r'#9a7b2e\b', '#64748B'),
    (r'#8a6d3b\b', '#64748B'),
    (r'#b8860b\b', '#475569'),
    (r'#d3922f\b', '#D97706'),
    (r'#8c6d1f\b', '#334155'),

    # ============ WARM BG → COOL / MUTED BG ============
    (r'#fdf6ec\b', '#FEF3C7'),   # warm cream → amber-50
    (r'#fde8c8\b', '#DBEAFE'),   # peach → blue-100
    (r'#fff3e0\b', '#FFFBEB'),   # warm orange light → amber-50
    (r'#e8d9b8\b', '#CBD5E1'),   # warm tan → slate
    (r'#f0e0c0\b', '#E2E8F0'),   # warm beige → slate
    (r'#ecd9ae\b', '#CBD5E1'),   # golden beige → slate

    # ============ CORAL/ORANGE ELEMENTS ============
    (r'#ffab91\b', '#FED7AA'),   # coral → orange-200
    (r'#ffcc80\b', '#FDE68A'),   # light orange → amber-200
    (r'#ffba45\b', '#FBBF24'),   # bright amber → amber-400
    (r'#ffdad6\b', '#E2E8F0'),   # light pink → slate
]

def fix_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content
    changes = 0
    for pattern, replacement in REPLACEMENTS:
        new_content = re.sub(pattern, replacement, content, flags=re.IGNORECASE)
        if new_content != content:
            changes += content.count(re.findall(pattern, content, re.IGNORECASE)[0]) if re.findall(pattern, content, re.IGNORECASE) else 0
        content = new_content

    if content != original:
        with open(filepath, 'w', encoding='utf-8', newline='') as f:
            f.write(content)

    return content != original, content.count('\ufffd')

# Scan all Vue/SCSS files
total_changed = 0
total_fffd = 0
for root, dirs, files in os.walk('src'):
    if 'node_modules' in root:
        continue
    dirs[:] = [d for d in dirs if d not in ('demo', 'node_modules')]
    for fn in files:
        if not fn.endswith(('.vue', '.scss', '.css')):
            continue
        path = os.path.join(root, fn)
        changed, fffd = fix_file(path)
        if changed:
            total_changed += 1
            short = path.replace('\\', '/').replace('src/', '')
            print(f'✓ {short}')
        if fffd > 0:
            print(f'  ⚠ {fffd} U+FFFD in {short}')
            total_fffd += fffd

print(f'\nFiles changed: {total_changed}')
print(f'U+FFFD total: {total_fffd}')
print('PASS' if total_fffd == 0 else 'ISSUES FOUND')
