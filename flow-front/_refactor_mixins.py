"""Refactor dashboard and table pages to use shared SCSS mixins."""
import re

new_dashboard_style = '''<style lang="scss" scoped>
@import "~@/styles/common.scss";

// -- Layout --
.dashboard-container { @include page-container; display: flex; color: #1b1c1c; }
.main-content        { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content        { @include page-content; }

// -- Page Header --
.page-header  { @include page-header; }
.breadcrumb   { @include breadcrumb; margin-bottom: 8px; }
.page-heading { @include page-heading; }

// -- Buttons --
.btn-primary   { @include btn-primary; padding: 10px 24px; height: auto; box-shadow: 0 1px 2px rgba(0,0,0,0.1); }
.btn-secondary { @include btn-secondary; padding: 10px 24px; height: auto; }
.btn-danger    { @include btn-danger; padding: 10px 24px; height: auto; }
.btn-reset     { @include btn-reset; }
.btn-search    { @include btn-search; }
.btn-import    { @include btn-success; }
.btn-export    { @include btn-warning; }

// -- Stats Grid --
.stats-grid  { @include stats-grid(5); }
.stat-card   { @include stat-card; }
.stat-icon   { @include stat-icon; }
.stat-content{ @include stat-content; }
.stat-value  { @include stat-value; }
.stat-label  { @include stat-label; }
.stat-total     .stat-icon { background: linear-gradient(135deg, #334155, #334155); }
.stat-pending   .stat-icon { background: linear-gradient(135deg, #334155, #475569); }
.stat-rectifying.stat-icon { background: linear-gradient(135deg, #B45309, #D97706); }
.stat-overdue   .stat-icon { background: linear-gradient(135deg, #DC2626, #991B1B); }
.stat-completed .stat-icon { background: linear-gradient(135deg, #15803D, #166534); }

// -- Filter --
.filter-section        { @include filter-section; }
.filter-grid           { @include filter-grid; }
.filter-item           { @include filter-item; }
.filter-label          { @include filter-label; }
.filter-select,
.filter-input          { @include filter-input; }
.filter-actions        { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba($color-primary, 0.08); }
.filter-actions-left,
.filter-actions-right  { display: flex; gap: 8px; }

// -- Table --
.table-card            { @include data-table-section; }
.table-loading-wrapper { position: relative; }
.loading-overlay       { position: absolute; inset: 0; background: rgba(255,255,255,0.9); display: flex; align-items: center; justify-content: center; z-index: 10; border-radius: $radius-md; }
.loading-spinner       { text-align: center; color: $color-primary;
  .el-icon-loading { font-size: 40px; display: block; margin-bottom: 8px; }
  p { font-size: $font-size-base; color: #606266; margin: 0; }
}
.data-table            { @include data-table; }

// -- Typography Utilities --
.font-mono  { font-family: monospace; font-size: $font-size-base; }
.font-bold  { font-weight: 700; }
.text-center{ text-align: center; }
.text-right { text-align: right; }
.text-error { color: $color-danger; }
.text-icon  { font-size: 16px; vertical-align: middle; }

// -- Status Chips --
.status-chip      { @include status-chip; display: inline-flex; }
.status-pending   { @include chip-pending;   border: 1px solid $color-primary; }
.status-rectifying{ background: rgba(125,84,0,0.1); border: 1px solid #7d5400; color: #7d5400; }
.status-overdue   { @include chip-urgent;    border: 1px solid $color-danger; }
.status-completed { @include chip-active;    border: 1px solid $color-success; }
.priority-chip    { @include status-chip; display: inline-flex; }
.priority-high    { @include chip-urgent;    border: 1px solid $color-danger; }
.priority-medium  { background: rgba(125,84,0,0.1); border: 1px solid #7d5400; color: #7d5400; }
.priority-low     { @include chip-pending;   border: 1px solid $color-primary; }
.overdue-tag      { font-size: 10px; margin-left: 4px; padding: 1px 4px; background: $border-light; border-radius: $radius-sm; }

// -- Action Links --
.action-link { color: $color-primary; background: none; border: none; cursor: pointer; font-size: $font-size-base; margin-right: $space-2;
  &:hover { text-decoration: underline; }
  &.disabled { opacity: 0.3; cursor: not-allowed; }
}

// -- Pagination --
.pagination           { @include pagination-wrapper; justify-content: space-between; background: $neutral-50; }
.pagination-info      { font-size: $font-size-xs; color: $text-secondary; }
.pagination-controls  { display: flex; align-items: center; gap: 4px; }
.page-btn             { width: 32px; height: 32px; border-radius: $radius-sm; display: flex; align-items: center; justify-content: center; background: transparent; border: none; cursor: pointer; font-size: $font-size-base; transition: background $transition-fast;
  &:hover:not(.active):not(:disabled) { background: $neutral-200; }
  &.active { background: $color-primary; color: #fff; font-weight: 700; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.page-size-select { margin-left: $space-4; background: transparent; border: 1px solid $text-tertiary; border-radius: $radius-sm; font-size: $font-size-xs; padding: $space-1; }

// -- Progress Stats --
.stats-section  { padding: 0 $space-6 $space-6; }
.stats-card     { @include card; padding: $space-6; }
.stats-progress { display: flex; flex-direction: column; justify-content: space-between; }
.stats-title    { font-size: $font-size-lg; line-height: $space-6; font-weight: 600; margin-bottom: $space-4; }
.progress-bar   { height: 12px; background: $neutral-200; border-radius: 9999px; overflow: hidden; display: flex; }
.progress-segment{ height: 100%; }
.progress-legend { display: flex; justify-content: space-between; margin-top: $space-4; font-size: $font-size-xs; font-weight: 700; color: $text-secondary; }
.legend-item     { display: flex; align-items: center; }
.legend-dot      { width: 12px; height: 12px; border-radius: 50%; margin-right: $space-2; }
.bg-completed    { background-color: $color-success; }
.bg-rectifying   { background-color: #7d5400; }
.bg-pending      { background-color: $color-primary; }
.bg-overdue      { background-color: $color-danger; }

@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.spinning, .loading-icon { animation: spin 1s linear infinite; }
</style>'''

new_table_style = '''<style lang="scss" scoped>
@import "~@/styles/common.scss";

// -- Layout --
.table-container { @include page-container; }
.main-content    { padding: $space-6; }

// -- Page Header --
.page-header  { @include page-header; }
.breadcrumb   { @include breadcrumb; }
.page-heading { font-size: $font-size-2xl; font-weight: 600; color: $color-primary; margin: 0; }

// -- Stats Grid --
.stats-section { margin-bottom: $space-4; }
.stats-grid    { @include stats-grid(4); }
.stat-card     { @include stat-card; }
.stat-icon     { @include stat-icon; border-radius: $radius-lg; }
.stat-content  { @include stat-content; }
.stat-value    { @include stat-value; }
.stat-label    { @include stat-label; }
.stat-total   .stat-icon { background: linear-gradient(135deg, #334155, #334155); }
.stat-pending .stat-icon { background: linear-gradient(135deg, #616161, #9e9e9e); }
.stat-progress.stat-icon { background: linear-gradient(135deg, #475569, #334155); }
.stat-closed  .stat-icon { background: linear-gradient(135deg, #15803D, #22c55e); }

// -- Filter --
.filter-section { @include filter-section; }
.filter-grid    { @include filter-grid; }
.filter-item    { @include filter-item; }
.filter-label   { @include filter-label; }
.filter-select,
.filter-input   { @include filter-input; }
.status-btns    { @include status-btns; }
.status-btn     { @include status-btn; }
.btn-search     { @include btn-search; }
.btn-reset      { @include btn-reset; }

// -- Table --
.data-table { @include data-table; }

// -- Status Tags --
.status-chip    { @include status-chip; display: inline-flex; margin: 0 4px; }
.status-pending { @include chip-pending;   border: 1px solid $color-primary; }
.status-active  { @include chip-active;    border: 1px solid $color-success; }
.status-closed  { @include chip-closed;    border: 1px solid $text-tertiary; }

// -- Pagination --
.pagination { @include pagination-wrapper; }
</style>'''

def replace_style_block(filepath, new_style):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Find the style block: <style ...> ... </style>
    pattern = r'(<style\s+lang="scss"\s+scoped\s*>).*?(</style>)'
    match = re.search(pattern, content, re.DOTALL)
    
    if not match:
        print(f"ERROR: Could not find style block in {filepath}")
        return False
    
    old_len = len(match.group(0))
    new_content = content[:match.start()] + new_style + content[match.end():]
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    new_len = len(new_style)
    print(f"  {filepath}: style {old_len} -> {new_len} chars ({old_len - new_len} saved)")
    return True

# Apply
replace_style_block('src/views/dashboard/index.vue', new_dashboard_style)
replace_style_block('src/views/table/index.vue', new_table_style)
print("\nDone!")
