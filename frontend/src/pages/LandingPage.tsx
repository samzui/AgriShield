import { useEffect, useRef, useState } from "react";
import {
  ArrowRight,
  ArrowUpRight,
  Check,
  Leaf,
  ShieldCheck,
  Sprout,
  Wheat,
  CloudSun,
  Tractor,
} from "lucide-react";

type LandingPageProps = {
  onNavigate: (view: "Overview") => void;
};

const imageUrl =
  "/agri-hero-fields-a.jpg";

const farmerImage =
  "https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=1600&q=90";

const handsImage =
  "https://images.unsplash.com/photo-1495107334309-fcf20504a5ab?auto=format&fit=crop&w=1600&q=90";

const features = [
  {
    icon: ShieldCheck,
    number: "01",
    title: "Protect every season",
    description:
      "Give farmers a clear, secure view of their policy, coverage and protection status.",
  },
  {
    icon: Sprout,
    number: "02",
    title: "Understand field reality",
    description:
      "Connect loss notifications, field evidence, surveys and assessments in one workflow.",
  },
  {
    icon: Wheat,
    number: "03",
    title: "Settle with confidence",
    description:
      "Move every validated claim through review, approval, DBT and audit milestones.",
  },
];

export default function LandingPage({ onNavigate }: LandingPageProps ) {
  const pageRef = useRef<HTMLDivElement>(null);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const root = pageRef.current;

    if (!root) return;

    const elements = root.querySelectorAll("[data-reveal]");

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("landing-visible");
            observer.unobserve(entry.target);
          }
        });
      },
      {
        threshold: 0.14,
        rootMargin: "0px 0px -60px 0px",
      },
    );

    elements.forEach((element) => observer.observe(element));

    return () => observer.disconnect();
  }, []);

  const goToApp = () => onNavigate("Overview");

  return (
    <div ref={pageRef} className="premium-landing">
      <style>{`
        .premium-landing {
          --forest: #123d2e;
          --forest-dark: #09291f;
          --sage: #b8d99b;
          --sage-light: #e7f1df;
          --cream: #f7f8f2;
          --muted: #74857a;
          --line: rgba(18, 61, 46, 0.12);
          min-height: 100vh;
          overflow: hidden;
          color: var(--forest);
          background: var(--cream);
          font-family: "DM Sans", Inter, ui-sans-serif, system-ui, sans-serif;
        }

        .premium-landing * {
          box-sizing: border-box;
        }

        .premium-landing button,
        .premium-landing a {
          font: inherit;
        }

        .landing-nav {
          position: sticky;
          top: 0;
          z-index: 50;
          height: 78px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 0 clamp(20px, 6vw, 92px);
          background: rgba(247, 248, 242, 0.82);
          border-bottom: 1px solid var(--line);
          backdrop-filter: blur(18px);
        }

        .landing-logo {
          display: inline-flex;
          align-items: center;
          gap: 11px;
          border: 0;
          background: transparent;
          color: var(--forest);
          cursor: pointer;
          font-size: 12px;
          font-weight: 800;
          letter-spacing: 0.16em;
        }

        .landing-logo-mark {
          width: 35px;
          height: 35px;
          display: grid;
          place-items: center;
          color: var(--forest);
          background: var(--sage);
          border-radius: 16px 5px 16px 5px;
          transform: rotate(-18deg);
        }

        .landing-logo-mark svg {
          transform: rotate(18deg);
        }

        .landing-nav-links {
          display: flex;
          align-items: center;
          gap: 35px;
        }

        .landing-nav-links a {
          color: #62766b;
          font-size: 11px;
          text-decoration: none;
          transition: color 180ms ease;
        }

        .landing-nav-links a:hover {
          color: #1d704e;
        }

        .landing-nav-actions {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .landing-signin,
        .landing-primary-button {
          height: 40px;
          padding: 0 18px;
          border-radius: 24px;
          cursor: pointer;
          font-size: 11px;
          transition:
            transform 180ms ease,
            box-shadow 180ms ease;
        }

        .landing-signin {
          border: 1px solid transparent;
          background: transparent;
          color: var(--forest);
        }

        .landing-primary-button {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          border: 0;
          color: white;
          background: var(--forest);
          box-shadow: 0 10px 24px rgba(18, 61, 46, 0.18);
        }

        .landing-signin:hover,
        .landing-primary-button:hover,
        .landing-outline-button:hover {
          transform: translateY(-2px);
        }

        .landing-menu-button {
          display: none;
          border: 0;
          background: transparent;
          color: var(--forest);
          cursor: pointer;
        }

        .landing-hero {
          position: relative;
          min-height: 680px;
          display: grid;
          grid-template-columns: minmax(360px, 0.86fr) 1.14fr;
          margin: 0;
          overflow: hidden;
          background: #dce8d8;
        }

        .landing-hero-copy {
          position: relative;
          z-index: 2;
          display: flex;
          flex-direction: column;
          justify-content: center;
          padding: clamp(60px, 8vw, 125px);
          background:
            radial-gradient(circle at 12% 22%, rgba(184, 217, 155, 0.32), transparent 28%),
            linear-gradient(90deg, #f7f8f2 73%, rgba(247, 248, 242, 0));
        }

        .landing-kicker {
          color: #6e927a;
          font-size: 10px;
          font-weight: 800;
          letter-spacing: 0.18em;
        }

        .landing-hero-title {
          max-width: 580px;
          margin: 20px 0 25px;
          color: var(--forest);
          font-family: Georgia, serif;
          font-size: clamp(56px, 7vw, 105px);
          font-weight: 500;
          letter-spacing: -0.075em;
          line-height: 0.86;
        }

        .landing-hero-title em,
        .landing-section-title em {
          color: #4f8c64;
          font-style: italic;
        }

        .landing-hero-title .tomorrows {
          color: #f6f4dc;
        }

        .landing-hero-description {
          max-width: 330px;
          color: var(--muted);
          font-size: 14px;
          line-height: 1.65;
        }

        .landing-hero-actions {
          display: flex;
          align-items: center;
          gap: 26px;
          margin-top: 34px;
        }

        .landing-enter-button {
          display: inline-flex;
          align-items: center;
          gap: 10px;
          padding: 0;
          border: 0;
          color: var(--forest);
          background: transparent;
          cursor: pointer;
          font-size: 11px;
          font-weight: 700;
        }

        .landing-enter-icon {
          width: 34px;
          height: 34px;
          display: grid;
          place-items: center;
          border-radius: 50%;
          color: white;
          background: var(--forest);
        }

        .landing-protected-pill {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px 15px;
          border-radius: 10px;
          background: #fffef8;
          box-shadow: 0 14px 28px rgba(31, 77, 50, 0.13);
        }

        .landing-protected-pill strong {
          color: #1b5a40;
          font-family: Georgia, serif;
          font-size: 21px;
          font-weight: 500;
        }

        .landing-protected-pill span {
          color: #718379;
          font-size: 9px;
          line-height: 1.25;
        }

        .landing-proof {
          display: flex;
          align-items: center;
          gap: 7px;
          margin-top: 60px;
          color: #8a9c91;
          font-size: 10px;
        }

        .landing-proof svg {
          color: #4f8c64;
        }

        .landing-hero-image {
          position: relative;
          min-height: 680px;
          overflow: hidden;
          background-image:
            linear-gradient(180deg, rgba(30, 68, 45, 0.1), rgba(9, 41, 31, 0.76)),
            url("${imageUrl}");
          background-position: center;
          background-size: cover;
        }

        .landing-hero-image::before {
          content: "";
          position: absolute;
          inset: 0;
          background:
            radial-gradient(circle at 68% 20%, rgba(248, 226, 157, 0.72), transparent 16%),
            linear-gradient(90deg, rgba(247, 248, 242, 0.8), transparent 30%);
          mix-blend-mode: screen;
        }

        .landing-hero-image::after {
          content: "";
          position: absolute;
          inset: 0;
          background:
            linear-gradient(110deg, transparent 65%, rgba(184, 217, 155, 0.15)),
            linear-gradient(0deg, rgba(9, 41, 31, 0.62), transparent 52%);
        }

        .landing-image-label {
          position: absolute;
          z-index: 2;
          top: 52px;
          right: clamp(25px, 7vw, 105px);
          width: 190px;
          padding: 16px;
          border-left: 2px solid var(--sage);
          color: white;
          background: rgba(238, 246, 226, 0.14);
          backdrop-filter: blur(12px);
        }

        .landing-image-label span {
          display: block;
          margin-bottom: 10px;
          color: var(--sage);
          font-size: 8px;
          font-weight: 800;
          letter-spacing: 0.16em;
        }

        .landing-image-label strong {
          font-family: Georgia, serif;
          font-size: 19px;
          font-weight: 500;
          line-height: 1.05;
        }

        .landing-image-caption {
          position: absolute;
          z-index: 2;
          right: clamp(25px, 7vw, 105px);
          bottom: 48px;
          color: #f6f4dc;
          font-family: Georgia, serif;
          font-size: clamp(20px, 2.5vw, 31px);
          line-height: 0.98;
          text-align: right;
        }

        .landing-stats {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          align-items: center;
          margin: 0 18px;
          padding: 28px clamp(25px, 5vw, 75px);
          background: white;
        }

        .landing-stat {
          display: flex;
          flex-direction: column;
          gap: 5px;
          padding: 0 24px;
          border-right: 1px solid var(--line);
        }

        .landing-stat:last-child {
          border-right: 0;
        }

        .landing-stat strong {
          color: var(--forest);
          font-family: Georgia, serif;
          font-size: 30px;
          font-weight: 500;
        }

        .landing-stat span {
          color: #829188;
          font-size: 10px;
        }

        .landing-intro {
          display: grid;
          place-items: center;
          padding: clamp(110px, 14vw, 190px) 25px;
          text-align: center;
        }

        .landing-section-title {
          margin: 20px 0;
          color: #194735;
          font-family: Georgia, serif;
          font-size: clamp(42px, 5.5vw, 78px);
          font-weight: 500;
          letter-spacing: -0.065em;
          line-height: 0.94;
        }

        .landing-intro p {
          max-width: 470px;
          margin: 0 auto 28px;
          color: #7b8d82;
          font-size: 14px;
          line-height: 1.7;
        }

        .landing-outline-button {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          padding: 13px 20px;
          border: 1px solid #b7cdbb;
          border-radius: 24px;
          color: #286047;
          background: transparent;
          cursor: pointer;
          font-size: 11px;
          transition: transform 180ms ease;
        }

        .landing-workflow {
          display: grid;
          grid-template-columns: 0.85fr 1.15fr;
          gap: clamp(50px, 9vw, 145px);
          padding: clamp(100px, 12vw, 165px) clamp(25px, 12vw, 190px);
          background:
            linear-gradient(rgba(231, 241, 223, 0.92), rgba(231, 241, 223, 0.92)),
            url("${handsImage}") center / cover;
        }

        .landing-workflow-copy p {
          max-width: 360px;
          color: #718379;
          font-size: 13px;
          line-height: 1.7;
        }

        .landing-text-link {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          margin-top: 24px;
          color: #2e7352;
          font-size: 11px;
          font-weight: 800;
          text-decoration: none;
        }

        .landing-process-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 1px;
          background: rgba(18, 61, 46, 0.18);
        }

        .landing-process-card {
          min-height: 175px;
          padding: 28px 24px;
          background: rgba(231, 241, 223, 0.94);
        }

        .landing-process-card b {
          color: #7da486;
          font-family: Georgia, serif;
          font-size: 12px;
        }

        .landing-process-card h3 {
          margin: 20px 0 8px;
          color: #1f503c;
          font-family: Georgia, serif;
          font-size: 25px;
          font-weight: 500;
        }

        .landing-process-card p {
          margin: 0;
          color: #7b8c82;
          font-size: 11px;
          line-height: 1.55;
        }

        .landing-features {
          padding: clamp(100px, 12vw, 165px) clamp(25px, 12vw, 190px);
          background: #fbfcf8;
        }

        .landing-feature-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 15px;
          margin-top: 52px;
        }

        .landing-feature-card {
          min-height: 270px;
          padding: 27px 25px;
          border: 1px solid #dfe8dd;
          background: white;
          transition:
            transform 220ms ease,
            box-shadow 220ms ease;
        }

        .landing-feature-card:hover {
          transform: translateY(-7px);
          box-shadow: 0 20px 40px rgba(45, 98, 64, 0.13);
        }

        .landing-feature-icon {
          width: 42px;
          height: 42px;
          display: grid;
          place-items: center;
          margin-bottom: 35px;
          border-radius: 13px;
          color: #367750;
          background: #e3f0dc;
        }

        .landing-feature-number {
          display: block;
          margin-bottom: 10px;
          color: #789382;
          font-size: 9px;
          font-weight: 800;
          letter-spacing: 0.13em;
        }

        .landing-feature-card h3 {
          margin: 0;
          color: #214e3b;
          font-family: Georgia, serif;
          font-size: 24px;
          font-weight: 500;
        }

        .landing-feature-card p {
          color: #829188;
          font-size: 12px;
          line-height: 1.65;
        }

        .landing-cta {
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          padding: clamp(75px, 9vw, 125px) clamp(25px, 12vw, 190px);
          background:
            linear-gradient(rgba(218, 235, 211, 0.94), rgba(218, 235, 211, 0.94)),
            url("${farmerImage}") center / cover;
        }

        .landing-cta h2 {
          margin-bottom: 0;
        }

        .landing-footer {
          display: flex;
          align-items: center;
          gap: 25px;
          padding: 28px clamp(25px, 6vw, 92px);
          color: #bad1c0;
          background: var(--forest-dark);
        }

        .landing-footer strong {
          color: white;
          font-size: 11px;
          letter-spacing: 0.15em;
        }

        .landing-footer span {
          color: #9eb8a8;
          font-size: 10px;
        }

        .landing-footer span:last-child {
          margin-left: auto;
        }

        [data-reveal] {
          opacity: 0;
          transform: translateY(45px);
          transition:
            opacity 800ms cubic-bezier(0.23, 1, 0.32, 1),
            transform 800ms cubic-bezier(0.23, 1, 0.32, 1);
        }

        [data-reveal="left"] {
          transform: translateX(-45px);
        }

        [data-reveal="right"] {
          transform: translateX(45px);
        }

        [data-reveal].landing-visible {
          opacity: 1;
          transform: translate(0);
        }

        .landing-feature-card:nth-child(2) {
          transition-delay: 80ms;
        }

        .landing-feature-card:nth-child(3) {
          transition-delay: 160ms;
        }

        @media (prefers-reduced-motion: reduce) {
          [data-reveal] {
            opacity: 1;
            transform: none;
            transition: none;
          }

          .landing-feature-card {
            transition: none;
          }
        }

        @media (max-width: 900px) {
          .landing-nav-links {
            display: none;
          }

          .landing-menu-button {
            display: block;
          }

          .landing-nav {
            height: 68px;
          }

          .landing-nav-actions {
            gap: 4px;
          }

          .landing-hero {
            grid-template-columns: 1fr;
            margin: 0;
          }

          .landing-hero-copy {
            min-height: 570px;
            padding: 75px 25px 60px;
          }

          .landing-hero-image {
            min-height: 430px;
          }

          .landing-stats {
            grid-template-columns: 1fr 1fr;
            gap: 25px;
            margin: 0;
          }

          .landing-stat {
            border-right: 0;
          }

          .landing-workflow {
            grid-template-columns: 1fr;
            padding-left: 25px;
            padding-right: 25px;
          }

          .landing-features {
            padding-left: 25px;
            padding-right: 25px;
          }

          .landing-feature-grid {
            grid-template-columns: 1fr;
          }

          .landing-cta {
            display: block;
            padding-left: 25px;
            padding-right: 25px;
          }

          .landing-cta .landing-primary-button {
            margin-top: 35px;
          }
        }

        @media (max-width: 560px) {
          .landing-nav {
            padding: 0 16px;
          }

          .landing-primary-button {
            padding: 0 13px;
            font-size: 10px;
          }

          .landing-signin {
            display: none;
          }

          .landing-hero-title {
            font-size: 58px;
          }

          .landing-hero-actions {
            align-items: flex-start;
            flex-direction: column;
          }

          .landing-proof {
            margin-top: 42px;
          }

          .landing-hero-image {
            min-height: 370px;
          }

          .landing-image-label {
            top: 25px;
            right: 20px;
          }

          .landing-image-caption {
            right: 20px;
            bottom: 28px;
            font-size: 22px;
          }

          .landing-stats {
            padding: 28px 18px;
          }

          .landing-stat {
            padding: 0 8px;
          }

          .landing-stat strong {
            font-size: 24px;
          }

          .landing-process-grid {
            grid-template-columns: 1fr;
          }

          .landing-footer {
            display: grid;
            gap: 12px;
          }

          .landing-footer span:last-child {
            margin-left: 0;
          }
        }
      `}</style>

      <nav className="landing-nav">
        <button
          className="landing-logo"
          onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
        >
          <span className="landing-logo-mark">
            <Leaf size={17} />
          </span>
          AGRISHIELD
        </button>

        <div className="landing-nav-links">
          <a href="#solutions">Solutions</a>
          <a href="#impact">Impact</a>
          <a href="#workflow">Workflow</a>
          <a href="#platform">Platform</a>
        </div>

        <div className="landing-nav-actions">
          <button className="landing-signin" onClick={goToApp}>
            Sign in
          </button>

          <button className="landing-primary-button" onClick={goToApp}>
            Get started
            <ArrowUpRight size={14} />
          </button>

          <button
            className="landing-menu-button"
            onClick={() => setMenuOpen((value) => !value)}
            aria-label="Toggle navigation"
          >
            <CloudSun size={21} />
          </button>
        </div>
      </nav>

      {menuOpen && (
        <div
          style={{
            position: "fixed",
            zIndex: 40,
            top: 68,
            right: 16,
            left: 16,
            padding: 18,
            border: "1px solid rgba(18,61,46,.12)",
            background: "#fff",
            boxShadow: "0 18px 35px rgba(18,61,46,.16)",
          }}
        >
          {["solutions", "impact", "workflow", "platform"].map((item) => (
            <a
              key={item}
              href={`#${item}`}
              onClick={() => setMenuOpen(false)}
              style={{
                display: "block",
                padding: "13px 4px",
                color: "#234d3b",
                fontSize: 13,
                textDecoration: "none",
                textTransform: "capitalize",
              }}
            >
              {item}
            </a>
          ))}
        </div>
      )}

      <main>
        <section className="landing-hero">
          <div className="landing-hero-copy">
            <span className="landing-kicker">CROP INSURANCE, REIMAGINED</span>

            <h1 className="landing-hero-title">
              Stronger
                

              farmers.
                

              <em>Brighter</em>
                

              <span className="tomorrows">tomorrows.</span>
            </h1>

            <p className="landing-hero-description">
              Technology-driven crop insurance for a more resilient,
              transparent and prosperous agriculture future.
            </p>

            <div className="landing-hero-actions">
              <button className="landing-enter-button" onClick={goToApp}>
                <span className="landing-enter-icon">
                  <ArrowRight size={14} />
                </span>
                Enter the platform
              </button>

              <span className="landing-protected-pill">
                <strong>10M+</strong>
                <span>
                  farmers
                    

                  protected
                </span>
              </span>
            </div>

            <span className="landing-proof">
              <Check size={14} />
              Secure, role-based operations for every season
            </span>
          </div>

          <div
            className="landing-hero-image"
            role="img"
            aria-label="Sunrise over cultivated agricultural fields"
          >
            <div className="landing-image-label">
              <span>FIELD NOTE · 01</span>
              <strong>
                Every harvest
                  

                deserves a safety net.
              </strong>
            </div>

            <div className="landing-image-caption">
              A better season
                

              starts with security.
            </div>
          </div>
        </section>

        <section className="landing-stats" id="impact" data-reveal>
          <div className="landing-stat">
            <strong>10M+</strong>
            <span>Farmers protected</span>
          </div>

          <div className="landing-stat">
            <strong>28</strong>
            <span>States covered</span>
          </div>

          <div className="landing-stat">
            <strong>95%</strong>
            <span>Claim settlement rate</span>
          </div>

          <div className="landing-stat">
            <strong>24/7</strong>
            <span>Operational visibility</span>
          </div>
        </section>

        <section className="landing-intro" id="solutions" data-reveal="left">
          <div>
            <span className="landing-kicker">
              BUILT FOR THE PEOPLE WHO FEED US
            </span>

            <h2 className="landing-section-title">
              When farmers thrive,
                

              <em>nations grow.</em>
            </h2>

            <p>
              One connected platform for policies, loss assessment, claims,
              compliance and the people at the heart of agriculture.
            </p>

            <button className="landing-outline-button" onClick={goToApp}>
              Explore the platform
              <ArrowUpRight size={15} />
            </button>
          </div>
        </section>

        <section className="landing-workflow" id="workflow">
          <div className="landing-workflow-copy" data-reveal="left">
            <span className="landing-kicker">ONE CONNECTED WORKFLOW</span>

            <h2 className="landing-section-title">
              From first notification
                

              to <em>final settlement.</em>
            </h2>

            <p>
              AgriShield brings policy enrollment, field evidence, loss
              assessment, claims, compliance and DBT into one traceable
              operational journey.
            </p>

            <a href="#platform" className="landing-text-link">
              See how it works
              <ArrowRight size={14} />
            </a>
          </div>

          <div className="landing-process-grid" data-reveal="right">
            <article className="landing-process-card">
              <b>01</b>
              <h3>Protect</h3>
              <p>
                Capture farmer and policy information with secure role-based
                access.
              </p>
            </article>

            <article className="landing-process-card">
              <b>02</b>
              <h3>Assess</h3>
              <p>
                Coordinate geo-tagged loss reports, surveys and field
                assessments.
              </p>
            </article>

            <article className="landing-process-card">
              <b>03</b>
              <h3>Decide</h3>
              <p>
                Move validated claims through review, approval and audit
                milestones.
              </p>
            </article>

            <article className="landing-process-card">
              <b>04</b>
              <h3>Settle</h3>
              <p>
                Record DBT settlement and keep every stakeholder informed.
              </p>
            </article>
          </div>
        </section>

        <section className="landing-features" id="platform" data-reveal>
          <span className="landing-kicker">
            DESIGNED FOR MODERN AGRICULTURE
          </span>

          <h2 className="landing-section-title">
            Clarity at every
              

            <em>operational layer.</em>
          </h2>

          <div className="landing-feature-grid">
            {features.map((feature) => {
              const Icon = feature.icon;

              return (
                <article className="landing-feature-card" key={feature.number}>
                  <div className="landing-feature-icon">
                    <Icon size={19} />
                  </div>

                  <span className="landing-feature-number">
                    {feature.number} · AGRISHIELD
                  </span>

                  <h3>{feature.title}</h3>
                  <p>{feature.description}</p>
                </article>
              );
            })}
          </div>
        </section>

        <section className="landing-cta" data-reveal="right">
          <div>
            <span className="landing-kicker">READY FOR THE NEXT SEASON</span>

            <h2 className="landing-section-title">
              Build trust into
                

              <em>every harvest.</em>
            </h2>
          </div>

          <button className="landing-primary-button" onClick={goToApp}>
            Enter AgriShield
            <ArrowUpRight size={15} />
          </button>
        </section>
      </main>

      <footer className="landing-footer">
        <strong>AGRISHIELD</strong>
        <span>A connected crop insurance operations platform.</span>
        <span>© 2026 AgriShield</span>
      </footer>
    </div>
  );
}
