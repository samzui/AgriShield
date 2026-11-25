import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, Banknote, CheckCircle2, Clock3, Droplets, FileCheck2, MapPin, Plus, ShieldCheck, Sparkles, Sprout, Waves, XCircle } from "lucide-react";
import { api, authToken, API_BASE_URL } from "../../services/api";
import { PageState, handleError, useApi } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";
import LossMap from "../../components/dashboard/LossMap";

const money = (v: any) => `₹${Number(v || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
const tone = (s: string) => ({ APPROVED: "approved", PAID: "paid", REJECTED: "rejected", UNDER_REVIEW: "review" } as any)[s] || "pending";
const statusIcon = (s: string) => s === "APPROVED" ? <CheckCircle2 size={16} /> : s === "PAID" ? <Banknote size={16} /> : s === "REJECTED" ? <XCircle size={16} /> : <Clock3 size={16} />;
const lossTone = (t: string) => t === "FLOOD" ? "blue" : t === "DROUGHT" ? "gold" : t === "PEST" ? "rose" : "green";
const lossIcon = (t: string) => t === "FLOOD" ? <Waves size={16} /> : t === "DROUGHT" ? <Sprout size={16} /> : t === "PEST" ? <AlertTriangle size={16} /> : <Droplets size={16} />;
const SURVEY_ELIGIBLE_STATUSES = ["SUBMITTED", "VERIFIED"];

function ClaimEvidenceImage({ path, onOpen }: { path: string; onOpen: (src: string) => void }) {
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
  return <img src={blobSrc} className="evidence-thumb" alt="Claim evidence" onClick={() => onOpen(blobSrc)} />;
}

type ModalType = "view" | "review" | "approve" | "reject" | "disburse";

export default function ClaimsPage() {
  const profile = useApi(api.profile);
  const pd: any = profile.data || {};
  const role = String(pd.role || "").toUpperCase();
  const farmer = role === "FARMER";
  const isIO = ["INSURANCE_OFFICER", "STATE_OFFICER", "ADMIN"].includes(role);
  const isBankOfficer = role === "BANK_OFFICER";
  const isAdmin = role === "ADMIN";
  // Admin can review/monitor claims but approval and rejection remain the Insurance Officer's responsibility.
  const canDecide = ["INSURANCE_OFFICER", "STATE_OFFICER"].includes(role);

  const claims = useApi(() => api.claims(0, 100));
  const policies = useApi(() => farmer && pd.id ? api.farmerPolicies(Number(pd.id)) : Promise.resolve([]), [farmer, pd.id]);
  const farmerSurveys = useApi(() => (farmer ? api.surveys() : Promise.resolve([])), [farmer]);
  const farmerLosses = useApi(() => (farmer ? api.losses() : Promise.resolve([])), [farmer]);

  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);
  const [claimStep, setClaimStep] = useState<1 | 2 | 3>(1);
  const [selectedSurveyId, setSelectedSurveyId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [modal, setModal] = useState<{ type: ModalType; claim: any } | null>(null);
  const [mForm, setMForm] = useState({ amount: "", reason: "", utr: "" });
  const [mBusy, setMBusy] = useState(false);
  const [lightbox, setLightbox] = useState<string | null>(null);
  const [history, setHistory] = useState<any[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [geoAddress, setGeoAddress] = useState("");

  const rows: any[] = Array.isArray(claims.data) ? claims.data : (claims.data as any)?.content || [];
  const policyRows: any[] = Array.isArray(policies.data) ? policies.data : [];
  const surveyRows: any[] = Array.isArray(farmerSurveys.data) ? farmerSurveys.data : [];
  const lossRows: any[] = Array.isArray(farmerLosses.data) ? farmerLosses.data : [];
  const filtered = useMemo(() => rows.filter((r) => JSON.stringify(r).toLowerCase().includes(search.toLowerCase())), [rows, search]);
  const displayRows = isBankOfficer ? filtered.filter((r) => ["APPROVED", "PAID"].includes(r.status)) : filtered;
  const approved = rows.filter((r) => ["APPROVED", "PAID"].includes(r.status)).reduce((sum, r) => sum + Number(r.approvedAmount || 0), 0);
  const pending = rows.filter((r) => ["INITIATED", "UNDER_REVIEW"].includes(r.status)).length;

  // A survey is claim-eligible once fieldwork is submitted/verified and no claim has been filed against it yet —
  // mirrors ClaimService.initiate()'s own status check and duplicate-claim guard.
  const claimedSurveyIds = useMemo(() => new Set(rows.map((c) => c.surveyId).filter((v) => v != null)), [rows]);
  const eligibleSurveys = useMemo(() => {
    if (!farmer) return [];
    return surveyRows
      .filter((s) => SURVEY_ELIGIBLE_STATUSES.includes(s.status) && !claimedSurveyIds.has(s.id))
      .map((s) => {
        const loss = lossRows.find((l) => l.id === s.notificationId) || null;
        const policy = policyRows.find((p) => p.id === s.policy?.id) || s.policy || null;
        const sumInsured = Number(policy?.sumInsured || 0);
        const lossPct = Number(s.lossAssessedPercentage || 0);
        const assessedAmount = Math.round(sumInsured * lossPct) / 100;
        return { survey: s, loss, policy, assessedAmount };
      })
      .filter((e) => e.assessedAmount > 0)
      .sort((a, b) => b.survey.id - a.survey.id);
  }, [farmer, surveyRows, lossRows, policyRows, claimedSurveyIds]);
  const selectedEligible = eligibleSurveys.find((e) => e.survey.id === selectedSurveyId) || null;

  function openModal(type: ModalType, claim: any) {
    setMForm({ amount: "", reason: "", utr: "" });
    setModal({ type, claim });
    setHistory([]); setGeoAddress("");
    if (type === "view") {
      setHistoryLoading(true);
      api.claimHistory(claim.id).then((data: any) => setHistory(Array.isArray(data) ? data : [])).catch(() => setHistory([])).finally(() => setHistoryLoading(false));
      const lat = claim.survey?.loss?.geoLat, lng = claim.survey?.loss?.geoLng;
      if (lat && lng) {
        fetch(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`, { headers: { "Accept-Language": "en" } })
          .then((r) => (r.ok ? r.json() : null))
          .then((data) => { if (data?.display_name) setGeoAddress(data.display_name); })
          .catch(() => { /* best-effort geocoding */ });
      }
    }
  }

  function toggleWizard() {
    if (open) { setOpen(false); return; }
    setClaimStep(1); setSelectedSurveyId(null); setOpen(true);
  }
  function selectEligible(surveyId: number) { setSelectedSurveyId(surveyId); setClaimStep(2); }

  async function submitClaim() {
    if (!selectedEligible) return;
    setSubmitting(true);
    try {
      await api.initiateClaim({ policyId: selectedEligible.policy.id, surveyId: selectedEligible.survey.id, claimedAmount: selectedEligible.assessedAmount });
      toast.success("Claim initiated");
      setOpen(false); setSelectedSurveyId(null); setClaimStep(1);
      claims.reload();
    } catch (error) { handleError(error); } finally { setSubmitting(false); }
  }

  async function doAction() {
    if (!modal) return;
    setMBusy(true);
    try {
      const id = modal.claim.id;
      if (modal.type === "review") await api.reviewClaim(id);
      if (modal.type === "approve") await api.approveClaim(id, { approvedAmount: Number(mForm.amount), reason: mForm.reason });
      if (modal.type === "reject") await api.rejectClaim(id, mForm.reason);
      if (modal.type === "disburse") await api.disburseClaim(id, mForm.utr);
      toast.success("Claim updated successfully");
      setModal(null);
      claims.reload();
    } catch (error) { handleError(error); } finally { setMBusy(false); }
  }

  const modalTitle: Record<ModalType, string> = { view: "Claim Details", review: "Mark Under Review", approve: "Approve Claim", reject: "Reject Claim", disburse: "Disburse Claim" };
  const modalKicker: Record<ModalType, string> = { view: "CLAIM DETAILS", review: "REVIEW ACTION", approve: "APPROVAL", reject: "REJECTION", disburse: "DISBURSEMENT" };
  const canSubmitModal = modal && (
    modal.type === "view" ? false :
    modal.type === "review" ? true :
    modal.type === "approve" ? !!mForm.amount :
    modal.type === "reject" ? !!mForm.reason :
    !!mForm.utr
  );

  return <>
    <style>{`.claim-intro{display:flex;justify-content:space-between;gap:25px;align-items:flex-end;margin:-5px 0 25px}.claim-intro h1{margin:0;color:#183f31;font:500 35px Georgia,serif}.claim-intro p{max-width:540px;margin:8px 0 0;color:#7c8c83;font-size:11px;line-height:1.6}.claim-actions-head{display:flex;gap:9px}.claim-metrics{display:grid;grid-template-columns:1.25fr 1fr 1fr;gap:14px;margin-bottom:22px}.claim-metric{position:relative;overflow:hidden;min-height:113px;padding:20px;border:1px solid #e3ece4;border-radius:14px;background:#fff;box-shadow:0 12px 28px #1a4c3510}.claim-metric.primary{color:#eef8eb;background:linear-gradient(120deg,#0f4b37,#397854)}.claim-metric:after{content:"";position:absolute;width:100px;height:100px;right:-38px;bottom:-51px;border-radius:50%;background:#c8e99d35}.claim-metric span{display:block;color:#89998f;font-size:10px}.claim-metric.primary span{color:#c5dfc6}.claim-metric strong{display:block;margin-top:10px;color:#254e3d;font:500 24px Georgia,serif}.claim-metric.primary strong{color:#f1f9e9}.claim-metric small{display:block;margin-top:5px;color:#9aa89f;font-size:9px}.claim-metric.primary small{color:#c6dfc8}.claim-metric-icon{display:grid;place-items:center;width:31px;height:31px;margin-bottom:10px;border-radius:9px;color:#43805a;background:#e5f2e2}.claim-metric.primary .claim-metric-icon{color:#dff2b5;background:#ffffff18}.claim-toolbar{display:flex;align-items:center;justify-content:space-between;gap:13px;padding:20px 23px;border-bottom:1px solid #edf1ee}.claim-table{padding:0 23px}.claim-row{display:grid;grid-template-columns:1.1fr 1fr .75fr .85fr .85fr .9fr 1.1fr;align-items:center;gap:12px;padding:16px 0;border-bottom:1px solid #edf1ee}.claim-row:last-child{border-bottom:0}.claim-row.header{padding:10px 0;color:#96a39b;font-size:9px;text-transform:uppercase;letter-spacing:.1em}.claim-main{display:flex;align-items:center;gap:10px;min-width:0}.claim-icon{display:grid;place-items:center;width:35px;height:35px;flex:none;border-radius:11px;color:#5974a0;background:#e9eef8}.claim-main strong{display:block;color:#315646;font-size:11px}.claim-main small,.claim-cell small{display:block;margin-top:4px;color:#9ba9a1;font-size:9px}.claim-cell{color:#526e60;font-size:10px}.claim-status{display:inline-flex;align-items:center;gap:5px;width:max-content;padding:5px 8px;border-radius:999px;color:#9a6a3e;background:#fbefd9;font-size:8px;font-weight:800;letter-spacing:.05em}.claim-status.approved{color:#4f7b60;background:#e7f3e7}.claim-status.paid{color:#5d6e99;background:#e9edfb}.claim-status.rejected{color:#9a625b;background:#fae5e1}.claim-status.review{color:#716e9f;background:#ecebf8}.claim-empty{padding:65px 20px;color:#9aa89f;text-align:center}.claim-empty strong{display:block;margin:12px 0 5px;color:#426354;font:500 19px Georgia,serif}.claim-empty span{font-size:10px}.claim-form{margin-bottom:20px}.claim-form form{padding:0 23px 22px}.claim-actions-row{display:flex;gap:5px;flex-wrap:wrap}.claim-row-btn{height:26px;padding:0 9px;border:1px solid #dde8e1;border-radius:7px;background:#f4f9f5;color:#3d6b52;font-size:9px;font-weight:600;cursor:pointer;white-space:nowrap}.claim-row-btn:hover{background:#e8f4ec}.claim-row-btn.approve{color:#4f7b60;border-color:#b8d8c4;background:#edf7f0}.claim-row-btn.reject{color:#9a625b;border-color:#e8c4bf;background:#fdf0ee}.claim-row-btn.disburse{color:#5d6e99;border-color:#c4cce8;background:#edf0fb}.claim-utr{font-size:9px;color:#5d6e99;font-weight:600}
/* Claims modal */
.cm-backdrop{position:fixed;inset:0;z-index:1000;display:grid;place-items:center;padding:20px;background:#102f2490;backdrop-filter:blur(4px)}.cm{width:min(480px,100%);border:1px solid #dce8df;border-radius:16px;background:#fff;box-shadow:0 24px 70px #0c332c40;overflow:hidden}.cm-head{display:flex;justify-content:space-between;gap:20px;padding:22px 24px 16px;border-bottom:1px solid #edf1ee;background:linear-gradient(135deg,#f8fbf6,#fff)}.cm-head h2{margin:7px 0 4px;color:#244b3b;font:500 21px Georgia,serif}.cm-head p{margin:0;color:#8c9a92;font-size:10px}.cm-close{width:30px;height:30px;border:0;border-radius:50%;color:#6f8378;background:#eef5ed;font-size:22px;line-height:1;cursor:pointer}.cm-body{padding:20px 24px;display:flex;flex-direction:column;gap:13px}.cm-body label{display:flex;flex-direction:column;gap:6px;color:#426354;font-size:10px;font-weight:600}.cm-body label input{border:1px solid #dce8df;border-radius:8px;padding:10px 12px;outline:none;color:#315849;background:#fbfdfb;font:inherit;font-size:11px}.cm-body label input:focus{border-color:#76a080;box-shadow:0 0 0 3px #dceedd}.cm-info{margin:0;color:#516e60;font-size:10px;line-height:1.6;padding:10px 12px;background:#f5fbf7;border:1px solid #d5e8da;border-radius:8px}.cm-bank{display:flex;flex-direction:column;gap:5px;padding:13px 15px;border:1px solid #d5e8da;border-radius:9px;background:#f5fbf7;color:#3d6b52;font-size:10px}.cm-bank b{font-size:13px;color:#244b3b;margin-bottom:3px}.cm-foot{display:flex;justify-content:flex-end;gap:9px;padding:16px 24px;border-top:1px solid #edf1ee}.cm-foot .secondary-button,.cm-foot .primary-button{height:38px;padding:0 18px}.cm-foot .danger{background:#8b4040}
.cm.cm-wide{width:min(640px,100%)}.cm-wide .cm-body{max-height:64vh;overflow-y:auto}
.evidence-grid{display:flex;flex-wrap:wrap;gap:8px}.evidence-thumb{width:82px;height:82px;object-fit:cover;border-radius:8px;border:1px solid #d5e8da;cursor:pointer;transition:opacity .15s}.evidence-thumb:hover{opacity:.85}.evidence-placeholder{width:82px;height:82px;border-radius:8px;display:grid;place-items:center;font-size:8px;color:#9aaa9f}.evidence-loading{background:linear-gradient(90deg,#edf5ea 25%,#daefd6 50%,#edf5ea 75%);background-size:200%;animation:shimmer 1.4s infinite}.evidence-error{background:#f5f5f5;border:1px solid #e5e5e5}@keyframes shimmer{0%{background-position:200% 0}100%{background-position:-200% 0}}
.lightbox-bd{position:fixed;inset:0;z-index:2000;display:grid;place-items:center;background:#000000cc;backdrop-filter:blur(4px);cursor:zoom-out}.lightbox-img{max-width:90vw;max-height:88vh;object-fit:contain;border-radius:10px;box-shadow:0 24px 80px #0009;cursor:default}.lightbox-close{position:fixed;top:18px;right:22px;width:38px;height:38px;border:0;border-radius:50%;background:#ffffff20;color:#fff;font-size:24px;line-height:1;cursor:pointer;display:grid;place-items:center;transition:background .15s}.lightbox-close:hover{background:#ffffff40}
/* Farmer claim wizard: select surveyed loss -> review assessment -> submit */
.claim-wizard-steps{display:flex;align-items:center;gap:9px;padding:2px 23px 16px;flex-wrap:wrap}.claim-wizard-step{display:flex;align-items:center;gap:7px;font-size:9px;font-weight:600;color:#9aa89f}.claim-wizard-step span{display:grid;place-items:center;width:20px;height:20px;flex:none;border-radius:50%;background:#edf1ee;color:#8a9990;font-size:9px;font-weight:700}.claim-wizard-step.active{color:#254e3d}.claim-wizard-step.active span{background:#176044;color:#fff}.claim-wizard-step.done{color:#4f7b60}.claim-wizard-step.done span{background:#dcefd0;color:#3d6b52}.claim-wizard-step:not(:last-child):after{content:"";width:20px;height:1px;background:#e3ece4;margin-left:5px}
.claim-wizard-body{padding:0 23px 22px;display:flex;flex-direction:column;gap:14px}
.claim-eligible-list{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:12px}.claim-eligible-card{text-align:left;border:1px solid #e3ece4;border-radius:12px;background:#fff;padding:14px;display:flex;flex-direction:column;gap:10px;transition:border-color .15s,background .15s,box-shadow .15s}.claim-eligible-card:hover{border-color:#b8d8c4;background:#fafdfb}.claim-eligible-card.selected{border-color:#3d8b5f;background:#f3faf5;box-shadow:0 0 0 3px #dceedd}
.claim-eligible-head{display:flex;align-items:center;gap:10px}.claim-eligible-head strong{display:block;font-size:11px;color:#315646}.claim-eligible-head small{display:block;margin-top:2px;font-size:9px;color:#9ba9a1}
.claim-eligible-icon{display:grid;place-items:center;width:32px;height:32px;flex:none;border-radius:10px}.claim-eligible-icon.blue{color:#467ca0;background:#e4f0f7}.claim-eligible-icon.gold{color:#a47a36;background:#faefd2}.claim-eligible-icon.rose{color:#b76e64;background:#f9e5e1}.claim-eligible-icon.green{color:#3b7a56;background:#e3f2df}
.claim-eligible-meta{display:grid;grid-template-columns:1fr 1fr;gap:8px}.claim-eligible-meta div{display:flex;flex-direction:column;gap:2px}.claim-eligible-meta span{font-size:8px;color:#9aa89f;text-transform:uppercase;letter-spacing:.05em}.claim-eligible-meta b{font-size:10px;color:#345547;font-weight:600}
.claim-eligible-status{align-self:flex-start;padding:4px 8px;border-radius:999px;background:#ecebf8;color:#6e6c9f;font-size:8px;font-weight:800;letter-spacing:.05em}
.claim-review-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px 18px;padding:14px 16px;border:1px solid #d5e8da;border-radius:10px;background:#f5fbf7}.claim-review-grid div{display:flex;flex-direction:column;gap:3px}.claim-review-grid span{font-size:9px;color:#6a8577}.claim-review-grid b{font-size:11px;color:#244b3b;font-weight:600}
.claim-amount-strip{display:flex;align-items:center;justify-content:space-between;padding:14px 16px;border:1px solid #c8e6d4;border-radius:10px;background:#eef8ea}.claim-amount-strip span{font-size:10px;color:#2e6646;font-weight:600}.claim-amount-strip b{font:500 22px Georgia,serif;color:#183f31}
.claim-amount-note{margin:0;font-size:9px;color:#7c8c83;line-height:1.6}
@media(max-width:620px){.claim-eligible-list{grid-template-columns:1fr}.claim-eligible-meta{grid-template-columns:1fr 1fr}.claim-review-grid{grid-template-columns:1fr}}
@media(max-width:1000px){.claim-metrics{grid-template-columns:1fr 1fr}.claim-metric.primary{grid-column:1/-1}.claim-row{grid-template-columns:1.2fr 1fr .8fr .9fr}.claim-row.header{display:none}.claim-row .claim-cell:nth-child(3),.claim-row .claim-cell:nth-child(4),.claim-row .claim-cell:nth-child(5){display:none}}@media(max-width:620px){.claim-intro{display:block}.claim-actions-head{margin-top:17px}.claim-metrics{grid-template-columns:1fr}.claim-metric.primary{grid-column:auto}.claim-toolbar{display:block}.claim-row{grid-template-columns:1fr auto}.claim-row .claim-cell:nth-child(2),.claim-row .claim-cell:nth-child(3),.claim-row .claim-cell:nth-child(4){display:none}.claim-row .claim-cell:nth-child(5){grid-column:1}.claim-row .claim-cell:nth-child(6){grid-column:2;grid-row:1/3}}`}</style>

    <section className="claim-intro">
      <div>
        <p className="eyebrow">{farmer ? "FARMER PORTAL / FINANCIAL PROTECTION" : isBankOfficer ? "BANK / DISBURSEMENT" : "OPERATIONS / CLAIM LIFECYCLE"}</p>
        <h1>{farmer ? "My claims" : isBankOfficer ? "Disbursement queue" : "Claims"}</h1>
        <p>{farmer ? "Follow every claim from field assessment to approval and DBT settlement." : isBankOfficer ? "Review approved claims and process DBT disbursements." : isAdmin ? "Monitor claim status and lifecycle across the portfolio." : "Review, approve and disburse claims through a clear operational workflow."}</p>
      </div>
      <div className="claim-actions-head">
        {farmer && <button className="primary-button" onClick={toggleWizard}><Plus size={15} /> Initiate claim</button>}
      </div>
    </section>

    <PageState {...profile} /><PageState {...claims} />

    <section className="claim-metrics">
      <div className="claim-metric primary"><div className="claim-metric-icon"><ShieldCheck size={17} /></div><span>{isBankOfficer ? "Disbursable claims" : "Claims in portfolio"}</span><strong>{isBankOfficer ? displayRows.length : rows.length}</strong><small>{isBankOfficer ? "Approved and awaiting disbursement" : "Live records from the claims service"}</small></div>
      <div className="claim-metric"><div className="claim-metric-icon"><Banknote size={17} /></div><span>Approved value</span><strong>{money(approved)}</strong><small>Approved or paid amount</small></div>
      <div className="claim-metric"><div className="claim-metric-icon"><Clock3 size={17} /></div><span>Needs attention</span><strong>{pending}</strong><small>Initiated or under review</small></div>
    </section>

    {farmer && open && (
      <section className="panel claim-form farmer-form">
        <div className="panel-heading"><div><span className="panel-kicker">CLAIM SELF-SERVICE</span><h2>Initiate a claim</h2><p className="panel-description">A submitted field survey is required before initiating a claim.</p></div></div>

        <div className="claim-wizard-steps">
          <div className={`claim-wizard-step${claimStep === 1 ? " active" : claimStep > 1 ? " done" : ""}`}><span>1</span>Select surveyed loss</div>
          <div className={`claim-wizard-step${claimStep === 2 ? " active" : claimStep > 2 ? " done" : ""}`}><span>2</span>Review assessment</div>
          <div className={`claim-wizard-step${claimStep === 3 ? " active" : ""}`}><span>3</span>Submit claim</div>
        </div>

        {claimStep === 1 && (
          <div className="claim-wizard-body">
            {(farmerSurveys.loading || farmerLosses.loading || policies.loading) ? (
              <p className="muted" style={{ padding: "20px 0", margin: 0 }}>Loading eligible surveys…</p>
            ) : eligibleSurveys.length === 0 ? (
              <div className="claim-empty">
                <FileCheck2 size={26} color="#a5c88c" />
                <strong>No eligible surveyed losses</strong>
                <span>A field survey for your reported loss must be completed and submitted before a claim can be initiated. Track survey progress from the Losses page.</span>
              </div>
            ) : (
              <div className="claim-eligible-list">
                {eligibleSurveys.map(({ survey, loss, policy, assessedAmount }) => (
                  <button type="button" key={survey.id} className={`claim-eligible-card${selectedSurveyId === survey.id ? " selected" : ""}`} onClick={() => selectEligible(survey.id)}>
                    <div className="claim-eligible-head">
                      <span className={`claim-eligible-icon ${lossTone(loss?.lossType)}`}>{lossIcon(loss?.lossType)}</span>
                      <div><strong>{policy?.cropName || "Crop policy"}</strong><small>{policy?.policyNumber || `Policy #${policy?.id ?? "—"}`}</small></div>
                    </div>
                    <div className="claim-eligible-meta">
                      <div><span>Loss type</span><b>{loss?.lossType || "—"}</b></div>
                      <div><span>Reported</span><b>{loss?.notificationDate ? new Date(loss.notificationDate).toLocaleDateString("en-IN", { day: "2-digit", month: "short" }) : "—"}</b></div>
                      <div><span>Affected area</span><b>{loss?.affectedAreaHa || 0} ha</b></div>
                      <div><span>Surveyed loss</span><b>{Number(survey.lossAssessedPercentage || 0)}%</b></div>
                    </div>
                    <span className="claim-eligible-status">{survey.status}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {claimStep === 2 && selectedEligible && (
          <div className="claim-wizard-body">
            <p className="cm-info">Review the field assessment below — this is the eligible amount computed from your policy's sum insured and the surveyed loss percentage, so there's nothing to calculate.</p>
            <div className="claim-review-grid">
              <div><span>Crop</span><b>{selectedEligible.policy?.cropName || "—"}</b></div>
              <div><span>Policy</span><b>{selectedEligible.policy?.policyNumber || `#${selectedEligible.policy?.id ?? "—"}`}</b></div>
              <div><span>Loss type</span><b>{selectedEligible.loss?.lossType || "—"}</b></div>
              <div><span>Reported date</span><b>{selectedEligible.loss?.notificationDate ? new Date(selectedEligible.loss.notificationDate).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "—"}</b></div>
              <div><span>Affected area</span><b>{selectedEligible.loss?.affectedAreaHa || 0} ha</b></div>
              <div><span>Surveyed loss / severity</span><b>{Number(selectedEligible.survey.lossAssessedPercentage || 0)}%</b></div>
              <div><span>Survey status</span><b>{selectedEligible.survey.status}</b></div>
              <div><span>Sum insured</span><b>{money(selectedEligible.policy?.sumInsured)}</b></div>
            </div>
            <div className="claim-amount-strip">
              <span>Assessed eligible claim amount</span>
              <b>{money(selectedEligible.assessedAmount)}</b>
            </div>
            <p className="claim-amount-note">Calculated as sum insured ({money(selectedEligible.policy?.sumInsured)}) × surveyed loss ({Number(selectedEligible.survey.lossAssessedPercentage || 0)}%). An insurance officer reviews and confirms this amount before final approval.</p>
            <div className="form-actions">
              <button type="button" className="secondary-button" onClick={() => setClaimStep(1)}>Back</button>
              <button type="button" className="primary-button" onClick={() => setClaimStep(3)}>Continue to submit</button>
            </div>
          </div>
        )}

        {claimStep === 3 && selectedEligible && (
          <div className="claim-wizard-body">
            <p className="cm-info">You're about to submit a claim for <b>{money(selectedEligible.assessedAmount)}</b> against policy {selectedEligible.policy?.policyNumber || `#${selectedEligible.policy?.id ?? "—"}`}. Once submitted, it moves to Insurance Officer review.</p>
            <div className="form-actions">
              <button type="button" className="secondary-button" onClick={() => setClaimStep(2)} disabled={submitting}>Back</button>
              <button type="button" className="primary-button" onClick={submitClaim} disabled={submitting}>{submitting ? "Submitting…" : "Submit claim"}</button>
            </div>
          </div>
        )}
      </section>
    )}

    <section className="panel">
      <div className="claim-toolbar">
        <div className="table-search"><Sparkles size={15} /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={farmer ? "Search your claims…" : "Search claim, policy or status…"} /></div>
        <span className="muted">{displayRows.length} claims</span>
      </div>
      <div className="claim-table">
        <div className="claim-row header">
          <span>Claim</span><span>Policy</span><span>Survey</span><span>Claimed</span><span>Approved</span><span>Status</span>
          <span>{farmer ? "Settlement" : "Action"}</span>
        </div>
        {displayRows.length ? displayRows.map((c) => (
          <div className="claim-row" key={c.id}>
            <div className="claim-main"><span className="claim-icon">{statusIcon(c.status)}</span><div><strong>Claim #{c.id}</strong><small>Assessment record</small></div></div>
            <div className="claim-cell">{c.policy?.policyNumber || `Policy #${c.policyId || "—"}`}<small>{c.policy?.cropName || "Crop policy"}</small></div>
            <div className="claim-cell">#{c.surveyId || "—"}</div>
            <div className="claim-cell">{money(c.claimedAmount)}</div>
            <div className="claim-cell">{money(c.approvedAmount)}</div>
            <span className={`claim-status ${tone(c.status)}`}>{c.status || "UNKNOWN"}</span>
            <div className="claim-actions-row">
              <button className="claim-row-btn" onClick={() => openModal("view", c)}>View</button>
              {farmer && <span className="claim-utr">{c.dbtUtr || (c.status === "PAID" ? "PAID" : "—")}</span>}
              {isIO && c.status === "INITIATED" && <button className="claim-row-btn" onClick={() => openModal("review", c)}>Review</button>}
              {canDecide && c.status === "UNDER_REVIEW" && <>
                <button className="claim-row-btn approve" onClick={() => openModal("approve", c)}>Approve</button>
                <button className="claim-row-btn reject" onClick={() => openModal("reject", c)}>Reject</button>
              </>}
              {isBankOfficer && c.status === "APPROVED" && <button className="claim-row-btn disburse" onClick={() => openModal("disburse", c)}>Disburse</button>}
              {isBankOfficer && c.status === "PAID" && <span className="claim-utr">{c.dbtUtr || "PAID"}</span>}
              {!farmer && !isIO && !isBankOfficer && <span className="claim-utr">{c.dbtUtr || "—"}</span>}
            </div>
          </div>
        )) : (
          <div className="claim-empty"><FileCheck2 size={28} color="#a5c88c" /><strong>{isBankOfficer ? "No claims awaiting disbursement" : "No matching claims"}</strong><span>{isBankOfficer ? "Approved claims will appear here for DBT processing." : "Claims and settlement progress will appear here."}</span></div>
        )}
      </div>
    </section>

    {modal && (
      <div className="cm-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setModal(null)}>
        <div className={`cm${["view", "review", "approve", "reject"].includes(modal.type) ? " cm-wide" : ""}`} role="dialog" aria-modal="true">
          <div className="cm-head">
            <div>
              <span className="panel-kicker">{modalKicker[modal.type]}</span>
              <h2>{modalTitle[modal.type]} #{modal.claim.id}</h2>
              <p>Policy: {modal.claim.policy?.policyNumber || `#${modal.claim.policyId}`} · {modal.claim.policy?.cropName || "Crop policy"} · Status: {modal.claim.status}</p>
            </div>
            <button className="cm-close" onClick={() => setModal(null)}>×</button>
          </div>
          <div className="cm-body">
            {["view", "review", "approve", "reject"].includes(modal.type) && (
              <>
                <div className="claim-review-grid">
                  <div><span>Farmer</span><b>{modal.claim.policy?.farmerName || "—"}</b></div>
                  <div><span>Policy</span><b>{modal.claim.policy?.policyNumber || `#${modal.claim.policyId}`}</b></div>
                  <div><span>Crop / season</span><b>{modal.claim.policy?.cropName || "—"} · {modal.claim.policy?.season || "—"}</b></div>
                  <div><span>Sum insured</span><b>{money(modal.claim.policy?.sumInsured)}</b></div>
                  {modal.type === "view" && <div><span>Farmer contact</span><b>{modal.claim.policy?.farmerPhone || modal.claim.policy?.farmerEmail || "—"}</b></div>}
                  <div><span>Survey ID / surveyor</span><b>#{modal.claim.surveyId ?? modal.claim.survey?.id ?? "—"} · {modal.claim.survey?.surveyorName || "—"}</b></div>
                  <div><span>Loss type</span><b>{modal.claim.survey?.loss?.lossType || "—"}</b></div>
                  <div><span>Reported</span><b>{modal.claim.survey?.loss?.notificationDate ? new Date(modal.claim.survey.loss.notificationDate).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "—"}</b></div>
                  <div><span>Affected area</span><b>{modal.claim.survey?.loss?.affectedAreaHa ?? 0} ha</b></div>
                  <div><span>Farmer-estimated loss</span><b>{modal.claim.survey?.loss?.lossPercentageEstimate ?? 0}%</b></div>
                  {modal.type === "view" && <div><span>Assessed yield</span><b>{modal.claim.survey?.yieldAssessedKgHa ?? "—"} kg/ha</b></div>}
                  <div><span>Surveyed loss</span><b>{modal.claim.survey?.lossAssessedPercentage ?? "—"}%</b></div>
                  <div><span>Survey status</span><b>{modal.claim.survey?.status || "—"}</b></div>
                  {modal.type === "view" && <>
                    <div><span>Claimed amount</span><b>{money(modal.claim.claimedAmount)}</b></div>
                    <div><span>Approved amount</span><b>{modal.claim.approvedAmount != null ? money(modal.claim.approvedAmount) : "—"}</b></div>
                    <div><span>Disbursement</span><b>{modal.claim.dbtUtr ? `${modal.claim.dbtUtr} (${modal.claim.disbursedDate ? new Date(modal.claim.disbursedDate).toLocaleDateString("en-IN") : "—"})` : "Not disbursed"}</b></div>
                  </>}
                </div>
                {modal.type === "view" && modal.claim.survey?.loss?.geoLat && modal.claim.survey?.loss?.geoLng && (
                  <div>
                    <span style={{ fontSize: 9, color: "#728179", fontWeight: 600, display: "block", marginBottom: 6 }}>FIELD LOCATION</span>
                    <div style={{ borderRadius: 10, overflow: "hidden", border: "1px solid #d5e8da", marginBottom: 6 }}>
                      <LossMap lat={Number(modal.claim.survey.loss.geoLat)} lng={Number(modal.claim.survey.loss.geoLng)} height={160} />
                    </div>
                    <p style={{ margin: 0, fontSize: 9, color: "#7a9a88", display: "flex", alignItems: "flex-start", gap: 4 }}>
                      <MapPin size={10} style={{ flex: "none", marginTop: 1 }} />
                      {geoAddress || `${Number(modal.claim.survey.loss.geoLat).toFixed(5)}° N, ${Number(modal.claim.survey.loss.geoLng).toFixed(5)}° E`}
                    </p>
                  </div>
                )}
                {(modal.claim.survey?.loss?.photoUrls?.length || modal.claim.survey?.surveyPhotos?.length) ? (
                  <div>
                    <span style={{ fontSize: 9, color: "#728179", fontWeight: 600, display: "block", marginBottom: 6 }}>EVIDENCE</span>
                    <div className="evidence-grid">
                      {[...(modal.claim.survey?.loss?.photoUrls || []), ...(modal.claim.survey?.surveyPhotos || [])].map((url: string) => <ClaimEvidenceImage key={url} path={url} onOpen={setLightbox} />)}
                    </div>
                  </div>
                ) : null}
                {modal.type === "view" && (
                  <div>
                    <span style={{ fontSize: 9, color: "#728179", fontWeight: 600, display: "block", marginBottom: 6 }}>STATUS HISTORY</span>
                    {historyLoading ? <p className="muted">Loading history…</p> : history.length ? (
                      <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                        {history.map((h: any) => (
                          <div key={h.id} className="cm-info" style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "flex-start" }}>
                            <span>{h.previousStatus ? `${h.previousStatus} → ` : ""}<b>{h.newStatus}</b>{h.reason ? ` — ${h.reason}` : ""}</span>
                            <span style={{ whiteSpace: "nowrap", color: "#8c9a92" }}>{h.changedAt ? new Date(h.changedAt).toLocaleString("en-IN") : "—"}</span>
                          </div>
                        ))}
                      </div>
                    ) : <p className="muted">No status history returned by the backend.</p>}
                  </div>
                )}
              </>
            )}
            {modal.type === "review" && (
              <p className="cm-info">Move Claim #{modal.claim.id} from <b>INITIATED</b> to <b>UNDER REVIEW</b>? The farmer will be notified and an assessor will be assigned.</p>
            )}
            {modal.type === "approve" && <>
              <p className="cm-info">Claimed: <b>{money(modal.claim.claimedAmount)}</b> · Policy: {modal.claim.policy?.policyNumber || "—"}</p>
              <label>Approved amount (₹)<input type="number" min="1" max={modal.claim.claimedAmount} value={mForm.amount} onChange={(e) => setMForm({ ...mForm, amount: e.target.value })} placeholder="Enter approved amount" autoFocus /></label>
              <label>Reason / notes<input value={mForm.reason} onChange={(e) => setMForm({ ...mForm, reason: e.target.value })} placeholder="Approval justification (optional)" /></label>
            </>}
            {modal.type === "reject" && <>
              <p className="cm-info">Claim #{modal.claim.id} · Claimed: <b>{money(modal.claim.claimedAmount)}</b>. This action cannot be undone.</p>
              <label>Rejection reason<input value={mForm.reason} onChange={(e) => setMForm({ ...mForm, reason: e.target.value })} placeholder="Reason for rejection" autoFocus required /></label>
            </>}
            {modal.type === "disburse" && <>
              <div className="cm-bank">
                <b>{modal.claim.policy?.bankAccountNo || "****"}</b>
                {modal.claim.policy?.bankHolderName && <span>{modal.claim.policy.bankHolderName}</span>}
                {modal.claim.policy?.bankName && <span>{modal.claim.policy.bankName}</span>}
                {modal.claim.policy?.bankIfsc && <span>IFSC: {modal.claim.policy.bankIfsc}</span>}
                <span style={{ marginTop: 6, fontWeight: 600 }}>Approved amount: {money(modal.claim.approvedAmount)}</span>
              </div>
              <label>DBT UTR reference number<input value={mForm.utr} onChange={(e) => setMForm({ ...mForm, utr: e.target.value })} placeholder="Enter UTR from payment gateway" autoFocus required /></label>
            </>}
          </div>
          <div className="cm-foot">
            <button className="secondary-button" onClick={() => setModal(null)} disabled={mBusy}>{modal.type === "view" ? "Close" : "Cancel"}</button>
            {modal.type !== "view" && (
              <button className={`primary-button${modal.type === "reject" ? " danger" : ""}`} onClick={doAction} disabled={mBusy || !canSubmitModal}>
                {mBusy ? "Processing…" : modal.type === "review" ? "Confirm Review" : modal.type === "approve" ? "Approve Claim" : modal.type === "reject" ? "Reject Claim" : "Confirm Disbursement"}
              </button>
            )}
          </div>
        </div>
      </div>
    )}

    {lightbox && (
      <div className="lightbox-bd" onClick={() => setLightbox(null)}>
        <img src={lightbox} className="lightbox-img" alt="Claim evidence" onClick={(e) => e.stopPropagation()} />
        <button className="lightbox-close" onClick={() => setLightbox(null)}>×</button>
      </div>
    )}
  </>;
}
