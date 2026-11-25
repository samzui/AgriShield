import { useState } from "react";
import { ChevronDown, ChevronRight, Plus, RotateCcw } from "lucide-react";
import { api } from "../../services/api";
import { PageState, useApi, handleError } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";

const dateTimeLabel = (value: any) => {
  if (!value) return "Never checked";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
};
const statusClass = (status: string) => {
  const s = String(status || "").toUpperCase();
  if (s === "ACTIVE") return "status-pill settled";
  if (s === "DEGRADED") return "status-pill enrolled";
  if (s === "FAILED") return "status-pill claimed-field";
  return "status-pill";
};

export default function IntegrationsPage() {
  const q = useApi(api.integrations.status);
  const rows: any[] = Array.isArray(q.data) ? q.data : [];
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [checking, setChecking] = useState("");
  const [registerOpen, setRegisterOpen] = useState(false);
  const [form, setForm] = useState({ code: "", name: "", baseUrl: "", environment: "staging" });
  const [registering, setRegistering] = useState(false);

  async function check(code: string) {
    setChecking(code);
    try { await api.integrations.check(code); toast.success(`${code} - connectivity check completed`); q.reload(); }
    catch (e) { handleError(e); q.reload(); } finally { setChecking(""); }
  }
  async function register() {
    if (!form.code || !form.name) return toast.error("Provide at least a code and a name.");
    setRegistering(true);
    try { await api.integrations.register(form); toast.success("Integration registered"); setRegisterOpen(false); setForm({ code: "", name: "", baseUrl: "", environment: "staging" }); q.reload(); }
    catch (e) { handleError(e); } finally { setRegistering(false); }
  }

  return <>
    <style>{`.int-row{display:flex;flex-direction:column;border-bottom:1px solid #edf1ee}.int-row:last-child{border-bottom:0}.int-summary{display:grid;grid-template-columns:1.4fr 1fr .9fr 1fr 1.2fr .9fr;align-items:center;gap:12px;padding:16px 18px;font-size:10px;color:#5f7168}.int-summary button.expand{border:0;background:transparent;color:#587166;display:flex;align-items:center;gap:5px;padding:0;font-size:10px}.int-name{display:block;font-weight:600;color:#315849;font-size:11px}.int-details{margin:0 18px 16px;padding:14px 16px;border:1px solid #e3ece4;border-radius:8px;background:#f9fbf9;display:grid;grid-template-columns:repeat(2,1fr);gap:10px 18px}.int-details div{display:flex;flex-direction:column;gap:3px}.int-details span{font-size:9px;color:#8b9891}.int-details b{font-size:10px;color:#2b5543;font-family:monospace}`}</style>

    <section className="page-heading">
      <div><p className="eyebrow">CONNECTIVITY / INTEGRATIONS</p><h1>External Systems</h1><p className="subhead">Connection health for external PMFBY, DBT and satellite services.</p></div>
      <div className="heading-actions"><button className="primary-button" onClick={() => setRegisterOpen(true)}><Plus size={14} /> Register integration</button></div>
    </section>

    <PageState {...q} />

    <section className="panel table-panel">
      <div className="int-summary" style={{ background: "#fbfcfb", textTransform: "uppercase", letterSpacing: ".08em", fontSize: 8, color: "#9ca9a2", fontWeight: 600 }}>
        <span>Integration</span><span>Environment</span><span>Status</span><span>Last checked</span><span>Connection</span><span>Action</span>
      </div>
      {rows.length ? rows.map((x: any) => (
        <div className="int-row" key={x.code}>
          <div className="int-summary">
            <span>
              <button className="expand" onClick={() => setExpanded({ ...expanded, [x.code]: !expanded[x.code] })}>
                {expanded[x.code] ? <ChevronDown size={13} /> : <ChevronRight size={13} />}
                <span className="int-name">{x.displayName}</span>
              </button>
            </span>
            <span>{x.environment || "—"}</span>
            <span><b className={statusClass(x.status)}>{x.status}</b></span>
            <span>{dateTimeLabel(x.lastCheckedAt)}</span>
            <span>{x.failureCount > 0 ? `${x.failureCount} recent failure${x.failureCount === 1 ? "" : "s"}` : "Healthy"}</span>
            <span><button className="row-action" disabled={checking === x.code} onClick={() => check(x.code)}><RotateCcw size={11} style={{ verticalAlign: "middle", marginRight: 4 }} />{checking === x.code ? "Checking…" : "Check"}</button></span>
          </div>
          {expanded[x.code] && (
            <div className="int-details">
              <div><span>PROVIDER / SYSTEM CODE</span><b>{x.code}</b></div>
              <div><span>CONNECTION / AUTH TYPE</span><b>{x.authType || "NONE"}</b></div>
              <div style={{ gridColumn: "1 / -1" }}><span>BASE URL</span><b>{x.baseUrl || "Not configured"}</b></div>
            </div>
          )}
        </div>
      )) : <div className="empty-row">No integrations registered by the backend yet.</div>}
    </section>

    {registerOpen && (
      <div className="doc-modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setRegisterOpen(false)}>
        <section className="panel doc-modal" onClick={(e) => e.stopPropagation()} style={{ width: "min(480px,100%)" }}>
          <div className="panel-heading"><div><span className="panel-kicker">NEW INTEGRATION</span><h2>Register a connection</h2></div></div>
          <div className="form-grid" style={{ margin: "0 19px 16px" }}>
            <label>Code<input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} placeholder="e.g. PMFBY_DACFW" /></label>
            <label>Display name<input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="e.g. PMFBY DAC&FW Gateway" /></label>
            <label>Base URL (optional)<input value={form.baseUrl} onChange={(e) => setForm({ ...form, baseUrl: e.target.value })} placeholder="https://…" /></label>
            <label>Environment<select value={form.environment} onChange={(e) => setForm({ ...form, environment: e.target.value })}><option value="staging">Staging</option><option value="production">Production</option><option value="demo">Demo</option></select></label>
          </div>
          <div className="doc-modal-actions" style={{ justifyContent: "flex-end" }}>
            <button className="secondary-button" onClick={() => setRegisterOpen(false)} disabled={registering}>Cancel</button>
            <button className="primary-button" onClick={register} disabled={registering}>{registering ? "Registering…" : "Register"}</button>
          </div>
        </section>
      </div>
    )}
  </>;
}
