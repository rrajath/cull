// Design tokens for CULL — Material You Expressive, pushed bold.
// All screens read from THEME (swappable via Tweaks).

const CULL_TOKENS = {
  // radii — Material 3 expressive leans big
  r: { xs: 8, sm: 12, md: 20, lg: 28, xl: 40, pill: 999 },
  // spacing
  s: (n) => n * 4,
  // font stacks
  display: `"Instrument Serif", "Times New Roman", Georgia, serif`,
  ui: `"Geist", "SF Pro Text", system-ui, -apple-system, sans-serif`,
  mono: `"Geist Mono", "SF Mono", ui-monospace, monospace`,
};

// Theme builder — takes accent hue (oklch hue deg) + dark boolean
function makeTheme({ dark = true, accentHue = 40 } = {}) {
  const accent = `oklch(0.74 0.18 ${accentHue})`;
  const accentBright = `oklch(0.82 0.17 ${accentHue})`;
  const accentDim = `oklch(0.32 0.10 ${accentHue})`;
  const accentSoft = `oklch(0.28 0.06 ${accentHue})`;
  if (dark) {
    return {
      ...CULL_TOKENS,
      dark: true,
      bg: '#0d0d0f',
      bgElev: '#17171a',
      bgElev2: '#1f1f23',
      surface: '#23232a',
      line: 'rgba(255,255,255,0.08)',
      lineStrong: 'rgba(255,255,255,0.16)',
      fg: '#f3f2ef',
      fgDim: 'rgba(243,242,239,0.66)',
      fgFaint: 'rgba(243,242,239,0.38)',
      accent, accentBright, accentDim, accentSoft,
      // semantic
      danger: 'oklch(0.66 0.22 25)',
      dangerSoft: 'oklch(0.32 0.12 25)',
      pin: `oklch(0.82 0.18 ${accentHue})`,
      scrim: 'rgba(0,0,0,0.55)',
    };
  }
  return {
    ...CULL_TOKENS,
    dark: false,
    bg: '#f6f4ef',
    bgElev: '#ffffff',
    bgElev2: '#eeeae2',
    surface: '#ffffff',
    line: 'rgba(20,18,14,0.08)',
    lineStrong: 'rgba(20,18,14,0.18)',
    fg: '#14120e',
    fgDim: 'rgba(20,18,14,0.64)',
    fgFaint: 'rgba(20,18,14,0.4)',
    accent: `oklch(0.62 0.19 ${accentHue})`,
    accentBright: `oklch(0.72 0.18 ${accentHue})`,
    accentDim: `oklch(0.82 0.10 ${accentHue})`,
    accentSoft: `oklch(0.92 0.05 ${accentHue})`,
    danger: 'oklch(0.56 0.22 25)',
    dangerSoft: 'oklch(0.92 0.08 25)',
    pin: `oklch(0.62 0.19 ${accentHue})`,
    scrim: 'rgba(0,0,0,0.45)',
  };
}

// Brand mark — ">>" slab. Rendered as text in the display face, not SVG art.
function CullMark({ size = 28, color, style }) {
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center', gap: size * 0.15,
      fontFamily: CULL_TOKENS.display,
      fontSize: size,
      fontStyle: 'italic',
      lineHeight: 1,
      letterSpacing: -size * 0.02,
      color,
      ...style,
    }}>
      <span style={{ fontWeight: 400 }}>C</span>
      <span style={{ fontWeight: 400, marginLeft: -size * 0.04 }}>ull</span>
      <span style={{
        fontFamily: CULL_TOKENS.ui, fontStyle: 'normal', fontWeight: 600,
        fontSize: size * 0.5, marginLeft: size * 0.2, opacity: 0.85,
        transform: 'translateY(-2px)', display: 'inline-block',
      }}>»</span>
    </div>
  );
}

Object.assign(window, { CULL_TOKENS, makeTheme, CullMark });
