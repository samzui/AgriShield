import { useEffect, useMemo, useState } from "react";
import { ArrowUpRight, BadgeCheck, CalendarDays, CheckCircle2, ChevronRight, CreditCard, FileText, Leaf, Plus, ShieldCheck, Sprout, WalletCards, X } from "lucide-react";
import { api } from "../../services/api";
import { PageState, handleError, useApi } from "../../components/dashboard/ApiPage";
import { toast } from "sonner";

const money = (v: any) => `₹${Number(v || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
const statusClass = (s: any) => `policy-status ${String(s || "unknown").toLowerCase().replaceAll("_", "-")}`;
const IFSC_RE = /^[A-Z]{4}0[A-Z0-9]{6}$/;
const ACRE_TO_HECTARE = 0.404686;
const AREA_OPTIONS = [
  { key: "lt1",    label: "Less than 1 acre",     acres: 0.5 },
  { key: "1to2",   label: "1 – 2 acres",          acres: 1.5 },
  { key: "2to5",   label: "2 – 5 acres",          acres: 3.5 },
  { key: "5to10",  label: "5 – 10 acres",         acres: 7.5 },
  { key: "10plus", label: "More than 10 acres",   acres: 12  },
  { key: "manual", label: "I know my exact area (in hectares)", acres: null },
];
const EMPTY = { cropName: "", season: "", cropKey: "", areaKey: "", sownAreaHa: "", bankAccountNo: "", bankConfirmAccountNo: "", bankHolderName: "", bankName: "", bankIfsc: "", bankBranch: "" };

export default function PoliciesPage() {
  const profile = useApi(api.profile);
  const farmerId = (profile.data as any)?.id;
  const role = String((profile.data as any)?.role || "").toUpperCase();
  const isFarmer = role === "FARMER";
  const policies = useApi(() => {
    if (isFarmer) return farmerId ? api.farmerPolicies(Number(farmerId)) : Promise.resolve([]);
    return profile.data ? api.policies(0, 100) : Promise.resolve([]);
  }, [isFarmer, farmerId, !!profile.data]);
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [crops, setCrops] = useState<any[]>([]);
  const [form, setForm] = useState(EMPTY);
  const [bankModal, setBankModal] = useState(false);
  const [bankDone, setBankDone] = useState(false);
  const [docsPolicy, setDocsPolicy] = useState<any>(null);

  useEffect(() => { api.crops.list().then((v: any) => setCrops(Array.isArray(v) ? v : [])).catch(() => undefined); }, []);

  const rows: any[] = Array.isArray(policies.data) ? policies.data : (policies.data as any)?.content || [];
  const filtered = useMemo(() => rows.filter((r) => JSON.stringify(r).toLowerCase().includes(search.toLowerCase())), [rows, search]);
  const covered = rows.reduce((sum, r) => sum + Number(r.sumInsured || 0), 0);
  const active = rows.filter((r) => !["LAPSED"].includes(String(r.status || "").toUpperCase())).length;
  // Always pick the policy with the most recent enrollmentDate
  const latestPolicy = rows.length
    ? rows.reduce((a, b) => new Date(a.enrollmentDate) >= new Date(b.enrollmentDate) ? a : b)
    : null;

  const selectedCrop = crops.find((c: any) => `${c.cropName}-${c.season}` === form.cropKey);
  const areaHa = Number(form.sownAreaHa) || 0;
  // Standard PMFBY sum-insured formula: (average yield in quintals/ha) x MSP rate (Rs/quintal) x sown area.
  // standardYieldKgHa/mspRate come straight from the crop master data returned by the backend — nothing invented here.
  const computedCoverage = selectedCrop && areaHa > 0
    ? Math.round((areaHa * (Number(selectedCrop.standardYieldKgHa) || 0) / 100) * (Number(selectedCrop.mspRate) || 0) * 100) / 100
    : 0;

  function applyArea(key: string) {
    const opt = AREA_OPTIONS.find((o) => o.key === key);
    if (!opt || opt.acres === null) { setForm((p) => ({ ...p, areaKey: key, sownAreaHa: "" })); return; }
    setForm((p) => ({ ...p, areaKey: key, sownAreaHa: (opt.acres * ACRE_TO_HECTARE).toFixed(4) }));
  }

  function saveBankDetails() {
    if (!form.bankAccountNo) { toast.error("Account number is required"); return; }
    if (form.bankAccountNo !== form.bankConfirmAccountNo) { toast.error("Account numbers do not match"); return; }
    if (form.bankIfsc && !IFSC_RE.test(form.bankIfsc.trim().toUpperCase())) { toast.error("Invalid IFSC format — expected AAAA0XXXXXX"); return; }
    setBankModal(false);
    setBankDone(true);
    toast.success("Bank details saved");
  }

  async function enroll(e: any) {
    e.preventDefault();
    if (!farmerId) return;
    if (!form.areaKey) { toast.error("Please select how much land you're cultivating."); return; }
    if (!areaHa) { toast.error("Please enter your exact land area in hectares."); return; }
    if (computedCoverage <= 0) { toast.error("Could not calculate your crop coverage — please re-select the crop and land area."); return; }
    if (!form.bankAccountNo) { toast.error("Bank details are required — click 'Bank details' to add them"); return; }
    if (form.bankAccountNo !== form.bankConfirmAccountNo) { toast.error("Account numbers do not match"); return; }
    if (form.bankIfsc && !IFSC_RE.test(form.bankIfsc.trim().toUpperCase())) { toast.error("Invalid IFSC format"); return; }
    try {
      await api.enrollPolicy({
        farmerId: Number(farmerId),
        cropName: form.cropName,
        season: form.season,
        sownAreaHa: areaHa,
        bankAccountNo: form.bankAccountNo,
        bankHolderName: form.bankHolderName || undefined,
        bankName: form.bankName || undefined,
        bankIfsc: form.bankIfsc ? form.bankIfsc.trim().toUpperCase() : undefined,
        bankBranch: form.bankBranch || undefined,
        sumInsured: computedCoverage,
      });
      toast.success("Policy enrollment submitted");
      setOpen(false);
      setForm(EMPTY);
      setBankDone(false);
      policies.reload();
    } catch (error) { handleError(error); }
  }

  const f = (k: keyof typeof EMPTY) => (e: any) => setForm((p) => ({ ...p, [k]: e.target.value }));

  return <>
    <style>{`.policy-intro{display:flex;justify-content:space-between;gap:24px;align-items:flex-end;margin:-5px 0 25px}.policy-intro h1{margin:0;color:#183f31;font:500 35px Georgia,serif}.policy-intro p{max-width:500px;margin:8px 0 0;color:#7c8c83;font-size:11px;line-height:1.6}.policy-head-actions{display:flex;gap:9px}.policy-summary{display:grid;grid-template-columns:1.2fr repeat(2,1fr);gap:14px;margin-bottom:22px}.policy-summary-card{position:relative;overflow:hidden;min-height:116px;padding:21px;border:1px solid #e3ece4;border-radius:14px;background:#fff;box-shadow:0 12px 28px #1a4c3510}.policy-summary-card.featured{color:#ecf7e9;background:linear-gradient(120deg,#0f4b37,#367251)}.policy-summary-card:after{content:"";position:absolute;width:110px;height:110px;right:-42px;bottom:-52px;border-radius:50%;background:#c5e69638}.policy-summary-card span{display:block;color:#8a9a90;font-size:10px}.policy-summary-card.featured span{color:#c1dfc1}.policy-summary-card strong{display:block;margin-top:12px;color:#1f4e3c;font:500 25px Georgia,serif}.policy-summary-card.featured strong{color:#eff8e8}.policy-summary-card small{display:block;margin-top:6px;color:#9aa9a0;font-size:9px}.policy-summary-card.featured small{color:#c2ddc4}.policy-summary-icon{display:grid;place-items:center;width:31px;height:31px;margin-bottom:12px;border-radius:9px;color:#40795a;background:#e7f2e3}.policy-summary-card.featured .policy-summary-icon{color:#d8efb5;background:#ffffff18}.policy-toolbar{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:20px 23px;border-bottom:1px solid #edf1ee}.policy-search{display:flex;align-items:center;gap:9px;min-width:240px;color:#8da098}.policy-search input{width:100%;border:0;outline:0;color:#345849;font-size:11px}.policy-search input::placeholder{color:#a3b0a8}.policy-table{padding:0 23px 10px}.policy-row{display:grid;grid-template-columns:1.2fr 1fr .8fr .8fr .9fr auto;align-items:center;gap:13px;padding:17px 0;border-bottom:1px solid #edf1ee}.policy-row:last-child{border-bottom:0}.policy-row.header{padding:11px 0;color:#96a39b;font-size:9px;text-transform:uppercase;letter-spacing:.1em}.policy-main{display:flex;align-items:center;gap:10px;min-width:0}.policy-crop-icon{display:grid;place-items:center;width:34px;height:34px;flex:none;border-radius:10px;color:#387653;background:#e5f2df}.policy-main strong{display:block;overflow:hidden;color:#2b5543;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.policy-main small,.policy-cell small{display:block;margin-top:4px;color:#9aa79f;font-size:9px}.policy-cell{color:#496659;font-size:10px}.policy-status{display:inline-flex;width:max-content;padding:5px 8px;border-radius:999px;color:#4f7b60;background:#e7f3e7;font-size:8px;font-weight:800;letter-spacing:.07em}.policy-status.claim-filed{color:#9a6a3e;background:#fbefd9}.policy-status.settled{color:#6b6a9a;background:#ebeaf7}.policy-status.lapsed{color:#9a625b;background:#fae5e1}.policy-empty{padding:65px 20px;color:#9aa89f;text-align:center}.policy-empty strong{display:block;margin:12px 0 5px;color:#426354;font:500 19px Georgia,serif}.policy-empty span{font-size:10px}.policy-form{margin-bottom:20px}.policy-form form{padding:0 23px 22px}.policy-form .form-grid{padding:12px 0 15px}.policy-field-hint{display:block;margin-top:5px;font-size:9px;color:#6a9278;font-weight:400}.policy-derived-strip{display:flex;align-items:center;gap:14px;flex-wrap:wrap;padding:10px 14px;border:1px solid #c8e6d4;border-radius:8px;background:#f3faf5;font-size:10px;color:#2e6646}.policy-derived-strip b{font-weight:700}.policy-derived-sep{width:1px;height:14px;background:#b5d9c5}.policy-derived-strip.policy-derived-pending{color:#8ea59a;background:#f7faf7;border-color:#dfe8e2}.bank-divider{grid-column:1/-1;display:flex;align-items:center;gap:10px;margin-top:6px;padding-top:14px;border-top:1px solid #e8f0e8}.bank-divider span{font-size:9px;color:#5a7a6b;font-weight:700;letter-spacing:.09em;white-space:nowrap}.pol-bank-btn{grid-column:1/-1;display:flex;align-items:center;gap:10px;padding:11px 14px;border:1px dashed #b0d4bc;border-radius:8px;background:#f7fbf8;cursor:pointer;color:#326e50;font-size:10px;font-weight:600;transition:background .15s}.pol-bank-btn:hover{background:#edf6ef}.pol-bank-done{grid-column:1/-1;display:flex;align-items:center;gap:10px;padding:11px 14px;border:1px solid #9dd4b0;border-radius:8px;background:#edf9f2;color:#2e6646;font-size:10px;font-weight:600}.pol-bank-done button{margin-left:auto;background:transparent;color:#3a7a58;font-size:9px;text-decoration:underline;border:0;cursor:pointer}.pol-bm-bd{position:fixed;inset:0;background:#0d2e1e66;z-index:100;display:flex;align-items:center;justify-content:center;padding:20px}.pol-bm{background:#fff;border-radius:14px;width:100%;max-width:580px;box-shadow:0 20px 50px #0d2e1e28;overflow:hidden}.pol-bm-head{display:flex;justify-content:space-between;align-items:center;padding:20px 24px 8px}.pol-bm-head h2{font-family:Georgia,serif;font-weight:500;font-size:16px;color:#254a3b;margin:0}.pol-bm-close{background:transparent;color:#7a9280;padding:4px;border-radius:6px}.pol-bm-close:hover{background:#f0f5f1;color:#345849}.pol-bm-foot{display:flex;gap:9px;padding:14px 24px 22px}.pol-docs-row{display:flex;padding:13px 24px;border-bottom:1px solid #edf1ee;gap:12px}.pol-docs-row:last-child{border-bottom:0}.pol-docs-row label{font-size:9px;color:#859a8f;letter-spacing:.08em;display:block;margin-bottom:4px;min-width:110px;flex:none}.pol-docs-row strong{font-size:11px;color:#2b5543;font-weight:600}.pol-docs-acct{font-family:monospace;letter-spacing:.06em;font-size:12px;color:#2b5543;font-weight:700}.pol-docs-empty{padding:28px 24px;font-size:10px;color:#9aa79f;text-align:center}.pol-docs-icon{cursor:pointer;display:grid;place-items:center;width:26px;height:26px;border-radius:6px;color:#7aaa8e}.pol-docs-icon:hover{background:#edf5ef;color:#2e6646}@media(max-width:900px){.policy-summary{grid-template-columns:1fr 1fr}.policy-summary-card.featured{grid-column:1/-1}.policy-row{grid-template-columns:1.4fr 1fr 1fr}.policy-row.header{display:none}.policy-row .policy-cell:nth-child(3),.policy-row .policy-cell:nth-child(4){display:none}}@media(max-width:620px){.policy-intro{display:block}.policy-head-actions{margin-top:17px}.policy-summary{grid-template-columns:1fr}.policy-summary-card.featured{grid-column:auto}.policy-toolbar{display:block}.policy-search{min-width:0}.policy-row{grid-template-columns:1fr auto}.policy-row .policy-cell:nth-child(2){display:none}.policy-row .policy-cell:nth-child(5){grid-column:1}.policy-row .policy-cell:nth-child(6){grid-column:2;grid-row:1/3}}`}</style>

    <section className="policy-intro">
      <div><p className="eyebrow">{isFarmer ? "FARMER PORTAL / COVERAGE" : "OPERATIONS / POLICY PORTFOLIO"}</p><h1>{isFarmer ? "My policies" : "Policies"}</h1><p>{isFarmer ? "Keep every field, season and protection detail within easy reach. Your coverage is synced live from the insurance system." : "Search and inspect enrolled policies across every farmer, crop and coverage tier."}</p></div>
      <div className="policy-head-actions"><button className="secondary-button" onClick={() => policies.reload()}><ArrowUpRight size={14} /> Refresh</button>{isFarmer && <button className="primary-button" onClick={() => setOpen(!open)}><Plus size={15} /> Enroll policy</button>}</div>
    </section>

    <PageState {...profile} /><PageState {...policies} />

    <section className="policy-summary">
      <div className="policy-summary-card featured"><div className="policy-summary-icon"><ShieldCheck size={17} /></div><span>Protected portfolio</span><strong>{active} {active === 1 ? "policy" : "policies"}</strong><small>Coverage currently in force or under review</small></div>
      <div className="policy-summary-card"><div className="policy-summary-icon"><WalletCards size={17} /></div><span>Total sum insured</span><strong>{money(covered)}</strong><small>{isFarmer ? "Across your listed policies" : "Across all listed policies"}</small></div>
      <div className="policy-summary-card"><div className="policy-summary-icon"><CalendarDays size={17} /></div><span>Latest enrollment</span>
        <strong>{latestPolicy ? new Date(latestPolicy.enrollmentDate).toLocaleDateString("en-IN", { day: "2-digit", month: "short" }) : "—"}</strong>
        <small>Seasonal coverage tracking</small>
      </div>
    </section>

    {isFarmer && open && (
      <section className="panel policy-form farmer-form">
        <div className="panel-heading"><div><span className="panel-kicker">NEW COVERAGE</span><h2>Enroll a crop policy</h2><p className="panel-description">Add a new field to your protected portfolio. Bank details are required for DBT claim disbursement.</p></div></div>
        <form onSubmit={enroll}>
          <div className="form-grid">
            <label>Crop
              <select required value={form.cropKey} onChange={(e) => { const found = crops.find((c: any) => `${c.cropName}-${c.season}` === e.target.value); setForm({ ...form, cropKey: e.target.value, cropName: found?.cropName || "", season: found?.season || "" }); }}>
                <option value="">Select crop</option>
                {crops.map((c: any) => <option key={`${c.cropName}-${c.season}`} value={`${c.cropName}-${c.season}`}>{c.cropName} · {c.season}</option>)}
              </select>
            </label>
            <label>Season<input readOnly value={form.season} placeholder="Auto-filled from crop" style={{ background: "#f8faf8", color: "#6b8278", cursor: "default" }} /></label>
            <label>How much land are you cultivating?
              <select required value={form.areaKey} onChange={(e) => applyArea(e.target.value)}>
                <option value="">Select land area</option>
                {AREA_OPTIONS.map((o) => <option key={o.key} value={o.key}>{o.label}</option>)}
              </select>
              {form.areaKey && form.areaKey !== "manual" && <span className="policy-field-hint">≈ {areaHa.toFixed(2)} hectares</span>}
            </label>
            {form.areaKey === "manual" && (
              <label>Exact land area (hectares)<input required min="0.0001" step="0.0001" type="number" value={form.sownAreaHa} onChange={f("sownAreaHa")} /></label>
            )}

            <label className="full-span">Crop coverage amount
              <span className="policy-field-hint" style={{ marginTop: 0, marginBottom: 6 }}>Maximum amount covered by this policy — calculated automatically, nothing to enter.</span>
              {selectedCrop && areaHa > 0 ? (
                <div className="policy-derived-strip">
                  <span>Your crop coverage: <b>{money(computedCoverage)}</b></span>
                  <div className="policy-derived-sep" />
                  <span>{selectedCrop.cropName} · {Number(selectedCrop.standardYieldKgHa).toLocaleString("en-IN")} kg/ha standard yield × {money(selectedCrop.mspRate)}/quintal MSP × {areaHa.toFixed(2)} ha</span>
                </div>
              ) : (
                <div className="policy-derived-strip policy-derived-pending"><span>Select a crop and your land area above to see your coverage amount.</span></div>
              )}
            </label>

            <div className="bank-divider"><span>BANK / DBT DETAILS</span></div>

            {bankDone ? (
              <div className="pol-bank-done">
                <CheckCircle2 size={15} color="#3a9e68" />
                <span>Bank details added{form.bankName ? ` — ${form.bankName}` : ""}{form.bankAccountNo ? ` ····${form.bankAccountNo.slice(-4)}` : ""}</span>
                <button type="button" onClick={() => setBankModal(true)}>Edit</button>
              </div>
            ) : (
              <button type="button" className="pol-bank-btn" onClick={() => setBankModal(true)}>
                <CreditCard size={15} /> Add bank details
              </button>
            )}
          </div>
          <button className="primary-button" type="submit" style={{ marginTop: 4 }}>Submit enrollment <ChevronRight size={14} /></button>
        </form>
      </section>
    )}

    {/* Bank details popup */}
    {bankModal && (
      <div className="pol-bm-bd" onClick={() => setBankModal(false)}>
        <div className="pol-bm" onClick={(e) => e.stopPropagation()}>
          <div className="pol-bm-head">
            <div><span className="panel-kicker">DBT DETAILS</span><h2>Bank account details</h2></div>
            <button className="pol-bm-close" onClick={() => setBankModal(false)}><X size={16} /></button>
          </div>
          <div className="form-grid" style={{ padding: "4px 24px 4px" }}>
            <label>Account holder name<input value={form.bankHolderName} onChange={f("bankHolderName")} placeholder="As per bank passbook" /></label>
            <label>Bank name<input value={form.bankName} onChange={f("bankName")} placeholder="e.g. State Bank of India" /></label>
            <label>Account number
              <input type="password" autoComplete="new-password" value={form.bankAccountNo} onChange={(e) => setForm((p) => ({ ...p, bankAccountNo: e.target.value.replace(/\D/g, "") }))} placeholder="Enter account number" />
            </label>
            <label>Confirm account number
              <input type="password" autoComplete="new-password" value={form.bankConfirmAccountNo} onChange={(e) => setForm((p) => ({ ...p, bankConfirmAccountNo: e.target.value.replace(/\D/g, "") }))} placeholder="Re-enter to confirm" />
            </label>
            <label>IFSC code<input value={form.bankIfsc} onChange={(e) => setForm((p) => ({ ...p, bankIfsc: e.target.value.toUpperCase() }))} placeholder="e.g. SBIN0001234" maxLength={11} /></label>
            <label>Branch (optional)<input value={form.bankBranch} onChange={f("bankBranch")} placeholder="Branch name or address" /></label>
          </div>
          <div className="pol-bm-foot">
            <button className="primary-button" type="button" onClick={saveBankDetails}>Save bank details <ChevronRight size={14} /></button>
            <button className="secondary-button" type="button" onClick={() => setBankModal(false)}>Cancel</button>
          </div>
        </div>
      </div>
    )}

    {/* Docs popup — bank details for a saved policy */}
    {docsPolicy && (
      <div className="pol-bm-bd" onClick={() => setDocsPolicy(null)}>
        <div className="pol-bm" onClick={(e) => e.stopPropagation()}>
          <div className="pol-bm-head">
            <div><span className="panel-kicker">POLICY DOCUMENTS</span><h2>Bank / DBT details</h2></div>
            <button className="pol-bm-close" onClick={() => setDocsPolicy(null)}><X size={16} /></button>
          </div>
          <p style={{ margin: "0 24px 8px", fontSize: 10, color: "#859a8f" }}>{docsPolicy.policyNumber || `Policy #${docsPolicy.id}`} · enrolled {docsPolicy.enrollmentDate ? new Date(docsPolicy.enrollmentDate).toLocaleDateString("en-IN") : "—"}</p>
          {docsPolicy.bankAccountNo ? <>
            <div className="pol-docs-row"><label>ACCOUNT NUMBER</label><strong className="pol-docs-acct">{docsPolicy.bankAccountNo}</strong></div>
            <div className="pol-docs-row"><label>ACCOUNT HOLDER</label><strong>{docsPolicy.bankHolderName || "—"}</strong></div>
            <div className="pol-docs-row"><label>BANK NAME</label><strong>{docsPolicy.bankName || "—"}{docsPolicy.bankBranch ? <span style={{ fontWeight: 400, color: "#7a9280", fontSize: 9, marginLeft: 6 }}>{docsPolicy.bankBranch}</span> : null}</strong></div>
            <div className="pol-docs-row" style={{ borderBottom: 0 }}><label>IFSC CODE</label><strong style={{ fontFamily: "monospace", letterSpacing: ".05em" }}>{docsPolicy.bankIfsc || "—"}</strong></div>
          </> : (
            <div className="pol-docs-empty">No bank details recorded for this policy.</div>
          )}
          <div className="pol-bm-foot">
            <button className="secondary-button" onClick={() => setDocsPolicy(null)}>Close</button>
          </div>
        </div>
      </div>
    )}

    <section className="panel">
      <div className="policy-toolbar">
        <div className="policy-search"><Leaf size={15} /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={isFarmer ? "Search crop, policy number or status…" : "Search farmer, crop, policy number or status…"} /></div>
        <span className="muted">{filtered.length} records</span>
      </div>
      <div className="policy-table">
        <div className="policy-row header"><span>Policy</span><span>Crop / season</span><span>Area</span><span>Coverage</span><span>Status</span><span>Docs</span></div>
        {filtered.length ? filtered.map((p) => (
          <div className="policy-row" key={p.id}>
            <div className="policy-main"><span className="policy-crop-icon"><Sprout size={16} /></span><div><strong>{p.policyNumber || `Policy #${p.id}`}</strong><small>Enrolled {p.enrollmentDate ? new Date(p.enrollmentDate).toLocaleDateString("en-IN") : "—"}</small>{!isFarmer && <small>{p.farmerName || "Unknown farmer"}</small>}</div></div>
            <div className="policy-cell">{p.cropName || "Crop policy"}<small>{p.season || "—"}</small></div>
            <div className="policy-cell">{p.sownAreaHa || 0} ha</div>
            <div className="policy-cell">{money(p.sumInsured)}</div>
            <div><span className={statusClass(p.status)}>{p.status || "UNKNOWN"}</span></div>
            <div className="pol-docs-icon" title="View bank details" onClick={() => setDocsPolicy(p)}><FileText size={15} /></div>
          </div>
        )) : (
          <div className="policy-empty"><BadgeCheck size={28} color="#a5c88c" /><strong>No matching policies</strong><span>Your live policy records will appear here once available.</span></div>
        )}
      </div>
    </section>
  </>;
}
