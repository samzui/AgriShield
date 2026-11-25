import { useMemo, useState } from "react";
import { Check, Plus } from "lucide-react";
import { api } from "../../services/api";
import { PageState, useApi, handleError } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";

const ENTITY_TYPES = ["CLAIM", "POLICY", "LOSS", "SURVEY"];
const listOf = (v: any): any[] => Array.isArray(v) ? v : (v?.content || []);

function recordLabel(type: string, r: any) {
  if (type === "CLAIM") return `Claim #${r.id} - ${r.policy?.policyNumber || `Policy #${r.policyId ?? "—"}`}`;
  if (type === "POLICY") return `${r.policyNumber || `Policy #${r.id}`} - ${r.farmerName || "Farmer"} - ${r.cropName || "Crop"}`;
  if (type === "LOSS") return `Loss #${r.id} - ${r.policy?.cropName || "Crop"} - ${r.lossType}`;
  return `Survey #${r.id} - ${r.farmerName || "Farmer"}`;
}

const dateTimeLabel = (value: any) => {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
};
const statusClass = (status: string) => {
  const s = String(status || "").toUpperCase();
  if (s === "REJECTED" || s === "CANCELLED") return "status-pill claimed-field";
  if (s === "COMPLETED" || s === "APPROVED") return "status-pill settled";
  return "status-pill enrolled";
};

export default function WorkflowsPage() {
  const workflows = useApi(api.workflows.list);
  const claimsQ = useApi(() => api.claims(0, 100));
  const policiesQ = useApi(() => api.policies(0, 100));
  const lossesQ = useApi(api.losses);
  const surveysQ = useApi(api.surveys);

  const [startOpen, setStartOpen] = useState(false);
  const [entityType, setEntityType] = useState("CLAIM");
  const [entityId, setEntityId] = useState("");
  const [starting, setStarting] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);

  const rows: any[] = Array.isArray(workflows.data) ? workflows.data : [];
  const recordOptions = useMemo(() => {
    const source = entityType === "CLAIM" ? listOf(claimsQ.data) : entityType === "POLICY" ? listOf(policiesQ.data) : entityType === "LOSS" ? listOf(lossesQ.data) : listOf(surveysQ.data);
    return source.map((r: any) => ({ id: r.id, label: recordLabel(entityType, r) }));
  }, [entityType, claimsQ.data, policiesQ.data, lossesQ.data, surveysQ.data]);

  async function start() {
    if (!entityId) return toast.error("Choose a record to start a workflow for.");
    setStarting(true);
    try {
      await api.workflows.start(entityType, entityType, Number(entityId));
      toast.success("Workflow started");
      setStartOpen(false); setEntityId("");
      workflows.reload();
    } catch (e) { handleError(e); } finally { setStarting(false); }
  }

  async function advance(id: number) {
    setBusyId(id);
    try {
      await api.workflows.advance(id);
      toast.success("Stage completed - workflow moved to the next step");
      workflows.reload();
    } catch (e) { handleError(e); } finally { setBusyId(null); }
  }

  return <>
    <style>{`
      .wf-list{display:flex;flex-direction:column;gap:16px}
      .wf-steps{display:flex;align-items:center;gap:9px;padding:0 24px 20px;flex-wrap:wrap}
      .wf-step{display:flex;align-items:center;gap:7px;font-size:10px;font-weight:600;color:#9aa89f}
      .wf-step span{display:grid;place-items:center;width:22px;height:22px;flex:none;border-radius:50%;background:#edf1ee;color:#8a9990;font-size:9px;font-weight:700}
      .wf-step.active{color:#254e3d}
      .wf-step.active span{background:#176044;color:#fff}
      .wf-step.done{color:#4f7b60}
      .wf-step.done span{background:#dcefd0;color:#3d6b52}
      .wf-step small{display:block;font-weight:400;color:#aab6ac;font-size:9px}
      .wf-step:not(:last-child):after{content:"";width:24px;height:1px;background:#e3ece4;margin-left:5px}
      .wf-meta-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px 18px;margin:0 24px 20px;padding:14px 16px;border:1px solid #d5e8da;border-radius:10px;background:#f5fbf7}
      .wf-meta-grid div{display:flex;flex-direction:column;gap:3px}
      .wf-meta-grid span{font-size:9px;color:#6a8577;letter-spacing:.04em}
      .wf-meta-grid b{font-size:11px;color:#244b3b;font-weight:600}
      .wf-actions{display:flex;justify-content:flex-end;padding:0 24px 22px}
    `}</style>

    <section className="page-heading">
      <div><p className="eyebrow">GOVERNANCE / WORKFLOWS</p><h1>Approval Workflows</h1><p className="subhead">Track every in-flight approval by case, stage, owner and deadline.</p></div>
      <div className="heading-actions"><button className="primary-button" onClick={() => setStartOpen(true)}><Plus size={14} /> Start workflow</button></div>
    </section>

    <PageState {...workflows} />

    {!workflows.loading && !workflows.error && (
      rows.length ? (
        <div className="wf-list">
          {rows.map((w: any) => (
            <section className="panel" key={w.id}>
              <div className="panel-heading">
                <div>
                  <span className="panel-kicker">{String(w.workflowType || "").replaceAll("_", " ")} WORKFLOW</span>
                  <h2>{w.record}</h2>
                </div>
                <b className={statusClass(w.status)}>{w.status === "IN_PROGRESS" ? `Stage ${w.currentStage} of ${w.totalStages}` : w.status}</b>
              </div>

              <div className="wf-steps">
                {(w.stages || []).map((s: any) => {
                  const isActive = w.status === "IN_PROGRESS" && s.order === w.currentStage;
                  return (
                    <div className={`wf-step${s.completed ? " done" : isActive ? " active" : ""}`} key={s.order}>
                      <span>{s.completed ? <Check size={12} /> : s.order}</span>
                      <div>{s.name}<small>{String(s.responsibleRole || "").replaceAll("_", " ")}</small></div>
                    </div>
                  );
                })}
              </div>

              <div className="wf-meta-grid">
                <div><span>OWNER (CURRENT STAGE)</span><b>{w.responsibleRole && w.responsibleRole !== "—" ? String(w.responsibleRole).replaceAll("_", " ") : "—"}</b></div>
                <div><span>NEXT STEP</span><b>{w.nextStep}</b></div>
                <div><span>DEADLINE</span><b>{w.status === "IN_PROGRESS" ? dateTimeLabel(w.dueAt) : "—"}</b></div>
              </div>

              {w.status === "IN_PROGRESS" && (
                <div className="wf-actions">
                  <button
                    className="primary-button"
                    disabled={!w.canAdvance || busyId === w.id}
                    title={w.canAdvance ? "" : `Only ${String(w.responsibleRole).replaceAll("_", " ")} can complete this stage`}
                    onClick={() => advance(w.id)}
                  >
                    {busyId === w.id ? "Working…" : `Complete: ${w.currentStageName}`}
                  </button>
                </div>
              )}
            </section>
          ))}
        </div>
      ) : (
        <div className="panel empty-row">No workflows started yet. Use "Start workflow" to begin approval tracking for a claim, policy, loss or survey.</div>
      )
    )}

    {startOpen && (
      <div className="doc-modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setStartOpen(false)}>
        <section className="panel doc-modal" onClick={(e) => e.stopPropagation()} style={{ width: "min(520px,100%)" }}>
          <div className="panel-heading"><div><span className="panel-kicker">NEW WORKFLOW</span><h2>Start an approval workflow</h2></div></div>
          <div className="form-grid" style={{ margin: "0 19px 16px" }}>
            <label>Record type
              <select value={entityType} onChange={(e) => { setEntityType(e.target.value); setEntityId(""); }}>
                {ENTITY_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </label>
            <label>Record
              <select value={entityId} onChange={(e) => setEntityId(e.target.value)}>
                <option value="">Select a record</option>
                {recordOptions.map((o) => <option key={o.id} value={o.id}>{o.label}</option>)}
              </select>
            </label>
          </div>
          <p className="report-note" style={{ margin: "0 19px 16px" }}>Uses the standard three-stage flow — Review, Approval and Completion — all owned by the Insurance Officer. Admin can monitor progress here but does not own a stage.</p>
          <div className="doc-modal-actions" style={{ justifyContent: "flex-end" }}>
            <button className="secondary-button" onClick={() => setStartOpen(false)} disabled={starting}>Cancel</button>
            <button className="primary-button" onClick={start} disabled={starting}>{starting ? "Starting…" : "Start workflow"}</button>
          </div>
        </section>
      </div>
    )}
  </>;
}
