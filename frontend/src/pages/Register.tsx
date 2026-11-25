import { FormEvent, useEffect, useState } from "react";
import { ArrowLeft, ArrowUpRight, Check, Eye, EyeOff, Leaf, LockKeyhole, UserRound } from "lucide-react";
import { api } from "../services/api";
import { toast } from "sonner";

const roles = ["FARMER", "BANK_OFFICER", "SURVEYOR", "INSURANCE_OFFICER", "STATE_OFFICER"];
export default function Register({ onBack }: { onBack: () => void }) {
  const [step, setStep] = useState<"register" | "verify">("register");
  const [form, setForm] = useState({ name: "", userId: "", email: "", phone: "", password: "", role: "FARMER" });
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [resending, setResending] = useState(false);
  const [emailDelivered, setEmailDelivered] = useState(false);
  const set = (key: keyof typeof form, value: string) => setForm(prev => ({ ...prev, [key]: value }));

  useEffect(() => {
    api.auth.mailMode().then((r: any) => setEmailDelivered(r?.mode === "smtp")).catch(() => setEmailDelivered(false));
  }, []);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!/^\d{10}$/.test(form.phone)) { toast.error("Phone number must be exactly 10 digits."); return; }
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(form.password)) { toast.error("Use 8+ characters with upper, lower, number and symbol."); return; }
    setBusy(true);
    try {
      await api.auth.register(form);
      setRegisteredEmail(form.email);
      setStep("verify");
      toast.success("Account created. Check your email (or server console in dev) for your OTP.");
    } catch (error: any) {
      toast.error(error?.message || "We could not create your account. Please review the details.");
    } finally {
      setBusy(false);
    }
  }

  async function verifyOtp(e: FormEvent) {
    e.preventDefault();
    if (!otp.trim()) { toast.error("Please enter the OTP sent to your email."); return; }
    setVerifying(true);
    try {
      await api.auth.verifyEmail(registeredEmail, otp.trim());
      toast.success("Email verified. You can now sign in.");
      onBack();
    } catch (error: any) {
      toast.error(error?.message || "Invalid or expired OTP. Please try again.");
    } finally {
      setVerifying(false);
    }
  }

  async function resendOtp() {
    setResending(true);
    try {
      await api.auth.resendOtp(registeredEmail);
      toast.success("A new OTP has been sent.");
    } catch (error: any) {
      toast.error(error?.message || "Could not resend OTP.");
    } finally {
      setResending(false);
    }
  }

  if (step === "verify") return (
    <div className="auth-page">
      <div className="auth-art auth-art-register">
        <button className="auth-back" onClick={onBack}><ArrowLeft size={15} /> Back to sign in</button>
        <div className="auth-art-copy">
          <span className="eyebrow-light">ONE LAST STEP</span>
          <h2>Confirm your<br /><em>email address.</em></h2>
          <p>Enter the 6-digit code we sent to your inbox to activate your account.</p>
        </div>
        <span className="auth-art-note"><Check size={15} /> Code expires in 15 minutes</span>
      </div>
      <div className="auth-panel">
        <div className="auth-brand"><span><Leaf size={17} /></span><strong>AGRISHIELD</strong></div>
        <div className="auth-heading">
          <p className="hero-kicker">EMAIL VERIFICATION</p>
          <h1>Enter your OTP.</h1>
          <p>{emailDelivered ? <>We sent a verification code to <strong>{registeredEmail}</strong>.</> : <>We sent a code to <strong>{registeredEmail}</strong>. In local development, check the server console.</>}</p>
        </div>
        {!emailDelivered && <div className="api-state" style={{marginBottom:16}}>In dev mode the OTP prints to the Spring Boot console — look for the <strong>[DEV OTP]</strong> banner.</div>}
        <form onSubmit={verifyOtp} className="auth-form">
          <label>Verification code
            <div className="input-wrap">
              <input
                autoFocus
                inputMode="numeric"
                maxLength={6}
                value={otp}
                onChange={e => setOtp(e.target.value.replace(/\D/g, ""))}
                placeholder="6-digit OTP"
                required
              />
            </div>
          </label>
          <button className="auth-submit" disabled={verifying}>{verifying ? "Verifying…" : "Verify email"} {!verifying && <ArrowUpRight size={16} />}</button>
        </form>
        <div className="auth-divider" />
        <button className="auth-link" onClick={resendOtp} disabled={resending}>{resending ? "Sending…" : "Resend OTP"} <ArrowUpRight size={14} /></button>
        <button className="auth-link" onClick={onBack}>Already verified? Sign in <ArrowUpRight size={14} /></button>
        <p className="auth-security"><LockKeyhole size={13} /> Your details are encrypted and handled securely</p>
      </div>
    </div>
  );

  return (
    <div className="auth-page">
      <div className="auth-art auth-art-register">
        <button className="auth-back" onClick={onBack}><ArrowLeft size={15} /> Sign in instead</button>
        <div className="auth-art-copy">
          <span className="eyebrow-light">A BETTER SEASON STARTS HERE</span>
          <h2>Put your work<br /><em>in safe hands.</em></h2>
          <p>Join a connected network built around clarity, fairness and faster recovery.</p>
        </div>
        <span className="auth-art-note"><Check size={15} /> Simple, transparent, human</span>
      </div>
      <div className="auth-panel auth-panel-register">
        <div className="auth-brand"><span><Leaf size={17} /></span><strong>AGRISHIELD</strong></div>
        <div className="auth-heading">
          <p className="hero-kicker">CREATE YOUR SECURE ACCOUNT</p>
          <h1>Join the platform.</h1>
          <p>Set up your workspace in less than two minutes.</p>
        </div>
        <form onSubmit={submit} className="auth-form">
          <div className="auth-grid">
            <label>Full name<div className="input-wrap"><UserRound size={16} /><input value={form.name} onChange={e => set("name", e.target.value)} placeholder="Your full name" required /></div></label>
            <label>User ID<div className="input-wrap"><input value={form.userId} onChange={e => set("userId", e.target.value)} placeholder="Choose a user ID" required /></div></label>
            <label>Email<div className="input-wrap"><input type="email" value={form.email} onChange={e => set("email", e.target.value)} placeholder="you@example.com" required /></div></label>
            <label>Phone<div className="input-wrap"><span className="prefix">+91</span><input inputMode="numeric" pattern="[0-9]{10}" maxLength={10} value={form.phone} onChange={e => set("phone", e.target.value.replace(/\D/g, ""))} placeholder="10 digit number" required /></div></label>
            <label className="password-field">Password<div className="input-wrap"><LockKeyhole size={16} /><input type={showPassword ? "text" : "password"} value={form.password} onChange={e => set("password", e.target.value)} placeholder="Create a strong password" minLength={8} required /><button type="button" className="input-action" onClick={() => setShowPassword(x => !x)} aria-label="Toggle password visibility">{showPassword ? <EyeOff size={16} /> : <Eye size={16} />}</button></div></label>
            <label>Account type<div className="input-wrap"><select value={form.role} onChange={e => set("role", e.target.value)}>{roles.map(role => <option key={role} value={role}>{role.replaceAll("_", " ")}</option>)}</select></div></label>
          </div>
          <button className="auth-submit" disabled={busy}>{busy ? "Creating account…" : "Create account"} {!busy && <ArrowUpRight size={16} />}</button>
        </form>
        <button className="auth-link" onClick={onBack}>Already have an account? Sign in <ArrowUpRight size={14} /></button>
        <p className="auth-security"><LockKeyhole size={13} /> Your details are encrypted and handled securely</p>
      </div>
    </div>
  );
}
