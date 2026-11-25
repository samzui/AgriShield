import { useState } from 'react';
import { Building2, CreditCard, ShieldCheck } from 'lucide-react';
import { api } from '../../services/api';
import { PageState, useApi } from '../../components/dashboard/ApiPage';
import { toast } from 'sonner';

const mask = (v: any) => {
  const s = String(v || '');
  if (s.length < 4) return s;
  return '••••••••' + s.slice(-4);
};

export default function ProfilePage() {
  const p = useApi(api.profile);
  const d: any = p.data || {};
  const isFarmer = String(d.role || '').toUpperCase() === 'FARMER';
  const farmerId = d?.id;
  const policies = useApi(
    () => isFarmer && farmerId ? api.farmerPolicies(Number(farmerId)) : Promise.resolve([]),
    [isFarmer, farmerId]
  );
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');

  const policyRows: any[] = Array.isArray(policies.data)
    ? policies.data
    : (policies.data as any)?.content || [];
  const bankPolicy = policyRows.find((r: any) => r.bankAccountNo);

  async function save() {
    try {
      const updated = await api.updateProfile({ name: name || d.name, phone: phone || d.phone });
      setName(updated.name || '');
      setPhone(updated.phone || '');
      toast.success('Profile updated');
    } catch (e: any) {
      toast.error(e.message);
    }
  }

  return <>
    <style>{`.fp-bank-panel{margin-top:14px}.fp-bank-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:0;padding:0 24px 24px}.fp-bank-cell{padding:16px 0;border-bottom:1px solid #edf1ee}.fp-bank-cell:nth-child(n+4){border-bottom:0}.fp-bank-cell label{display:block;font-size:9px;color:#859a8f;letter-spacing:.09em;margin-bottom:6px}.fp-bank-cell strong{display:block;font-size:11px;color:#2b5543;font-weight:600}.fp-bank-cell small{display:block;font-size:9px;color:#9aa79f;margin-top:3px}.fp-bank-acct{display:inline-flex;align-items:center;gap:8px;padding:8px 12px;background:#f4faf5;border:1px solid #d8ece0;border-radius:8px;color:#2b5543;font-size:12px;font-weight:600;letter-spacing:.08em;font-family:monospace}.fp-bank-empty{padding:28px 24px;display:flex;align-items:center;gap:14px;color:#8b9a91}.fp-bank-empty div{flex:1}.fp-bank-empty strong{display:block;font-size:11px;color:#426354;font-weight:600;margin-bottom:4px}.fp-bank-empty span{font-size:9px;line-height:1.6}`}</style>

    <section className="page-heading">
      <div>
        <p className="eyebrow">ACCOUNT / PROFILE</p>
        <h1>My Profile</h1>
        <p className="subhead">Manage your authenticated account details and verification status.</p>
      </div>
    </section>

    <PageState {...p} />

    <section className="profile-grid">
      <div className="panel profile-card">
        <div className="profile-avatar">{(d.name || 'U').split(' ').map((x: string) => x[0]).join('').slice(0, 2)}</div>
        <h2>{d.name || 'User'}</h2>
        <span className="profile-role">{d.role || ''}</span>
        <div className="verification-list">
          <span>Account <b>{d.active ? 'Active' : 'Inactive'}</b></span>
          <span>Email verification <b>{d.emailVerified ? 'Verified' : 'Pending'}</b></span>
          <span>Supervisor approval <b>{d.supervisorApproved ? 'Approved' : 'Pending'}</b></span>
        </div>
      </div>
      <div className="panel form-panel profile-form">
        <div className="panel-heading">
          <div>
            <span className="panel-kicker">PERSONAL DETAILS</span>
            <h2>Update profile</h2>
          </div>
        </div>
        <div className="form-grid">
          <label>Full name<input value={name || d.name || ''} onChange={e => setName(e.target.value)} /></label>
          <label>Phone number<input value={phone || d.phone || ''} maxLength={10} onChange={e => setPhone(e.target.value.replace(/\D/g, ''))} /></label>
          <label>Email<input value={d.email || ''} readOnly /></label>
          <label>User ID<input value={d.userId || ''} readOnly /></label>
        </div>
        <button className="primary-button" onClick={save}>Save profile</button>
      </div>
    </section>

    {isFarmer && (
      <section className="panel fp-bank-panel">
        <div className="panel-heading">
          <div>
            <span className="panel-kicker">DBT / DISBURSEMENT</span>
            <h2>Bank account details</h2>
            <p className="panel-description">Account registered for Direct Benefit Transfer of approved crop insurance claims.</p>
          </div>
          <CreditCard size={18} color="#6b9e80" />
        </div>

        {bankPolicy ? (
          <div className="fp-bank-grid">
            <div className="fp-bank-cell" style={{ gridColumn: '1 / -1', paddingBottom: 18 }}>
              <label>ACCOUNT NUMBER</label>
              <span className="fp-bank-acct"><ShieldCheck size={13} color="#4a9b6a" />{mask(bankPolicy.bankAccountNo)}</span>
            </div>
            <div className="fp-bank-cell">
              <label>ACCOUNT HOLDER</label>
              <strong>{bankPolicy.bankHolderName || d.name || '—'}</strong>
            </div>
            <div className="fp-bank-cell">
              <label>BANK NAME</label>
              <strong>{bankPolicy.bankName || '—'}</strong>
              <small>{bankPolicy.bankBranch || ''}</small>
            </div>
            <div className="fp-bank-cell">
              <label>IFSC CODE</label>
              <strong style={{ fontFamily: 'monospace', letterSpacing: '.05em' }}>{bankPolicy.bankIfsc || '—'}</strong>
              <small>Policy #{bankPolicy.id}</small>
            </div>
          </div>
        ) : (
          <div className="fp-bank-empty">
            <Building2 size={30} color="#b0ccbc" />
            <div>
              <strong>No bank account on file</strong>
              <span>Your DBT account details will appear here once you enroll a policy with bank information. You can add bank details during policy enrollment on the Policies page.</span>
            </div>
          </div>
        )}
      </section>
    )}
  </>;
}
