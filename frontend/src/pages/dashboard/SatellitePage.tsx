import { useMemo, useState } from "react";
import { api } from "../../services/api";
import { PageState, useApi } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";

const listOf = (v: any): any[] => Array.isArray(v) ? v : (v?.content || []);

export default function SatellitePage() {
  const lossesQ = useApi(api.losses);
  // Only cases with a real recorded GPS location can drive an assessment - nothing invented for the rest.
  const geoCases = useMemo(() => listOf(lossesQ.data).filter((l: any) => l.geoLat != null && l.geoLng != null), [lossesQ.data]);

  const [caseId, setCaseId] = useState("");
  const [lat, setLat] = useState("");
  const [lng, setLng] = useState("");
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState("");

  const selectedCase = geoCases.find((l: any) => String(l.id) === caseId) || null;

  function selectCase(id: string) {
    setCaseId(id);
    const loss = geoCases.find((l: any) => String(l.id) === id);
    if (loss) { setLat(String(loss.geoLat)); setLng(String(loss.geoLng)); }
  }

  async function run() {
    setError(""); setResult(null);
    if (lat === "" || lng === "" || Number.isNaN(Number(lat)) || Number.isNaN(Number(lng))) {
      toast.error("Enter a valid latitude and longitude, or select a case to auto-fill them.");
      return;
    }
    setRunning(true);
    try {
      const r = await api.satellite.assessment({ lat, lng });
      setResult(r);
    } catch (e: any) {
      setError(e.message || "Assessment request failed.");
    } finally { setRunning(false); }
  }

  return <>
    <style>{`.sat-result-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:10px 18px;margin:0 19px 19px;padding:14px 16px;border:1px solid #d5e8da;border-radius:10px;background:#f5fbf7}.sat-result-grid div{display:flex;flex-direction:column;gap:3px}.sat-result-grid span{font-size:9px;color:#6a8577;letter-spacing:.04em}.sat-result-grid b{font-size:12px;color:#244b3b;font-weight:600}.sat-source-note{margin:0 19px 19px;font-size:9px;color:#8b9891;line-height:1.6}`}</style>

    <section className="page-heading">
      <div>
        <p className="eyebrow">FIELD INTELLIGENCE / REMOTE SENSING</p>
        <h1>Remote Sensing Assessment</h1>
        <p className="subhead">Use geo-tagged policy/loss information to request and review satellite-based assessment context.</p>
      </div>
    </section>

    <PageState {...lossesQ} />

    <section className="panel form-panel">
      <div className="panel-heading">
        <div>
          <span className="panel-kicker">STEP 1 · SELECT A CASE (OPTIONAL)</span>
          <h2>Policy / loss selection</h2>
          <p className="panel-description">Choosing a case auto-fills its recorded location. You can also enter coordinates directly.</p>
        </div>
      </div>
      <div className="form-grid">
        <label>Loss / Case
          <select value={caseId} onChange={(e) => selectCase(e.target.value)}>
            <option value="">{lossesQ.loading ? "Loading cases…" : "Select a geo-tagged case"}</option>
            {geoCases.map((l: any) => <option key={l.id} value={l.id}>Loss #{l.id} - {l.policy?.cropName || "Crop"} - {l.policy?.policyNumber || `Policy #${l.policyId ?? "—"}`}</option>)}
          </select>
        </label>
        <label>Policy<input value={selectedCase?.policy?.policyNumber || ""} readOnly placeholder="Auto-filled from selected case" /></label>
      </div>
      {!lossesQ.loading && geoCases.length === 0 && (
        <p className="cm-info" style={{ margin: "0 19px 16px" }}>No loss reports with recorded GPS coordinates are available yet — enter latitude/longitude directly below.</p>
      )}

      <div className="panel-heading">
        <div>
          <span className="panel-kicker">STEP 2-3 · LOCATION &amp; DATE</span>
          <h2>Assessment request</h2>
        </div>
      </div>
      <div className="form-grid">
        <label>Latitude<input value={lat} onChange={(e) => setLat(e.target.value)} placeholder="e.g. 13.0827" /></label>
        <label>Longitude<input value={lng} onChange={(e) => setLng(e.target.value)} placeholder="e.g. 80.2707" /></label>
        <label>Assessment date<input type="date" value={date} onChange={(e) => setDate(e.target.value)} /></label>
      </div>
      <button className="primary-button" onClick={run} disabled={running} style={{ margin: "0 19px 19px" }}>{running ? "Running assessment…" : "Run assessment"}</button>
    </section>

    <section className="panel">
      <div className="panel-heading"><div><span className="panel-kicker">STEP 4 · RESULT</span><h2>Assessment result</h2></div></div>
      {error ? (
        <div className="empty-row"><div><strong>Assessment failed</strong><span>{error}</span></div></div>
      ) : result ? (
        <>
          <div className="sat-result-grid">
            <div><span>NDVI</span><b>{result.ndvi ?? "—"}</b></div>
            <div><span>VEGETATION HEALTH</span><b>{result.vegetationHealth != null ? `${result.vegetationHealth}%` : "—"}</b></div>
            <div><span>SOURCE</span><b>{result.source || "—"}</b></div>
            <div><span>REQUESTED FOR</span><b>{date} · {lat}, {lng}{selectedCase ? ` · Loss #${selectedCase.id}` : ""}</b></div>
          </div>
          <p className="sat-source-note">"Source" reflects exactly what the assessment backend returned — a sandbox/demo value is labeled as such rather than presented as live satellite imagery.</p>
        </>
      ) : (
        <div className="empty-row"><div><strong>No assessment yet</strong><span>Fill in the request above and run an assessment to see results here.</span></div></div>
      )}
    </section>
  </>;
}
