"""Fix collapse arrow consistency across all Vue files."""
import os, re

files = [
    'src/views/taskprocess/index.vue',
    'src/views/flowdispatch/index.vue',
    'src/views/flowdispatch/PersonView.vue',
]

for fp in files:
    with open(fp, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Fix rotate(-180deg) → rotate(180deg)
    new_content = content.replace('rotate(-180deg)', 'rotate(180deg)')
    
    if new_content != content:
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'  Fixed {fp}: rotate(-180deg) → rotate(180deg)')

# Also unify EditTask card-fold to use primary color and cursor
fp = 'src/views/flowdispatch/EditTask.vue'
with open(fp, 'r', encoding='utf-8') as f:
    content = f.read()

# Update .card-fold style
old = '.card-fold { font-size: 16px; color: #c0c4cc; transition: all .25s;\n  &.folded { transform: rotate(180deg); color: $primary; }'
new = '.card-fold { font-size: 16px; color: #94A3B8; transition: all .25s; cursor: pointer;\n  &.folded { transform: rotate(180deg); color: $primary; }\n  &:hover { color: $primary; }'
content = content.replace(old, new)

with open(fp, 'w', encoding='utf-8') as f:
    f.write(content)
print(f'  Updated {fp}: card-fold style (hover + cursor)')

# Unify arrow styles in flowdispatch/index.vue
fp = 'src/views/flowdispatch/index.vue'
with open(fp, 'r', encoding='utf-8') as f:
    content = f.read()

# Add cursor + hover to .tp-arrow
old = '.tp-arrow { color: #909399; font-size: 14px; flex-shrink: 0; transition: transform .25s; }'
new = '.tp-arrow { color: #94A3B8; font-size: 14px; flex-shrink: 0; transition: transform .25s; cursor: pointer; }\n.task-panel:hover .tp-arrow { color: $primary; }'
if old in content:
    content = content.replace(old, new)

with open(fp, 'w', encoding='utf-8') as f:
    f.write(content)
print(f'  Updated {fp}: tp-arrow (hover + cursor)')

# Same for taskprocess/index.vue
fp = 'src/views/taskprocess/index.vue'
with open(fp, 'r', encoding='utf-8') as f:
    content = f.read()

old = '.tp-arrow { color: #909399; font-size: 14px; flex-shrink: 0; transition: transform .25s; }'
new = '.tp-arrow { color: #94A3B8; font-size: 14px; flex-shrink: 0; transition: transform .25s; cursor: pointer; }\n.task-panel:hover .tp-arrow { color: $primary; }'
if old in content:
    content = content.replace(old, new)

with open(fp, 'w', encoding='utf-8') as f:
    f.write(content)
print(f'  Updated {fp}: tp-arrow (hover + cursor)')

# Unify PersonView arrows  
fp = 'src/views/flowdispatch/PersonView.vue'
with open(fp, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix .pp-arrow
content = content.replace(
    '.pp-arrow { color: #909399; font-size: 14px; flex-shrink: 0; transition: transform .25s; }',
    '.pp-arrow { color: #94A3B8; font-size: 14px; flex-shrink: 0; transition: transform .25s; cursor: pointer; }'
)
# Fix .pdp-arrow
content = content.replace(
    '.pdp-arrow { color: #909399; font-size: 14px; flex-shrink: 0; transition: transform .25s; }',
    '.pdp-arrow { color: #94A3B8; font-size: 14px; flex-shrink: 0; transition: transform .25s; cursor: pointer; }'
)
# Fix .rec-toggle
content = content.replace(
    '.rec-toggle { color: #909399; font-size: 14px; flex-shrink: 0; transition: transform .25s; }',
    '.rec-toggle { color: #94A3B8; font-size: 14px; flex-shrink: 0; transition: transform .25s; cursor: pointer; }'
)

with open(fp, 'w', encoding='utf-8') as f:
    f.write(content)
print(f'  Updated {fp}: person arrows (hover + cursor + rotate fix)')

print('\nDone!')
