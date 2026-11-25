import { BarChart, Bar, LineChart, Line, AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, FunnelChart, Funnel, LabelList } from "recharts";
import { api } from "../../services/api";
import { PageState, useApi } from "../../components/dashboard/ApiPage";

const listOf = (x: any): any[] => Array.isArray(x) ? x : Array.isArray(x?.content) ? x.content : [];
const money = (v: any) => `₹${Number(v || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;

function ChartEmpty({ label }: { label: string }) {
  return <div className="chart-empty"><strong>No data yet</strong><span>{label}</span></div>;
}

function ClaimRatioCard({ value }: { value: any }) {
  const num = Number(value);
  return <div className="panel insight-card"><span className="panel-kicker">CLAIM RATIO</span><div className="insight-value">{Number.isFinite(num) ? `${num}%` : "—"}</div><p className="muted" style={{ margin: 0, fontSize: 9 }}>Approved amount as a share of claimed amount, across all claims.</p></div>;
}

function BenchmarksCard({ rows }: { rows: any[] }) {
  if (!rows.length) return <div className="panel insight-card"><span className="panel-kicker">BENCHMARKS</span><div className="insight-value" style={{ fontSize: 12 }}>No benchmark data yet</div></div>;
  return <div className="panel insight-card"><span className="panel-kicker">BENCHMARKS</span>{rows.map((b: any) => <div key={b.metric} style={{ fontSize: 10, color: "#516e60", margin: "8px 0" }}><b style={{ color: "#254e3d" }}>{b.metric}</b>: {Number(b.currentValue)}% vs target {Number(b.targetValue)}% <b style={{ color: b.direction === "ABOVE" ? "#4f7b60" : "#9a625b" }}>({b.direction})</b></div>)}</div>;
}

function AnomaliesCard({ rows }: { rows: any[] }) {
  if (!rows.length) return <div className="panel insight-card"><span className="panel-kicker">ANOMALY DETECTION</span><div className="insight-value" style={{ fontSize: 12 }}>No anomalies detected</div><p className="muted" style={{ margin: 0, fontSize: 9 }}>Needs at least two loss assessments to compute.</p></div>;
  return <div className="panel insight-card"><span className="panel-kicker">ANOMALY DETECTION</span>{rows.map((a: any, i: number) => <div key={i} style={{ fontSize: 10, color: "#516e60", margin: "8px 0" }}><b style={{ color: a.severity === "HIGH" ? "#9a625b" : a.severity === "MEDIUM" ? "#a4713f" : "#254e3d" }}>{a.severity}</b> — {a.metric}: z-score {Number(a.zScore).toFixed(2)}<br /><span style={{ fontSize: 9, color: "#8b9891" }}>{a.explanation}</span></div>)}</div>;
}

export default function AnalyticsPage() {
  const q = useApi(async () => {
    const results = await Promise.allSettled([
      api.analytics.dashboard(),
      api.analytics.claimRatio(),
      api.analytics.lossPatterns(),
      api.analytics.trends(),
      api.analytics.benchmarks(),
      api.analytics.anomalies(),
      api.analytics.funnel(),
      api.analytics.weeklyInsight(),
      api.analytics.forecast(7),
      api.claims(0, 100),
      api.losses(),
      api.surveys(),
    ]);
    return results.map((r) => (r.status === "fulfilled" ? r.value : null));
  });

  const data = q.data || [];
  const d: any = data[0] || {};
  const claimRatio = data[1];
  const lossPatterns = listOf(data[2]);
  const trend = listOf(data[3]);
  const benchmarks = listOf(data[4]);
  const anomalies = listOf(data[5]);
  const funnelStages: Record<string, number> = (data[6] as any)?.stages || {};
  const funnelData = Object.entries(funnelStages).map(([name, value]) => ({ name: name.replaceAll("_", " "), value: Number(value) }));
  const weeklyInsight = data[7];
  const forecastValues: any[] = Array.isArray((data[8] as any)?.values) ? (data[8] as any).values : [];
  const forecastMethod = (data[8] as any)?.method;
  const forecastData = forecastValues.map((v, i) => ({ day: `Day ${i + 1}`, value: Number(v) }));
  const claimsList = listOf(data[9]);
  const lossesList = listOf(data[10]);
  const surveysList = listOf(data[11]);

  const pendingAssessments = lossesList.filter((l: any) => l.status === "SUBMITTED").length;
  const surveysAssigned = surveysList.length;
  const surveysSubmitted = surveysList.filter((s: any) => ["SUBMITTED", "VERIFIED"].includes(s.status)).length;
  const claimsUnderReview = claimsList.filter((c: any) => ["INITIATED", "UNDER_REVIEW"].includes(c.status)).length;
  const claimsApproved = claimsList.filter((c: any) => ["APPROVED", "PAID"].includes(c.status)).length;
  const claimsPaid = claimsList.filter((c: any) => c.status === "PAID").length;

  const cards: [string, string][] = [
    ["Policies under management", d.policies != null ? String(d.policies) : "—"],
    ["Active policies", d.activePolicies != null ? String(d.activePolicies) : "—"],
    ["Total coverage", d.exposure != null ? money(d.exposure) : "—"],
    ["Loss reports", d.losses != null ? String(d.losses) : "—"],
    ["Pending assessments", q.loading ? "—" : String(pendingAssessments)],
    ["Claims", d.claims != null ? String(d.claims) : "—"],
    ["Claims awaiting review", q.loading ? "—" : String(claimsUnderReview)],
    ["Paid claims", d.paidClaims != null ? String(d.paidClaims) : "—"],
  ];

  const workflow: [string, number][] = [
    ["Losses reported", lossesList.length],
    ["Surveyor assigned", surveysAssigned],
    ["Survey submitted", surveysSubmitted],
    ["Claim initiated", claimsList.length],
    ["Under review", claimsUnderReview],
    ["Approved", claimsApproved],
    ["Paid", claimsPaid],
  ];

  return <>
    <section className="page-heading">
      <div><p className="eyebrow">INTELLIGENCE / ANALYTICS</p><h1>Insurance Intelligence</h1><p className="subhead">Real-time KPIs, loss patterns, claim workflow and operational signals for the insurance desk.</p></div>
    </section>

    <PageState {...q} />

    <section className="stats-grid compact">
      {cards.map((c) => <div className="stat-card" key={c[0]}><div className="stat-icon mint">◈</div><div className="stat-copy"><span>{c[0]}</span><strong>{c[1]}</strong><small className="muted">Live backend KPI</small></div></div>)}
    </section>

    <section className="panel table-panel" style={{ marginBottom: 20 }}>
      <div className="panel-heading"><div><span className="panel-kicker">CLAIM WORKFLOW</span><h2>Loss-to-payout lifecycle</h2><p className="panel-description">{q.loading ? "Loading live counts…" : "Live counts at every stage, from reported loss through to a paid claim."}</p></div></div>
      <div className="stats-grid compact" style={{ padding: "0 19px 19px" }}>
        {workflow.map(([label, value]) => <div className="stat-card" key={label}><div className="stat-icon sand">◈</div><div className="stat-copy"><span>{label}</span><strong>{q.loading ? "—" : value}</strong></div></div>)}
      </div>
    </section>

    <div className="analytics-layout">
      <div className="panel chart-card wide">
        <div className="panel-heading"><div><span className="panel-kicker">PERFORMANCE</span><h2>Trend intelligence</h2><p className="panel-description">Daily policies enrolled and claims filed over the last 30 days.</p></div></div>
        <div className="chart-box">{trend.length ? <ResponsiveContainer width="100%" height={280}><LineChart data={trend}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" /><YAxis /><Tooltip /><Line type="monotone" dataKey="claims" strokeWidth={2} name="Claims" /><Line type="monotone" dataKey="policies" strokeWidth={2} name="Policies" /></LineChart></ResponsiveContainer> : <ChartEmpty label="No policy or claim activity in the trend window yet." />}</div>
      </div>
      <div className="panel chart-card">
        <div className="panel-heading"><div><span className="panel-kicker">LOSS PATTERNS</span><h2>Loss reports by type</h2></div></div>
        <div className="chart-box">{lossPatterns.length ? <ResponsiveContainer width="100%" height={280}><BarChart data={lossPatterns}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="lossType" /><YAxis /><Tooltip /><Bar dataKey="count" name="Loss reports" /></BarChart></ResponsiveContainer> : <ChartEmpty label="No loss notifications recorded yet." />}</div>
      </div>
      <div className="panel chart-card">
        <div className="panel-heading"><div><span className="panel-kicker">CLAIM FUNNEL</span><h2>Workflow conversion</h2></div></div>
        <div className="chart-box">{funnelData.some((f) => f.value > 0) ? <ResponsiveContainer width="100%" height={280}><FunnelChart><Tooltip /><Funnel dataKey="value" data={funnelData} isAnimationActive><LabelList position="right" fill="#315849" stroke="none" dataKey="name" /></Funnel></FunnelChart></ResponsiveContainer> : <ChartEmpty label="No policy, loss or claim records yet." />}</div>
      </div>
      <div className="panel chart-card wide">
        <div className="panel-heading"><div><span className="panel-kicker">FORECAST</span><h2>{forecastValues.length ? `${forecastValues.length}-day` : "7-day"} operational outlook</h2><p className="panel-description">{forecastMethod ? `Method: ${forecastMethod}` : ""}</p></div></div>
        <div className="chart-box">{forecastData.length ? <ResponsiveContainer width="100%" height={250}><AreaChart data={forecastData}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="day" /><YAxis /><Tooltip /><Area type="monotone" dataKey="value" name="Projected policy demand" fill="#d9ead8" stroke="#3f785c" /></AreaChart></ResponsiveContainer> : <ChartEmpty label="The forecasting backend did not return a projection." />}</div>
      </div>
    </div>

    <section className="insight-grid">
      <ClaimRatioCard value={claimRatio} />
      <BenchmarksCard rows={benchmarks} />
      <AnomaliesCard rows={anomalies} />
      <div className="panel insight-card"><span className="panel-kicker">WEEKLY MANAGEMENT INSIGHT</span><div className="insight-value" style={{ fontSize: 11, lineHeight: 1.6, fontFamily: "inherit", fontWeight: 400 }}>{typeof weeklyInsight === "string" && weeklyInsight ? weeklyInsight : "Not available yet."}</div></div>
    </section>
  </>;
}
