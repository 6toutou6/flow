import os

targets = {
    '#A20513': 'deep red', '#BA1A1A': 'danger red', '#E65100': 'dark orange',
    '#8F1D1D': 'dark red', '#991B1B': 'red', '#93000A': 'deep red',
    '#C91827': 'bright red', '#C71530': 'red', '#8A0410': 'deep red',
    '#B83280': 'pink-red', '#FFAB91': 'coral',
    '#B7791F': 'amber', '#E6A23C': 'gold', '#B78F5C': 'warm brown',
    '#D97706': 'orange', '#8C6D1F': 'dark gold', '#9A7B2E': 'gold brown',
    '#A0690C': 'brown', '#8A6D3B': 'warm brown', '#FFBA45': 'bright amber',
    '#FFCC80': 'light orange', '#D3922F': 'gold',
    '#92400E': 'dark amber', '#B8860B': 'dark gold', '#FFF3E0': 'warm orange light',
    '#FDF6EC': 'warm cream', '#FDE8C8': 'peach', '#F0E0C0': 'warm beige',
    '#ECD9AE': 'golden beige', '#E8D9B8': 'warm tan',
    '#67C23A': 'bright green', '#85FA51': 'neon green', '#5AAD32': 'green',
    '#2EA043': 'green', '#2F855A': 'dark green', '#266D00': 'dark green',
    '#166534': 'green', '#7D5400': 'dark brown', '#2E7D32': 'green',
    '#22C55E': 'green', '#BBF7D0': 'light green', '#E8F5E9': 'light green bg',
    '#C8E6C9': 'light green bg', '#F0FDF4': 'green tint',
    '#F6F3F2': 'warm gray', '#FCF9F8': 'warm near-white', '#F0E3E1': 'warm pink gray',
    '#C8B3B0': 'warm brown gray', '#FDF9F9': 'warm white', '#F5F1F0': 'warm gray',
    '#F0E8E7': 'warm gray', '#E0D2CF': 'warm gray', '#FBF6F5': 'warm gray',
    '#F1E7E5': 'warm gray', '#B8A6A3': 'warm gray', '#E4BEBA': 'warm pink',
    '#FFDAD6': 'light pink', '#D0A6A0': 'warm pink', '#C9A3A0': 'warm pink',
    '#8A6A66': 'warm dark', '#D3DCE6': 'warm gray',
    '#7A5C2E': 'brown', '#7A5A12': 'dark brown', '#5C514E': 'warm dark gray',
    '#9A8F8C': 'warm gray text',
}

found = {}
for root, dirs, fs in os.walk('src'):
    if 'node_modules' in root:
        continue
    dirs[:] = [d for d in dirs if d != 'demo' and d != 'node_modules']
    for fn in fs:
        if not fn.endswith(('.vue', '.scss', '.css')):
            continue
        path = os.path.join(root, fn)
        with open(path, 'r', encoding='utf-8') as f:
            c = f.read()
        for color, desc in targets.items():
            n = c.count(color)
            if n > 0:
                key = path.replace('\\', '/').replace('src/', '') + '>>' + color
                found[key] = (n, desc)

if found:
    for k, (n, desc) in sorted(found.items(), key=lambda x: -x[1][0]):
        print(f'{n:>3}x  {desc:<20}  {k}')
    total_files = len(set(k.split('>>')[0] for k in found))
    total_occ = sum(v[0] for v in found.values())
    print(f'\nTotal: {total_occ} occurrences across {total_files} unique files')
else:
    print('ALL CLEAN - no warm colors found!')
