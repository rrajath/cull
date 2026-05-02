// LIBRARY — thumbnail grid. Material 3 expressive varying tile shapes.

function LibraryScreen({ t, marks = {} /* id -> 'deleted' | 'pinned' */, source = 'hybrid' }) {
  const meta = {
    local:  { icon: <IFolder size={13}/>, label: 'LOCAL',  sub: '1,208 photos · /DCIM/IMG_2020 · 3 marked' },
    immich: { icon: <ICloud size={13}/>,  label: 'IMMICH', sub: '14,202 assets · immich.home.lan · 3 marked' },
    hybrid: { icon: <ILibrary size={13}/>,label: 'HYBRID', sub: '1,208 local · mirrored · 3 marked' },
  }[source];
  return (
    <>
      <StatusBar t={t}/>
      {/* Top app bar */}
      <div style={{ padding: '8px 16px 14px', display: 'flex', alignItems: 'center', gap: 8 }}>
        <CircleIcon t={t}><IChevR size={22} style={{ transform: 'rotate(180deg)' }}/></CircleIcon>
        <div style={{
          flex: 1, fontFamily: t.display, fontStyle: 'italic',
          fontSize: 28, lineHeight: 1, letterSpacing: -0.6,
          paddingLeft: 4,
        }}>
          <div style={{ display:'flex', alignItems:'baseline', gap: 8 }}>
            <span>Library</span>
            <span style={{
              fontFamily: t.ui, fontStyle: 'normal',
              fontSize: 10, fontWeight: 800, letterSpacing: 1.4,
              color: t.accent, display:'inline-flex', alignItems:'center', gap: 4,
              background: t.accentSoft, padding: '3px 7px', borderRadius: 6,
              transform: 'translateY(-3px)',
            }}>{meta.icon}{meta.label}</span>
          </div>
          <div style={{
            fontFamily: t.ui, fontStyle: 'normal',
            fontSize: 12, color: t.fgDim, fontVariantNumeric: 'tabular-nums',
            marginTop: 4, fontWeight: 500, letterSpacing: 0.2,
          }}>
            {meta.sub}
          </div>
        </div>
        <CircleIcon t={t}><ISettings size={20}/></CircleIcon>
      </div>

      {/* grid — 3 cols, varied radii per Material 3 expressive */}
      <div style={{
        flex: 1, overflow: 'hidden',
        padding: '0 12px 12px',
      }}>
        <div style={{
          display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 6,
        }}>
          {Array.from({ length: 27 }).map((_, i) => {
            const ph = PHOTOS[i % PHOTOS.length];
            const mark = marks[i];
            // vary radius for expressive feel
            const r = [14, 14, 14][i % 3];
            return (
              <div key={i} style={{ position: 'relative' }}>
                <Photo photo={ph} grayscale={mark === 'deleted'} style={{
                  width: '100%', aspectRatio: '3/4',
                  borderRadius: r,
                }}>
                  {mark === 'pinned' && (
                    <div style={{
                      position: 'absolute', top: 6, left: 6,
                      width: 26, height: 26, borderRadius: 999,
                      background: t.pin, color: '#0d0d0f',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.35)',
                    }}>
                      <IPinFilled size={14}/>
                    </div>
                  )}
                  {mark === 'deleted' && (
                    <div style={{
                      position: 'absolute', top: 6, right: 6,
                      width: 22, height: 22, borderRadius: 999,
                      background: t.danger, color: '#fff',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                      <ITrash size={12}/>
                    </div>
                  )}
                </Photo>
              </div>
            );
          })}
        </div>
      </div>

      {/* bottom: marked-for-deletion pill (mini HUD) */}
      <div style={{
        position: 'absolute', bottom: 40, left: 0, right: 0,
        display: 'flex', justifyContent: 'center', pointerEvents: 'none',
      }}>
        <div style={{
          background: t.fg, color: t.bg, borderRadius: 999,
          padding: '10px 8px 10px 18px',
          display: 'flex', alignItems: 'center', gap: 10,
          boxShadow: '0 10px 30px rgba(0,0,0,0.35)',
          fontSize: 14, fontWeight: 600,
        }}>
          <ITrash size={16}/>
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>3 marked</span>
          <div style={{
            background: t.accent, color: t.dark ? '#0d0d0f' : '#fff',
            borderRadius: 999, padding: '6px 14px', fontSize: 13,
          }}>Review</div>
        </div>
      </div>
      <NavBar t={t}/>
    </>
  );
}

function CircleIcon({ t, children, bg }) {
  return (
    <div style={{
      width: 44, height: 44, borderRadius: 999,
      background: bg || t.bgElev, border: `1px solid ${t.line}`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      color: t.fg, flexShrink: 0,
    }}>{children}</div>
  );
}

Object.assign(window, { LibraryScreen, CircleIcon });
