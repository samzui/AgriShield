import { useMemo } from "react";
import { api } from "../../services/api";
import { PageState, useApi, DataTable } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";

const dateLabel = (value: any) => {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
};

// Derived purely from real backend fields (dueDate, completed) - no invented regulatory states.
function statusOf(d: any) {
  if (d.completed) return "Completed";
  if (!d.dueDate) return "Upcoming";
  const due = new Date(d.dueDate);
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const in7 = new Date(today); in7.setDate(in7.getDate() + 7);
  if (due < today) return "Overdue";
  if (due <= in7) return "Due Soon";
  return "Upcoming";
}
const statusClass = (s: string) => {
  if (s === "Overdue") return "status-pill claimed-field";
  if (s === "Due Soon") return "status-pill enrolled";
  if (s === "Completed") return "status-pill settled";
  return "status-pill";
};

export default function CompliancePage() {
  const q = useApi(() => api.compliance.calendar(180));
  const rows: any[] = useMemo(
    () => (Array.isArray(q.data) ? [...q.data] : []).sort((a: any, b: any) => new Date(a.dueDate || 0).getTime() - new Date(b.dueDate || 0).getTime()),
    [q.data]
  );
  const overdueCount = rows.filter((x) => statusOf(x) === "Overdue").length;
  const dueSoonCount = rows.filter((x) => statusOf(x) === "Due Soon").length;
  const completedCount = rows.filter((x) => x.completed).length;

  async function complete(id: number) {
    try { await api.compliance.complete(id); toast.success("Deadline marked complete"); q.reload(); }
    catch (e: any) { toast.error(e.message); }
  }

  return <>
    <section className="page-heading">
      <div><p className="eyebrow">GOVERNANCE / COMPLIANCE</p><h1>Compliance Calendar</h1><p className="subhead">Regulatory deadlines, renewals and evidence readiness - nearest due date first.</p></div>
    </section>

    <section className="stats-grid compact">
      <div className="stat-card"><div className="stat-icon mint">◆</div><div className="stat-copy"><span>Total requirements</span><strong>{q.loading ? "—" : rows.length}</strong><small className="muted">Next 180 days</small></div></div>
      <div className="stat-card"><div className="stat-icon rose">◆</div><div className="stat-copy"><span>Overdue</span><strong>{q.loading ? "—" : overdueCount}</strong><small className="muted">Past due date</small></div></div>
      <div className="stat-card"><div className="stat-icon sand">◆</div><div className="stat-copy"><span>Due soon</span><strong>{q.loading ? "—" : dueSoonCount}</strong><small className="muted">Within 7 days</small></div></div>
      <div className="stat-card"><div className="stat-icon green">◆</div><div className="stat-copy"><span>Completed</span><strong>{q.loading ? "—" : completedCount}</strong><small className="positive">Evidence filed</small></div></div>
    </section>

    <PageState {...q} />

    <section className="panel table-panel">
      <DataTable
        widths="1.2fr 1.6fr .9fr 1fr .85fr .95fr"
        headers={["Requirement", "What needs to be done", "Due date", "Owner", "Status", "Action"]}
        rows={rows.map((x: any) => [
          x.title,
          x.notes || "—",
          dateLabel(x.dueDate),
          x.regulator || "—",
          <b className={statusClass(statusOf(x))}>{statusOf(x)}</b>,
          x.completed ? "—" : <button className="row-action" onClick={() => complete(x.id)}>Mark complete</button>,
        ])}
      />
    </section>
  </>;
}
