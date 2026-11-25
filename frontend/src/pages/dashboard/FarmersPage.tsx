import { useMemo, useState } from 'react';
import { Sprout } from 'lucide-react';
import { api } from '../../services/api';
import { PageState, useApi, DataToolbar } from '../../components/dashboard/ApiPage';
import { toast } from 'sonner';

export default function FarmersPage() {
  const users = useApi(api.admin.users);
  const [search, setSearch] = useState('');
  const [busy, setBusy] = useState(false);

  const farmers: any[] = useMemo(
    () => (Array.isArray(users.data) ? users.data : []).filter((u: any) => String(u.role).toUpperCase() === 'FARMER'),
    [users.data]
  );
  const filtered = farmers.filter((f) => JSON.stringify(f).toLowerCase().includes(search.toLowerCase()));
  const activeCount = farmers.filter((f) => f.active).length;

  async function toggleStatus(f: any) {
    setBusy(true);
    try {
      await api.admin.setStatus(f.id, !f.active);
      toast.success(f.active ? 'Farmer account suspended' : 'Farmer account activated');
      users.reload();
    } catch (e: any) {
      toast.error(e.message);
    } finally {
      setBusy(false);
    }
  }

  return <>
    <section className="page-heading">
      <div>
        <p className="eyebrow">ADMINISTRATION / FARMER ACCOUNTS</p>
        <h1>Farmers</h1>
        <p className="subhead">View and manage registered farmer accounts across the platform.</p>
      </div>
    </section>

    <section className="stats-grid compact" style={{ marginBottom: 20 }}>
      <div className="stat-card"><div className="stat-icon mint"><Sprout size={16} /></div><div className="stat-copy"><span>Total farmers</span><strong>{users.loading ? '—' : farmers.length}</strong><small className="muted">Live from user accounts</small></div></div>
      <div className="stat-card"><div className="stat-icon green"><Sprout size={16} /></div><div className="stat-copy"><span>Active accounts</span><strong>{users.loading ? '—' : activeCount}</strong><small className="positive">Can currently sign in</small></div></div>
      <div className="stat-card"><div className="stat-icon rose"><Sprout size={16} /></div><div className="stat-copy"><span>Suspended</span><strong>{users.loading ? '—' : farmers.length - activeCount}</strong><small className="muted">Access disabled</small></div></div>
      <div className="stat-card"><div className="stat-icon lilac"><Sprout size={16} /></div><div className="stat-copy"><span>Email verified</span><strong>{users.loading ? '—' : farmers.filter((f) => f.verified).length}</strong><small className="muted">Completed OTP verification</small></div></div>
    </section>

    <PageState {...users} />

    <section className="panel table-panel">
      <DataToolbar value={search} onChange={setSearch} placeholder="Search farmer name, email or user ID…" />
      <div className="data-table">
        <div className="table-row table-header"><span>ID</span><span>Name</span><span>Email</span><span>Phone</span><span>Email verified</span><span>Status</span><span>Action</span></div>
        {filtered.length ? filtered.map((f) => (
          <div className="table-row" key={f.id}>
            <span>{f.id}</span>
            <span>{f.name}</span>
            <span>{f.email}</span>
            <span>{f.phone || '—'}</span>
            <span>{f.verified ? 'Verified' : 'Pending'}</span>
            <span><b className={`status-pill ${f.active ? '' : 'claimed-field'}`}>{f.active ? 'Active' : 'Suspended'}</b></span>
            <span><button className="row-action" disabled={busy} onClick={() => toggleStatus(f)}>{f.active ? 'Suspend' : 'Activate'}</button></span>
          </div>
        )) : <div className="empty-row">{users.loading ? 'Loading farmer accounts…' : 'No farmer accounts match this search.'}</div>}
      </div>
    </section>
  </>;
}
