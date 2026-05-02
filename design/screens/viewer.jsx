// VIEWER — full-screen photo + HUD + variants of state (normal, long-press,
// marked-for-deletion, zoomed). Built to be used both in canvas mockups and
// in the live prototype.

// The HUD has multiple layout variants — selectable via t.hudStyle:
//   'pill'   — bottom floating pill (default)
//   'rail'   — side rail on the right (thumb-reachable)
//   'bar'    — full-width bottom bar (classic)

function ViewerScreen({
  t,
  photo,             // active photo obj
  pinnedPhoto,       // pinned photo obj, or null
  idx = 142, total = 1208,
  hudOpen = true,
  state = 'idle',    // 'idle' | 'long-press' | 'marked' | 'zoomed' | 'loading'
  pinned = false,    // is THIS photo pinned
  cloudMissing = false,
  source = 'hybrid', // 'local' | 'immich' | 'hybrid'
  showToast = null,  // string
}) {
  const displayed = state === 'long-press' && pinnedPhoto ? pinnedPhoto : photo;
  const grayscale = state === 'marked';

  return (
    <>
      <StatusBar t={t} tint="#fff"/>
      <div style={{ flex: 1, position: 'relative', background: '#000', overflow: 'hidden' }}>
        {/* scrim on top+bottom for legibility */}
        <div style={{
          position: 'absolute', inset: 0, zIndex: 0,
          background: `linear-gradient(180deg, rgba(0,0,0,0.5) 0%, transparent 10%, transparent 80%, rgba(0,0,0,0.55) 100%)`,
          pointerEvents: 'none',
        }}/>

        <Photo photo={displayed} grayscale={grayscale} style={{
          width: '100%', height: '100%', position: 'absolute', inset: 0, zIndex: -1,
          transform: state === 'marked' ? 'translateY(18px)' : (state === 'zoomed' ? 'scale(1.8) translate(4%, -6%)' : 'none'),
          transition: 'transform 200ms ease-out',
        }}/>

        {/* loading indicator */}
        {state === 'loading' && <LoadingRing t={t}/>}

        {/* pin indicator, top-left */}
        {pinned && <PinBadge t={t}/>}

        {/* marked-for-deletion banner, top-right */}
        {state === 'marked' && <MarkedBanner t={t}/>}

        {/* reset-both-zooms button, top-right (only when zoomed) */}
        {state === 'zoomed' && <ResetZoomButton t={t}/>}

        {/* index counter, top-center */}
        {!(state === 'marked' || state === 'zoomed') && (
          <div style={{
            position: 'absolute', top: 18, left: '50%', transform: 'translateX(-50%)',
            background: 'rgba(0,0,0,0.55)', color: '#fff',
            padding: '6px 12px', borderRadius: 999,
            fontSize: 12, fontWeight: 600, fontVariantNumeric: 'tabular-nums',
            backdropFilter: 'blur(10px)',
            letterSpacing: 0.3,
          }}>
            {String(idx).padStart(4, '0')}  ·  {total.toLocaleString()}
          </div>
        )}

        {/* long-press "revealing pinned" overlay label */}
        {state === 'long-press' && pinnedPhoto && (
          <div style={{
            position: 'absolute', top: 60, left: '50%', transform: 'translateX(-50%)',
            display: 'flex', alignItems: 'center', gap: 8,
            background: t.accent, color: t.dark ? '#0d0d0f' : '#fff',
            padding: '8px 14px', borderRadius: 999,
            fontSize: 13, fontWeight: 600, letterSpacing: 0.2,
          }}>
            <IPinFilled size={14}/>
            Showing pinned · release to return
          </div>
        )}

        {/* HUD */}
        {hudOpen && state !== 'long-press' && (
          <HUD t={t} pinned={pinned} cloudMissing={cloudMissing} markedCount={3} style={t.hudStyle} source={source}/>
        )}

        {/* HUD trigger pill — only when HUD is closed and not long-press */}
        {!hudOpen && state !== 'long-press' && (
          <HUDTrigger t={t} markedCount={3}/>
        )}

        {/* swipe-down affordance (hint chevron, subtle) */}
        {state === 'idle' && hudOpen && (
          <SwipeHint t={t}/>
        )}

        {/* toast */}
        {showToast && <Toast t={t}>{showToast}</Toast>}
      </div>
      <NavBar t={t} tint="#fff"/>
    </>
  );
}

function LoadingRing({ t }) {
  return (
    <div style={{
      position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
    }}>
      <div style={{
        width: 46, height: 46, borderRadius: 999,
        border: `3px solid rgba(255,255,255,0.2)`,
        borderTopColor: t.accent,
        animation: 'cull-spin 0.9s linear infinite',
      }}/>
    </div>
  );
}

function PinBadge({ t }) {
  return (
    <div style={{
      position: 'absolute', top: 18, left: 16,
      display: 'flex', alignItems: 'center', gap: 8,
    }}>
      <div style={{
        width: 40, height: 40, borderRadius: 999,
        background: t.pin, color: '#0d0d0f',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        boxShadow: '0 4px 14px rgba(0,0,0,0.5)',
      }}>
        <IPinFilled size={18}/>
      </div>
      <div style={{
        fontSize: 11, fontWeight: 700, letterSpacing: 1.4, textTransform: 'uppercase',
        color: '#fff', textShadow: '0 1px 4px rgba(0,0,0,0.6)',
      }}>Pinned</div>
    </div>
  );
}

function MarkedBanner({ t }) {
  return (
    <div style={{
      position: 'absolute', top: 16, right: 12, left: 12,
      display: 'flex', justifyContent: 'flex-end',
    }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        background: t.danger, color: '#fff',
        padding: '8px 8px 8px 16px', borderRadius: 999,
        boxShadow: '0 6px 20px rgba(0,0,0,0.45)',
        fontSize: 13, fontWeight: 600,
      }}>
        <ITrash size={16}/>
        <span>Marked for deletion</span>
        <div style={{
          background: 'rgba(255,255,255,0.22)', color: '#fff',
          padding: '5px 10px', borderRadius: 999,
          fontSize: 12, fontWeight: 600,
          display: 'flex', alignItems: 'center', gap: 4,
        }}>
          <IUndo size={13}/> Undo
        </div>
      </div>
    </div>
  );
}

function ResetZoomButton({ t }) {
  return (
    <div style={{
      position: 'absolute', top: 56, right: 14,
      background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(14px)',
      border: '1px solid rgba(255,255,255,0.14)',
      color: '#fff', padding: '8px 14px 8px 10px',
      borderRadius: 999, display: 'flex', alignItems: 'center', gap: 6,
      fontSize: 12, fontWeight: 600, letterSpacing: 0.2,
    }}>
      <IZoomReset size={15}/>
      Reset both zooms
    </div>
  );
}

function SwipeHint({ t }) {
  return (
    <div style={{
      position: 'absolute', top: 66, left: '50%', transform: 'translateX(-50%)',
      color: 'rgba(255,255,255,0.38)', fontSize: 11,
      letterSpacing: 1, textTransform: 'uppercase', fontWeight: 600,
      display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
      pointerEvents: 'none',
    }}>
      {/* <IArrowDown size={14}/>
      <span>swipe down to cull</span> */}
    </div>
  );
}

function Toast({ t, children }) {
  return (
    <div style={{
      position: 'absolute', bottom: 160, left: '50%', transform: 'translateX(-50%)',
      background: 'rgba(20,20,24,0.92)', color: '#fff',
      padding: '12px 18px', borderRadius: 14,
      fontSize: 13, fontWeight: 500,
      boxShadow: '0 10px 30px rgba(0,0,0,0.5)',
      border: '1px solid rgba(255,255,255,0.08)',
      maxWidth: 300, textAlign: 'center',
      whiteSpace: 'nowrap',
    }}>{children}</div>
  );
}

// ─── HUD variants ──────────────────────────────────────────────────────

// Cloud STATUS — not a button. Small tucked chip above the HUD, thin stroke,
// no fill, no tap affordance. Communicates state only.
function CloudStatus({ t, missing, source = 'hybrid' }) {
  // in immich-only mode every image is on Immich by definition
  if (source === 'local') return null; // no Immich wired → no chip
  const label = source === 'immich'
    ? 'Immich'
    : (missing ? 'Local only' : 'On Immich');
  const color = missing ? 'rgba(255,255,255,0.55)' : 'rgba(255,255,255,0.82)';
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      padding: '4px 10px 4px 8px',
      borderRadius: 999,
      background: 'rgba(0,0,0,0.42)',
      backdropFilter: 'blur(10px)',
      border: `1px solid rgba(255,255,255,0.10)`,
      color,
      fontSize: 10.5, fontWeight: 600, letterSpacing: 0.4,
      textTransform: 'uppercase',
      pointerEvents: 'none',
    }}>
      {missing ? <ICloudOff size={12}/> : <ICloud size={12}/>}
      <span style={{ fontVariantNumeric: 'tabular-nums' }}>{label}</span>
    </div>
  );
}

function HUD({ t, pinned, cloudMissing, markedCount, style = 'pill', source = 'hybrid' }) {
  if (style === 'rail') return <HUDRail {...{ t, pinned, cloudMissing, markedCount, source }}/>;
  if (style === 'bar')  return <HUDBar {...{ t, pinned, cloudMissing, markedCount, source }}/>;
  return <HUDPill {...{ t, pinned, cloudMissing, markedCount, source }}/>;
}

function HUDPill({ t, pinned, cloudMissing, markedCount, source }) {
  return (
    <div style={{
      position: 'absolute', bottom: 18, left: 14, right: 14,
      display: 'flex', flexDirection: 'column', gap: 8,
      alignItems: 'stretch',
    }}>
      {/* status row — left: cloud chip (indicator), right: nothing */}
      <div style={{ display: 'flex', justifyContent: 'flex-start', paddingLeft: 4 }}>
        <CloudStatus t={t} missing={cloudMissing} source={source}/>
      </div>
      {/* delete confirm strip */}
      <div style={{
        background: 'rgba(16,16,18,0.86)', backdropFilter: 'blur(18px)',
        border: '1px solid rgba(255,255,255,0.08)',
        borderRadius: 999, padding: 6,
        display: 'flex', alignItems: 'center', gap: 8,
        width: '100%', justifyContent: 'space-between',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, paddingLeft: 14 }}>
          <div style={{
            width: 28, height: 28, borderRadius: 999,
            background: t.danger, color: '#fff',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><ITrash size={15}/></div>
          <div style={{ color: '#fff', fontSize: 13, fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
            {markedCount} marked
          </div>
        </div>

        {/* action cluster */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <HUDBtn t={t} active={pinned}><IPinFilled size={18}/></HUDBtn>
          <div style={{
            background: t.accent, color: t.dark ? '#0d0d0f' : '#fff',
            borderRadius: 999, padding: '8px 14px',
            fontSize: 13, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 6,
          }}>
            <ICheck size={15}/> Confirm
          </div>
        </div>
      </div>
    </div>
  );
}

function HUDBtn({ t, children, active }) {
  return (
    <div style={{
      width: 40, height: 40, borderRadius: 999,
      background: active ? t.pin : 'rgba(255,255,255,0.08)',
      color: active ? '#0d0d0f' : '#fff',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      border: active ? 'none' : '1px solid rgba(255,255,255,0.08)',
    }}>{children}</div>
  );
}

function HUDRail({ t, pinned, cloudMissing, markedCount, source }) {
  return (
    <>
    <div style={{ position: 'absolute', right: 14, top: 60 }}>
      <CloudStatus t={t} missing={cloudMissing} source={source}/>
    </div>
    <div style={{
      position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)',
      background: 'rgba(16,16,18,0.86)', backdropFilter: 'blur(18px)',
      border: '1px solid rgba(255,255,255,0.08)',
      borderRadius: 28, padding: 8,
      display: 'flex', flexDirection: 'column', gap: 6,
      alignItems: 'center',
    }}>
      <HUDBtn t={t} active={pinned}><IPinFilled size={18}/></HUDBtn>
      <div style={{ width: 26, height: 1, background: 'rgba(255,255,255,0.1)', margin: '2px 0' }}/>
      <div style={{
        width: 40, height: 40, borderRadius: 999, background: t.danger,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: '#fff', position: 'relative',
      }}>
        <ITrash size={17}/>
        <div style={{
          position: 'absolute', top: -4, right: -4,
          minWidth: 18, height: 18, borderRadius: 999, padding: '0 4px',
          background: t.accent, color: t.dark ? '#0d0d0f' : '#fff',
          fontSize: 10, fontWeight: 700,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontVariantNumeric: 'tabular-nums',
        }}>{markedCount}</div>
      </div>
    </div>
    </>
  );
}

function HUDBar({ t, pinned, cloudMissing, markedCount, source }) {
  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0,
      background: 'linear-gradient(180deg, transparent 0%, rgba(0,0,0,0.7) 60%)',
      padding: '40px 16px 20px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <HUDBtn t={t} active={pinned}><IPinFilled size={18}/></HUDBtn>
        <CloudStatus t={t} missing={cloudMissing} source={source}/>
      </div>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        background: t.danger, color: '#fff',
        borderRadius: 999, padding: '8px 14px 8px 10px',
        fontSize: 13, fontWeight: 700,
      }}>
        <ITrash size={16}/>
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{markedCount}</span>
        <div style={{ width: 1, height: 14, background: 'rgba(255,255,255,0.3)' }}/>
        <span>Confirm</span>
      </div>
    </div>
  );
}

function HUDTrigger({ t, markedCount }) {
  return (
    <div style={{
      position: 'absolute', bottom: 18, left: '50%', transform: 'translateX(-50%)',
    }}>
      <div style={{
        background: 'rgba(16,16,18,0.82)', backdropFilter: 'blur(14px)',
        border: '1px solid rgba(255,255,255,0.1)',
        borderRadius: 999, padding: '8px 16px 8px 8px',
        display: 'flex', alignItems: 'center', gap: 10,
        color: '#fff', fontSize: 13, fontWeight: 600,
      }}>
        <div style={{
          width: 26, height: 26, borderRadius: 999,
          background: t.accent, color: t.dark ? '#0d0d0f' : '#fff',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 11, fontWeight: 800, fontVariantNumeric: 'tabular-nums',
        }}>{markedCount}</div>
        Tap for HUD
      </div>
    </div>
  );
}

Object.assign(window, { ViewerScreen, HUD, HUDPill, HUDRail, HUDBar, HUDBtn, HUDTrigger,
  LoadingRing, PinBadge, MarkedBanner, ResetZoomButton, Toast });
