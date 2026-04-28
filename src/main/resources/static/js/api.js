// ===== API Service =====
const API = '/api';

const api = {
    async get(url) {
        const r = await fetch(API + url);
        if (!r.ok) throw new Error(`GET ${url}: ${r.status}`);
        return r.json();
    },
    async post(url, data) {
        const r = await fetch(API + url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) });
        if (!r.ok) {
            const e = await r.json().catch(() => ({}));
            throw new Error(e.message || `POST ${url}: ${r.status}`);
        }
        return r.json();
    },
    async patch(url, data) {
        const r = await fetch(API + url, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) });
        if (!r.ok) {
            const e = await r.json().catch(() => ({}));
            throw new Error(e.message || `PATCH ${url}: ${r.status}`);
        }
        return r.json();
    },

    getComplaints(p = {}) {
        const q = new URLSearchParams();
        Object.entries(p).forEach(([k, v]) => { if (v !== '' && v !== null && v !== undefined) q.set(k, v); });
        return this.get('/complaints?' + q);
    },
    getComplaint(id) { return this.get('/complaints/' + id); },
    createComplaint(d) { return this.post('/complaints', d); },
    /** Payload: { status, note?, assignToAgentId?, notifyCustomer?, customerNotifyMessage?, notifyChannel? } or legacy (id, statusString, note) */
    updateStatus(id, payload, legacyNote) {
        if (typeof payload === 'string') {
            return this.patch('/complaints/' + id + '/status', { status: payload, note: legacyNote });
        }
        return this.patch('/complaints/' + id + '/status', payload);
    },
    assignAgent(id, agentId) { return this.post('/complaints/' + id + '/assign', { agentId }); },
    addComment(id, d) { return this.post('/complaints/' + id + '/comments', d); },
    classifyComplaint(id) { return this.post('/complaints/' + id + '/classify', {}); },
    notifyCustomer(id, d) { return this.post('/complaints/' + id + '/notify', d); },
    /** Escalate case: sets ESCALATED, assigns targetAgentId, optional internalNote, notifyAssignedAgent */
    escalateComplaint(id, d) { return this.post('/complaints/' + id + '/escalate', d); },

    getSummary() { return this.get('/analytics/summary'); },
    getTrend(days) { return this.get('/analytics/trend?days=' + (days || 7)); },

    getSla(id) { return this.get('/sla/' + id); },
    getBreached() { return this.get('/sla/breached'); },
    getSlaEscalated() { return this.get('/sla/escalated'); },
    getSlaRules() { return this.get('/sla/rules'); },

    getAgents() { return this.get('/agents'); },
    getCustomers() { return this.get('/customers'); },
    getCustomerComplaints(id, p, s) { return this.get('/customers/' + id + '/complaints?page=' + (p||0) + '&size=' + (s||20)); },

    async downloadCsv(from, to) {
        const r = await fetch(API + '/reports/export?from=' + from + '&to=' + to);
        const b = await r.blob();
        const u = URL.createObjectURL(b);
        const a = document.createElement('a'); a.href = u;
        a.download = 'complaints-' + from + '-to-' + to + '.csv';
        a.click(); URL.revokeObjectURL(u);
    }
};
