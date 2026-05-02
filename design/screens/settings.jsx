// SETTINGS + CONFIRM DIALOG

function SettingsScreen({ t, dryRun = true, longPressMs = 220, source = 'hybrid', accentHue = 40, onHueChange }) {
  return (
    <>
      <StatusBar t={t}/>
      <div style={{ padding: '8px 16px 14px', display: 'flex', alignItems: 'center', gap: 8 }}>
        <CircleIcon t={t}><IChevR size={22} style={{ transform: 'rotate(180deg)' }}/></CircleIcon>
        <div style={{
          fontFamily: t.display, fontStyle: 'italic',
          fontSize: 30, lineHeight: 1, letterSpacing: -0.6,
          paddingLeft: 4,
        }}>Settings</div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '4px 16px 24px' }}>
        <SectionLabel t={t}>Source</SectionLabel>
        <SettingCard t={t}>
          <div style={{ padding: 12 }}>
            <SourceRadio t={t} value={source}/>
          </div>
        </SettingCard>

        <SectionLabel t={t}>{source === 'immich' ? 'Immich' : 'Sources'}</SectionLabel>
        <SettingCard t={t}>
          {source !== 'immich' && <>
            <Row t={t} icon={<IFolder size={20}/>} label="Library folder" value="/storage/emulated/0/DCIM/IMG_2020" action="Change"/>
            <Div t={t}/>
          </>}
          <Row t={t} icon={<ICloud size={20}/>} label="Immich URL" value="immich.home.lan:2283" action="Edit"/>
          <Div t={t}/>
          <Row t={t} icon={<ISettings size={20}/>} label="API key" value="•••• •••• •••• k9xQ" action="Edit"/>
          <Div t={t}/>
          <div style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              flex: 1, display: 'flex', alignItems: 'center', gap: 8,
              color: 'oklch(0.72 0.18 150)', fontSize: 13, fontWeight: 600,
            }}>
              <div style={{ width: 8, height: 8, borderRadius: 999, background: 'oklch(0.72 0.18 150)' }}/>
              Connected · 14,202 assets
            </div>
            <div style={{
              background: t.bgElev2, color: t.fg, border: `1px solid ${t.lineStrong}`,
              padding: '8px 14px', borderRadius: 999, fontSize: 13, fontWeight: 600,
            }}>Test connection</div>
          </div>
        </SettingCard>

        <SectionLabel t={t}>Culling</SectionLabel>
        <SettingCard t={t}>
          <SliderRow t={t} label="Long-press threshold" value={`${longPressMs}ms`} pct={(longPressMs - 80) / (800 - 80)}/>
          <Div t={t}/>
          <ToggleRow t={t}
            label="Dry run mode"
            sub="Never actually deletes. Shows a toast instead."
            on={dryRun}/>
          {source === 'hybrid' && <>
            <Div t={t}/>
            <ToggleRow t={t}
              label="Mirror deletes to Immich"
              sub="If a photo exists on Immich, call its delete API too."
              on={true}/>
          </>}
        </SettingCard>

        <SectionLabel t={t}>Appearance</SectionLabel>
        <SettingCard t={t}>
          <ToggleRow t={t} label="Dark theme" on={t.dark}/>
          <Div t={t}/>
          <div style={{ padding: '14px 16px' }}>
            <div style={{ fontSize: 14, fontWeight: 600, color: t.fg, marginBottom: 12 }}>Accent hue</div>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'space-between' }}>
              {[40, 25, 150, 220, 300].map(h => {
                const on = h === accentHue;
                return (
                  <div key={h}
                    onClick={() => onHueChange && onHueChange(h)}
                    style={{
                      flex: 1, height: 44, borderRadius: 14, cursor: 'pointer',
                      background: `oklch(0.74 0.18 ${h})`,
                      position: 'relative',
                      boxShadow: on ? `0 0 0 3px ${t.bg}, 0 0 0 5px ${t.fg}` : 'none',
                      transition: 'box-shadow 160ms',
                    }}>
                    {on && (
                      <div style={{
                        position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)',
                        color: '#0d0d0f',
                      }}><ICheck size={20}/></div>
                    )}
                  </div>
                );
              })}
            </div>
            <div style={{
              marginTop: 10, fontSize: 11, color: t.fgFaint,
              letterSpacing: 0.3, fontVariantNumeric: 'tabular-nums',
            }}>
              oklch(0.74 0.18 {accentHue})
            </div>
          </div>
        </SettingCard>
      </div>
      <NavBar t={t}/>
    </>
  );
}

function SourceRadio({ t, value }) {
  const opts = [
    { k: 'local',  title: 'Local',  sub: 'Cull photos in a device folder. Immich is not touched.', icon: <IFolder size={18}/> },
    { k: 'immich', title: 'Immich', sub: 'Cull a remote Immich library directly. No local files.', icon: <ICloud size={18}/> },
    { k: 'hybrid', title: 'Local + Immich', sub: 'Cull on device; mirror deletions to your Immich instance.', icon: <ILibrary size={18}/>, recommended: true },
  ];
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      {opts.map(o => {
        const on = o.k === value;
        return (
          <div key={o.k} style={{
            padding: '12px 14px', borderRadius: 16,
            background: on ? t.accentSoft : 'transparent',
            border: on ? `1px solid ${t.accent}` : `1px solid ${t.line}`,
            display: 'flex', alignItems: 'flex-start', gap: 12,
          }}>
            <div style={{
              width: 36, height: 36, borderRadius: 999, flexShrink: 0,
              background: on ? t.accent : t.bgElev2,
              color: on ? (t.dark ? '#0d0d0f' : '#fff') : t.fgDim,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>{o.icon}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: t.fg }}>{o.title}</div>
                {o.recommended && (
                  <div style={{
                    fontSize: 9, fontWeight: 800, letterSpacing: 0.6, textTransform: 'uppercase',
                    color: t.accent, background: t.bgElev, padding: '2px 6px', borderRadius: 4,
                    border: `1px solid ${t.line}`,
                  }}>recommended</div>
                )}
              </div>
              <div style={{ fontSize: 12, color: t.fgDim, marginTop: 2, lineHeight: 1.4 }}>{o.sub}</div>
            </div>
            <div style={{
              width: 20, height: 20, borderRadius: 999, flexShrink: 0, marginTop: 2,
              border: `2px solid ${on ? t.accent : t.lineStrong}`,
              background: on ? t.accent : 'transparent',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: t.dark ? '#0d0d0f' : '#fff',
            }}>{on && <ICheck size={12}/>}</div>
          </div>
        );
      })}
    </div>
  );
}

function SectionLabel({ t, children }) {
  return (
    <div style={{
      fontSize: 11, letterSpacing: 1.6, textTransform: 'uppercase',
      fontWeight: 700, color: t.fgDim,
      padding: '18px 4px 8px',
    }}>{children}</div>
  );
}

function SettingCard({ t, children }) {
  return (
    <div style={{
      background: t.bgElev, border: `1px solid ${t.line}`, borderRadius: 22,
      overflow: 'hidden',
    }}>{children}</div>
  );
}

function Div({ t }) { return <div style={{ height: 1, background: t.line, margin: '0 16px' }}/>; }

function Row({ t, icon, label, value, action }) {
  return (
    <div style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
      {icon && <div style={{ color: t.fgDim, flexShrink: 0 }}>{icon}</div>}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: t.fg }}>{label}</div>
        <div style={{ fontSize: 12, color: t.fgDim, marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {value}
        </div>
      </div>
      {action && (
        <div style={{ color: t.accent, fontSize: 13, fontWeight: 600 }}>{action}</div>
      )}
    </div>
  );
}

function ToggleRow({ t, label, sub, on }) {
  return (
    <div style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: t.fg }}>{label}</div>
        {sub && <div style={{ fontSize: 12, color: t.fgDim, marginTop: 2 }}>{sub}</div>}
      </div>
      <div style={{
        width: 48, height: 28, borderRadius: 999,
        background: on ? t.accent : t.bgElev2,
        border: `1px solid ${on ? 'transparent' : t.lineStrong}`,
        position: 'relative', flexShrink: 0,
      }}>
        <div style={{
          position: 'absolute', top: 3, left: on ? 23 : 3,
          width: 22, height: 22, borderRadius: 999,
          background: on ? (t.dark ? '#0d0d0f' : '#fff') : t.fgDim,
          transition: 'left 180ms',
        }}/>
      </div>
    </div>
  );
}

function SliderRow({ t, label, value, pct = 0.3 }) {
  return (
    <div style={{ padding: '14px 16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: t.fg }}>{label}</div>
        <div style={{
          fontSize: 12, fontWeight: 700, color: t.accent,
          fontVariantNumeric: 'tabular-nums',
          background: t.bgElev2, padding: '4px 10px', borderRadius: 999,
        }}>{value}</div>
      </div>
      <div style={{ position: 'relative', marginTop: 14, height: 20 }}>
        <div style={{
          position: 'absolute', top: 9, left: 0, right: 0, height: 2,
          background: t.lineStrong, borderRadius: 999,
        }}/>
        <div style={{
          position: 'absolute', top: 9, left: 0, width: `${pct * 100}%`, height: 2,
          background: t.accent, borderRadius: 999,
        }}/>
        <div style={{
          position: 'absolute', top: 2, left: `calc(${pct * 100}% - 8px)`,
          width: 16, height: 16, borderRadius: 999, background: t.accent,
          border: `3px solid ${t.bgElev}`, boxShadow: '0 2px 6px rgba(0,0,0,0.2)',
        }}/>
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }}>
        <span style={{ fontSize: 11, color: t.fgFaint, fontVariantNumeric: 'tabular-nums' }}>80ms</span>
        <span style={{ fontSize: 11, color: t.fgFaint, fontVariantNumeric: 'tabular-nums' }}>800ms</span>
      </div>
    </div>
  );
}

// Delete confirm dialog (overlay)
function ConfirmDialog({ t }) {
  return (
    <div style={{
      position: 'absolute', inset: 0, background: t.scrim,
      display: 'flex', alignItems: 'flex-end', justifyContent: 'center',
      padding: 14, zIndex: 20,
    }}>
      <div style={{
        width: '100%', background: t.bgElev, color: t.fg,
        border: `1px solid ${t.line}`,
        borderRadius: 28, padding: 24,
      }}>
        <div style={{
          width: 52, height: 52, borderRadius: 999,
          background: t.dangerSoft, color: t.danger,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          marginBottom: 14,
        }}>
          <ITrash size={24}/>
        </div>
        <div style={{
          fontFamily: t.display, fontStyle: 'italic', fontSize: 30, lineHeight: 1,
          letterSpacing: -0.4, marginBottom: 10,
        }}>
          Delete 3 photos?
        </div>
        <div style={{ fontSize: 13, color: t.fgDim, lineHeight: 1.45, marginBottom: 14 }}>
          They'll move to your device Trash and be recoverable for 30 days.
        </div>

        <div style={{
          background: t.bgElev2, borderRadius: 16, padding: 14,
          display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 18,
          border: `1px solid ${t.line}`,
        }}>
          <SumRow t={t} label="On device only" n={1}/>
          <SumRow t={t} label="On device + Immich" n={2} badge="immich"/>
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          <div style={{
            flex: 1, background: t.bgElev2, color: t.fg,
            border: `1px solid ${t.lineStrong}`, borderRadius: 999,
            padding: '14px 16px', textAlign: 'center',
            fontSize: 14, fontWeight: 600,
          }}>Cancel</div>
          <div style={{
            flex: 1.3, background: t.danger, color: '#fff',
            borderRadius: 999, padding: '14px 16px', textAlign: 'center',
            fontSize: 14, fontWeight: 700,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
          }}>
            <ITrash size={16}/> Move to Trash
          </div>
        </div>
      </div>
    </div>
  );
}

function SumRow({ t, label, n, badge }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <div style={{
        minWidth: 28, height: 28, borderRadius: 8, background: t.bg,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 13, fontWeight: 700, color: t.fg,
        fontVariantNumeric: 'tabular-nums', padding: '0 8px',
      }}>{n}</div>
      <div style={{ fontSize: 13, color: t.fg, flex: 1 }}>{label}</div>
      {badge && (
        <div style={{
          fontSize: 10, fontWeight: 700, letterSpacing: 0.6, textTransform: 'uppercase',
          background: t.accentSoft, color: t.accent,
          padding: '3px 7px', borderRadius: 6,
        }}>{badge}</div>
      )}
    </div>
  );
}

Object.assign(window, { SettingsScreen, ConfirmDialog, SourceRadio });
