import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, CheckCircle2, Loader2 } from "lucide-react";
import { api } from "../../services/api";
import { PageState, useApi, handleError } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";

const OTHER_SCHEDULE_TYPES = ["INSPECTION", "MEETING", "MAINTENANCE", "OTHER"];
const SCHEDULE_TYPES = ["SURVEY", ...OTHER_SCHEDULE_TYPES];

const statusClass = (status: string) => {
  const s = String(status || "").toUpperCase();
  if (s === "CANCELLED" || s === "REJECTED") return "status-pill claimed-field";
  if (s === "RESCHEDULED" || s === "IN_PROGRESS") return "status-pill enrolled";
  if (s === "COMPLETED" || s === "VERIFIED") return "status-pill settled";
  return "status-pill";
};

const dateTimeLabel = (value: any) => {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
};
const dateLabel = (value: any) => {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
};
const listOf = (value: any): any[] => Array.isArray(value) ? value : (value?.content || []);

export default function SchedulingPage() {
  const profile = useApi(api.profile);
  const role = String(profile.data?.role || "").toUpperCase();
  // Only the Insurance Officer schedules/assigns surveys - enforced server-side on /api/survey-assignments too.
  const canScheduleSurvey = role === "INSURANCE_OFFICER";
  const availableTypes = canScheduleSurvey ? SCHEDULE_TYPES : OTHER_SCHEDULE_TYPES;

  const q = useApi(api.schedules.list);
  const surveyAssignments = useApi(api.surveys);
  const openCases = useApi(api.losses);
  const resources = useApi(api.surveyors);
  const resourceRows: any[] = Array.isArray(resources.data) ? resources.data : [];
  const resourceName = (id: any) => resourceRows.find((r) => String(r.id) === String(id))?.name || `User #${id}`;

  const caseRows: any[] = useMemo(() => listOf(openCases.data).filter((l: any) => String(l.status).toUpperCase() === "SUBMITTED"), [openCases.data]);

  const [form, setForm] = useState({ resourceUserId: "", caseId: "", start: "", end: "", surveyDate: "", purpose: "", location: "", type: "SURVEY" });
  const [creating, setCreating] = useState(false);
  const [availability, setAvailability] = useState<{ checking: boolean; available: boolean | null; message: string }>({ checking: false, available: null, message: "" });

  useEffect(() => {
    if (!availableTypes.includes(form.type)) setForm((f) => ({ ...f, type: availableTypes[0] }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canScheduleSurvey]);

  const isSurvey = form.type === "SURVEY" && canScheduleSurvey;
  const scheduleRows: any[] = Array.isArray(q.data) ? q.data : [];
  const surveyRows: any[] = Array.isArray(surveyAssignments.data) ? surveyAssignments.data : [];

  const combinedRows = useMemo(() => {
    const fromSchedules = scheduleRows.map((r: any) => ({
      key: `sch-${r.id}`, type: String(r.scheduleType || "—").replaceAll("_", " "), resource: resourceName(r.resourceUserId),
      record: "—", when: `${dateTimeLabel(r.startTime)} to ${dateTimeLabel(r.endTime)}`, status: r.status, sortKey: r.startTime,
    }));
    const fromSurveys = surveyRows.map((s: any) => ({
      key: `srv-${s.id}`, type: "SURVEY", resource: s.surveyorName || resourceName(s.surveyorId),
      record: `${s.farmerName || "Farmer"} - ${s.cropName || "Crop"}`, when: dateLabel(s.surveyDate), status: s.status, sortKey: s.surveyDate,
    }));
    return [...fromSchedules, ...fromSurveys].sort((a, b) => new Date(b.sortKey || 0).getTime() - new Date(a.sortKey || 0).getTime());
  }, [scheduleRows, surveyRows, resourceRows]);

  const startEndValid = useMemo(() => {
    if (!form.start || !form.end) return false;
    return new Date(form.start).getTime() < new Date(form.end).getTime();
  }, [form.start, form.end]);

  useEffect(() => {
    if (isSurvey || !form.resourceUserId || !startEndValid) { setAvailability({ checking: false, available: null, message: "" }); return; }
    let cancelled = false;
    setAvailability({ checking: true, available: null, message: "" });
    const timer = setTimeout(() => {
      api.schedules.availability(Number(form.resourceUserId), new Date(form.start).toISOString(), new Date(form.end).toISOString())
        .then((res: any) => {
          if (cancelled) return;
          const isAvailable = res === true || res?.available === true || res?.data === true;
          setAvailability({ checking: false, available: isAvailable, message: isAvailable ? "This resource is free for the selected window." : "This resource already has a schedule that overlaps this window." });
        })
        .catch(() => { if (!cancelled) setAvailability({ checking: false, available: null, message: "" }); });
    }, 350);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [isSurvey, form.resourceUserId, form.start, form.end, startEndValid]);

  function resetForm() {
    setForm({ resourceUserId: "", caseId: "", start: "", end: "", surveyDate: "", purpose: "", location: "", type: form.type });
    setAvailability({ checking: false, available: null, message: "" });
  }

  async function create() {
    if (!form.resourceUserId) return toast.error(isSurvey ? "Choose a surveyor to assign." : "Choose a resource to schedule.");
    if (isSurvey) {
      if (!form.caseId) return toast.error("Choose a loss case that needs a survey.");
      if (!form.surveyDate) return toast.error("Choose a survey date.");
      setCreating(true);
      try {
        await api.assignSurvey({ notificationId: Number(form.caseId), surveyorId: Number(form.resourceUserId), surveyDate: form.surveyDate });
        toast.success("Survey scheduled - it now appears in the surveyor's Assigned Surveys and they've been notified.");
        resetForm();
        surveyAssignments.reload();
        openCases.reload();
      } catch (e) { handleError(e); } finally { setCreating(false); }
      return;
    }
    if (!form.start || !form.end) return toast.error("Choose both a start and an end time.");
    if (!startEndValid) return toast.error("End time must be after the start time.");
    if (availability.available === false) return toast.error("This resource is already booked in that window. Pick a different time.");
    setCreating(true);
    try {
      await api.schedules.create({ scheduleType: form.type, resourceUserId: Number(form.resourceUserId), startTime: new Date(form.start).toISOString(), endTime: new Date(form.end).toISOString(), purpose: form.purpose || undefined, location: form.location || undefined });
      toast.success(`Schedule created - ${resourceName(form.resourceUserId)} has been notified.`);
      resetForm();
      q.reload();
    } catch (e) { handleError(e); } finally { setCreating(false); }
  }

  return <>
    <section className="page-heading">
      <div>
        <p className="eyebrow">OPERATIONS / CALENDAR</p>
        <h1>Scheduling &amp; Resources</h1>
        <p className="subhead">Assign surveyors to open cases, plan operational time blocks, and check availability before booking.</p>
      </div>
    </section>

    <section className="panel form-panel">
      <div className="panel-heading"><div><span className="panel-kicker">CREATE EVENT</span><h2>New schedule</h2><p className="panel-description">{isSurvey ? "Assign a surveyor to an open case - it will appear on their dashboard and they'll be notified." : "Choose a resource and a time window - availability is checked before you create it."}</p></div></div>
      <div className="form-grid">
        <label>Schedule type
          <select value={form.type} onChange={(e) => setForm({ resourceUserId: "", caseId: "", start: "", end: "", surveyDate: "", type: e.target.value })}>
            {availableTypes.map((t) => <option key={t} value={t}>{t.replaceAll("_", " ")}</option>)}
          </select>
        </label>
        <label>{isSurvey ? "Surveyor" : "Resource"}
          <select value={form.resourceUserId} onChange={(e) => setForm({ ...form, resourceUserId: e.target.value })}>
            <option value="">{resources.loading ? "Loading surveyors…" : `Select a ${isSurvey ? "surveyor" : "resource"}`}</option>
            {resourceRows.map((r: any) => <option key={r.id} value={r.id}>{r.name} ({r.userId})</option>)}
          </select>
        </label>
        {isSurvey ? (
          <>
            <label>Case (open loss report)
              <select value={form.caseId} onChange={(e) => setForm({ ...form, caseId: e.target.value })}>
                <option value="">{openCases.loading ? "Loading cases…" : "Select a case needing a survey"}</option>
                {caseRows.map((l: any) => <option key={l.id} value={l.id}>Loss #{l.id} - {l.policy?.cropName || "Crop"} - {l.policy?.policyNumber || `Policy #${l.policyId ?? "—"}`} - {l.lossType}</option>)}
              </select>
            </label>
            <label>Survey date<input type="date" value={form.surveyDate} onChange={(e) => setForm({ ...form, surveyDate: e.target.value })} /></label>
          </>
        ) : (
          <>
            <label>Start<input type="datetime-local" value={form.start} onChange={(e) => setForm({ ...form, start: e.target.value })} /></label>
            <label>End<input type="datetime-local" value={form.end} onChange={(e) => setForm({ ...form, end: e.target.value })} /></label>
            <label>Purpose (optional)<input value={form.purpose} onChange={(e) => setForm({ ...form, purpose: e.target.value })} placeholder="e.g. Quarterly compliance review" /></label>
            <label>Location (optional)<input value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} placeholder="e.g. District office, Chennai" /></label>
          </>
        )}
      </div>

      {isSurvey && !openCases.loading && caseRows.length === 0 && (
        <p className="cm-info" style={{ display: "flex", alignItems: "center", gap: 6, margin: "0 0 14px" }}><AlertTriangle size={14} /> No open cases currently need a surveyor - every submitted loss report already has one assigned.</p>
      )}
      {!isSurvey && form.start && form.end && !startEndValid && (
        <p className="cm-info" style={{ display: "flex", alignItems: "center", gap: 6, margin: "0 0 14px", color: "#9a625b" }}><AlertTriangle size={14} /> End time must be after the start time.</p>
      )}
      {!isSurvey && availability.checking && (
        <p className="cm-info" style={{ display: "flex", alignItems: "center", gap: 6, margin: "0 0 14px" }}><Loader2 size={14} className="spin" /> Checking availability…</p>
      )}
      {!isSurvey && !availability.checking && availability.available !== null && (
        <p className="cm-info" style={{ display: "flex", alignItems: "center", gap: 6, margin: "0 0 14px", color: availability.available ? "#3d8b5f" : "#9a625b" }}>
          {availability.available ? <CheckCircle2 size={14} /> : <AlertTriangle size={14} />} {availability.message}
        </p>
      )}

      <button className="primary-button" onClick={create} disabled={creating || availability.checking}>
        {creating ? "Creating…" : isSurvey ? "Assign surveyor" : "Check availability & create"}
      </button>
    </section>

    <PageState {...q} />
    <PageState {...surveyAssignments} />

    <section className="panel table-panel">
      <div className="table-toolbar"><span className="panel-kicker">SCHEDULED EVENTS</span><span className="muted">{combinedRows.length} event{combinedRows.length === 1 ? "" : "s"}</span></div>
      <div className="data-table">
        <div className="table-row table-header" style={{ gridTemplateColumns: "1fr 1.2fr 1.5fr 1.5fr .9fr" }}><span>Type</span><span>Resource</span><span>Case / Record</span><span>Date / Window</span><span>Status</span></div>
        {combinedRows.length ? combinedRows.map((r: any) => (
          <div className="table-row" key={r.key} style={{ gridTemplateColumns: "1fr 1.2fr 1.5fr 1.5fr .9fr" }}>
            <span><b>{r.type}</b></span>
            <span>{r.resource}</span>
            <span>{r.record}</span>
            <span>{r.when}</span>
            <span><b className={statusClass(r.status)}>{r.status || "—"}</b></span>
          </div>
        )) : (
          <div className="empty-row">No scheduled events yet. Create one above.</div>
        )}
      </div>
    </section>
  </>;
}
