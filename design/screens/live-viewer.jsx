// LIVE PROTOTYPE — interactive viewer that supports tap, long-press, swipe-down.
// Mounted inside a Phone frame. Uses the same ViewerScreen visuals but with real state.

function LiveViewer({ t, longPressMs = 220 }) {
  const [idx, setIdx] = React.useState(2);          // current photo index
  const [pinnedIdx, setPinnedIdx] = React.useState(0); // pinned photo index, or null
  const [marked, setMarked] = React.useState(new Set([5, 11, 14]));
  const [hudOpen, setHudOpen] = React.useState(false);
  const [longPress, setLongPress] = React.useState(false);
  const [zoomed, setZoomed] = React.useState(false);
  const [toast, setToast] = React.useState(null);

  const photo = PHOTOS[idx];
  const pinnedPhoto = pinnedIdx != null ? PHOTOS[pinnedIdx] : null;
  const isPinned = pinnedIdx === idx;
  const isMarked = marked.has(idx);
  const cloudMissing = idx % 3 === 0;

  const toastIt = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 1800);
  };

  // long-press logic
  const lpTimerRef = React.useRef(null);
  const startRef = React.useRef(null);
  const movedRef = React.useRef(false);

  const onPointerDown = (e) => {
    startRef.current = { x: e.clientX, y: e.clientY, t: Date.now() };
    movedRef.current = false;
    lpTimerRef.current = setTimeout(() => {
      if (isPinned) {
        toastIt('Cannot compare a pinned image against itself');
        return;
      }
      if (pinnedPhoto) {
        setLongPress(true);
        setHudOpen(false);
      }
    }, longPressMs);
  };
  const onPointerMove = (e) => {
    if (!startRef.current) return;
    const dx = e.clientX - startRef.current.x;
    const dy = e.clientY - startRef.current.y;
    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) movedRef.current = true;
  };
  const onPointerUp = (e) => {
    clearTimeout(lpTimerRef.current);
    if (!startRef.current) return;
    const dx = e.clientX - startRef.current.x;
    const dy = e.clientY - startRef.current.y;
    const dt = Date.now() - startRef.current.t;
    startRef.current = null;

    if (longPress) {
      setLongPress(false);
      return;
    }

    // swipe down → mark
    if (dy > 60 && Math.abs(dy) > Math.abs(dx)) {
      const next = new Set(marked);
      if (next.has(idx)) next.delete(idx); else next.add(idx);
      setMarked(next);
      return;
    }
    // horizontal swipe → change photo
    if (Math.abs(dx) > 40 && Math.abs(dx) > Math.abs(dy)) {
      setIdx(i => Math.max(0, Math.min(PHOTOS.length - 1, dx < 0 ? i + 1 : i - 1)));
      return;
    }
    // tap (not moved) → toggle HUD
    if (!movedRef.current && dt < 250) {
      setHudOpen(h => !h);
    }
  };
  const onPointerCancel = () => { clearTimeout(lpTimerRef.current); startRef.current = null; };

  const displayed = longPress && pinnedPhoto ? pinnedPhoto : photo;

  return (
    <div
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerCancel}
      style={{ flex: 1, position: 'relative', background: '#000', overflow: 'hidden', touchAction: 'none', userSelect: 'none' }}
    >
      <Photo photo={displayed} grayscale={isMarked && !longPress} style={{
        width: '100%', height: '100%', position: 'absolute', inset: 0,
        transform: (isMarked && !longPress) ? 'translateY(18px)' : (zoomed ? 'scale(1.8)' : 'none'),
        transition: 'transform 220ms cubic-bezier(.2,.8,.2,1)',
      }}/>

      {/* index counter */}
      <div style={{
        position: 'absolute', top: 14, left: '50%', transform: 'translateX(-50%)',
        background: 'rgba(0,0,0,0.55)', color: '#fff',
        padding: '6px 12px', borderRadius: 999,
        fontSize: 12, fontWeight: 600, fontVariantNumeric: 'tabular-nums',
        backdropFilter: 'blur(10px)', letterSpacing: 0.3, pointerEvents: 'none',
      }}>
        {String(idx + 1).padStart(4,'0')}  ·  {PHOTOS.length.toLocaleString()}
      </div>

      {/* pin indicator */}
      {isPinned && !longPress && <PinBadge t={t}/>}

      {/* marked banner */}
      {isMarked && !longPress && (
        <div style={{ position: 'absolute', top: 16, right: 12 }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 8,
            background: t.danger, color: '#fff',
            padding: '8px 8px 8px 16px', borderRadius: 999,
            boxShadow: '0 6px 20px rgba(0,0,0,0.45)',
            fontSize: 12, fontWeight: 600,
          }}>
            <ITrash size={14}/>
            <span>Marked</span>
            <div
              onPointerDown={(e) => { e.stopPropagation(); }}
              onPointerUp={(e) => {
                e.stopPropagation();
                const n = new Set(marked); n.delete(idx); setMarked(n);
              }}
              style={{
                background: 'rgba(255,255,255,0.24)', color: '#fff',
                padding: '5px 10px', borderRadius: 999,
                fontSize: 11, fontWeight: 700,
                display: 'flex', alignItems: 'center', gap: 4,
              }}><IUndo size={12}/> Undo</div>
          </div>
        </div>
      )}

      {/* long-press reveal label */}
      {longPress && pinnedPhoto && (
        <div style={{
          position: 'absolute', top: 56, left: '50%', transform: 'translateX(-50%)',
          display: 'flex', alignItems: 'center', gap: 8,
          background: t.accent, color: t.dark ? '#0d0d0f' : '#fff',
          padding: '8px 14px', borderRadius: 999,
          fontSize: 12, fontWeight: 700, letterSpacing: 0.2,
          pointerEvents: 'none',
        }}>
          <IPinFilled size={13}/> Showing pinned · release to return
        </div>
      )}

      {/* HUD */}
      {hudOpen && !longPress && (
        <div onPointerDown={(e) => e.stopPropagation()} onPointerUp={(e) => e.stopPropagation()}>
          <div style={{
            position: 'absolute', bottom: 14, left: 10, right: 10,
            display: 'flex', flexDirection: 'column', gap: 8,
          }}>
            <div style={{
              background: 'rgba(16,16,18,0.86)', backdropFilter: 'blur(18px)',
              border: '1px solid rgba(255,255,255,0.08)',
              borderRadius: 999, padding: 6,
              display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 6,
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, paddingLeft: 12 }}>
                <div style={{
                  width: 26, height: 26, borderRadius: 999, background: t.danger, color: '#fff',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}><ITrash size={14}/></div>
                <div style={{ color: '#fff', fontSize: 13, fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                  {marked.size}
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <div onPointerUp={() => setHudOpen(false)} style={iconBtnS(t, cloudMissing)}>
                  {cloudMissing ? <ICloudOff size={16}/> : <ICloud size={16}/>}
                </div>
                <div
                  onPointerUp={(e) => {
                    e.stopPropagation();
                    setPinnedIdx(isPinned ? null : idx);
                  }}
                  style={iconBtnS(t, false, isPinned)}>
                  <IPinFilled size={16}/>
                </div>
                <div style={{
                  background: t.accent, color: t.dark ? '#0d0d0f' : '#fff',
                  borderRadius: 999, padding: '7px 14px',
                  fontSize: 12, fontWeight: 700,
                  display: 'flex', alignItems: 'center', gap: 6,
                }}>
                  <ICheck size={14}/> Confirm
                </div>
              </div>
            </div>
            <div style={{
              textAlign: 'center', fontSize: 10, letterSpacing: 1.4,
              textTransform: 'uppercase', fontWeight: 700,
              color: 'rgba(255,255,255,0.4)',
            }}>
              tap anywhere to dismiss · hold to compare
            </div>
          </div>
        </div>
      )}

      {/* trigger */}
      {!hudOpen && !longPress && (
        <div style={{
          position: 'absolute', bottom: 16, left: '50%', transform: 'translateX(-50%)',
          pointerEvents: 'none',
        }}>
          <div style={{
            background: 'rgba(16,16,18,0.76)', backdropFilter: 'blur(12px)',
            border: '1px solid rgba(255,255,255,0.08)',
            borderRadius: 999, padding: '6px 14px 6px 6px',
            display: 'flex', alignItems: 'center', gap: 8,
            color: '#fff', fontSize: 11, fontWeight: 600, letterSpacing: 0.3,
          }}>
            <div style={{
              width: 22, height: 22, borderRadius: 999,
              background: t.accent, color: t.dark ? '#0d0d0f' : '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 10, fontWeight: 800, fontVariantNumeric: 'tabular-nums',
            }}>{marked.size}</div>
            Tap for HUD
          </div>
        </div>
      )}

      {toast && (
        <div style={{
          position: 'absolute', bottom: 90, left: '50%', transform: 'translateX(-50%)',
          background: 'rgba(20,20,24,0.94)', color: '#fff',
          padding: '10px 16px', borderRadius: 14,
          fontSize: 12, fontWeight: 500,
          border: '1px solid rgba(255,255,255,0.08)',
          whiteSpace: 'nowrap', pointerEvents: 'none',
        }}>{toast}</div>
      )}

      {/* gesture legend */}
      <div style={{
        position: 'absolute', top: 48, right: 10,
        display: 'flex', flexDirection: 'column', gap: 4,
        color: 'rgba(255,255,255,0.38)', fontSize: 9, letterSpacing: 0.5,
        textTransform: 'uppercase', fontWeight: 600, textAlign: 'right',
        pointerEvents: 'none',
      }}>
        <span>← → swipe</span>
        <span>↓ mark</span>
        <span>hold · compare</span>
        <span>tap · HUD</span>
      </div>
    </div>
  );
}

function iconBtnS(t, dim, active) {
  return {
    width: 34, height: 34, borderRadius: 999,
    background: active ? t.pin : 'rgba(255,255,255,0.08)',
    color: active ? '#0d0d0f' : (dim ? 'rgba(255,255,255,0.5)' : '#fff'),
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    border: active ? 'none' : '1px solid rgba(255,255,255,0.08)',
  };
}

Object.assign(window, { LiveViewer });
