import { useEffect, useState } from 'react';
import { api, authToken, API_BASE_URL } from '../../services/api';
import { DataToolbar, PageState, useApi, handleError } from '../../components/dashboard/ApiPage';
import { toast } from 'sonner';
import LossMap from '../../components/dashboard/LossMap';

const list = (v: any): any[] => Array.isArray(v) ? v : (v?.content || v?.items || v?.data || []);
const display = (...vs: any[]) => vs.find((v) => v !== undefined && v !== null && v !== '') ?? '—';
const date = (v: any) => v ? new Date(v).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—';
const ns = (v: any) => String(v || '').toUpperCase();

function SurveyEvidenceImage({ path, onOpen }: { path: string; onOpen?: (src: string) => void }) {
  const [blobSrc, setBlobSrc] = useState<string | null>(null);
  const [err, setErr] = useState(false);
  useEffect(() => {
    const token = authToken();
    let objectUrl = '';
    let cancelled = false;
    fetch(`${API_BASE_URL}${path}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      .then((r) => { if (!r.ok) throw new Error(); return r.blob(); })
      .then((blob) => { if (cancelled) return; objectUrl = URL.createObjectURL(blob); setBlobSrc(objectUrl); })
      .catch(() => { if (!cancelled) setErr(true); });
    return () => { cancelled = true; if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [path]);
  if (err) return <div className="evidence-placeholder evidence-error">Unavailable</div>;
  if (!blobSrc) return <div className="evidence-placeholder evidence-loading" />;
  return <img src={blobSrc} className="evidence-thumb" alt="Field evidence" onClick={() => onOpen?.(blobSrc)} />;
}

function AssessmentModal({ survey, onClose, onSubmitted, readOnly = ['SUBMITTED', 'VERIFIED'].includes(ns(survey?.status)) }: { survey: any; onClose: () => void; onSubmitted: () => void; readOnly?: boolean }) {
  const [yieldVal, setYieldVal] = useState(survey?.yieldAssessedKgHa != null ? String(survey.yieldAssessedKgHa) : '');
  const [lossVal, setLossVal] = useState(survey?.lossAssessedPercentage != null ? String(survey.lossAssessedPercentage) : '');
  const [existingPhotos, setExistingPhotos] = useState<string[]>(Array.isArray(survey?.surveyPhotos) ? survey.surveyPhotos : []);
  const [files, setFiles] = useState<File[]>([]);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [lightbox, setLightbox] = useState<string | null>(null);
  const lossDetail = useApi(() => (survey?.notificationId ? api.loss(Number(survey.notificationId)) : Promise.resolve(null)), [survey?.notificationId]);
  const loss: any = lossDetail.data;

  function handleFiles(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(e.target.files || []);
    const valid = selected.filter((f) => {
      if (!f.type.startsWith('image/')) { toast.error(`${f.name}: only image files are allowed.`); return false; }
      if (f.size > 10 * 1024 * 1024) { toast.error(`${f.name}: exceeds the 10 MB limit.`); return false; }
      return true;
    });
    setFiles((prev) => [...prev, ...valid]);
    e.target.value = '';
  }
  function removeFile(index: number) { setFiles((prev) => prev.filter((_, i) => i !== index)); }
  function removeExisting(index: number) { setExistingPhotos((prev) => prev.filter((_, i) => i !== index)); }

  async function submit(e: React.FormEvent) {
    if (readOnly) return;
    e.preventDefault();
    const yieldKgHa = Number(yieldVal);
    const lossPercent = Number(lossVal);
    if (!yieldVal || !Number.isFinite(yieldKgHa) || yieldKgHa < 0) { setError('Enter a valid assessed yield (kg/ha).'); return; }
    if (!lossVal || !Number.isFinite(lossPercent) || lossPercent < 0 || lossPercent > 100) { setError('Enter a loss percentage between 0 and 100.'); return; }
    setSaving(true); setError('');
    try {
      const uploadedUrls: string[] = [];
      if (files.length) {
        setUploading(true);
        for (const file of files) {
          const doc: any = await api.documents.upload(file, { entityType: 'SURVEY_ASSIGNMENT', entityId: survey.id, category: 'EVIDENCE' });
          uploadedUrls.push(`/api/documents/${doc.id}/content`);
        }
      }
      await api.submitSurvey(Number(survey.id), { yieldAssessedKgHa: yieldKgHa, lossAssessedPercentage: lossPercent, surveyPhotos: [...existingPhotos, ...uploadedUrls] });
      toast.success(`Assessment for survey #${survey.id} submitted`);
      onSubmitted();
    } catch (err) { setError(err instanceof Error ? err.message : 'Unable to submit assessment.'); }
    finally { setSaving(false); setUploading(false); }
  }

  return (
    <div className="assessment-modal-backdrop" role="presentation" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="assessment-modal" role="dialog" aria-modal="true" aria-labelledby="assessment-title">
        <div className="assessment-modal-header">
          <div><span className="panel-kicker">FIELD ASSESSMENT</span><h2 id="assessment-title">{readOnly ? 'View survey' : `Submit survey #${survey.id}`}</h2><p>{display(survey.farmerName)} · {display(survey.cropName)} · {date(survey.surveyDate)}</p></div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Close">×</button>
        </div>
        <form onSubmit={submit}>
          <div className="assessment-detail-grid">
            <div><span>Policy</span><b>{display(survey.policyNumber)}</b></div>
            <div><span>Crop / season</span><b>{display(survey.cropName)}</b></div>
            <div><span>Loss type</span><b>{display(loss?.lossType)}</b></div>
            <div><span>Reported</span><b>{loss?.notificationDate ? date(loss.notificationDate) : '—'}</b></div>
            <div><span>Affected area</span><b>{loss?.affectedAreaHa != null ? `${loss.affectedAreaHa} ha` : '—'}</b></div>
            <div><span>Farmer-estimated loss</span><b>{loss?.lossPercentageEstimate != null ? `${loss.lossPercentageEstimate}%` : '—'}</b></div>
          </div>
          <div className="assessment-map-evidence">
            <div>
              <p className="assessment-section-label">Field location</p>
              {(survey.notificationGeoLat != null || loss?.geoLat != null) ? (
                <LossMap lat={Number(survey.notificationGeoLat ?? loss?.geoLat)} lng={Number(survey.notificationGeoLng ?? loss?.geoLng)} height={150} />
              ) : <div className="evidence-empty">No location recorded</div>}
            </div>
            <div>
              <p className="assessment-section-label">Farmer's evidence</p>
              {lossDetail.loading ? (
                <div className="evidence-grid"><div className="evidence-placeholder evidence-loading" /></div>
              ) : loss?.photoUrls?.length ? (
                <div className="evidence-grid">{loss.photoUrls.map((url: string) => <SurveyEvidenceImage key={url} path={url} onOpen={setLightbox} />)}</div>
              ) : <div className="evidence-empty">No evidence photos submitted by farmer</div>}
            </div>
          </div>
          <div className="assessment-form-grid">
            <label>Assessed yield (kg/ha)<input disabled={readOnly} type="number" min="0" step="0.01" value={yieldVal} onChange={(e) => setYieldVal(e.target.value)} placeholder="Enter yield" autoFocus={!readOnly} required /></label>
            <label>Loss percentage (%)<input disabled={readOnly} type="number" min="0" max="100" step="0.01" value={lossVal} onChange={(e) => setLossVal(e.target.value)} placeholder="0–100" required /></label>
            <div className="assessment-full-field file-section">
              <span className="file-section-label">Your field evidence photos</span>
              {existingPhotos.length > 0 && (
                <div className="evidence-grid">
                  {existingPhotos.map((url, i) => (
                    <div key={url} className="evidence-thumb-wrap">
                      <SurveyEvidenceImage path={url} onOpen={setLightbox} />
                      {!readOnly && <button type="button" className="evidence-remove" onClick={() => removeExisting(i)} aria-label="Remove photo">×</button>}
                    </div>
                  ))}
                </div>
              )}
              {!readOnly && <label className="file-pick-btn">Add photos<input type="file" accept="image/*" multiple onChange={handleFiles} /></label>}
              {files.length > 0 && (
                <div className="file-chips">
                  {files.map((file, i) => <div key={i} className="file-chip"><span>{file.name}</span><button type="button" className="file-chip-remove" onClick={() => removeFile(i)} aria-label="Remove file">×</button></div>)}
                </div>
              )}
            </div>
          </div>
          {error && <div className="assessment-form-error">{error}</div>}
          <div className="assessment-modal-actions">
            <button type="button" className="secondary-button" onClick={onClose} disabled={saving}>{readOnly ? 'Close' : 'Cancel'}</button>
            {!readOnly && <button type="submit" className="primary-button" disabled={saving}>{uploading ? 'Uploading photos…' : saving ? 'Submitting…' : 'Submit assessment'}</button>}
          </div>
        </form>
      </div>
      {lightbox && (
        <div className="lightbox-bd" onClick={(e) => { e.stopPropagation(); setLightbox(null); }}>
          <img src={lightbox} className="lightbox-img" alt="Field evidence" onClick={(e) => e.stopPropagation()} />
          <button className="lightbox-close" onClick={(e) => { e.stopPropagation(); setLightbox(null); }}>×</button>
        </div>
      )}
    </div>
  );
}

export default function SurveysPage() {
  const q = useApi(api.surveys);
  const profile = useApi(api.profile);
  const [search, setSearch] = useState('');
  const [selectedSurvey, setSelectedSurvey] = useState<any | null>(null);
  const rows: any[] = list(q.data);
  const isSurveyor = ns(profile.data?.role) === 'SURVEYOR';
  const filtered = rows.filter((r) => JSON.stringify(r).toLowerCase().includes(search.toLowerCase()));

  const openAssessment = async (survey?: any) => {
    const candidate = survey || filtered.find((r) => ['ASSIGNED', 'IN_PROGRESS'].includes(ns(r.status)));
    if (!candidate) { toast.info('No survey assignment is available for assessment.'); return; }
    try {
      const started = ns(candidate.status) === 'ASSIGNED' ? await api.startSurvey(Number(candidate.id)) : candidate;
      setSelectedSurvey(started || { ...candidate, status: 'IN_PROGRESS' });
    } catch (error) { handleError(error); }
  };

  const viewSurvey = (survey: any) => setSelectedSurvey(survey);
  const actionLabel = (status: any) => ns(status) === 'ASSIGNED' ? 'Start assessment' : ns(status) === 'IN_PROGRESS' ? 'Continue' : 'View';

  const statusPill = (status: any) => {
    const s = ns(status);
    const cls = (s === 'VERIFIED' || s === 'SUBMITTED') ? 'settled' : s === 'IN_PROGRESS' ? 'enrolled' : 'claimed-field';
    return <b className={`status-pill ${cls}`}>{display(status)}</b>;
  };

  return <>
    <style>{`.assessment-modal-backdrop{position:fixed;inset:0;z-index:1000;display:grid;place-items:center;padding:20px;background:#102f2490;backdrop-filter:blur(4px)}.assessment-modal{width:min(720px,100%);border:1px solid #dce8df;border-radius:16px;background:#fff;box-shadow:0 24px 70px #0c332c40;overflow:hidden}.assessment-modal-header{display:flex;justify-content:space-between;gap:20px;padding:24px 26px 18px;border-bottom:1px solid #edf1ee;background:linear-gradient(135deg,#f8fbf6,#fff)}.assessment-modal-header h2{margin:7px 0 4px;color:#244b3b;font:500 23px Georgia,serif}.assessment-modal-header p{margin:0;color:#8c9a92;font-size:10px}.modal-close{width:30px;height:30px;border:0;border-radius:50%;color:#6f8378;background:#eef5ed;font-size:22px;line-height:1;cursor:pointer}.assessment-modal form{padding:22px 26px 24px;max-height:74vh;overflow-y:auto;box-sizing:border-box}.assessment-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.assessment-form-grid label{display:flex;flex-direction:column;gap:7px;color:#426354;font-size:10px;font-weight:600}.assessment-form-grid label span{color:#9aa69f;font-size:9px;font-weight:400}.assessment-form-grid input,.assessment-form-grid textarea{width:100%;box-sizing:border-box;border:1px solid #dce8df;border-radius:8px;padding:11px 12px;outline:none;color:#315849;background:#fbfdfb;font:inherit;font-size:11px}.assessment-form-grid input:focus,.assessment-form-grid textarea:focus{border-color:#76a080;box-shadow:0 0 0 3px #dceedd}.assessment-form-grid textarea{resize:vertical;line-height:1.5}.assessment-full-field{grid-column:1/-1}.assessment-form-error{margin-top:16px;padding:10px 12px;border:1px solid #efd4ce;border-radius:7px;color:#a65d51;background:#fff7f4;font-size:10px}.assessment-modal-actions{display:flex;justify-content:flex-end;gap:9px;margin-top:22px}.assessment-modal-actions .secondary-button{height:38px;padding:0 16px}.assessment-modal-actions .primary-button{height:38px;padding:0 16px}
.assessment-detail-grid{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px 16px;padding:12px 14px;margin-bottom:14px;border:1px solid #d5e8da;border-radius:10px;background:#f5fbf7}.assessment-detail-grid div{display:flex;flex-direction:column;gap:2px}.assessment-detail-grid span{font-size:9px;color:#6a8577}.assessment-detail-grid b{font-size:10px;color:#244b3b;font-weight:600}
.assessment-map-evidence{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px}.assessment-section-label{margin:0 0 7px;color:#3d6b52;font-size:9px;font-weight:700;text-transform:uppercase;letter-spacing:.07em}
.evidence-grid{display:flex;flex-wrap:wrap;gap:8px}.evidence-thumb{width:72px;height:72px;object-fit:cover;border-radius:8px;border:1px solid #d5e8da;cursor:pointer;transition:opacity .15s}.evidence-thumb:hover{opacity:.85}.evidence-placeholder{width:72px;height:72px;border-radius:8px;display:grid;place-items:center;font-size:8px;color:#9aaa9f}.evidence-loading{background:linear-gradient(90deg,#edf5ea 25%,#daefd6 50%,#edf5ea 75%);background-size:200%;animation:shimmer 1.4s infinite}.evidence-error{background:#f5f5f5;border:1px solid #e5e5e5}.evidence-empty{font-size:10px;color:#9ba99f;padding:8px 0}@keyframes shimmer{0%{background-position:200% 0}100%{background-position:-200% 0}}
.evidence-thumb-wrap{position:relative}.evidence-remove{position:absolute;top:-6px;right:-6px;width:18px;height:18px;border:1px solid #e8c4bf;border-radius:50%;background:#fff;color:#9a625b;font-size:12px;line-height:1;cursor:pointer;display:grid;place-items:center}
.file-section{display:flex;flex-direction:column;gap:8px}.file-section-label{font-size:9px;color:#728179;font-weight:600}.file-pick-btn{display:inline-flex;align-items:center;gap:7px;width:max-content;padding:8px 14px;border:1px dashed #b0ccb8;border-radius:8px;color:#3d6e52;background:#f6fdf7;font-size:10px;cursor:pointer;transition:background .15s,border-color .15s}.file-pick-btn:hover{background:#ecf8ee;border-color:#82b090}.file-pick-btn input{display:none}.file-chips{display:flex;flex-wrap:wrap;gap:5px;margin-top:2px}.file-chip{display:flex;align-items:center;gap:5px;padding:3px 8px 3px 10px;border:1px solid #d3e8d8;border-radius:999px;color:#3a6b50;background:#edf7ea;font-size:9px;max-width:180px}.file-chip span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.file-chip-remove{border:none;background:none;color:#8aaa94;cursor:pointer;font-size:14px;line-height:1;padding:0;flex:none;display:grid;place-items:center}
.lightbox-bd{position:fixed;inset:0;z-index:2000;display:grid;place-items:center;background:#000000cc;backdrop-filter:blur(4px);cursor:zoom-out}.lightbox-img{max-width:90vw;max-height:88vh;object-fit:contain;border-radius:10px;box-shadow:0 24px 80px #0009;cursor:default}.lightbox-close{position:fixed;top:18px;right:22px;width:38px;height:38px;border:0;border-radius:50%;background:#ffffff20;color:#fff;font-size:24px;line-height:1;cursor:pointer;display:grid;place-items:center;transition:background .15s}.lightbox-close:hover{background:#ffffff40}
@media(max-width:640px){.assessment-detail-grid{grid-template-columns:1fr 1fr}.assessment-map-evidence{grid-template-columns:1fr}}
/* Surveyor table */
.surveyor-surveys-table .table-row{align-items:center;border-bottom:1px solid #edf1ee}.surveyor-surveys-table .table-row:last-child{border-bottom:0}
/* IO oversight table */
.io-surveys-table .table-row{align-items:center;border-bottom:1px solid #edf1ee}.io-surveys-table .table-row:last-child{border-bottom:0}
@media(max-width:560px){.assessment-form-grid{grid-template-columns:1fr}.assessment-full-field{grid-column:auto}.assessment-modal-header,.assessment-modal form{padding-left:18px;padding-right:18px}}`}</style>

    <section className="page-heading">
      <div>
        <p className="eyebrow">FIELD / SURVEYS</p>
        <h1>Survey Assignments</h1>
        <p className="subhead">{isSurveyor ? 'Review your assigned field assessments and submit evidence.' : 'Oversight of all field survey assignments.'}</p>
      </div>
      {isSurveyor && <button className="primary-button" onClick={() => openAssessment()}>Submit Assessment</button>}
    </section>

    <PageState {...q} /><PageState {...profile} />

    <section className="panel table-panel">
      <DataToolbar value={search} onChange={setSearch} placeholder="Search assignments…" />

      {isSurveyor ? (
        <div className="data-table surveyor-surveys-table">
          <div className="table-row table-header" style={{ gridTemplateColumns: '.5fr 1.1fr 1fr 1.2fr .85fr .9fr 1.1fr', minWidth: 920 }}>
            <span>ID</span><span>Farmer</span><span>Crop</span><span>Location</span><span>Survey date</span><span>Status</span><span>Action</span>
          </div>
          {filtered.length ? filtered.map((row, i) => {
            const location = row.location || [row.notificationGeoLat, row.notificationGeoLng].filter((v) => v !== undefined && v !== null).join(', ') || row.ccePlotId;
            const canAct = ['ASSIGNED', 'IN_PROGRESS'].includes(ns(row.status));
            return (
              <div className="table-row" style={{ gridTemplateColumns: '.5fr 1.1fr 1fr 1.2fr .85fr .9fr 1.1fr', minWidth: 920 }} key={row.id || i}>
                <span><b>#{row.id}</b></span>
                <span>{display(row.farmerName, row.farmer?.name)}</span>
                <span>{display(row.cropName, row.policy?.cropName)}</span>
                <span style={{ fontSize: 9, color: '#7a9080' }}>{display(location)}</span>
                <span>{date(row.surveyDate)}</span>
                <span>{statusPill(row.status)}</span>
                <span><button className="row-action" onClick={() => canAct ? openAssessment(row) : viewSurvey(row)}>{actionLabel(row.status)}</button></span>
              </div>
            );
          }) : <div className="empty-row">No survey assignments returned by the backend.</div>}
        </div>
      ) : (
        <div className="data-table io-surveys-table">
          <div className="table-row table-header" style={{ gridTemplateColumns: '.45fr .7fr 1.4fr 1.4fr 1fr .9fr', minWidth: 820 }}>
            <span>ID</span><span>Notif.</span><span>Farmer / Crop</span><span>Surveyor</span><span>Survey date</span><span>Status</span>
          </div>
          {filtered.length ? filtered.map((row, i) => (
            <div className="table-row" style={{ gridTemplateColumns: '.45fr .7fr 1.4fr 1.4fr 1fr .9fr', minWidth: 820 }} key={row.id || i}>
              <span><b>#{row.id}</b></span>
              <span>#{display(row.notificationId, row.notification?.id)}</span>
              <span>
                {display(row.farmerName, row.farmer?.name, row.notification?.policy?.farmer?.name)}
                {(row.cropName || row.policy?.cropName) && <small style={{ display: 'block', color: '#9ba9a1', fontSize: 9 }}>{display(row.cropName, row.policy?.cropName)}</small>}
              </span>
              <span>{display(row.surveyor?.name, row.surveyorName, `ID ${row.surveyorId}`)}</span>
              <span>{date(row.surveyDate)}</span>
              <span>{statusPill(row.status)}</span>
            </div>
          )) : <div className="empty-row">No survey assignments found.</div>}
        </div>
      )}
    </section>

    {selectedSurvey && (
      <AssessmentModal survey={selectedSurvey} onClose={() => setSelectedSurvey(null)} onSubmitted={() => { setSelectedSurvey(null); q.reload(); }} />
    )}
  </>;
}
