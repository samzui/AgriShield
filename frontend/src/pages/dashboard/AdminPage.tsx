import { useMemo, useRef, useState } from "react";
import { api } from "../../services/api";
import { PageState, useApi, DataTable } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";

const ROLE_FILTERS = ["ALL", "FARMER", "SURVEYOR", "INSURANCE_OFFICER", "BANK_OFFICER", "STATE_OFFICER", "ADMIN"];
const dateLabel = (value: any) => {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
};

export default function AdminPage() {
  const dash = useApi(api.admin.dashboard);
  const users = useApi(api.admin.users);
  const perms = useApi(api.admin.permissions);
  const backup = useApi(api.admin.backup);
  const [tab, setTab] = useState("users");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [busy, setBusy] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<any>(null);
  const [deleting, setDeleting] = useState(false);
  const file = useRef<HTMLInputElement>(null);
  const d: any = dash.data || {};
  // Backend already returns users sorted newest-first by real createdAt; only role filtering happens client-side.
  const rows: any[] = Array.isArray(users.data) ? users.data : [];
  const filteredRows = useMemo(() => roleFilter === "ALL" ? rows : rows.filter((x) => x.role === roleFilter), [rows, roleFilter]);

  async function status(x: any) {
    setBusy(true);
    try { await api.admin.setStatus(x.id, !x.active); toast.success("User status updated"); users.reload(); }
    catch (e: any) { toast.error(e.message); } finally { setBusy(false); }
  }
  async function approve(x: any) {
    try { await api.admin.approve(x.id); toast.success("User approved"); users.reload(); }
    catch (e: any) { toast.error(e.message); }
  }
  async function reject(x: any) {
    try { await api.admin.reject(x.id); toast.success("User rejected"); users.reload(); }
    catch (e: any) { toast.error(e.message); }
  }
  async function confirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await api.admin.deleteUser(deleteTarget.id);
      toast.success(`${deleteTarget.name}'s account was permanently deleted`);
      setDeleteTarget(null);
      users.reload();
    } catch (e: any) { toast.error(e.message); } finally { setDeleting(false); }
  }
  async function imp(kind: string) {
    const f = file.current?.files?.[0];
    if (!f) return toast.error("Select a file");
    try { await (kind === "csv" ? api.admin.importCsv(f) : api.admin.importXml(f)); toast.success(`${kind.toUpperCase()} import completed`); }
    catch (e: any) { toast.error((e as Error).message); }
  }

  return <>
    <section className="page-heading">
      <div><p className="eyebrow">CONTROL ROOM / ADMINISTRATION</p><h1>Administration Center</h1><p className="subhead">Lifecycle management, runtime permissions, bulk data operations and resilience.</p></div>
    </section>
    <section className="stats-grid compact">{Object.entries(d).slice(0, 4).map(([k, v]) => <div className="stat-card" key={k}><div className="stat-icon lilac">◆</div><div className="stat-copy"><span>{k}</span><strong>{String(v)}</strong><small className="muted">Live admin metric</small></div></div>)}</section>
    <div className="admin-tabs">{["users", "permissions", "imports", "backup"].map((x) => <button className={tab === x ? "selected" : ""} key={x} onClick={() => setTab(x)}>{x}</button>)}</div>

    {tab === "users" && (
      <section className="panel table-panel">
        <div className="panel-heading">
          <div>
            <span className="panel-kicker">USER LIFECYCLE</span>
            <h2>Accounts, approvals &amp; access</h2>
            <p className="panel-description">Authority roles (Surveyor, Insurance Officer, Bank Officer, State Officer) require approval before they can sign in and reach their dashboard. Newest registrations first.</p>
          </div>
          <label className="doc-filter"><select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>{ROLE_FILTERS.map((r) => <option key={r} value={r}>{r === "ALL" ? "All roles" : r.replaceAll("_", " ")}</option>)}</select></label>
        </div>
        <PageState {...users} />
        <DataTable
          widths=".5fr 1.1fr 1.5fr .95fr 1fr .95fr .7fr .9fr 1.5fr"
          headers={["ID", "Name", "Email", "Phone", "Role", "Registered", "Active", "Approval", "Action"]}
          rows={filteredRows.map((x) => [
            x.id, x.name, x.email, x.phone || "—", x.role, dateLabel(x.createdAt), String(x.active),
            x.pendingApproval ? "Pending" : x.rejected ? "Rejected" : x.approved ? "Approved" : "—",
            <div className="toolbar-actions">
              <button disabled={busy} onClick={() => status(x)}>{x.active ? "Suspend" : "Activate"}</button>
              {x.pendingApproval && <><button onClick={() => approve(x)}>Approve</button><button onClick={() => reject(x)}>Reject</button></>}
              {!x.active && <button onClick={() => setDeleteTarget(x)}>Delete</button>}
            </div>,
          ])}
        />
      </section>
    )}
    {tab === "permissions" && (
      <section className="panel data-json">
        <span className="panel-kicker">RUNTIME RBAC MATRIX</span>
        <PageState {...perms} />
        <pre>{JSON.stringify(perms.data, null, 2)}</pre>
        <p className="report-note">Permissions are displayed from the backend permission store; changes remain subject to server-side authorization.</p>
      </section>
    )}
    {tab === "imports" && (
      <section className="panel form-panel">
        <div className="panel-heading"><div><span className="panel-kicker">BULK OPERATIONS</span><h2>Validated CSV / XML import</h2></div></div>
        <div className="form-grid"><label>File<input ref={file} type="file" accept=".csv,.xml" /></label></div>
        <div className="toolbar-actions"><button onClick={() => imp("csv")}>Import CSV</button><button onClick={() => imp("xml")}>Import XML</button></div>
      </section>
    )}
    {tab === "backup" && (
      <section className="panel data-json">
        <span className="panel-kicker">RESILIENCE / BACKUPS</span>
        <PageState {...backup} />
        <pre>{JSON.stringify(backup.data, null, 2)}</pre>
      </section>
    )}

    {deleteTarget && (
      <div className="doc-modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setDeleteTarget(null)}>
        <section className="panel doc-confirm" onClick={(e) => e.stopPropagation()}>
          <div className="panel-heading"><div><span className="panel-kicker">PERMANENT DELETE</span><h2>Delete this account?</h2></div></div>
          <p className="doc-confirm-body">
            This will permanently remove <b>{deleteTarget.name}</b> ({deleteTarget.email}) and its authentication
            records from the database. This cannot be undone. The same email can be used to register a brand-new
            account afterward, going through OTP verification again.
          </p>
          <div className="doc-modal-actions">
            <button className="secondary-button" onClick={() => setDeleteTarget(null)} disabled={deleting}>Cancel</button>
            <button className="primary-button danger" onClick={confirmDelete} disabled={deleting}>{deleting ? "Deleting…" : "Permanently delete"}</button>
          </div>
        </section>
      </div>
    )}
  </>;
}
