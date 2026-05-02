// HOME — two giant buttons (Library, Albums). Material You Expressive, pushed bold.
// The display face carries the brand voice.

function HomeScreen({ t, hasContinue = false, source = 'hybrid' }) {
  const sourceMeta = {
    local:  { label: 'Local',  sub: '1,208 photos · /DCIM/IMG_2020' },
    immich: { label: 'Immich', sub: '14,202 assets · immich.home.lan' },
    hybrid: { label: 'Local + Immich', sub: '1,208 local · mirrored to Immich' },
  }[source];
  return (
    <>
      <StatusBar t={t}/>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', padding: '8px 20px 20px' }}>
        {/* header row */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 4px 4px' }}>
          <CullMark size={32} color={t.fg}/>
          <div style={{
            width: 44, height: 44, borderRadius: 999,
            background: t.bgElev, border: `1px solid ${t.line}`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: t.fgDim,
          }}>
            <ISettings size={20}/>
          </div>
        </div>

        {/* hero line */}
        <div style={{
          fontFamily: t.display, fontSize: 54, lineHeight: 0.92,
          letterSpacing: -1.2,
          color: t.fg,
          padding: '28px 4px 12px',
          fontStyle: 'italic',
        }}>
          Keep only<br/>
          <span style={{ color: t.accent }}>the best.</span>
        </div>
        <div style={{
          fontSize: 14, color: t.fgDim, padding: '0 4px 18px',
          maxWidth: 280, lineHeight: 1.4,
        }}>
          Pin a reference, long&#8209;press any photo to compare against it. Swipe down to cull.
        </div>

        {/* source switcher — 3 segs, glanceable */}
        <SourceSwitcher t={t} value={source}/>

        {/* giant buttons — asymmetric, not a plain 2-col grid */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginTop: 'auto' }}>
          <GiantButton t={t} primary label="Library"
            sub={sourceMeta.sub}
            icon={source === 'immich' ? <ICloud size={34}/> : source === 'local' ? <IFolder size={34}/> : <ILibrary size={36}/>}/>
          <GiantButton t={t} label="Albums" sub="Coming soon" icon={<IAlbums size={30}/>} disabled/>
        </div>

        {hasContinue && (
          <ContinuePill t={t}/>
        )}

        <div style={{
          textAlign: 'center', marginTop: 16, fontSize: 11,
          letterSpacing: 2, textTransform: 'uppercase',
          color: t.fgFaint, fontWeight: 500,
        }}>
          v0.1 · local + immich
        </div>
      </div>
      <NavBar t={t}/>
    </>
  );
}

function GiantButton({ t, label, sub, icon, primary, disabled }) {
  const bg = primary ? t.accent : t.bgElev;
  const fg = primary ? (t.dark ? '#0d0d0f' : '#fff') : t.fg;
  const subFg = primary ? 'rgba(13,13,15,0.62)' : t.fgDim;
  return (
    <div style={{
      background: bg, borderRadius: 32,
      padding: '26px 24px 22px',
      border: primary ? 'none' : `1px solid ${t.line}`,
      opacity: disabled ? 0.55 : 1,
      position: 'relative', overflow: 'hidden',
      minHeight: primary ? 160 : 118,
      display: 'flex', flexDirection: 'column',
    }}>
      <div style={{ color: fg, opacity: 0.92 }}>{icon}</div>
      <div style={{
        marginTop: 'auto',
        display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
      }}>
        <div>
          <div style={{
            fontFamily: t.display, fontStyle: 'italic',
            fontSize: primary ? 44 : 36, lineHeight: 1, letterSpacing: -1,
            color: fg,
          }}>{label}</div>
          <div style={{ fontSize: 13, color: subFg, marginTop: 4, fontVariantNumeric: 'tabular-nums' }}>
            {sub}
          </div>
        </div>
        <div style={{
          width: 44, height: 44, borderRadius: 999,
          background: primary ? 'rgba(13,13,15,0.12)' : (t.dark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)'),
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: fg,
        }}>
          <IArrow size={20}/>
        </div>
      </div>
    </div>
  );
}

function ContinuePill({ t }) {
  return (
    <div style={{
      marginTop: 14,
      background: t.bgElev2,
      border: `1px solid ${t.lineStrong}`,
      borderRadius: 24,
      padding: 10,
      display: 'flex', alignItems: 'center', gap: 12,
    }}>
      {/* thumb of last image */}
      <Photo photo={0} style={{ width: 54, height: 54, borderRadius: 16, flexShrink: 0 }}/>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 12, color: t.fgDim, letterSpacing: 0.3, textTransform: 'uppercase', fontWeight: 600 }}>
          Continue where you left off
        </div>
        <div style={{ fontSize: 14, color: t.fg, marginTop: 2, fontVariantNumeric: 'tabular-nums' }}>
          IMG_2020/DSC_04217.jpg · 142 of 1,208
        </div>
      </div>
      <div style={{
        width: 40, height: 40, borderRadius: 999, background: t.accent,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: t.dark ? '#0d0d0f' : '#fff', flexShrink: 0,
      }}>
        <IArrow size={18}/>
      </div>
    </div>
  );
}

function SourceSwitcher({ t, value = 'hybrid' }) {
  const opts = [
    { k: 'local',  label: 'Local',  icon: <IFolder size={14}/> },
    { k: 'hybrid', label: 'Local + Immich', icon: <ILibrary size={14}/> },
    { k: 'immich', label: 'Immich', icon: <ICloud size={14}/> },
  ];
  return (
    <div style={{
      display: 'flex', gap: 4, padding: 4,
      background: t.bgElev, border: `1px solid ${t.line}`,
      borderRadius: 999, marginBottom: 20,
    }}>
      {opts.map(o => {
        const on = o.k === value;
        return (
          <div key={o.k} style={{
            flex: o.k === 'hybrid' ? 1.4 : 1,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
            padding: '9px 8px', borderRadius: 999,
            background: on ? t.accent : 'transparent',
            color: on ? (t.dark ? '#0d0d0f' : '#fff') : t.fgDim,
            fontSize: 12, fontWeight: 700, letterSpacing: 0.2,
            whiteSpace: 'nowrap',
          }}>
            {o.icon}
            {o.label}
          </div>
        );
      })}
    </div>
  );
}

Object.assign(window, { HomeScreen, GiantButton, ContinuePill, SourceSwitcher });
