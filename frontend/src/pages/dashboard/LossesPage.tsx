import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, Camera, ChevronDown, ChevronUp, Crosshair, Droplets, FileText, MapPin, Plus, Sprout, Waves, X } from "lucide-react";
import { api, authToken, API_BASE_URL } from "../../services/api";
import { DataToolbar, PageState, handleError, useApi } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";
import LossMap from "../../components/dashboard/LossMap";

const money = (v: any) => `₹${Number(v || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
const lossTypes = ["DROUGHT", "FLOOD", "PEST", "HAILSTORM", "FIRE", "OTHER"];
const SEVERITY_OPTIONS = [
  { key: "lt25",    label: "Small part — less than 25% of the field",      pct: 12.5 },
  { key: "25to50",  label: "About half — roughly 25% to 50% damaged",       pct: 37.5 },
  { key: "50to75",  label: "Most of the field — 50% to 75% crop damage",    pct: 62.5 },
  { key: "75to100", label: "Almost entire field — 75% to 100%, near total loss", pct: 87.5 },
  { key: "manual",  label: "Enter exact values manually",                   pct: null  },
];
const tone = (t: string) => t === "FLOOD" ? "blue" : t === "DROUGHT" ? "gold" : t === "PEST" ? "rose" : "green";
const iconFor = (t: string) => t === "FLOOD" ? <Waves size={18} /> : t === "DROUGHT" ? <Sprout size={18} /> : t === "PEST" ? <AlertTriangle size={18} /> : <Droplets size={18} />;

function EvidenceImage({ path, onOpen }: { path: string; onOpen: (src: string) => void }) {
  const [blobSrc, setBlobSrc] = useState<string | null>(null);
  const [err, setErr] = useState(false);
  useEffect(() => {
    const token = authToken();
    let objectUrl = "";
    let cancelled = false;
    fetch(`${API_BASE_URL}${path}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      .then((r) => { if (!r.ok) throw new Error(); return r.blob(); })
      .then((blob) => { if (cancelled) return; objectUrl = URL.createObjectURL(blob); setBlobSrc(objectUrl); })
      .catch(() => { if (!cancelled) setErr(true); });
    return () => { cancelled = true; if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [path]);
  if (err) return <div className="evidence-placeholder evidence-error">Unavailable</div>;
  if (!blobSrc) return <div className="evidence-placeholder evidence-loading" />;
  return <img src={blobSrc} className="evidence-thumb" alt="Field evidence" onClick={() => onOpen(blobSrc)} />;
}

export default function LossesPage() {
  const profile = useApi(api.profile);
  const farmerId = (profile.data as any)?.id;
  const role = String((profile.data as any)?.role || "").toUpperCase();
  const isFarmer = role === "FARMER";
  const canAssign = ["INSURANCE_OFFICER", "STATE_OFFICER", "ADMIN"].includes(role);

  const policies = useApi(() => farmerId && isFarmer ? api.farmerPolicies(Number(farmerId)) : Promise.resolve([]), [farmerId, isFarmer]);
  const losses = useApi(api.losses);

  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [form, setForm] = useState({ policyId: "", lossType: "DROUGHT", affectedAreaHa: "", lossPercentageEstimate: "", geoLat: "", geoLng: "", severityKey: "" });
  const [files, setFiles] = useState<File[]>([]);
  const [locating, setLocating] = useState(false);
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [details, setDetails] = useState<Record<number, any>>({});
  const [detailError, setDetailError] = useState<Record<number, boolean>>({});
  const [geoAddresses, setGeoAddresses] = useState<Record<number, string>>({});
  const [lightbox, setLightbox] = useState<string | null>(null);

  // Officer loss-details modal (also hosts Assign Surveyor)
  const [detailsModal, setDetailsModal] = useState<number | null>(null);
  const [policyDetails, setPolicyDetails] = useState<Record<number, any>>({});
  const [policyDetailError, setPolicyDetailError] = useState<Record<number, boolean>>({});
  const [surveyors, setSurveyors] = useState<any[]>([]);
  const [surveyAssignments, setSurveyAssignments] = useState<any[]>([]);
  const [assignForm, setAssignForm] = useState({ surveyorId: "", surveyDate: "" });
  const [assignBusy, setAssignBusy] = useState(false);

  useEffect(() => {
    if (detailsModal == null) return;
    api.surveyors().then((data: any) => setSurveyors(Array.isArray(data) ? data : [])).catch(() => {});
    api.surveys().then((data: any) => setSurveyAssignments(Array.isArray(data) ? data : (data?.content || []))).catch(() => {});
  }, [detailsModal]);

  const rows: any[] = Array.isArray(losses.data) ? losses.data : (losses.data as any)?.content || [];
  const policyRows: any[] = Array.isArray(policies.data) ? policies.data : [];
  // Active/in-progress assignment count per surveyor — real backend data, used to show workload before assigning.
  const workloadBySurveyor = useMemo(() => {
    const map: Record<string, number> = {};
    surveyAssignments.forEach((s: any) => { if (["ASSIGNED", "IN_PROGRESS"].includes(s.status)) map[s.surveyorId] = (map[s.surveyorId] || 0) + 1; });
    return map;
  }, [surveyAssignments]);
  // Flags (does not block) if the selected surveyor already has an active assignment on the chosen date.
  const dateConflicts = useMemo(() => {
    if (!assignForm.surveyorId || !assignForm.surveyDate) return 0;
    return surveyAssignments.filter((s: any) => String(s.surveyorId) === assignForm.surveyorId && s.surveyDate === assignForm.surveyDate && ["ASSIGNED", "IN_PROGRESS"].includes(s.status)).length;
  }, [surveyAssignments, assignForm.surveyorId, assignForm.surveyDate]);
  const filtered = useMemo(() => rows.filter((r) => JSON.stringify(r).toLowerCase().includes(search.toLowerCase())), [rows, search]);
  const surveyed = rows.filter((r) => ["SURVEYED", "PROCESSED"].includes(r.status)).length;
  const avgLoss = rows.length ? Math.round(rows.reduce((sum, r) => sum + Number(r.lossPercentageEstimate || 0), 0) / rows.length) : 0;
  const hasCoords = form.geoLat !== "" && form.geoLng !== "";
  const selectedPolicy = policyRows.find((p) => String(p.id) === form.policyId);
  const sownAreaHa = selectedPolicy ? Number(selectedPolicy.sownAreaHa) : 0;

  function applyPreset(key: string, area: number) {
    const opt = SEVERITY_OPTIONS.find((o) => o.key === key);
    if (!opt || opt.pct === null || area <= 0) {
      setForm((p) => ({ ...p, severityKey: key }));
      return;
    }
    setForm((p) => ({ ...p, severityKey: key, lossPercentageEstimate: String(opt.pct), affectedAreaHa: (area * opt.pct / 100).toFixed(2) }));
  }

  function handlePolicyChange(e: any) {
    const id = e.target.value;
    const pol = policyRows.find((p) => String(p.id) === id);
    const area = pol ? Number(pol.sownAreaHa) : 0;
    if (form.severityKey && form.severityKey !== "manual") {
      const opt = SEVERITY_OPTIONS.find((o) => o.key === form.severityKey);
      if (opt && opt.pct !== null && area > 0) {
        setForm((p) => ({ ...p, policyId: id, affectedAreaHa: (area * opt.pct / 100).toFixed(2), lossPercentageEstimate: String(opt.pct) }));
        return;
      }
    }
    setForm((p) => ({ ...p, policyId: id }));
  }

  function locate() {
    if (!navigator.geolocation) { toast.error("Geolocation is not supported by your browser."); return; }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => { setForm((prev) => ({ ...prev, geoLat: pos.coords.latitude.toFixed(7), geoLng: pos.coords.longitude.toFixed(7) })); setLocating(false); toast.success("GPS location captured"); },
      (err) => {
        setLocating(false);
        const msgs: Record<number, string> = { 1: "Location permission denied. Please allow location access and try again.", 2: "Location unavailable. Check that your device GPS is on.", 3: "Location request timed out. Please try again." };
        toast.error(msgs[err.code] || "Could not get your location.");
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
  }

  function handleFiles(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(e.target.files || []);
    const valid = selected.filter((f) => {
      if (!f.type.startsWith("image/")) { toast.error(`${f.name}: only image files are allowed.`); return false; }
      if (f.size > 10 * 1024 * 1024) { toast.error(`${f.name}: exceeds the 10 MB limit.`); return false; }
      return true;
    });
    setFiles((prev) => [...prev, ...valid]);
    e.target.value = "";
  }

  function removeFile(index: number) { setFiles((prev) => prev.filter((_, i) => i !== index)); }

  async function create(e: any) {
    e.preventDefault();
    if (!form.geoLat || !form.geoLng) { toast.error("Please capture your GPS location before submitting."); return; }
    if (!form.severityKey) { toast.error("Please select how much of your field was damaged."); return; }
    if (!form.affectedAreaHa || !form.lossPercentageEstimate) { toast.error("Please complete the damage details."); return; }
    setBusy(true);
    try {
      const created = await api.createLoss({ policyId: Number(form.policyId), lossType: form.lossType, affectedAreaHa: Number(form.affectedAreaHa), lossPercentageEstimate: Number(form.lossPercentageEstimate), geoLat: Number(form.geoLat), geoLng: Number(form.geoLng), photoUrls: [] });
      if (files.length > 0) { setUploading(true); for (const file of files) { await api.uploadLossPhoto(created.id, file); } }
      toast.success(`Loss report #${created.id} submitted${files.length ? ` with ${files.length} photo${files.length > 1 ? "s" : ""}` : ""}`);
      setOpen(false);
      setForm({ policyId: "", lossType: "DROUGHT", affectedAreaHa: "", lossPercentageEstimate: "", geoLat: "", geoLng: "", severityKey: "" });
      setFiles([]);
      losses.reload();
    } catch (error) { handleError(error); } finally { setBusy(false); setUploading(false); }
  }

  async function geocode(id: number, lat: number, lng: number) {
    if (geoAddresses[id]) return;
    try {
      const res = await fetch(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`, { headers: { "Accept-Language": "en" } });
      if (!res.ok) return;
      const data = await res.json();
      if (data?.display_name) setGeoAddresses((prev) => ({ ...prev, [id]: data.display_name }));
    } catch { /* best-effort geocoding */ }
  }

  async function expand(id: number) {
    if (expandedId === id) { setExpandedId(null); return; }
    setExpandedId(id);
    // Fetch detail for photoUrls; location uses list row data directly
    if (!details[id] && !detailError[id]) {
      try { const d = await api.loss(id); setDetails((prev) => ({ ...prev, [id]: d })); }
      catch { setDetailError((prev) => ({ ...prev, [id]: true })); }
    }
    // Reverse-geocode the GPS coordinates from the list row
    const row = rows.find((r) => r.id === id);
    if (row?.geoLat && row?.geoLng) geocode(id, Number(row.geoLat), Number(row.geoLng));
  }

  async function openDetails(loss: any) {
    setDetailsModal(loss.id);
    setAssignForm({ surveyorId: "", surveyDate: "" });
    if (!details[loss.id] && !detailError[loss.id]) {
      try { const d = await api.loss(loss.id); setDetails((prev) => ({ ...prev, [loss.id]: d })); }
      catch { setDetailError((prev) => ({ ...prev, [loss.id]: true })); }
    }
    if (loss.policyId && !policyDetails[loss.policyId] && !policyDetailError[loss.policyId]) {
      try { const p = await api.policy(loss.policyId); setPolicyDetails((prev) => ({ ...prev, [loss.policyId]: p })); }
      catch { setPolicyDetailError((prev) => ({ ...prev, [loss.policyId]: true })); }
    }
    if (loss.geoLat && loss.geoLng) geocode(loss.id, Number(loss.geoLat), Number(loss.geoLng));
  }

  async function assignSurveyor() {
    if (!assignForm.surveyorId || !assignForm.surveyDate) { toast.error("Please select a surveyor and survey date."); return; }
    if (detailsModal == null) return;
    setAssignBusy(true);
    try {
      await api.assignSurvey({ notificationId: detailsModal, surveyorId: Number(assignForm.surveyorId), surveyDate: assignForm.surveyDate });
      toast.success("Surveyor assigned successfully");
      setDetailsModal(null);
      setAssignForm({ surveyorId: "", surveyDate: "" });
      losses.reload();
    } catch (error) { handleError(error); } finally { setAssignBusy(false); }
  }

  return <>
    <style>{`.loss-intro{display:flex;justify-content:space-between;gap:25px;align-items:flex-end;margin:-5px 0 25px}.loss-intro h1{margin:0;color:#183f31;font:500 35px Georgia,serif}.loss-intro p{max-width:530px;margin:8px 0 0;color:#7c8c83;font-size:11px;line-height:1.6}.loss-stats{display:grid;grid-template-columns:1.3fr 1fr 1fr;gap:14px;margin-bottom:22px}.loss-stat{display:flex;align-items:center;gap:14px;min-height:108px;padding:20px;border:1px solid #e3ece4;border-radius:14px;background:#fff;box-shadow:0 12px 28px #1a4c3510}.loss-stat.primary{color:#eef8eb;background:linear-gradient(120deg,#0f4b37,#397854)}.loss-stat-icon{display:grid;place-items:center;width:39px;height:39px;flex:none;border-radius:12px;color:#41805c;background:#e5f2e2}.loss-stat.primary .loss-stat-icon{color:#e3f5b9;background:#ffffff18}.loss-stat span{display:block;color:#8c9c92;font-size:10px}.loss-stat.primary span{color:#c3dec6}.loss-stat strong{display:block;margin-top:7px;color:#234d3c;font:500 24px Georgia,serif}.loss-stat.primary strong{color:#f0f9e9}.loss-stat small{display:block;margin-top:4px;color:#9aa9a0;font-size:9px}.loss-stat.primary small{color:#c6dfc8}.loss-toolbar{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:20px 22px;border-bottom:1px solid #edf1ee}.loss-table{padding:0 22px}.loss-row{display:grid;grid-template-columns:1.25fr 1fr .85fr .8fr .9fr 1fr 28px;align-items:center;gap:13px;padding:17px 0;border-bottom:1px solid #edf1ee;cursor:pointer;user-select:none}.loss-row:hover{background:#fafdfb}.loss-row.loss-row-active{background:#f5fbf5;border-bottom-color:transparent}.loss-row:last-child{border-bottom:0}.loss-row.header{padding:11px 0;color:#96a39b;font-size:9px;text-transform:uppercase;letter-spacing:.1em;cursor:default;background:transparent!important}.loss-event{display:flex;align-items:center;gap:10px;min-width:0}.loss-event-icon{display:grid;place-items:center;width:35px;height:35px;flex:none;border-radius:11px}.loss-event-icon.blue{color:#467ca0;background:#e4f0f7}.loss-event-icon.gold{color:#a47a36;background:#faefd2}.loss-event-icon.rose{color:#b76e64;background:#f9e5e1}.loss-event-icon.green{color:#3b7a56;background:#e3f2df}.loss-event strong{display:block;color:#315646;font-size:11px}.loss-event small,.loss-cell small{display:block;margin-top:4px;color:#9ba9a1;font-size:9px}.loss-cell{color:#516e60;font-size:10px}.loss-status{display:inline-flex;width:max-content;padding:5px 8px;border-radius:999px;color:#4f7b60;background:#e7f3e7;font-size:8px;font-weight:800;letter-spacing:.06em}.loss-status.submitted{color:#9a6a3e;background:#fbefd9}.loss-status.surveyor-assigned{color:#6e6c9f;background:#ecebf8}.loss-expand-btn{display:grid;place-items:center;color:#a0b0a8}.loss-empty{padding:65px 20px;color:#9aa89f;text-align:center}.loss-empty strong{display:block;margin:12px 0 5px;color:#426354;font:500 19px Georgia,serif}.loss-empty span{font-size:10px}.loss-form{margin-bottom:20px}.loss-form form{padding:0 22px 22px}.loss-form .form-grid{padding:12px 0 15px}.loss-form .form-actions{display:flex;gap:9px}
/* Severity derived info */
.loss-field-hint{display:block;margin-top:5px;font-size:9px;color:#6a9278;font-weight:400}.loss-derived-strip{display:flex;align-items:center;gap:14px;grid-column:1/-1;padding:10px 14px;border:1px solid #c8e6d4;border-radius:8px;background:#f3faf5;font-size:10px;color:#2e6646}.loss-derived-strip b{font-weight:700}.loss-derived-sep{width:1px;height:14px;background:#b5d9c5}
/* GPS location card */
.location-card{border:1px solid #c5ddca;border-radius:10px;overflow:hidden;background:#f5fbf3;margin-bottom:4px}.location-card-meta{display:flex;align-items:center;justify-content:space-between;padding:10px 14px;gap:10px}.location-card-coords{display:flex;align-items:center;gap:7px;color:#2a5e41;font-size:10px;font-weight:600}.location-card-coords svg{color:#3a8056;flex:none}.location-card-clear{border:1px solid #c5ddca;border-radius:6px;padding:4px 10px;color:#4d7a60;background:#eaf4e6;font-size:9px;cursor:pointer;white-space:nowrap}.location-section{display:flex;flex-direction:column;gap:8px}.locate-row{display:flex;align-items:center;gap:9px}.locate-hint{font-size:9px;color:#8ea59a}
/* Evidence & detail */
.loss-detail-wrap{border-bottom:1px solid #edf1ee}.loss-detail-panel{background:#f8fcf8;padding:18px 22px 20px;border-top:1px solid #e8f0e8}.loss-assign-action{margin-bottom:14px;padding-bottom:14px;border-bottom:1px solid #e8f0e8;display:flex;align-items:center;gap:10px}.loss-detail-inner{display:grid;grid-template-columns:1.5fr 1fr;gap:18px}.loss-detail-section-label{margin:0 0 9px;color:#3d6b52;font-size:9px;font-weight:700;text-transform:uppercase;letter-spacing:.07em}.loss-detail-coords{margin-top:6px;font-size:9px;color:#7a9a88}.loss-detail-map{border-radius:10px;overflow:hidden;border:1px solid #d5e8da}.evidence-grid{display:flex;flex-wrap:wrap;gap:8px}.evidence-thumb{width:82px;height:82px;object-fit:cover;border-radius:8px;border:1px solid #d5e8da;cursor:pointer;transition:opacity .15s}.evidence-thumb:hover{opacity:.85}.evidence-placeholder{width:82px;height:82px;border-radius:8px;display:grid;place-items:center;font-size:8px;color:#9aaa9f}.evidence-loading{background:linear-gradient(90deg,#edf5ea 25%,#daefd6 50%,#edf5ea 75%);background-size:200%;animation:shimmer 1.4s infinite}.evidence-error{background:#f5f5f5;border:1px solid #e5e5e5}.evidence-empty{font-size:10px;color:#9ba99f;padding:8px 0}
/* File picker */
.file-section{display:flex;flex-direction:column;gap:8px}.file-section-label{font-size:9px;color:#728179;font-weight:600;display:flex;align-items:center;gap:5px}.file-pick-btn{display:inline-flex;align-items:center;gap:7px;width:max-content;padding:8px 14px;border:1px dashed #b0ccb8;border-radius:8px;color:#3d6e52;background:#f6fdf7;font-size:10px;cursor:pointer;transition:background .15s,border-color .15s}.file-pick-btn:hover{background:#ecf8ee;border-color:#82b090}.file-pick-btn input{display:none}.file-chips{display:flex;flex-wrap:wrap;gap:5px;margin-top:2px}.file-chip{display:flex;align-items:center;gap:5px;padding:3px 8px 3px 10px;border:1px solid #d3e8d8;border-radius:999px;color:#3a6b50;background:#edf7ea;font-size:9px;max-width:180px}.file-chip span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.file-chip-remove{border:none;background:none;color:#8aaa94;cursor:pointer;font-size:14px;line-height:1;padding:0;flex:none;display:grid;place-items:center}
/* Assign surveyor modal */
.assign-backdrop{position:fixed;inset:0;z-index:1000;display:grid;place-items:center;padding:20px;background:#102f2490;backdrop-filter:blur(4px)}.assign-modal{width:min(440px,100%);border:1px solid #dce8df;border-radius:16px;background:#fff;box-shadow:0 24px 70px #0c332c40;overflow:hidden}.assign-modal-head{display:flex;justify-content:space-between;gap:20px;padding:22px 24px 16px;border-bottom:1px solid #edf1ee;background:linear-gradient(135deg,#f8fbf6,#fff)}.assign-modal-head h2{margin:7px 0 4px;color:#244b3b;font:500 21px Georgia,serif}.assign-modal-head p{margin:0;color:#8c9a92;font-size:10px}.assign-modal-close{width:30px;height:30px;border:0;border-radius:50%;color:#6f8378;background:#eef5ed;font-size:22px;line-height:1;cursor:pointer}.assign-modal-body{padding:20px 24px;display:flex;flex-direction:column;gap:14px}.assign-modal-body label{display:flex;flex-direction:column;gap:6px;color:#426354;font-size:10px;font-weight:600}.assign-modal-body select,.assign-modal-body input[type=date]{border:1px solid #dce8df;border-radius:8px;padding:10px 12px;outline:none;color:#315849;background:#fbfdfb;font:inherit;font-size:11px}.assign-modal-body select:focus,.assign-modal-body input[type=date]:focus{border-color:#76a080;box-shadow:0 0 0 3px #dceedd}.assign-modal-foot{display:flex;justify-content:flex-end;gap:9px;padding:16px 24px;border-top:1px solid #edf1ee}.assign-modal-foot .secondary-button,.assign-modal-foot .primary-button{height:38px;padding:0 18px}.assign-modal.assign-modal-wide{width:min(760px,100%)}.assign-modal-wide .assign-modal-body{max-height:66vh;overflow-y:auto}
@keyframes shimmer{0%{background-position:200% 0}100%{background-position:-200% 0}}
/* Address display */
.loss-detail-addr{margin-top:6px;font-size:9px;color:#5a7a6a;line-height:1.5;max-width:300px;display:flex;align-items:flex-start;gap:4px}
/* Lightbox */
.lightbox-bd{position:fixed;inset:0;z-index:2000;display:grid;place-items:center;background:#000000cc;backdrop-filter:blur(4px);cursor:zoom-out}.lightbox-img{max-width:90vw;max-height:88vh;object-fit:contain;border-radius:10px;box-shadow:0 24px 80px #0009;cursor:default}.lightbox-close{position:fixed;top:18px;right:22px;width:38px;height:38px;border:0;border-radius:50%;background:#ffffff20;color:#fff;font-size:24px;line-height:1;cursor:pointer;display:grid;place-items:center;transition:background .15s}.lightbox-close:hover{background:#ffffff40}
@media(max-width:900px){.loss-stats{grid-template-columns:1fr 1fr}.loss-stat.primary{grid-column:1/-1}.loss-row{grid-template-columns:1.4fr 1fr 1fr 28px}.loss-row.header{display:none}.loss-row .loss-cell:nth-child(3),.loss-row .loss-cell:nth-child(4){display:none}.loss-detail-inner{grid-template-columns:1fr}}@media(max-width:620px){.loss-intro{display:block}.loss-intro .primary-button{margin-top:17px}.loss-stats{grid-template-columns:1fr}.loss-stat.primary{grid-column:auto}.loss-toolbar{display:block}.loss-row{grid-template-columns:1fr auto 28px}.loss-row .loss-cell:nth-child(2),.loss-row .loss-cell:nth-child(3){display:none}.loss-row .loss-cell:nth-child(5){grid-column:1}.loss-row .loss-cell:nth-child(6){grid-column:2;grid-row:1/3}}`}</style>

    <section className="loss-intro">
      <div>
        <p className="eyebrow">{isFarmer ? "FARMER PORTAL / FIELD MONITORING" : "OPERATIONS / LOSS NOTIFICATIONS"}</p>
        <h1>Loss reports</h1>
        <p>{isFarmer ? "Capture crop damage with GPS evidence, photos and timely updates so every field event moves through assessment." : "Review submitted loss notifications and assign field surveyors to complete on-site assessments."}</p>
      </div>
      {isFarmer && <button className="primary-button" onClick={() => setOpen(!open)}><Plus size={15} /> Report a loss</button>}
    </section>

    <PageState {...profile} /><PageState {...losses} />

    <section className="loss-stats">
      <div className="loss-stat primary"><div className="loss-stat-icon"><Crosshair size={18} /></div><div><span>Field events recorded</span><strong>{rows.length}</strong><small>Live loss notifications</small></div></div>
      <div className="loss-stat"><div className="loss-stat-icon"><MapPin size={18} /></div><div><span>Assessed events</span><strong>{surveyed}</strong><small>Surveyed or processed</small></div></div>
      <div className="loss-stat"><div className="loss-stat-icon"><AlertTriangle size={18} /></div><div><span>Average estimated loss</span><strong>{avgLoss}%</strong><small>Across reported events</small></div></div>
    </section>

    {isFarmer && open && (
      <section className="panel loss-form farmer-form">
        <div className="panel-heading"><div><span className="panel-kicker">LOSS NOTIFICATION</span><h2>Report crop damage</h2><p className="panel-description">Capture the GPS location and upload photographic evidence.</p></div></div>
        <form onSubmit={create}>
          <div className="form-grid">
            <label className="full-span">Which policy / field is affected?
              <select required value={form.policyId} onChange={handlePolicyChange}>
                <option value="">Select a policy</option>
                {policyRows.map((p) => <option key={p.id} value={p.id}>{p.policyNumber || `Policy #${p.id}`} · {p.cropName} · Total field: {p.sownAreaHa} ha</option>)}
              </select>
              {selectedPolicy && <span className="loss-field-hint">Total insured field area: {selectedPolicy.sownAreaHa} ha — {selectedPolicy.cropName}, {selectedPolicy.season} season</span>}
            </label>
            <label>What caused the damage?
              <select value={form.lossType} onChange={(e) => setForm({ ...form, lossType: e.target.value })}>
                {lossTypes.map((t) => <option key={t}>{t}</option>)}
              </select>
            </label>
            <label className="full-span" style={{ gridColumn: "2 / -1" }}>How much of your field was damaged?
              <select required value={form.severityKey} onChange={(e) => applyPreset(e.target.value, sownAreaHa)}>
                <option value="">Select damage extent</option>
                {SEVERITY_OPTIONS.map((o) => <option key={o.key} value={o.key}>{o.label}</option>)}
              </select>
            </label>
            {form.severityKey && form.severityKey !== "manual" && (
              <div className="loss-derived-strip">
                <span>Crop loss estimate: <b>{form.lossPercentageEstimate}%</b></span>
                <div className="loss-derived-sep" />
                <span>Area affected: <b>{form.affectedAreaHa} ha</b>{sownAreaHa > 0 ? ` out of ${sownAreaHa} ha total field` : ""}</span>
              </div>
            )}
            {form.severityKey === "manual" && <>
              <label>Affected area (in hectares)<input required min="0.01" step="0.01" type="number" value={form.affectedAreaHa} onChange={(e) => setForm({ ...form, affectedAreaHa: e.target.value })} /></label>
              <label>Estimated crop loss (%)<input required min="0" max="100" step="0.01" type="number" value={form.lossPercentageEstimate} onChange={(e) => setForm({ ...form, lossPercentageEstimate: e.target.value })} /></label>
            </>}

            <div className="full-span location-section">
              <span style={{ fontSize: 9, color: "#728179", fontWeight: 600 }}>FIELD LOCATION (GPS REQUIRED)</span>
              <div className="location-card">
                {hasCoords && (
                  <div className="location-card-meta">
                    <div className="location-card-coords"><MapPin size={13} />{parseFloat(form.geoLat).toFixed(5)}° N, {parseFloat(form.geoLng).toFixed(5)}° E</div>
                    <button type="button" className="location-card-clear" onClick={() => setForm((p) => ({ ...p, geoLat: "", geoLng: "" }))}><X size={10} style={{ display: "inline", marginRight: 3 }} />Clear</button>
                  </div>
                )}
                <LossMap lat={hasCoords ? parseFloat(form.geoLat) : null} lng={hasCoords ? parseFloat(form.geoLng) : null} height={200} />
              </div>
              <div className="locate-row">
                <button type="button" className="secondary-button" onClick={locate} disabled={locating} style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <MapPin size={14} />{locating ? "Locating…" : hasCoords ? "Re-capture location" : "Capture GPS location"}
                </button>
                {!hasCoords && <span className="locate-hint">Showing India overview — capture GPS to pin your exact field</span>}
              </div>
            </div>

            <div className="full-span file-section">
              <span className="file-section-label"><Camera size={12} />Evidence photos (optional, images only, max 10 MB each)</span>
              <label className="file-pick-btn"><Camera size={14} /> Add photos<input type="file" accept="image/*" multiple onChange={handleFiles} /></label>
              {files.length > 0 && (
                <div className="file-chips">
                  {files.map((file, i) => (
                    <div key={i} className="file-chip"><span>{file.name}</span><button type="button" className="file-chip-remove" onClick={() => removeFile(i)} aria-label="Remove file">×</button></div>
                  ))}
                </div>
              )}
            </div>
          </div>
          <div className="form-actions">
            <button className="primary-button" type="submit" disabled={busy || locating}>{uploading ? "Uploading photos…" : busy ? "Submitting…" : "Submit loss report"}</button>
          </div>
        </form>
      </section>
    )}

    <section className="panel">
      <div className="loss-toolbar">
        <DataToolbar value={search} onChange={setSearch} placeholder="Search loss type, status or policy…" />
        <span className="muted">{filtered.length} reports</span>
      </div>
      <div className="loss-table">
        <div className="loss-row header"><span>Field event</span><span>Policy</span><span>Area</span><span>Severity</span><span>Reported</span><span>Status</span><span /></div>
        {filtered.length ? filtered.map((loss) => (
          <div key={loss.id} className="loss-detail-wrap">
            <div className={`loss-row${expandedId === loss.id ? " loss-row-active" : ""}`} onClick={() => canAssign ? openDetails(loss) : expand(loss.id)}>
              <div className="loss-event">
                <span className={`loss-event-icon ${tone(loss.lossType)}`}>{iconFor(loss.lossType)}</span>
                <div><strong>{loss.lossType || "Crop loss"}</strong><small>Report #{loss.id}</small></div>
              </div>
              <div className="loss-cell">{loss.policy?.policyNumber || `Policy #${loss.policyId || "—"}`}<small>{loss.policy?.cropName || "Crop policy"}</small></div>
              <div className="loss-cell">{loss.affectedAreaHa || 0} ha</div>
              <div className="loss-cell">{loss.lossPercentageEstimate ?? 0}%</div>
              <div className="loss-cell">{loss.notificationDate ? new Date(loss.notificationDate).toLocaleDateString("en-IN", { day: "2-digit", month: "short" }) : "—"}</div>
              <span className={`loss-status ${String(loss.status || "").toLowerCase().replaceAll("_", "-")}`}>{loss.status || "UNKNOWN"}</span>
              <span className="loss-expand-btn" title={canAssign ? "View details" : undefined}>{canAssign ? <FileText size={15} /> : (expandedId === loss.id ? <ChevronUp size={15} /> : <ChevronDown size={15} />)}</span>
            </div>
            {!canAssign && expandedId === loss.id && (
              <div className="loss-detail-panel">
                <div className="loss-detail-inner">
                  <div>
                    <p className="loss-detail-section-label">Field location</p>
                    {loss.geoLat && loss.geoLng ? (
                      <>
                        <div className="loss-detail-map"><LossMap lat={Number(loss.geoLat)} lng={Number(loss.geoLng)} height={200} /></div>
                        {geoAddresses[loss.id] ? (
                          <p className="loss-detail-addr"><MapPin size={10} style={{ display: "inline", marginRight: 4 }} />{geoAddresses[loss.id]}</p>
                        ) : (
                          <p className="loss-detail-coords">{Number(loss.geoLat).toFixed(5)}° N, {Number(loss.geoLng).toFixed(5)}° E</p>
                        )}
                      </>
                    ) : (
                      <div className="evidence-empty">No location recorded</div>
                    )}
                  </div>
                  <div>
                    <p className="loss-detail-section-label">Evidence photos</p>
                    {!details[loss.id] && !detailError[loss.id] ? (
                      <div className="evidence-grid"><div className="evidence-placeholder evidence-loading" /><div className="evidence-placeholder evidence-loading" /></div>
                    ) : detailError[loss.id] ? (
                      <div className="evidence-empty">Could not load evidence photos</div>
                    ) : details[loss.id]?.photoUrls?.length ? (
                      <><div className="evidence-grid">{details[loss.id].photoUrls.map((url: string) => <EvidenceImage key={url} path={url} onOpen={setLightbox} />)}</div>
                      <p className="loss-detail-coords" style={{ marginTop: 6 }}>Click a photo to view full size</p></>
                    ) : (
                      <div className="evidence-empty">No evidence photos submitted</div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        )) : (
          <div className="loss-empty">
            <Camera size={28} color="#a5c88c" />
            <strong>No matching loss reports</strong>
            <span>Submitted field events will appear here with their assessment journey.</span>
          </div>
        )}
      </div>
    </section>

    {lightbox && (
      <div className="lightbox-bd" onClick={() => setLightbox(null)}>
        <img src={lightbox} className="lightbox-img" alt="Field evidence" onClick={(e) => e.stopPropagation()} />
        <button className="lightbox-close" onClick={() => setLightbox(null)}>×</button>
      </div>
    )}

    {detailsModal != null && (() => {
      const loss = rows.find((r) => r.id === detailsModal);
      if (!loss) return null;
      const detail = details[loss.id];
      const policy = loss.policyId ? policyDetails[loss.policyId] : null;
      const showAssign = canAssign && loss.status === "SUBMITTED";
      return (
        <div className="assign-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setDetailsModal(null)}>
          <div className="assign-modal assign-modal-wide" role="dialog" aria-modal="true">
            <div className="assign-modal-head">
              <div><span className="panel-kicker">LOSS NOTIFICATION DETAILS</span><h2>Report #{loss.id}</h2><p>{loss.policy?.policyNumber || `Policy #${loss.policyId}`}{policy?.farmerName ? ` · Farmer: ${policy.farmerName}` : ""}</p></div>
              <button className="assign-modal-close" onClick={() => setDetailsModal(null)}>×</button>
            </div>
            <div className="assign-modal-body">
              <div className="loss-detail-inner">
                <div>
                  <p className="loss-detail-section-label">Farmer &amp; policy</p>
                  {policyDetailError[loss.policyId] ? (
                    <div className="evidence-empty">Could not load policy details</div>
                  ) : !policy ? (
                    <div className="evidence-empty">Loading policy details…</div>
                  ) : (
                    <>
                      <p className="loss-detail-coords"><b>{policy.farmerName || "—"}</b></p>
                      <p className="loss-detail-coords">{policy.policyNumber} · {policy.cropName} ({policy.season})</p>
                      <p className="loss-detail-coords">Sown area: {policy.sownAreaHa} ha · Sum insured: {money(policy.sumInsured)}</p>
                    </>
                  )}
                  <p className="loss-detail-section-label" style={{ marginTop: 14 }}>Loss event</p>
                  <p className="loss-detail-coords">Type: <b>{loss.lossType}</b> · Status: <b>{loss.status}</b></p>
                  <p className="loss-detail-coords">Reported: {loss.notificationDate ? new Date(loss.notificationDate).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "—"}</p>
                  <p className="loss-detail-coords">Affected area: {loss.affectedAreaHa || 0} ha · Estimated loss: {loss.lossPercentageEstimate ?? 0}%</p>
                </div>
                <div>
                  <p className="loss-detail-section-label">Field location</p>
                  {loss.geoLat && loss.geoLng ? (
                    <>
                      <div className="loss-detail-map"><LossMap lat={Number(loss.geoLat)} lng={Number(loss.geoLng)} height={170} /></div>
                      {geoAddresses[loss.id] ? (
                        <p className="loss-detail-addr"><MapPin size={10} style={{ display: "inline", marginRight: 4 }} />{geoAddresses[loss.id]}</p>
                      ) : (
                        <p className="loss-detail-coords">{Number(loss.geoLat).toFixed(5)}° N, {Number(loss.geoLng).toFixed(5)}° E</p>
                      )}
                    </>
                  ) : (
                    <div className="evidence-empty">No location recorded</div>
                  )}
                </div>
              </div>

              <p className="loss-detail-section-label" style={{ marginTop: 16 }}>Evidence photos</p>
              {!detail && !detailError[loss.id] ? (
                <div className="evidence-grid"><div className="evidence-placeholder evidence-loading" /><div className="evidence-placeholder evidence-loading" /></div>
              ) : detailError[loss.id] ? (
                <div className="evidence-empty">Could not load evidence photos</div>
              ) : detail?.photoUrls?.length ? (
                <div className="evidence-grid">{detail.photoUrls.map((url: string) => <EvidenceImage key={url} path={url} onOpen={setLightbox} />)}</div>
              ) : (
                <div className="evidence-empty">No evidence photos submitted</div>
              )}

              {showAssign && (
                <>
                  <p className="loss-detail-section-label" style={{ marginTop: 16 }}>Assign surveyor</p>
                  <label>Surveyor
                    <select required value={assignForm.surveyorId} onChange={(e) => setAssignForm({ ...assignForm, surveyorId: e.target.value })}>
                      <option value="">Select surveyor</option>
                      {surveyors.map((s: any) => <option key={s.id} value={s.id}>{s.name} ({s.userId}) — {workloadBySurveyor[s.id] ? `${workloadBySurveyor[s.id]} active assignment${workloadBySurveyor[s.id] > 1 ? "s" : ""}` : "available"}</option>)}
                    </select>
                  </label>
                  <label>Survey date
                    <input required type="date" min={new Date().toISOString().slice(0, 10)} value={assignForm.surveyDate} onChange={(e) => setAssignForm({ ...assignForm, surveyDate: e.target.value })} />
                  </label>
                  {dateConflicts > 0 && <p className="loss-detail-coords" style={{ color: "#a4713f" }}>⚠ This surveyor already has {dateConflicts} active assignment{dateConflicts > 1 ? "s" : ""} scheduled on this date.</p>}
                </>
              )}
            </div>
            <div className="assign-modal-foot">
              <button type="button" className="secondary-button" onClick={() => setDetailsModal(null)} disabled={assignBusy}>Close</button>
              {showAssign && (
                <button type="button" className="primary-button" onClick={assignSurveyor} disabled={assignBusy || !assignForm.surveyorId || !assignForm.surveyDate}>
                  {assignBusy ? "Assigning…" : "Assign Surveyor"}
                </button>
              )}
            </div>
          </div>
        </div>
      );
    })()}
  </>;
}
