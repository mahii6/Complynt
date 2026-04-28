// ===== Shared Layout & Helpers =====

function initLayout(activePage) {
    // Top NavBar
    const header = document.createElement('header');
    header.className = 'fixed top-0 right-0 left-64 h-16 flex justify-between items-center px-8 bg-surface/80 backdrop-blur-md z-40 border-b border-slate-200/50';
    header.innerHTML = `
        <div class="flex items-center gap-4 flex-1">
            <h1 class="text-xl font-bold tracking-tight font-headline">${activePage.charAt(0).toUpperCase() + activePage.slice(1)}</h1>
            <div class="relative w-full max-w-md ml-4">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline-variant text-xl">search</span>
                <input class="w-full bg-surface-container-low border-none rounded-full pl-10 pr-4 py-1.5 text-sm focus:ring-2 focus:ring-primary/20 placeholder:text-outline" placeholder="Search audit trails, entities..." type="text"/>
            </div>
        </div>
        <div class="flex items-center gap-4">
            <button class="p-2 text-on-surface-variant hover:bg-surface-container rounded-full transition-colors relative">
                <span class="material-symbols-outlined">notifications</span>
                <span class="absolute top-2 right-2 w-2 h-2 bg-primary rounded-full border-2 border-surface"></span>
            </button>
            <button class="p-2 text-on-surface-variant hover:bg-surface-container rounded-full transition-colors" onclick="window.location.href='/sla'">
                <span class="material-symbols-outlined">help_outline</span>
            </button>
            <div class="h-8 w-[1px] bg-outline-variant/30 mx-2"></div>
            <div class="flex items-center gap-2">
                <span class="text-[10px] text-on-surface-variant font-mono uppercase bg-surface-container px-2 py-0.5 rounded">ID: CMS-2026</span>
                <div class="flex items-center gap-3 ml-2">
                    <span class="text-xs font-bold text-on-surface">Admin User</span>
                    <div class="w-8 h-8 rounded-full bg-primary flex items-center justify-center text-white text-xs font-bold">A</div>
                </div>
            </div>
        </div>
    `;
    document.body.prepend(header);

    // SideNavBar
    const aside = document.createElement('aside');
    aside.className = 'fixed left-0 top-0 h-full w-64 flex flex-col p-4 glass-sidebar border-r border-primary-container/10 z-50';
    aside.innerHTML = `
        <div class="mb-8 px-2 flex items-center gap-3">
            <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white shadow-lg shadow-primary/20">
                <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' 1;">shield_with_heart</span>
            </div>
            <span class="text-xl font-bold tracking-tighter text-slate-900 font-headline">Complynt</span>
        </div>
        <nav class="flex-1 space-y-1">
            ${navItem('/', 'dashboard', 'dashboard', 'Dashboard', activePage === 'dashboard')}
            ${navItem('/complaints', 'verified_user', 'compliance', 'Complaints', activePage === 'complaints')}
            ${navItem('/agents', 'history_edu', 'audit', 'Agents', activePage === 'agents')}
            ${navItem('/customers', 'assessment', 'risk', 'Customers', activePage === 'customers')}
            ${navItem('/sla', 'monitoring', 'analytics', 'SLA Monitor', activePage === 'sla')}
            ${navItem('/reports', 'settings', 'settings', 'Reports', activePage === 'reports')}
        </nav>
        <div class="mt-auto pt-6 border-t border-primary-container/10">
            <button class="w-full primary-gradient text-white rounded-lg py-2.5 text-sm font-semibold flex items-center justify-center gap-2 shadow-lg shadow-primary/20 hover:opacity-90 active:scale-95 transition-all" onclick="window.location.href='/new-complaint'">
                <span class="material-symbols-outlined text-lg">add</span>
                New Complaint
            </button>
            <div class="mt-6 flex items-center gap-3 px-2">
                <div class="w-9 h-9 rounded-full bg-surface-container-highest flex items-center justify-center text-xs font-bold text-on-surface-variant">AU</div>
                <div class="flex flex-col min-w-0">
                    <span class="text-xs font-bold text-on-surface truncate">Admin User</span>
                    <span class="text-[10px] text-on-surface-variant uppercase tracking-wider font-bold">Enterprise Tier</span>
                </div>
            </div>
        </div>
    `;
    document.body.prepend(aside);

    // Adjust main content margin
    const main = document.querySelector('.main') || document.body.querySelector('main');
    if (main) {
        main.style.marginLeft = '256px'; // 64 * 4
        main.style.marginTop = '0';
        main.classList.add('pt-20', 'p-8', 'min-h-screen', 'bg-surface');
    }

    // Toast container
    if (!document.getElementById('toast-wrap')) {
        const tw = document.createElement('div');
        tw.id = 'toast-wrap';
        tw.className = 'fixed top-20 right-8 z-[9999] flex flex-col gap-3';
        document.body.appendChild(tw);
    }
}

function navItem(href, icon, dataIcon, label, isActive) {
    const activeClass = isActive 
        ? 'bg-primary-container/10 text-primary vibrant-pill shadow-sm shadow-primary/5' 
        : 'text-on-surface-variant hover:bg-primary-container/5 hover:text-primary';
    const iconFill = isActive ? "'FILL' 1" : "'FILL' 0";
    
    return `
        <a class="relative flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 group ${activeClass}" href="${href}">
            <span class="material-symbols-outlined text-[22px]" style="font-variation-settings: ${iconFill};">${icon}</span>
            <span class="font-medium text-sm tracking-tight">${label}</span>
        </a>
    `;
}

// ===== Toast =====
function showToast(msg, type = 'info') {
    const wrap = document.getElementById('toast-wrap');
    const t = document.createElement('div');
    const typeClasses = {
        success: 'bg-primary text-white shadow-primary/20',
        error: 'bg-error text-white shadow-error/20',
        info: 'bg-surface-container-highest text-on-surface-variant shadow-lg'
    };
    t.className = `px-6 py-3 rounded-xl text-sm font-bold shadow-2xl animate-toast-in ${typeClasses[type] || typeClasses.info}`;
    t.innerHTML = `
        <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-lg">${type === 'success' ? 'check_circle' : type === 'error' ? 'error' : 'info'}</span>
            ${msg}
        </div>
    `;
    wrap.appendChild(t);
    setTimeout(() => {
        t.classList.add('animate-toast-out');
        setTimeout(() => t.remove(), 500);
    }, 4000);
}

// Add animation styles for toast
const style = document.createElement('style');
style.textContent = `
    @keyframes toast-in {
        from { transform: translateX(100%) scale(0.9); opacity: 0; }
        to { transform: translateX(0) scale(1); opacity: 1; }
    }
    @keyframes toast-out {
        from { transform: translateX(0) scale(1); opacity: 1; }
        to { transform: translateX(100%) scale(0.9); opacity: 0; }
    }
    .animate-toast-in { animation: toast-in 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards; }
    .animate-toast-out { animation: toast-out 0.4s cubic-bezier(0.6, -0.28, 0.735, 0.045) forwards; }
    .vibrant-pill::before {
        content: ''; position: absolute; left: 0; top: 0; height: 100%; width: 3px; background-color: #0058c2; border-radius: 0 4px 4px 0;
    }
    .primary-gradient { background: linear-gradient(135deg, #0058c2 0%, #0070f2 100%); }
    .glass-sidebar { background: rgba(0, 112, 242, 0.05); backdrop-filter: blur(20px) saturate(150%); }
`;
document.head.appendChild(style);

// ===== Formatters =====
function formatDate(d) {
    if (!d) return '\u2014';
    const dt = new Date(d);
    return dt.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
        + ' ' + dt.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
}

function formatDateShort(d) {
    if (!d) return '\u2014';
    return new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
}

function statusBadge(s) {
    if (!s) return '';
    const colors = {
        OPEN: 'bg-primary/10 text-primary',
        IN_PROGRESS: 'bg-tertiary/10 text-tertiary',
        RESOLVED: 'bg-emerald-100 text-emerald-800',
        ESCALATED: 'bg-error/10 text-error',
        CLOSED: 'bg-slate-100 text-slate-600'
    };
    return `<span class="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider whitespace-nowrap ${colors[s] || 'bg-slate-100 text-slate-600'}">${s.replace(/_/g, ' ')}</span>`;
}

function severityBadge(s) {
    if (!s) return '';
    const labels = { P1: 'P1 Critical', P2: 'P2 High', P3: 'P3 Medium', P4: 'P4 Low' };
    const colors = {
        P1: 'bg-error text-white',
        P2: 'bg-tertiary text-white',
        P3: 'bg-primary text-white',
        P4: 'bg-slate-200 text-slate-700'
    };
    return `<span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-tight whitespace-nowrap ${colors[s] || 'bg-slate-200'}">${labels[s] || s}</span>`;
}

function slaBadge(sla) {
    if (!sla || !sla.status) return '<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-500">N/A</span>';
    const colors = {
        ON_TRACK: 'bg-emerald-100 text-emerald-800',
        AT_RISK: 'bg-amber-100 text-amber-800',
        BREACHED: 'bg-error text-white'
    };
    return `<span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase whitespace-nowrap ${colors[sla.status] || 'bg-slate-100'}">${sla.status.replace(/_/g, ' ')}</span>`;
}

function productLabel(p) {
    if (!p) return '\u2014';
    return p.replace(/_/g, ' ').replace(/\b\w/g, function(l) { return l.toUpperCase(); });
}

// ===== Chart Helper =====
function renderBarChart(containerId, data, colors) {
    var el = document.getElementById(containerId);
    if (!el) return;
    var entries = Object.entries(data);
    if (!entries.length) { el.innerHTML = '<div class="flex items-center justify-center p-8 bg-surface-container-low rounded-xl text-on-surface-variant text-xs font-bold uppercase tracking-widest">No data available</div>'; return; }
    var max = Math.max.apply(null, entries.map(function(e) { return e[1]; }).concat([1]));
    
    el.className = 'flex items-end gap-3 justify-between h-48 py-4';
    el.innerHTML = entries.map(function(entry, i) {
        var label = entry[0], value = entry[1];
        var pct = (value / max) * 100;
        var short = label.replace(/_/g, ' ');
        return `
            <div class="flex-1 flex flex-col items-center gap-2 group h-full justify-end">
                <div class="w-full bg-primary/10 rounded-t-md relative overflow-hidden" style="height:${Math.max(pct, 5)}%">
                    <div class="absolute inset-0 bg-primary/20 scale-y-0 group-hover:scale-y-100 transition-transform origin-bottom duration-300"></div>
                </div>
                <div class="text-[10px] font-bold text-on-surface-variant font-mono truncate w-full text-center" title="${short}">${short.length > 8 ? short.substring(0, 6) + '..' : short}</div>
                <div class="text-[10px] font-bold text-primary font-mono">${value}</div>
            </div>
        `;
    }).join('');
}
