// Android chrome: status bar (photo-app style) and gesture nav.
// Our own, because the default starter is too Material-Google.

function StatusBar({ t, tint }) {
  const c = tint || t.fg;
  return (
    <div style={{
      height: 40, display: 'flex', alignItems: 'center',
      justifyContent: 'space-between', padding: '0 22px 0 22px',
      position: 'relative', flexShrink: 0,
      fontFamily: t.ui, color: c,
    }}>
      <div style={{ fontSize: 15, fontWeight: 600, letterSpacing: 0.2, fontVariantNumeric: 'tabular-nums' }}>
        9:41
      </div>
      <div style={{
        position: 'absolute', left: '50%', top: 10, transform: 'translateX(-50%)',
        width: 22, height: 22, borderRadius: 999, background: '#000',
        boxShadow: '0 0 0 1px rgba(255,255,255,0.08)',
      }}/>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <svg width="15" height="15" viewBox="0 0 16 16"><path d="M8 13.3L.67 5.97a10.37 10.37 0 0114.66 0L8 13.3z" fill={c}/></svg>
        <svg width="13" height="13" viewBox="0 0 16 16"><path d="M14.67 14.67V1.33L1.33 14.67h13.34z" fill={c}/></svg>
        <div style={{ display:'flex', alignItems:'center', gap: 3 }}>
          <span style={{ fontSize: 11, fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>78</span>
          <svg width="20" height="12" viewBox="0 0 20 12">
            <rect x="0.5" y="1" width="16" height="10" rx="2" fill="none" stroke={c} strokeWidth="1"/>
            <rect x="17" y="4" width="2" height="4" rx="0.5" fill={c}/>
            <rect x="2" y="2.5" width="12" height="7" rx="1" fill={c}/>
          </svg>
        </div>
      </div>
    </div>
  );
}

function NavBar({ t, tint }) {
  return (
    <div style={{ height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
      <div style={{
        width: 130, height: 4, borderRadius: 2,
        background: tint || t.fg, opacity: 0.5,
      }}/>
    </div>
  );
}

// Device frame — our own, sleeker, photo-app appropriate
function Phone({ t, children, chromeTint, width = 380, height = 820, bezel }) {
  return (
    <div style={{
      width, height,
      borderRadius: 48,
      padding: 6,
      background: bezel || (t.dark ? '#050506' : '#1a1a1d'),
      boxShadow: '0 40px 100px rgba(0,0,0,0.35), 0 2px 0 rgba(255,255,255,0.04) inset',
      boxSizing: 'content-box',
      flexShrink: 0,
      position: 'relative',
    }}>
      <div style={{
        width: '100%', height: '100%',
        borderRadius: 42, overflow: 'hidden',
        background: t.bg, color: t.fg,
        fontFamily: t.ui,
        display: 'flex', flexDirection: 'column',
        position: 'relative',
      }}>
        {children}
      </div>
    </div>
  );
}

Object.assign(window, { StatusBar, NavBar, Phone });
