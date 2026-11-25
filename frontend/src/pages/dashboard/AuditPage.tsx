import { useMemo, useState } from "react";
import { api } from "../../services/api";
import { PageState, useApi, DataTable } from "../../components/dashboard/ApiPage";

const distinct = (rows: any[], key: string) => Array.from(new Set(rows.map((r) => r[key]).filter(Boolean))).sort();
const dateTimeLabel = (value: any) => {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit", second: "2-digit" });
};

export default function AuditPage() {
  // Backend already returns these newest-first (findTop500ByOrderByTimestampDesc) with actorName/actorRole resolved.
  const q = useApi(api.admin.audit);
  const rows: any[] = Array.isArray(q.data) ? q.data : [];

  const [actor, setActor] = useState("");
  const [role, setRole] = useState("ALL");
  const [action, setAction] = useState("ALL");
  const [entity, setEntity] = useState("ALL");
  const [result, setResult] = useState("ALL");

  const roles = useMemo(() => distinct(rows, "actorRole"), [rows]);
  const actions = useMemo(() => distinct(rows, "action"), [rows]);
  const entities = useMemo(() => distinct(rows, "entityType"), [rows]);
  const results = useMemo(() => distinct(rows, "result"), [rows]);

  const filtered = useMemo(() => {
    const term = actor.trim().toLowerCase();
    return rows.filter((x) =>
      (!term || [x.actorName, x.action, x.entityType, x.purpose].some((v) => String(v || "").toLowerCase().includes(term))) &&
      (role === "ALL" || x.actorRole === role) &&
      (action === "ALL" || x.action === action) &&
      (entity === "ALL" || x.entityType === entity) &&
      (result === "ALL" || x.result === result)
    );
  }, [rows, actor, role, action, entity, result]);

  return <>
    <section className="page-heading">
      <div><p className="eyebrow">GOVERNANCE / AUDIT</p><h1>Audit Trail</h1><p className="subhead">Searchable, human-readable record of access and modification events. Newest first.</p></div>
    </section>

    <PageState {...q} />

    <section className="panel table-panel">
      <div className="table-toolbar" style={{ flexWrap: "wrap", gap: 10 }}>
        <div className="table-search" style={{ flex: "1 1 200px", width: "auto" }}>
          <input value={actor} onChange={(e) => setActor(e.target.value)} placeholder="Search actor, action, entity or purpose…" />
        </div>
        <div className="toolbar-actions">
          <label className="doc-filter"><select value={role} onChange={(e) => setRole(e.target.value)}><option value="ALL">All roles</option>{roles.map((r) => <option key={r} value={r}>{String(r).replaceAll("_", " ")}</option>)}</select></label>
          <label className="doc-filter"><select value={action} onChange={(e) => setAction(e.target.value)}><option value="ALL">All actions</option>{actions.map((a) => <option key={a} value={a}>{String(a).replaceAll("_", " ")}</option>)}</select></label>
          <label className="doc-filter"><select value={entity} onChange={(e) => setEntity(e.target.value)}><option value="ALL">All entities</option>{entities.map((en) => <option key={en} value={en}>{en}</option>)}</select></label>
          <label className="doc-filter"><select value={result} onChange={(e) => setResult(e.target.value)}><option value="ALL">All results</option>{results.map((r) => <option key={r} value={r}>{r}</option>)}</select></label>
        </div>
      </div>
      <DataTable
        widths="1.15fr 1fr .95fr 1.05fr .95fr 1.7fr .8fr"
        headers={["Time", "Actor", "Role", "Action", "Entity", "Purpose", "Result"]}
        rows={filtered.map((x) => [
          dateTimeLabel(x.timestamp),
          x.actorName || "System",
          x.actorRole ? String(x.actorRole).replaceAll("_", " ") : "—",
          x.action ? String(x.action).replaceAll("_", " ") : "—",
          x.entityType ? `${x.entityType}${x.entityId ? ` #${x.entityId}` : ""}` : "—",
          x.purpose || "—",
          x.result || "—",
        ])}
      />
    </section>
  </>;
}
