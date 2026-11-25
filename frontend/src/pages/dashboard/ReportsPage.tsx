import { useMemo, useState } from "react";
import { Plus } from "lucide-react";
import { api } from "../../services/api";
import { useApi, PageState, handleError } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";

const money = (v: any) => `₹${Number(v || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
const listOf = (x: any): any[] => Array.isArray(x) ? x : Array.isArray(x?.content) ? x.content : [];
const REPORT_FORMATS = ["csv", "xlsx", "pdf"];
const SEASONS = ["KHARIF", "RABI", "ZAID"];
const POLICY_STATUSES = ["ENROLLED", "ACTIVE", "CLAIM_FILED", "SETTLED", "LAPSED"];
const LOSS_STATUSES = ["SUBMITTED", "SURVEYOR_ASSIGNED", "SURVEYED", "PROCESSED"];
const SURVEY_STATUSES = ["ASSIGNED", "IN_PROGRESS", "SUBMITTED", "VERIFIED"];
const CLAIM_STATUSES = ["INITIATED", "UNDER_REVIEW", "APPROVED", "REJECTED", "PAID"];
const LOSS_TYPES = ["DROUGHT", "FLOOD", "PEST", "HAILSTORM", "FIRE", "OTHER"];
const REPORT_TYPES = [
  { key: "PMFBY", label: "PMFBY Summary / Disbursement", exportable: true },
  { key: "POLICY", label: "Policy", exportable: true },
  { key: "LOSS", label: "Loss Notifications", exportable: true },
  { key: "SURVEY", label: "Surveys", exportable: true },
  { key: "CLAIM", label: "Claims", exportable: true },
];
const FREQUENCIES = ["DAILY", "WEEKLY", "MONTHLY"];
const DAYS_OF_WEEK = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];
const EMPTY_SCHEDULE = { name: "", format: "pdf", recipientList: "", frequency: "MONTHLY", dayOfWeek: "MON", dayOfMonth: "1", hour: "8" };

function buildCron({ frequency, dayOfWeek, dayOfMonth, hour }: any) {
  const h = String(Math.min(23, Math.max(0, Number(hour) || 0)));
  if (frequency === "DAILY") return `0 0 ${h} * * ?`;
  if (frequency === "WEEKLY") return `0 0 ${h} ? * ${dayOfWeek}`;
  return `0 0 ${h} ${dayOfMonth} * ?`;
}
function describeCron(cron: string) {
  const parts = String(cron || "").trim().split(/\s+/);
  if (parts.length < 6) return cron || "—";
  const [, , h, dom, , dow] = parts;
  const time = `${h.padStart(2, "0")}:00`;
  if (dom !== "*" && dom !== "?") return `Monthly, day ${dom} at ${time}`;
  if (dow && dow !== "*" && dow !== "?") return `Weekly on ${dow} at ${time}`;
  return `Daily at ${time}`;
}
function inRange(dateStr: any, from: string, to: string) {
  if (!from) return true;
  if (!dateStr) return false;
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return true;
  if (d < new Date(from)) return false;
  if (to && d > new Date(`${to}T23:59:59`)) return false;
  return true;
}

export default function ReportsPage() {
  const scheduled = useApi(api.scheduledReports);
  const policiesQ = useApi(() => api.policies(0, 100));
  const lossesQ = useApi(api.losses);
  const surveysQ = useApi(api.surveys);
  const claimsQ = useApi(() => api.claims(0, 100));
  const dashboardQ = useApi(api.analytics.dashboard);

  const [reportType, setReportType] = useState("PMFBY");
  const [range, setRange] = useState({ from: "", to: "" });
  const trendsQ = useApi(() => (reportType === "PMFBY" && range.from ? api.analytics.trends(range.from, range.to || undefined) : Promise.resolve(null)), [reportType, range.from, range.to]);
  const [cropFilter, setCropFilter] = useState("ALL");
  const [seasonFilter, setSeasonFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [lossTypeFilter, setLossTypeFilter] = useState("ALL");
  const [busyFormat, setBusyFormat] = useState("");
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [scheduleForm, setScheduleForm] = useState(EMPTY_SCHEDULE);
  const [scheduleBusy, setScheduleBusy] = useState(false);

  const policyRows = listOf(policiesQ.data);
  const lossRows = listOf(lossesQ.data);
  const surveyRows = listOf(surveysQ.data);
  const claimRows = listOf(claimsQ.data);
  const crops = useMemo(() => Array.from(new Set(policyRows.map((p: any) => p.cropName).filter(Boolean))).sort(), [policyRows]);

  function selectReportType(next: string) { setReportType(next); setStatusFilter("ALL"); setLossTypeFilter("ALL"); }

  const statusOptions = reportType === "POLICY" ? POLICY_STATUSES : reportType === "LOSS" ? LOSS_STATUSES : reportType === "SURVEY" ? SURVEY_STATUSES : reportType === "CLAIM" ? CLAIM_STATUSES : [];

  const filteredPolicies = useMemo(() => policyRows.filter((p: any) =>
    (cropFilter === "ALL" || p.cropName === cropFilter) && (seasonFilter === "ALL" || p.season === seasonFilter) &&
    (statusFilter === "ALL" || p.status === statusFilter) && inRange(p.enrollmentDate, range.from, range.to)
  ), [policyRows, cropFilter, seasonFilter, statusFilter, range]);
  const filteredLosses = useMemo(() => lossRows.filter((l: any) =>
    (cropFilter === "ALL" || l.policy?.cropName === cropFilter) && (lossTypeFilter === "ALL" || l.lossType === lossTypeFilter) &&
    (statusFilter === "ALL" || l.status === statusFilter) && inRange(l.notificationDate, range.from, range.to)
  ), [lossRows, cropFilter, lossTypeFilter, statusFilter, range]);
  const filteredSurveys = useMemo(() => surveyRows.filter((s: any) =>
    (cropFilter === "ALL" || s.cropName === cropFilter) && (statusFilter === "ALL" || s.status === statusFilter) && inRange(s.surveyDate, range.from, range.to)
  ), [surveyRows, cropFilter, statusFilter, range]);
  const filteredClaims = useMemo(() => claimRows.filter((c: any) =>
    (cropFilter === "ALL" || c.policy?.cropName === cropFilter) && (statusFilter === "ALL" || c.status === statusFilter) && inRange(c.createdAt, range.from, range.to)
  ), [claimRows, cropFilter, statusFilter, range]);

  const activeRows = reportType === "POLICY" ? filteredPolicies : reportType === "LOSS" ? filteredLosses : reportType === "SURVEY" ? filteredSurveys : reportType === "CLAIM" ? filteredClaims : [];
  const activeExposure = reportType === "POLICY" ? filteredPolicies.reduce((s: number, p: any) => s + Number(p.sumInsured || 0), 0) : reportType === "CLAIM" ? filteredClaims.reduce((s: number, c: any) => s + Number(c.claimedAmount || 0), 0) : 0;
  const activeLoading = reportType === "POLICY" ? policiesQ.loading : reportType === "LOSS" ? lossesQ.loading : reportType === "SURVEY" ? surveysQ.loading : reportType === "CLAIM" ? claimsQ.loading : false;

  const dash: any = dashboardQ.data || {};
  const trendRows: any[] = Array.isArray(trendsQ.data) ? trendsQ.data : [];
  const scheduledRows: any[] = Array.isArray(scheduled.data) ? scheduled.data : [];
  const currentType = REPORT_TYPES.find((t) => t.key === reportType)!;

  const activeFilters = { from: range.from, to: range.to, crop: cropFilter, season: seasonFilter, status: statusFilter, lossType: lossTypeFilter };

  async function download(format: string) {
    setBusyFormat(format);
    try {
      if (reportType === "PMFBY") { await api.reports(format); toast.success(`${format.toUpperCase()} PMFBY report downloaded`); }
      else { await api.reportsDetail(reportType, format, activeFilters); toast.success(`${format.toUpperCase()} ${currentType.label} report downloaded`); }
    }
    catch (e: any) { toast.error(e.message || "Report generation failed"); }
    finally { setBusyFormat(""); }
  }

  const [caseBusy, setCaseBusy] = useState<string>("");
  async function downloadCase(id: number, format: string) {
    setCaseBusy(`${id}-${format}`);
    try { await api.reportsDetail(reportType, format, { id }); toast.success(`Case report for ${currentType.label} #${id} downloaded`); }
    catch (e: any) { toast.error(e.message || "Report generation failed"); }
    finally { setCaseBusy(""); }
  }

  async function createSchedule(e: any) {
    e.preventDefault();
    if (!scheduleForm.name || !scheduleForm.recipientList) { toast.error("Please fill in name and recipients."); return; }
    setScheduleBusy(true);
    try {
      await api.reportsScheduled.create({ name: scheduleForm.name, reportType: "PMFBY_SUMMARY", format: scheduleForm.format, recipientList: scheduleForm.recipientList, cronExpression: buildCron(scheduleForm), active: true });
      toast.success("Scheduled report created");
      setScheduleOpen(false); setScheduleForm(EMPTY_SCHEDULE); scheduled.reload();
    } catch (error) { handleError(error); } finally { setScheduleBusy(false); }
  }

  async function toggleActive(row: any) {
    try {
      await api.reportsScheduled.update(row.id, { name: row.name, reportType: row.reportType, format: row.format, recipientList: row.recipientList, cronExpression: row.cronExpression, active: !row.active });
      toast.success(row.active ? "Schedule paused" : "Schedule resumed");
      scheduled.reload();
    } catch (error) { handleError(error); }
  }

  return <>
    <style>{`.report-preview-strip{display:flex;align-items:center;gap:14px;flex-wrap:wrap;margin-top:12px;padding:10px 14px;border:1px solid #c8e6d4;border-radius:8px;background:#f3faf5;font-size:10px;color:#2e6646}.report-preview-strip b{font-weight:700}.report-preview-sep{width:1px;height:14px;background:#b5d9c5}.report-export-actions{display:flex;gap:9px;padding:0 19px 19px}
.rp-backdrop{position:fixed;inset:0;z-index:1000;display:grid;place-items:center;padding:20px;background:#102f2490;backdrop-filter:blur(4px)}.rp-modal{width:min(480px,100%);border:1px solid #dce8df;border-radius:16px;background:#fff;box-shadow:0 24px 70px #0c332c40;overflow:hidden}.rp-head{display:flex;justify-content:space-between;gap:20px;padding:22px 24px 16px;border-bottom:1px solid #edf1ee;background:linear-gradient(135deg,#f8fbf6,#fff)}.rp-head h2{margin:7px 0 4px;color:#244b3b;font:500 21px Georgia,serif}.rp-close{width:30px;height:30px;border:0;border-radius:50%;color:#6f8378;background:#eef5ed;font-size:22px;line-height:1;cursor:pointer}.rp-body{padding:20px 24px;display:flex;flex-direction:column;gap:13px}.rp-body label{display:flex;flex-direction:column;gap:6px;color:#426354;font-size:10px;font-weight:600}.rp-body input,.rp-body select{border:1px solid #dce8df;border-radius:8px;padding:10px 12px;outline:none;color:#315849;background:#fbfdfb;font:inherit;font-size:11px}.rp-body input:focus,.rp-body select:focus{border-color:#76a080;box-shadow:0 0 0 3px #dceedd}.rp-foot{display:flex;justify-content:flex-end;gap:9px;padding:16px 24px;border-top:1px solid #edf1ee}.rp-foot .secondary-button,.rp-foot .primary-button{height:38px;padding:0 18px}
.rp-empty{min-height:150px;display:grid;place-items:center;padding:24px;color:#92a198;font-size:11px;text-align:center}.rp-empty strong{display:block;margin-bottom:6px;color:#426354;font:500 15px Georgia,serif}`}</style>

    <section className="page-heading">
      <div><p className="eyebrow">REPORTING / PMFBY</p><h1>Reports & Distribution</h1><p className="subhead">Select a report type, apply real filters, preview the result, then export.</p></div>
    </section>

    <section className="panel form-panel">
      <div className="panel-heading"><div><span className="panel-kicker">STEP 1 · REPORT TYPE</span><h2>Select report type</h2></div></div>
      <div className="admin-tabs" style={{ margin: "0 19px 19px" }}>
        {REPORT_TYPES.map((t) => <button type="button" key={t.key} className={reportType === t.key ? "selected" : ""} onClick={() => selectReportType(t.key)}>{t.label}</button>)}
      </div>
    </section>

    <section className="panel form-panel">
      <div className="panel-heading"><div><span className="panel-kicker">STEP 2 · DATE RANGE &amp; FILTERS</span><h2>Apply filters</h2></div></div>
      <div className="form-grid">
        <label>From<input type="date" value={range.from} onChange={(e) => setRange({ ...range, from: e.target.value })} /></label>
        <label>To<input type="date" value={range.to} onChange={(e) => setRange({ ...range, to: e.target.value })} disabled={!range.from} /></label>
        {reportType !== "SURVEY" && <label>Crop<select value={cropFilter} onChange={(e) => setCropFilter(e.target.value)}><option value="ALL">All crops</option>{crops.map((c) => <option key={c}>{c}</option>)}</select></label>}
        {reportType === "POLICY" && <label>Season<select value={seasonFilter} onChange={(e) => setSeasonFilter(e.target.value)}><option value="ALL">All seasons</option>{SEASONS.map((s) => <option key={s}>{s}</option>)}</select></label>}
        {reportType === "LOSS" && <label>Loss type<select value={lossTypeFilter} onChange={(e) => setLossTypeFilter(e.target.value)}><option value="ALL">All loss types</option>{LOSS_TYPES.map((t) => <option key={t}>{t}</option>)}</select></label>}
        {statusOptions.length > 0 && <label>Status<select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option value="ALL">All statuses</option>{statusOptions.map((s) => <option key={s}>{s}</option>)}</select></label>}
      </div>
    </section>

    <section className="panel">
      <div className="panel-heading">
        <div>
          <span className="panel-kicker">STEP 3 · PREVIEW</span>
          <h2>{currentType.label} preview</h2>
          <p className="panel-description">{reportType === "PMFBY" ? (dashboardQ.loading ? "Loading what will be included…" : `Includes: ${dash.policies ?? 0} policies, ${dash.activePolicies ?? 0} active, ${dash.losses ?? 0} loss reports, ${dash.claims ?? 0} claims (${dash.paidClaims ?? 0} paid), ${money(dash.exposure)} exposure, ${dash.claimRatio ?? 0}% claim ratio.`) : activeLoading ? "Loading matching records…" : `${activeRows.length} matching record${activeRows.length === 1 ? "" : "s"}${activeExposure ? ` · ${money(activeExposure)} total` : ""}`}</p>
        </div>
      </div>

      {reportType === "PMFBY" ? (
        range.from && (
          trendsQ.loading ? <p className="muted" style={{ margin: "0 19px 16px" }}>Loading trend for the selected date range…</p> :
          trendRows.length ? (
            <div className="report-preview-strip" style={{ margin: "0 19px 16px" }}>
              <span>Policies enrolled: <b>{trendRows.reduce((s, d: any) => s + Number(d.policies || 0), 0)}</b></span>
              <div className="report-preview-sep" />
              <span>Losses reported: <b>{trendRows.reduce((s, d: any) => s + Number(d.losses || 0), 0)}</b></span>
              <div className="report-preview-sep" />
              <span>Claims filed: <b>{trendRows.reduce((s, d: any) => s + Number(d.claims || 0), 0)}</b></span>
            </div>
          ) : <p className="muted" style={{ margin: "0 19px 16px" }}>No trend data returned by the backend for this range.</p>
        )
      ) : !activeLoading && (
        activeRows.length ? (
          <div className="data-table" style={{ margin: "0 19px 16px" }}>
            {reportType === "POLICY" && <><div className="table-row table-header"><span>Policy</span><span>Crop / season</span><span>Farmer</span><span>Sum insured</span><span>Status</span><span>Case report</span></div>{activeRows.slice(0, 8).map((p: any) => <div className="table-row" key={p.id}><span><b>{p.policyNumber}</b></span><span>{p.cropName} · {p.season}</span><span>{p.farmerName || "—"}</span><span>{money(p.sumInsured)}</span><span><b className={`status-pill ${String(p.status || "").toLowerCase().replaceAll("_", "-")}`}>{p.status}</b></span><span><button className="row-action" disabled={caseBusy === `${p.id}-pdf`} onClick={() => downloadCase(p.id, "pdf")}>{caseBusy === `${p.id}-pdf` ? "Preparing…" : "PDF"}</button></span></div>)}</>}
            {reportType === "LOSS" && <><div className="table-row table-header"><span>Loss</span><span>Policy</span><span>Type</span><span>Affected area</span><span>Status</span><span>Case report</span></div>{activeRows.slice(0, 8).map((l: any) => <div className="table-row" key={l.id}><span><b>#{l.id}</b></span><span>{l.policy?.policyNumber || "—"}</span><span>{l.lossType}</span><span>{l.affectedAreaHa ?? 0} ha</span><span><b className={`status-pill ${String(l.status || "").toLowerCase().replaceAll("_", "-")}`}>{l.status}</b></span><span><button className="row-action" disabled={caseBusy === `${l.id}-pdf`} onClick={() => downloadCase(l.id, "pdf")}>{caseBusy === `${l.id}-pdf` ? "Preparing…" : "PDF"}</button></span></div>)}</>}
            {reportType === "SURVEY" && <><div className="table-row table-header"><span>Survey</span><span>Farmer / crop</span><span>Survey date</span><span>Loss assessed</span><span>Status</span><span>Case report</span></div>{activeRows.slice(0, 8).map((s: any) => <div className="table-row" key={s.id}><span><b>#{s.id}</b></span><span>{s.farmerName || "—"} · {s.cropName || "—"}</span><span>{s.surveyDate || "—"}</span><span>{s.lossAssessedPercentage != null ? `${s.lossAssessedPercentage}%` : "—"}</span><span><b className={`status-pill ${String(s.status || "").toLowerCase().replaceAll("_", "-")}`}>{s.status}</b></span><span><button className="row-action" disabled={caseBusy === `${s.id}-pdf`} onClick={() => downloadCase(s.id, "pdf")}>{caseBusy === `${s.id}-pdf` ? "Preparing…" : "PDF"}</button></span></div>)}</>}
            {reportType === "CLAIM" && <><div className="table-row table-header"><span>Claim</span><span>Policy</span><span>Claimed</span><span>Approved</span><span>Status</span><span>Case report</span></div>{activeRows.slice(0, 8).map((c: any) => <div className="table-row" key={c.id}><span><b>#{c.id}</b></span><span>{c.policy?.policyNumber || "—"}</span><span>{money(c.claimedAmount)}</span><span>{c.approvedAmount != null ? money(c.approvedAmount) : "—"}</span><span><b className={`status-pill ${String(c.status || "").toLowerCase().replaceAll("_", "-")}`}>{c.status}</b></span><span><button className="row-action" disabled={caseBusy === `${c.id}-pdf`} onClick={() => downloadCase(c.id, "pdf")}>{caseBusy === `${c.id}-pdf` ? "Preparing…" : "PDF"}</button></span></div>)}</>}
            {activeRows.length > 8 && <p className="muted" style={{ padding: "10px 0 0" }}>+{activeRows.length - 8} more in the export</p>}
          </div>
        ) : <div className="rp-empty" style={{ margin: "0 19px 16px" }}><div><strong>No matching records</strong><span>Adjust the date range or filters above.</span></div></div>
      )}

      <p className="report-note">{reportType === "PMFBY" ? "This export is generated by the reporting backend as the operational PMFBY summary shown above." : `This export is generated by the reporting backend from ${activeRows.length} matching record${activeRows.length === 1 ? "" : "s"} for the selected filters. Use a row's case report button for a single detailed record instead.`}</p>

      <div className="report-export-actions">
        <button className="secondary-button" disabled={!!busyFormat} onClick={() => download("csv")}>{busyFormat === "csv" ? "Preparing…" : "CSV"}</button>
        <button className="secondary-button" disabled={!!busyFormat} onClick={() => download("xlsx")}>{busyFormat === "xlsx" ? "Preparing…" : "Excel"}</button>
        <button className="primary-button" disabled={!!busyFormat} onClick={() => download("pdf")}>{busyFormat === "pdf" ? "Preparing…" : "PDF"}</button>
      </div>
    </section>

    <PageState {...scheduled} />

    <section className="panel table-panel">
      <div className="panel-heading">
        <div><span className="panel-kicker">SCHEDULED DELIVERY (OPTIONAL)</span><h2>Scheduled reports</h2><p className="panel-description">Recurring PMFBY delivery managed by the reporting backend.</p></div>
        <button className="primary-button" onClick={() => setScheduleOpen(true)}><Plus size={14} /> New schedule</button>
      </div>
      {!scheduled.loading && !scheduled.error && (
        scheduledRows.length ? (
          <div className="data-table">
            <div className="table-row table-header" style={{ gridTemplateColumns: "1.2fr .9fr .6fr 1.4fr 1.1fr .8fr .8fr" }}><span>Name</span><span>Type</span><span>Format</span><span>Recipients</span><span>Schedule</span><span>Status</span><span /></div>
            {scheduledRows.map((row: any) => (
              <div className="table-row" style={{ gridTemplateColumns: "1.2fr .9fr .6fr 1.4fr 1.1fr .8fr .8fr" }} key={row.id}>
                <span><b>{row.name}</b></span>
                <span>{row.reportType}</span>
                <span>{String(row.format || "").toUpperCase()}</span>
                <span style={{ fontSize: 9, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{row.recipientList}</span>
                <span style={{ fontSize: 9 }}>{describeCron(row.cronExpression)}</span>
                <span><b className={`status-pill ${row.active ? "enrolled" : "lapsed"}`}>{row.active ? "ACTIVE" : "PAUSED"}</b></span>
                <span><button className="row-action" onClick={() => toggleActive(row)}>{row.active ? "Pause" : "Resume"}</button></span>
              </div>
            ))}
          </div>
        ) : (
          <div className="rp-empty"><div><strong>No scheduled reports yet</strong><span>Recurring delivery is optional — set one up if you'd like the PMFBY summary emailed automatically.</span></div></div>
        )
      )}
    </section>

    {scheduleOpen && (
      <div className="rp-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setScheduleOpen(false)}>
        <div className="rp-modal" role="dialog" aria-modal="true">
          <div className="rp-head"><div><span className="panel-kicker">SCHEDULED DELIVERY</span><h2>New scheduled report</h2></div><button className="rp-close" onClick={() => setScheduleOpen(false)}>×</button></div>
          <form onSubmit={createSchedule}>
            <div className="rp-body">
              <label>Name<input value={scheduleForm.name} onChange={(e) => setScheduleForm({ ...scheduleForm, name: e.target.value })} placeholder="e.g. Monthly PMFBY filing" autoFocus required /></label>
              <label>Format<select value={scheduleForm.format} onChange={(e) => setScheduleForm({ ...scheduleForm, format: e.target.value })}>{REPORT_FORMATS.map((f) => <option key={f} value={f}>{f.toUpperCase()}</option>)}</select></label>
              <label>Recipients (comma-separated emails)<input value={scheduleForm.recipientList} onChange={(e) => setScheduleForm({ ...scheduleForm, recipientList: e.target.value })} placeholder="ops@agrishield.example, state.office@agrishield.example" required /></label>
              <label>Frequency<select value={scheduleForm.frequency} onChange={(e) => setScheduleForm({ ...scheduleForm, frequency: e.target.value })}>{FREQUENCIES.map((f) => <option key={f} value={f}>{f.charAt(0) + f.slice(1).toLowerCase()}</option>)}</select></label>
              {scheduleForm.frequency === "WEEKLY" && <label>Day of week<select value={scheduleForm.dayOfWeek} onChange={(e) => setScheduleForm({ ...scheduleForm, dayOfWeek: e.target.value })}>{DAYS_OF_WEEK.map((d) => <option key={d}>{d}</option>)}</select></label>}
              {scheduleForm.frequency === "MONTHLY" && <label>Day of month<select value={scheduleForm.dayOfMonth} onChange={(e) => setScheduleForm({ ...scheduleForm, dayOfMonth: e.target.value })}>{Array.from({ length: 28 }, (_, i) => String(i + 1)).map((d) => <option key={d}>{d}</option>)}</select></label>}
              <label>Time<select value={scheduleForm.hour} onChange={(e) => setScheduleForm({ ...scheduleForm, hour: e.target.value })}>{Array.from({ length: 24 }, (_, i) => String(i)).map((h) => <option key={h} value={h}>{h.padStart(2, "0")}:00</option>)}</select></label>
              <p className="report-note" style={{ margin: 0 }}>Runs {describeCron(buildCron(scheduleForm)).toLowerCase()}. Converted internally to the schedule format the backend expects.</p>
            </div>
            <div className="rp-foot">
              <button type="button" className="secondary-button" onClick={() => setScheduleOpen(false)} disabled={scheduleBusy}>Cancel</button>
              <button type="submit" className="primary-button" disabled={scheduleBusy}>{scheduleBusy ? "Creating…" : "Create schedule"}</button>
            </div>
          </form>
        </div>
      </div>
    )}
  </>;
}
