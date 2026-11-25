import { isValidElement, useEffect, useState } from "react";
import { Download, Filter, Loader2, Plus, Search } from "lucide-react";
import { toast } from "sonner";

export function useApi<T>(loader: () => Promise<T>, deps: any[] = []) {
  const [data, setData] = useState<T | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const reload = async () => { setLoading(true); setError(""); try { setData(await loader()); } catch (e:any) { setError(e.message || "Unable to load data"); } finally { setLoading(false); } };
  useEffect(() => { reload(); // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps); return { data, loading, error, reload };
}

export function PageState({loading,error,onRetry,reload}:{loading:boolean;error:string;onRetry?:()=>void;reload?:()=>void}) {
  if (loading) return <div className="api-state"><Loader2 className="spin" size={22}/> Loading live data…</div>;
  if (error) return <div className="api-state api-error"><strong>Backend unavailable</strong><span>{error}</span>{(onRetry || reload) && <button className="primary-button" onClick={onRetry || reload}>Retry</button>}</div>;
  return null;
}

function renderCell(c: any) {
  if (isValidElement(c)) return c;
  if (c && typeof c === "object") return JSON.stringify(c);
  return String(c ?? "—");
}

export function DataTable({headers,rows,widths}:{headers:string[];rows:any[][];widths?:string}) {
  // Columns must match the actual header count — the shared .table-row CSS ships a fixed 8-track default
  // that mismatches any table with a different number of columns, squeezing/overlapping content.
  const gridTemplateColumns = widths || `repeat(${headers.length}, minmax(110px, 1fr))`;
  const minWidth = Math.max(780, headers.length * 130);
  return <div className="data-table">
    <div className="table-row table-header" style={{ gridTemplateColumns, minWidth }}>{headers.map(h=><span key={h}>{h}</span>)}</div>
    {rows.length ? rows.map((r,i)=><div className="table-row" key={i} style={{ gridTemplateColumns, minWidth }}>{r.map((c,j)=><span key={j}>{renderCell(c)}</span>)}</div>) : <div className="empty-row">No records returned by the backend.</div>}
  </div>;
}

export function DataToolbar({value,onChange,onExport,onCreate,placeholder="Search…"}:{value:string;onChange:(v:string)=>void;onExport?:()=>void;onCreate?:()=>void;placeholder?:string}) {
 return <div className="table-toolbar"><div className="table-search"><Search size={15}/><input value={value} onChange={e=>onChange(e.target.value)} placeholder={placeholder}/></div><div className="toolbar-actions"><button><Filter size={14}/> Filters</button>{onExport&&<button onClick={onExport}><Download size={14}/> Export</button>}{onCreate&&<button onClick={onCreate}><Plus size={14}/> Create</button>}</div></div>;
}
export function handleError(e:any){toast.error(e?.message || "Operation failed");}
