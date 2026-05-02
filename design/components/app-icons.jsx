// Four launcher-icon variations for CULL. Each renders as an Android
// adaptive-icon squircle (108dp foreground on a square bg, shown at ~88dp
// visible). Same square canvas, same safe area. The wordmark stays the
// "Cull»" italic one from the brand.

// Android adaptive icon mask — squircle-ish superellipse.
const SQUIRCLE = '42% 42% 42% 42% / 42% 42% 42% 42%';

// Wrapper: a single adaptive-icon tile (square canvas, squircle mask)
function IconTile({ size = 156, bg, children, label, caption }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 10 }}>
      <div style={{
        width: size, height: size, borderRadius: SQUIRCLE,
        background: bg, position: 'relative', overflow: 'hidden',
        boxShadow: '0 18px 40px rgba(0,0,0,0.28), 0 2px 4px rgba(0,0,0,0.15), inset 0 1px 0 rgba(255,255,255,0.08)',
      }}>
        {children}
      </div>
      <div style={{ maxWidth: size + 24 }}>
        <div style={{
          fontFamily: '"Instrument Serif", serif', fontStyle: 'italic',
          fontSize: 22, lineHeight: 1, letterSpacing: -0.4, color: '#14120e',
        }}>{label}</div>
        {caption && (
          <div style={{ fontSize: 11.5, color: 'rgba(60,50,40,0.68)', marginTop: 4, lineHeight: 1.35, maxWidth: 180 }}>
            {caption}
          </div>
        )}
      </div>
    </div>
  );
}

// ─── v1 · Cull» wordmark (the current one) ─────────────────────────────
function IconV1_Wordmark({ size = 156 }) {
  return (
    <div style={{
      position: 'absolute', inset: 0,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: '"Instrument Serif", serif', fontStyle: 'italic',
      color: '#f6f1e4', letterSpacing: -size * 0.02,
      fontSize: size * 0.48, lineHeight: 1, paddingLeft: size * 0.02,
    }}>
      Cull
      <span style={{
        fontFamily: 'Geist, system-ui', fontStyle: 'normal', fontWeight: 700,
        fontSize: size * 0.26, marginLeft: size * 0.04,
        color: 'oklch(0.78 0.18 40)', transform: 'translateY(-18%)',
        display: 'inline-block',
      }}>»</span>
    </div>
  );
}

// ─── v2 · The Pin ─────────────────────────────────────────────────────
// Concept: the single most distinctive product interaction rendered as
// a glyph. Tangerine pin head on deep ink, single accent dot = "this one".
function IconV2_Pin({ size = 156 }) {
  const s = size;
  return (
    <>
      {/* soft radial glow behind the pin */}
      <div style={{
        position: 'absolute', inset: 0,
        background: `radial-gradient(circle at 50% 42%, oklch(0.5 0.18 40 / 0.35) 0%, transparent 55%)`,
      }}/>
      <svg width={s} height={s} viewBox="0 0 100 100" style={{ position: 'absolute', inset: 0 }}>
        {/* pin stem (down) */}
        <rect x="48" y="58" width="4" height="26" rx="2" fill="#f6f1e4"/>
        {/* pin head */}
        <circle cx="50" cy="42" r="22" fill="oklch(0.74 0.19 40)"/>
        <circle cx="50" cy="42" r="22" fill="url(#shineV2)" opacity="0.6"/>
        {/* inner shadow */}
        <circle cx="50" cy="42" r="22" fill="none" stroke="rgba(0,0,0,0.2)" strokeWidth="1"/>
        {/* accent dot — "the pinned one" */}
        <circle cx="50" cy="42" r="5" fill="#f6f1e4"/>
        <defs>
          <radialGradient id="shineV2" cx="35%" cy="30%" r="60%">
            <stop offset="0%" stopColor="#fff" stopOpacity="0.5"/>
            <stop offset="100%" stopColor="#fff" stopOpacity="0"/>
          </radialGradient>
        </defs>
      </svg>
    </>
  );
}

// ─── v3 · Two frames ──────────────────────────────────────────────────
// Concept: the compare gesture. Two overlapping photo frames — one tinted
// accent (pinned reference), one neutral (current). Shows the app's dual
// nature in a single beat.
function IconV3_Frames({ size = 156 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" style={{ position: 'absolute', inset: 0 }}>
      {/* back frame — neutral, current photo */}
      <g transform="rotate(-8 58 56)">
        <rect x="36" y="28" width="44" height="56" rx="6" fill="#2a2520" stroke="#f6f1e4" strokeWidth="2"/>
        {/* horizon line suggestion */}
        <rect x="40" y="60" width="36" height="20" rx="2" fill="#4a3f32"/>
        <circle cx="50" cy="46" r="5" fill="#f6f1e4" opacity="0.6"/>
      </g>
      {/* front frame — accent-tinted, the pinned reference */}
      <g transform="rotate(7 42 46)">
        <rect x="20" y="18" width="44" height="56" rx="6" fill="oklch(0.74 0.19 40)" stroke="#f6f1e4" strokeWidth="2"/>
        {/* pin notch on top-left */}
        <circle cx="26" cy="24" r="3.5" fill="#f6f1e4"/>
        {/* subject blob */}
        <circle cx="42" cy="40" r="9" fill="#f6f1e4" opacity="0.38"/>
        <rect x="24" y="52" width="36" height="18" rx="2" fill="#f6f1e4" opacity="0.28"/>
      </g>
    </svg>
  );
}

// ─── v4 · Monogram C» ─────────────────────────────────────────────────
// Concept: maximally reductive — a single glyph-lockup at icon scale.
// Serif 'C' in italic, with the guillemets carrying all the forward-motion.
// Readable at 24dp favicon scale.
function IconV4_Mono({ size = 156 }) {
  const s = size;
  return (
    <div style={{
      position: 'absolute', inset: 0,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      paddingLeft: s * 0.04,
    }}>
      <div style={{
        fontFamily: '"Instrument Serif", serif', fontStyle: 'italic',
        fontSize: s * 0.86, lineHeight: 1,
        color: '#f6f1e4', letterSpacing: -s * 0.04,
        display: 'flex', alignItems: 'baseline', gap: s * 0.02,
      }}>
        C
        <span style={{
          fontFamily: 'Geist, system-ui', fontStyle: 'normal', fontWeight: 800,
          fontSize: s * 0.44, color: 'oklch(0.78 0.18 40)',
          transform: 'translateY(-15%)', display: 'inline-block',
        }}>»</span>
      </div>
    </div>
  );
}

// ─── v5 · Stacked thumbnails ──────────────────────────────────────────
// Concept: reveal the act of culling itself — a stack of photos with the
// top one being "kept" (accent ring) and a swipe-trail suggesting motion.
function IconV5_Stack({ size = 156 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" style={{ position: 'absolute', inset: 0 }}>
      {/* motion trail — 3 faint cards fanning down-right (discarded) */}
      <rect x="54" y="64" width="30" height="24" rx="4" fill="#f6f1e4" opacity="0.1"/>
      <rect x="60" y="70" width="30" height="24" rx="4" fill="#f6f1e4" opacity="0.05"/>

      {/* bottom kept card */}
      <rect x="18" y="32" width="54" height="46" rx="7" fill="#2a2520" stroke="#f6f1e4" strokeWidth="1.5" opacity="0.7"/>
      {/* middle kept card */}
      <rect x="22" y="24" width="54" height="46" rx="7" fill="#3a2f24" stroke="#f6f1e4" strokeWidth="1.5" opacity="0.85"/>
      {/* top card — the current pick, accent ring */}
      <rect x="26" y="16" width="54" height="46" rx="7" fill="oklch(0.74 0.19 40)"/>
      <rect x="26" y="16" width="54" height="46" rx="7" fill="none" stroke="#f6f1e4" strokeWidth="2.5"/>
      {/* landscape glyph inside */}
      <circle cx="40" cy="30" r="3.5" fill="#f6f1e4" opacity="0.7"/>
      <path d="M 30 52 L 42 38 L 52 46 L 62 34 L 76 50 L 76 58 L 30 58 Z" fill="#f6f1e4" opacity="0.42"/>
    </svg>
  );
}

const SQUIRCLE_MAIN = () => SQUIRCLE;

Object.assign(window, {
  IconTile, IconV1_Wordmark, IconV2_Pin, IconV3_Frames, IconV4_Mono, IconV5_Stack,
  SQUIRCLE, SQUIRCLE_MAIN,
});
