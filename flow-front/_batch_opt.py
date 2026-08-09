import os

changes_log = []

def replace_in_file(path, old, new, desc):
    """Read file, replace old->new, write if changed, log it."""
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    if old not in content:
        return 0
    new_content = content.replace(old, new)
    if new_content == content:
        return 0
    with open(path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    count = content.count(old)
    changes_log.append(f"  [{os.path.basename(path)}] {desc}: {count}x")
    return count

def check_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

total = 0

# ============================================================
# H1: AppMain height 50->56 + Breadcrumb line-height
# ============================================================
total += replace_in_file(
    'src/layout/components/AppMain.vue',
    'calc(100vh - 50px)', 'calc(100vh - 56px)',
    'H1: AppMain min-height 50->56'
)
total += replace_in_file(
    'src/layout/components/AppMain.vue',
    'padding-top: 50px;', 'padding-top: 56px;',
    'H1: AppMain padding-top 50->56'
)
total += replace_in_file(
    'src/components/Breadcrumb/index.vue',
    'line-height: 50px;', 'line-height: 56px;',
    'H1: Breadcrumb line-height 50->56'
)

# ============================================================
# H5: Clear warm-tone rgba(228,190,186,x) borders
# ============================================================
warm_rgba_map = {
    'rgba(228, 190, 186, 0.3)': 'rgba(51, 65, 85, 0.08)',
    'rgba(228,190,186,0.3)': 'rgba(51,65,85,0.08)',
    'rgba(228, 190, 186, 0.4)': 'rgba(51, 65, 85, 0.1)',
    'rgba(228,190,186,0.4)': 'rgba(51,65,85,0.1)',
    'rgba(228, 190, 186, 0.5)': 'rgba(51, 65, 85, 0.12)',
    'rgba(228,190,186,0.5)': 'rgba(51,65,85,0.12)',
}

for root, dirs, files in os.walk('src'):
    if 'node_modules' in root: continue
    dirs[:] = [d for d in dirs if d not in ('demo', 'node_modules')]
    for fn in files:
        if not fn.endswith(('.vue', '.scss', '.css')): continue
        path = os.path.join(root, fn)
        for old, new in warm_rgba_map.items():
            total += replace_in_file(path, old, new, 'H5: warm border->slate')

# ============================================================
# H6+L7: Unify page backgrounds + table header colors
# ============================================================
for root, dirs, files in os.walk('src'):
    if 'node_modules' in root: continue
    dirs[:] = [d for d in dirs if d not in ('demo', 'node_modules')]
    for fn in files:
        if not fn.endswith(('.vue', '.scss', '.css')): continue
        path = os.path.join(root, fn)
        total += replace_in_file(path, 'background-color: #F5F7FA', 'background-color: #F8FAFC', 'H6: page bg->#F8FAFC')
        total += replace_in_file(path, 'background: #F5F7FA', 'background: #F8FAFC', 'H6: page bg->#F8FAFC')
        total += replace_in_file(path, 'background-color: #FAFAFA', 'background-color: #F1F5F9', 'L7: table header->#F1F5F9')
        total += replace_in_file(path, 'background: #FAFAFA', 'background: #F1F5F9', 'L7: table header->#F1F5F9')

# ============================================================
# M1: Remove 'Inter' font-family from business pages
# ============================================================
for root, dirs, files in os.walk('src'):
    if 'node_modules' in root: continue
    dirs[:] = [d for d in dirs if d not in ('demo', 'node_modules')]
    for fn in files:
        if not fn.endswith('.vue'): continue
        path = os.path.join(root, fn)
        total += replace_in_file(path, "font-family: 'Inter', sans-serif;", '', 'M1: Remove Inter font')

# ============================================================
# M2: Page heading 30px->24px
# ============================================================
for root, dirs, files in os.walk('src'):
    if 'node_modules' in root: continue
    dirs[:] = [d for d in dirs if d not in ('demo', 'node_modules')]
    for fn in files:
        if not fn.endswith(('.vue', '.scss', '.css')): continue
        path = os.path.join(root, fn)
        total += replace_in_file(path, 'font-size: 30px; line-height: 38px;', 'font-size: 24px; line-height: 32px;', 'M2: heading->24px')
        total += replace_in_file(path, 'font-size: 30px;', 'font-size: 24px;', 'M2: heading 30->24')

# ============================================================
# M7: chip border-radius 2px->4px
# ============================================================
for root, dirs, files in os.walk('src'):
    if 'node_modules' in root: continue
    dirs[:] = [d for d in dirs if d not in ('demo', 'node_modules')]
    for fn in files:
        if not fn.endswith(('.vue', '.scss', '.css')): continue
        path = os.path.join(root, fn)
        total += replace_in_file(path, 'border-radius: 2px;', 'border-radius: 4px;', 'M7: chip radius 2->4')

# ============================================================
# L1: Fix mixin.scss scrollbar colors
# ============================================================
total += replace_in_file('src/styles/mixin.scss', 'background: #d3dce6;', 'background: #E2E8F0;', 'L1: mixin scrollbar track')
total += replace_in_file('src/styles/mixin.scss', 'background: #99a9bf;', 'background: rgba(51, 65, 85, 0.25);', 'L1: mixin scrollbar thumb')

# ============================================================
# L2: Fix Dashboard stat-icon gradients
# ============================================================
total += replace_in_file(
    'src/views/dashboard/index.vue',
    '.stat-pending .stat-icon { background: linear-gradient(135deg, #334155, #E2E8F0); }',
    '.stat-pending .stat-icon { background: linear-gradient(135deg, #334155, #475569); }',
    'L2: stat-pending gradient'
)
total += replace_in_file(
    'src/views/dashboard/index.vue',
    '.stat-rectifying .stat-icon { background: linear-gradient(135deg, #7d5400, #FBBF24); }',
    '.stat-rectifying .stat-icon { background: linear-gradient(135deg, #B45309, #D97706); }',
    'L2: stat-rectifying gradient'
)
total += replace_in_file(
    'src/views/dashboard/index.vue',
    '.stat-overdue .stat-icon { background: linear-gradient(135deg, #DC2626, #E2E8F0); }',
    '.stat-overdue .stat-icon { background: linear-gradient(135deg, #DC2626, #991B1B); }',
    'L2: stat-overdue gradient'
)
total += replace_in_file(
    'src/views/dashboard/index.vue',
    '.stat-completed .stat-icon { background: linear-gradient(135deg, #15803D, #85fa51); }',
    '.stat-completed .stat-icon { background: linear-gradient(135deg, #15803D, #166534); }',
    'L2: stat-completed gradient'
)

# ============================================================
# L3: Fix btn-import/btn-export hover states
# ============================================================
total += replace_in_file(
    'src/views/dashboard/index.vue',
    '.btn-import {\n  padding: 0 16px;\n  height: 36px;\n  border: 1px solid #22C55E;\n  border-radius: 4px;\n  font-size: 13px;\n  font-weight: bold;\n  color: white;\n  background: #22C55E;\n  cursor: pointer;\n  transition: all 0.2s;\n  display: flex;\n  align-items: center;\n  gap: 4px;\n\n  &:hover {\n    background: #22C55E;\n  }\n}',
    '.btn-import {\n  padding: 0 16px;\n  height: 36px;\n  border: 1px solid #16A34A;\n  border-radius: 6px;\n  font-size: 13px;\n  font-weight: bold;\n  color: white;\n  background: #22C55E;\n  cursor: pointer;\n  transition: all 0.2s;\n  display: flex;\n  align-items: center;\n  gap: 4px;\n\n  &:hover {\n    background: #16A34A;\n    border-color: #15803D;\n  }\n}',
    'L3: btn-import hover'
)
total += replace_in_file(
    'src/views/dashboard/index.vue',
    '.btn-export {\n  padding: 0 16px;\n  height: 36px;\n  border: 1px solid #D97706;\n  border-radius: 4px;\n  font-size: 13px;\n  font-weight: bold;\n  color: white;\n  background: #D97706;\n  cursor: pointer;\n  transition: all 0.2s;\n  display: flex;\n  align-items: center;\n  gap: 4px;\n\n  &:hover {\n    background: #D97706;\n  }\n}',
    '.btn-export {\n  padding: 0 16px;\n  height: 36px;\n  border: 1px solid #B45309;\n  border-radius: 6px;\n  font-size: 13px;\n  font-weight: bold;\n  color: white;\n  background: #D97706;\n  cursor: pointer;\n  transition: all 0.2s;\n  display: flex;\n  align-items: center;\n  gap: 4px;\n\n  &:hover {\n    background: #B45309;\n    border-color: #92400E;\n  }\n}',
    'L3: btn-export hover'
)

# ============================================================
# L5: Fix Sidebar warm text colors
# ============================================================
total += replace_in_file('src/layout/components/Sidebar/index.vue', 'color: #1E1A19;', 'color: #0F172A;', 'L5: sidebar title warm->slate')
total += replace_in_file('src/layout/components/Sidebar/index.vue', 'color: #9A8F8C;', 'color: #94A3B8;', 'L5: sidebar subtitle warm->slate')
total += replace_in_file('src/layout/components/Sidebar/index.vue', 'color: #5C514E !important;', 'color: #334155 !important;', 'L5: sidebar menu text warm->slate')
total += replace_in_file('src/layout/components/Sidebar/index.vue', 'color: #BEB4B2;', 'color: #94A3B8;', 'L5: sidebar version warm->slate')
total += replace_in_file('src/layout/components/Navbar.vue', 'color: #5C514E;', 'color: #334155;', 'L5: navbar user text warm->slate')
total += replace_in_file('src/layout/components/Navbar.vue', 'color: #DCD5D3;', 'color: #94A3B8;', 'L5: navbar divider warm->slate')
total += replace_in_file('src/styles/sidebar.scss', '&::-webkit-scrollbar-track-piece { background: #F1F5F9; }', '&::-webkit-scrollbar-track-piece { background: transparent; }', 'L5: sidebar popup scrollbar track')

# ============================================================
# L8: Fix login password toggle icon
# ============================================================
old_l8 = "passwordType === 'password' ? 'el-icon-view' : 'el-icon-loading'"
new_l8 = "passwordType === 'password' ? 'el-icon-view' : 'el-icon-view pwd-visible'"
total += replace_in_file('src/views/login/index.vue', old_l8, new_l8, 'L8: password icon fixed')

# Add pwd-visible style
total += replace_in_file(
    'src/views/login/index.vue',
    '.pwd-toggle {\n      cursor: pointer;\n      color: #94A3B8;\n      font-size: 18px;\n      transition: color 0.2s;\n\n      &:hover {\n        color: #334155;\n      }\n    }',
    '.pwd-toggle {\n      cursor: pointer;\n      color: #94A3B8;\n      font-size: 18px;\n      transition: color 0.2s;\n\n      &:hover {\n        color: #334155;\n      }\n      &.pwd-visible { opacity: 0.55; }\n    }',
    'L8: pwd-visible style'
)

# ============================================================
# M3+M5: Unify btn-reset/btn-search border-radius 4px->6px
# ============================================================
for root, dirs, files in os.walk('src'):
    if 'node_modules' in root: continue
    dirs[:] = [d for d in dirs if d not in ('demo', 'node_modules')]
    for fn in files:
        if not fn.endswith('.vue'): continue
        path = os.path.join(root, fn)
        if 'login' in path or 'Sidebar' in path:
            continue
        total += replace_in_file(path,
            '.btn-reset {\n  padding: 0 16px;\n  height: 36px;\n  border: 1px solid #CBD5E1;\n  border-radius: 4px;',
            '.btn-reset {\n  padding: 0 16px;\n  height: 36px;\n  border: 1px solid #CBD5E1;\n  border-radius: 6px;',
            'M3: btn-reset radius 4->6')
        total += replace_in_file(path,
            '.btn-reset {\n  padding: 0 16px;\n  height: 36px;\n  border: 1px solid $border;\n  border-radius: 4px;',
            '.btn-reset {\n  padding: 0 16px;\n  height: 36px;\n  border: 1px solid $border;\n  border-radius: 6px;',
            'M3: btn-reset radius 4->6')
        total += replace_in_file(path,
            '.btn-search {\n  padding: 0 16px;\n  height: 36px;\n  border: none;\n  border-radius: 4px;',
            '.btn-search {\n  padding: 0 16px;\n  height: 36px;\n  border: none;\n  border-radius: 6px;',
            'M3: btn-search radius 4->6')

# ============================================================
# M5: Fix filter-select/filter-input border-radius 4px->6px
# ============================================================
for root, dirs, files in os.walk('src'):
    if 'node_modules' in root: continue
    dirs[:] = [d for d in dirs if d not in ('demo', 'node_modules')]
    for fn in files:
        if not fn.endswith('.vue'): continue
        path = os.path.join(root, fn)
        if 'login' in path: continue
        total += replace_in_file(path,
            '.filter-select,\n.filter-input {\n  height: 36px;\n  border: 1px solid #CBD5E1;\n  border-radius: 4px;',
            '.filter-select,\n.filter-input {\n  height: 36px;\n  border: 1px solid #CBD5E1;\n  border-radius: 6px;',
            'M5: filter radius 4->6')

# ============================================================
# Fix status-btn border-radius 4px->6px
# ============================================================
total += replace_in_file(
    'src/views/table/index.vue',
    '.status-btn {\n  padding: 0 8px;\n  height: 36px;\n  border: 1px solid #CBD5E1;\n  border-radius: 4px;',
    '.status-btn {\n  padding: 0 8px;\n  height: 36px;\n  border: 1px solid #CBD5E1;\n  border-radius: 6px;',
    'M3: status-btn radius 4->6'
)

# ============================================================
# Unified filter-section border-radius 8px->10px
# ============================================================
for root, dirs, files in os.walk('src'):
    if 'node_modules' in root: continue
    dirs[:] = [d for d in dirs if d not in ('demo', 'node_modules')]
    for fn in files:
        if not fn.endswith('.vue'): continue
        path = os.path.join(root, fn)
        total += replace_in_file(path,
            'border: 1px solid #CBD5E1;\n  border-radius: 8px;\n  padding: 16px;\n  margin-bottom: 16px;',
            'border: 1px solid #CBD5E1;\n  border-radius: 10px;\n  padding: 16px;\n  margin-bottom: 16px;',
            'Filter section radius 8->10')

# ============================================================
# Summary
# ============================================================
print("=" * 60)
print("BULK OPTIMIZATION COMPLETE")
print("=" * 60)
for log in changes_log:
    print(log)
print(f"\nTotal replacements: {total}")
