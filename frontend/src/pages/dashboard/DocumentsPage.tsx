import { useEffect, useMemo, useRef, useState } from "react";
import { CheckCircle2, ChevronDown, Clock3, Download, Eye, FileArchive, FileImage, FileText, Filter, History, Loader2, LockKeyhole, Plus, RotateCcw, Search, ShieldCheck, Signature, Upload, X } from "lucide-react";
import { api, authToken } from "../../services/api";
import { PageState, useApi, handleError } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";

type Role = "FARMER" | "SURVEYOR" | "INSURANCE_OFFICER" | "STATE_OFFICER" | "ADMIN" | "BANK_OFFICER" | string;
type DocumentRecord = Record<string, any>;

const roleLabel: Record<string, string> = { FARMER: "Farmer", SURVEYOR: "Surveyor", INSURANCE_OFFICER: "Insurance Officer", STATE_OFFICER: "State Officer", ADMIN: "Administrator", BANK_OFFICER: "Bank Officer" };
const scopeForRole: Record<string, string[]> = { FARMER: ["POLICY", "CLAIM", "LOSS"], SURVEYOR: ["SURVEY"] };
const entityOptions: Record<string, string[]> = { FARMER: ["POLICY", "CLAIM", "LOSS"], SURVEYOR: ["SURVEY"], INSURANCE_OFFICER: ["POLICY", "CLAIM", "LOSS", "SURVEY"], STATE_OFFICER: ["POLICY", "CLAIM", "LOSS", "SURVEY"], ADMIN: ["POLICY", "CLAIM", "LOSS", "SURVEY"], BANK_OFFICER: ["CLAIM"] };
const categoryOptions = ["EVIDENCE", "POLICY_DOCUMENT", "CLAIM_DOCUMENT", "IDENTITY", "SURVEY_REPORT", "OTHER"];

function listOf(value: any): any[] { if (Array.isArray(value)) return value; if (Array.isArray(value?.content)) return value.content; if (Array.isArray(value?.items)) return value.items; if (Array.isArray(value?.data)) return value.data; return []; }
function idOf(value: any): string | null { const id = value?.id ?? value?.entityId ?? value?.policyId ?? value?.claimId ?? value?.lossNotificationId ?? value?.surveyId; return id === undefined || id === null ? null : String(id); }
function dateLabel(value: any) { if (!value) return "—"; const date = new Date(value); return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" }); }
function fileIcon(mime = "", name = "") { return mime.startsWith("image/") ? <FileImage size={16} /> : name.toLowerCase().endsWith(".pdf") || mime === "application/pdf" ? <FileText size={16} /> : <FileArchive size={16} />; }
function statusClass(status: string) { return `status-pill ${String(status || "ACTIVE").toLowerCase()}`; }

export default function DocumentsPage() {
  const [role, setRole] = useState<Role>("USER");
  const [profile, setProfile] = useState<DocumentRecord | null>(null);
  const [term, setTerm] = useState("");
  const [entityFilter, setEntityFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selected, setSelected] = useState<DocumentRecord | null>(null);
  const [versions, setVersions] = useState<any[] | null>(null);
  const [busy, setBusy] = useState("");
  const [uploadOpen, setUploadOpen] = useState(false);
  const [meta, setMeta] = useState({ entityType: "CLAIM", entityId: "", category: "EVIDENCE", sensitivity: "NORMAL", expiryDate: "", retentionDate: "" });
  const fileRef = useRef<HTMLInputElement>(null);
  const versionFileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    try { const cached = JSON.parse(localStorage.getItem("agrishield_user") || "null"); if (cached?.role) setRole(cached.role); } catch { /* profile API below is authoritative */ }
    api.profile().then((value) => { setProfile(value); if (value?.role) setRole(value.role); }).catch(() => undefined);
  }, []);

  const accessibleIds = useApi(async () => {
    if (!scopeForRole[role]) return null;
    const calls = role === "SURVEYOR" ? [api.surveys()] : [api.policies(), api.losses(), api.claims()];
    const results = await Promise.allSettled(calls);
    // Scope discovery is defence-in-depth; it must not mask the documents API.
    if (results.some((result) => result.status === "rejected")) return null;
    return new Set(results.flatMap((result) => result.status === "fulfilled" ? listOf(result.value).map(idOf).filter(Boolean) as string[] : []));
  }, [role]);

  const entityChoices = useApi(async () => {
    if (meta.entityType === "POLICY") return listOf(await api.policies(0, 100)).map((p: any) => ({ id: p.id, label: `${p.policyNumber || `Policy #${p.id}`} · ${p.cropName || "Crop"}${p.farmerName ? ` · ${p.farmerName}` : ""}` }));
    if (meta.entityType === "LOSS") return listOf(await api.losses()).map((l: any) => ({ id: l.id, label: `Loss #${l.id} · ${l.lossType || "—"} · ${l.policy?.policyNumber || `Policy #${l.policyId ?? "—"}`}` }));
    if (meta.entityType === "SURVEY") return listOf(await api.surveys()).map((s: any) => ({ id: s.id, label: `Survey #${s.id} · ${s.farmerName || s.cropName || "—"}` }));
    if (meta.entityType === "CLAIM") return listOf(await api.claims(0, 100)).map((c: any) => ({ id: c.id, label: `Claim #${c.id} · ₹${Number(c.claimedAmount || 0).toLocaleString("en-IN")} · ${c.status}` }));
    return [];
  }, [meta.entityType]);
  const [signConfirm, setSignConfirm] = useState<DocumentRecord | null>(null);
  const [rollbackConfirm, setRollbackConfirm] = useState<{ doc: DocumentRecord; versionNumber: number } | null>(null);
  const signatureInfo = useApi(async () => (selected?.signed ? api.documents.signature(selected.id) : Promise.resolve(null)), [selected?.id, selected?.signed]);

  const documents = useApi(() => api.documents.search(term.trim()), [term]);
  const allRows = listOf(documents.data);
  const rows = useMemo(() => allRows.filter((doc) => {
    const entity = String(doc.entityType || "").toUpperCase();
    const allowed = !scopeForRole[role] || (accessibleIds.data && accessibleIds.data.has(String(doc.entityId)));
    return allowed && (entityFilter === "ALL" || entity === entityFilter) && (statusFilter === "ALL" || String(doc.status || "ACTIVE").toUpperCase() === statusFilter);
  }), [allRows, role, accessibleIds.data, entityFilter, statusFilter]);

  const options = entityOptions[role] || ["POLICY", "CLAIM", "LOSS", "SURVEY"];
  const canUpload = ["FARMER", "SURVEYOR", "INSURANCE_OFFICER", "STATE_OFFICER", "ADMIN", "BANK_OFFICER"].includes(role);
  const governance = ["INSURANCE_OFFICER", "STATE_OFFICER", "ADMIN"].includes(role);
  // Admin can view/manage/rollback documents but must not sign them (backend rejects it with 403).
  const canSign = ["INSURANCE_OFFICER", "STATE_OFFICER"].includes(role);
  const title = roleLabel[role] ? `${roleLabel[role]} document workspace` : "Document workspace";

  useEffect(() => { if (options.length && !options.includes(meta.entityType)) setMeta((old) => ({ ...old, entityType: options[0] })); }, [role]); // eslint-disable-line react-hooks/exhaustive-deps

  async function upload() {
    const file = fileRef.current?.files?.[0];
    if (!file || !meta.entityId) return toast.error("Choose a file and enter the related entity ID");
    setBusy("upload");
    try { await api.documents.upload(file, meta); toast.success("Document uploaded and audit recorded"); setUploadOpen(false); setMeta((old) => ({ ...old, entityId: "", expiryDate: "", retentionDate: "" })); if (fileRef.current) fileRef.current.value = ""; documents.reload(); } catch (error) { handleError(error); } finally { setBusy(""); }
  }
  async function loadVersions(doc: DocumentRecord) { setSelected(doc); setBusy(`versions-${doc.id}`); try { setVersions(listOf(await api.documents.versions(doc.id))); } catch (error) { handleError(error); } finally { setBusy(""); } }
  async function confirmSign() {
    const doc = signConfirm; if (!doc) return;
    setBusy(`sign-${doc.id}`);
    try { await api.documents.sign(doc.id, { signatureReference: "Approved through AgriShield", algorithm: "SHA256withRSA" }); toast.success("Document signed"); setSignConfirm(null); documents.reload(); }
    catch (error) { handleError(error); } finally { setBusy(""); }
  }
  async function confirmRollback() {
    const target = rollbackConfirm; if (!target) return;
    setBusy(`rollback-${target.doc.id}`);
    try { await api.documents.rollback(target.doc.id, target.versionNumber, "Governance rollback requested from Documents"); toast.success("Document rolled back"); setRollbackConfirm(null); setVersions(listOf(await api.documents.versions(target.doc.id))); documents.reload(); }
    catch (error) { handleError(error); } finally { setBusy(""); }
  }
  async function addVersion(doc: DocumentRecord) { const file = versionFileRef.current?.files?.[0]; if (!file) return toast.error("Choose a file for the new version"); setBusy(`version-${doc.id}`); try { await api.documents.version(doc.id, file, "New version uploaded from Documents"); toast.success("New version created"); if (versionFileRef.current) versionFileRef.current.value = ""; documents.reload(); loadVersions(doc); } catch (error) { handleError(error); } finally { setBusy(""); } }
  async function preview(doc: DocumentRecord) { try { const base = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, "") || "http://localhost:8080"; const response = await fetch(`${base}/api/documents/${doc.id}/content`, { headers: { Authorization: `Bearer ${authToken() || ""}` } }); if (!response.ok) throw new Error(`Preview unavailable (${response.status})`); const url = URL.createObjectURL(await response.blob()); window.open(url, "_blank", "noopener,noreferrer"); setTimeout(() => URL.revokeObjectURL(url), 60_000); } catch (error) { handleError(error); } }

  return <><style>{`.icon-action{height:30px;width:30px;border:1px solid #dfe8e2;background:#fff;border-radius:5px;display:flex;align-items:center;justify-content:center;color:#426754;position:relative;overflow:hidden}.icon-action input{position:absolute;inset:0;opacity:0;cursor:pointer}.doc-modal{width:min(720px,100%);max-height:calc(100vh - 40px);overflow:auto;padding-bottom:20px;box-shadow:0 24px 70px #0d3b2d33}.doc-detail-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:1px;margin:12px 19px 18px;border:1px solid #edf1ee;border-radius:7px;overflow:hidden;background:#edf1ee}.doc-detail-grid>div{background:#fff;padding:12px}.doc-detail-grid span,.version-row small{display:block;color:#8b9891;font-size:9px;margin-bottom:5px}.doc-detail-grid strong{display:block;color:#315849;font-size:10px;font-weight:600;word-break:break-word}.doc-mono{font-family:monospace;font-size:8px!important}.version-list{border-top:1px solid #edf1ee;padding:18px 19px 0}.version-row{display:flex;justify-content:space-between;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid #edf1ee}.version-row strong{display:block;color:#315849;font-size:10px}.version-row .secondary-button{flex:none}@media(max-width:600px){.doc-detail-grid{grid-template-columns:1fr}.doc-modal{max-height:calc(100vh - 20px)}}`}</style>
    <section className="page-heading"><div><p className="eyebrow">RECORDS / DOCUMENT CONTROL</p><h1>Documents & evidence</h1><p className="subhead">{title}. Every file stays connected to its policy, claim, loss or survey.</p></div><div className="heading-actions">{canUpload && <button className="primary-button" onClick={() => setUploadOpen(true)}><Plus size={14} /> Upload document</button>}</div></section>

    <section className="stats-grid compact" style={{ marginBottom: 20 }}><div className="stat-card"><div className="stat-icon mint"><FileText size={16} /></div><div className="stat-copy"><span>Visible documents</span><strong>{documents.loading ? "—" : rows.length}</strong><small className="positive">Live API result</small></div></div><div className="stat-card"><div className="stat-icon sand"><History size={16} /></div><div className="stat-copy"><span>Signed in view</span><strong>{documents.loading ? "—" : rows.filter((x) => x.signed).length}</strong><small className="muted">Current scope</small></div></div><div className="stat-card"><div className="stat-icon lilac"><Clock3 size={16} /></div><div className="stat-copy"><span>Retention / expiry set</span><strong>{documents.loading ? "—" : rows.filter((x) => x.retentionDate || x.expiryDate).length}</strong><small className="muted">Metadata present</small></div></div><div className="stat-card"><div className="stat-icon green"><ShieldCheck size={16} /></div><div className="stat-copy"><span>Access scope</span><strong style={{ fontSize: 15 }}>{scopeForRole[role] ? "Assigned" : "Governed"}</strong><small className="positive">{profile?.role ? roleLabel[role] || role : "Authenticated role"}</small></div></div></section>

    {uploadOpen && <section className="panel form-panel" style={{ marginBottom: 20 }}><div className="panel-heading"><div><span className="panel-kicker">CONTROLLED UPLOAD</span><h2>{role === "SURVEYOR" ? "Add field evidence" : "Register a document"}</h2></div><button className="secondary-button" onClick={() => setUploadOpen(false)}><X size={14} /> Close</button></div><div className="form-grid"><label>Entity type<select value={meta.entityType} onChange={(e) => setMeta({ ...meta, entityType: e.target.value, entityId: "" })}>{options.map((option) => <option key={option}>{option}</option>)}</select></label><label>Entity / reference<select value={meta.entityId} onChange={(e) => setMeta({ ...meta, entityId: e.target.value })}><option value="">{entityChoices.loading ? "Loading…" : `Select ${meta.entityType.toLowerCase()}`}</option>{(entityChoices.data || []).map((choice: any) => <option key={choice.id} value={choice.id}>{choice.label}</option>)}</select></label><label>Document category<select value={meta.category} onChange={(e) => setMeta({ ...meta, category: e.target.value })}>{categoryOptions.map((option) => <option key={option}>{option}</option>)}</select></label><label>Sensitivity<select value={meta.sensitivity} onChange={(e) => setMeta({ ...meta, sensitivity: e.target.value })}><option>NORMAL</option><option>CONFIDENTIAL</option><option>RESTRICTED</option></select></label><label>Expiry date<input type="date" value={meta.expiryDate} onChange={(e) => setMeta({ ...meta, expiryDate: e.target.value })} /></label><label>Retention date<input type="date" value={meta.retentionDate} onChange={(e) => setMeta({ ...meta, retentionDate: e.target.value })} /></label><label style={{ gridColumn: "1 / -1" }}>File · PDF, image, CSV or spreadsheet · max 20 MB<input ref={fileRef} type="file" accept="application/pdf,image/*,text/csv,.xlsx,.xls" /></label></div><button className="primary-button" disabled={busy === "upload"} onClick={upload}>{busy === "upload" ? <Loader2 size={14} className="spin" /> : <Upload size={14} />} Upload & audit</button></section>}

    <section className="panel table-panel"><div className="table-toolbar" style={{ flexWrap: "wrap" }}><div className="table-search" style={{ flex: "1 1 240px", width: "auto" }}><Search size={15} /><input value={term} onChange={(e) => setTerm(e.target.value)} placeholder="Search filename, category, entity or sensitivity…" /></div><div className="toolbar-actions"><label className="doc-filter"><Filter size={13} /><select value={entityFilter} onChange={(e) => setEntityFilter(e.target.value)}><option value="ALL">All entities</option>{options.map((option) => <option key={option}>{option}</option>)}</select></label><label className="doc-filter"><select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="ARCHIVED">Archived</option></select></label><button onClick={documents.reload}><RotateCcw size={13} /> Refresh</button></div></div><PageState loading={documents.loading || accessibleIds.loading} error={documents.error || accessibleIds.error} reload={() => { documents.reload(); accessibleIds.reload(); }} />{!documents.loading && !accessibleIds.loading && !documents.error && <div className="data-table"><div className="table-row table-header" style={{ gridTemplateColumns: "1.5fr .8fr .8fr .9fr .9fr .9fr 1.3fr" }}><span>Document</span><span>Type</span><span>Entity</span><span>Version</span><span>Uploaded</span><span>Status</span><span>Actions</span></div>{rows.length === 0 ? <div className="empty-row">No documents returned for this role and filter scope.</div> : rows.map((doc) => <div className="table-row" key={doc.id} style={{ gridTemplateColumns: "1.5fr .8fr .8fr .9fr .9fr .9fr 1.3fr", minWidth: 960 }}><span style={{ display: "flex", alignItems: "center", gap: 8 }}>{fileIcon(doc.mimeType, doc.originalFilename)}<span><b>{doc.originalFilename || doc.name || `Document ${doc.id}`}</b><small style={{ display: "block", color: "#9aa59f", marginTop: 3 }}>{doc.sizeBytes ? `${Math.ceil(doc.sizeBytes / 1024)} KB` : "Size not returned"}</small></span></span><span>{doc.category || "—"}</span><span>{doc.entityType || "—"} <small style={{ display: "block", color: "#9aa59f" }}>#{doc.entityId ?? "—"}</small></span><span>{doc.versionNumber ? `v${doc.versionNumber}` : "Current"}</span><span>{dateLabel(doc.createdAt || doc.uploadedAt)}</span><span><b className={statusClass(doc.status)}>{doc.signed ? "SIGNED" : (doc.status || "ACTIVE")}</b></span><span className="toolbar-actions"><button title="View details" onClick={() => { setSelected(doc); setVersions(null); }}><Eye size={13} /></button><button title="Version history" onClick={() => loadVersions(doc)} disabled={busy === `versions-${doc.id}`}><History size={13} /></button>{canUpload && <label title="Upload new version" className="icon-action"><Upload size={13} /><input ref={selected?.id === doc.id ? versionFileRef : undefined} type="file" accept="application/pdf,image/*,text/csv,.xlsx,.xls" onChange={() => addVersion(doc)} /></label>}{canSign && !doc.signed && <button title="Sign document" onClick={() => setSignConfirm(doc)} disabled={busy === `sign-${doc.id}`}><Signature size={13} /></button>}</span></div>)}</div>}</section>

    {selected && <div className="doc-modal-backdrop" onClick={() => setSelected(null)}><section className="panel doc-modal" onClick={(e) => e.stopPropagation()}><div className="panel-heading"><div><span className="panel-kicker">DOCUMENT DETAILS</span><h2>{selected.originalFilename || selected.name || `Document ${selected.id}`}</h2></div><button className="secondary-button" onClick={() => setSelected(null)}><X size={14} /></button></div><div className="doc-detail-grid"><div><span>Entity</span><strong>{selected.entityType || "—"} #{selected.entityId ?? "—"}</strong></div><div><span>Category</span><strong>{selected.category || "—"}</strong></div><div><span>Uploaded</span><strong>{dateLabel(selected.createdAt || selected.uploadedAt)}</strong></div><div><span>Verification</span><strong>{selected.signed ? `Signed${signatureInfo.data?.signerName ? ` by ${signatureInfo.data.signerName}` : ""}` : "Not signed"}</strong></div>{selected.signed && <div><span>Signed at</span><strong>{signatureInfo.loading ? "Loading…" : signatureInfo.data?.signedAt ? new Date(signatureInfo.data.signedAt).toLocaleString("en-IN") : "—"}</strong></div>}<div><span>Expiry</span><strong>{dateLabel(selected.expiryDate)}</strong></div><div><span>Retention</span><strong>{dateLabel(selected.retentionDate)} · {selected.retentionPolicyCode || "Policy not returned"}</strong></div><div><span>Checksum</span><strong className="doc-mono">{selected.checksum || "Not returned"}</strong></div><div><span>Status</span><strong>{selected.status || "ACTIVE"}</strong></div></div><div className="doc-modal-actions"><button className="primary-button" onClick={() => preview(selected)}><Eye size={14} /> Preview / open</button><a className="secondary-button" href={`${(import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, "") || "http://localhost:8080"}/api/documents/${selected.id}/content`} target="_blank" rel="noreferrer"><Download size={14} /> Content endpoint</a></div>{versions && <div className="version-list"><div className="panel-kicker">VERSION HISTORY</div>{versions.length === 0 ? <p className="muted">No version records returned.</p> : versions.map((version) => <div className="version-row" key={version.id || version.versionNumber}><div><strong>Version {version.versionNumber}</strong><small>{dateLabel(version.uploadedAt)} · {version.comment || "No comment"}</small></div><button className="secondary-button" disabled={!governance || busy === `rollback-${selected.id}`} onClick={() => setRollbackConfirm({ doc: selected, versionNumber: version.versionNumber })}><RotateCcw size={13} /> Roll back</button></div>)}</div>}</section></div>}

    {signConfirm && <div className="doc-modal-backdrop" onClick={() => setSignConfirm(null)}><section className="panel doc-confirm" onClick={(e) => e.stopPropagation()}><div className="panel-heading"><div><span className="panel-kicker">DIGITAL SIGNATURE</span><h2>Sign document</h2></div><button className="secondary-button" onClick={() => setSignConfirm(null)}><X size={14} /></button></div><p className="doc-confirm-body">This will mark <b>“{signConfirm.originalFilename || signConfirm.name || `document ${signConfirm.id}`}”</b> as digitally signed using your authenticated officer credentials. This cannot be undone.</p><div className="doc-modal-actions"><button className="secondary-button" onClick={() => setSignConfirm(null)} disabled={busy === `sign-${signConfirm.id}`}>Cancel</button><button className="primary-button" onClick={confirmSign} disabled={busy === `sign-${signConfirm.id}`}><Signature size={14} /> {busy === `sign-${signConfirm.id}` ? "Signing…" : "Sign document"}</button></div></section></div>}

    {rollbackConfirm && <div className="doc-modal-backdrop" onClick={() => setRollbackConfirm(null)}><section className="panel doc-confirm" onClick={(e) => e.stopPropagation()}><div className="panel-heading"><div><span className="panel-kicker">VERSION ROLLBACK</span><h2>Roll back document</h2></div><button className="secondary-button" onClick={() => setRollbackConfirm(null)}><X size={14} /></button></div><p className="doc-confirm-body">Roll back <b>“{rollbackConfirm.doc.originalFilename || rollbackConfirm.doc.name || `document ${rollbackConfirm.doc.id}`}”</b> to version <b>{rollbackConfirm.versionNumber}</b>? The current version will no longer be active.</p><div className="doc-modal-actions"><button className="secondary-button" onClick={() => setRollbackConfirm(null)} disabled={busy === `rollback-${rollbackConfirm.doc.id}`}>Cancel</button><button className="primary-button" onClick={confirmRollback} disabled={busy === `rollback-${rollbackConfirm.doc.id}`}><RotateCcw size={14} /> {busy === `rollback-${rollbackConfirm.doc.id}` ? "Rolling back…" : "Roll back"}</button></div></section></div>}
  </>;
}

/* Page-local styling keeps the change isolated to DocumentsPage. */
