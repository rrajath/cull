// Hand-rolled Material-ish icons, stroked. 24x24 viewbox.
const Ic = ({ children, size = 24, stroke = 'currentColor', fill = 'none', sw = 2, style }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={fill} stroke={stroke}
    strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round" style={style}>
    {children}
  </svg>
);

const IPin = (p) => (
  <Ic {...p}>
    <path d="M12 3l4.5 4.5-1.5 1.5.5 3.5L12 16l-3.5-3.5.5-3.5L7.5 7.5 12 3z"/>
    <line x1="12" y1="16" x2="12" y2="21"/>
  </Ic>
);
const IPinFilled = (p) => (
  <Ic {...p} fill="currentColor">
    <path d="M12 3l4.5 4.5-1.5 1.5.5 3.5L12 16l-3.5-3.5.5-3.5L7.5 7.5 12 3z" stroke="none"/>
    <line x1="12" y1="16" x2="12" y2="21"/>
  </Ic>
);
const ITrash = (p) => (
  <Ic {...p}>
    <path d="M4 7h16"/>
    <path d="M10 11v6M14 11v6"/>
    <path d="M5 7l1 13a2 2 0 002 2h8a2 2 0 002-2l1-13"/>
    <path d="M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3"/>
  </Ic>
);
const ICloud = (p) => (
  <Ic {...p}>
    <path d="M7 18a4.5 4.5 0 01-.8-8.93A6 6 0 0118 10a4 4 0 010 8H7z"/>
  </Ic>
);
const ICloudOff = (p) => (
  <Ic {...p}>
    <path d="M7 18a4.5 4.5 0 01-.8-8.93A6 6 0 0118 10a4 4 0 010 8H7z"/>
    <line x1="3" y1="3" x2="21" y2="21"/>
  </Ic>
);
const ICheck = (p) => <Ic {...p}><polyline points="4 12 10 18 20 6"/></Ic>;
const IX = (p) => <Ic {...p}><line x1="6" y1="6" x2="18" y2="18"/><line x1="18" y1="6" x2="6" y2="18"/></Ic>;
const IUndo = (p) => <Ic {...p}><path d="M9 14l-4-4 4-4"/><path d="M5 10h9a5 5 0 010 10h-2"/></Ic>;
const ISettings = (p) => (
  <Ic {...p}>
    <circle cx="12" cy="12" r="3"/>
    <path d="M19.4 15a1.7 1.7 0 00.3 1.8l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.8-.3 1.7 1.7 0 00-1 1.5V21a2 2 0 01-4 0v-.1a1.7 1.7 0 00-1.1-1.5 1.7 1.7 0 00-1.8.3l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.7 1.7 0 00.3-1.8 1.7 1.7 0 00-1.5-1H3a2 2 0 010-4h.1a1.7 1.7 0 001.5-1.1 1.7 1.7 0 00-.3-1.8l-.1-.1a2 2 0 112.8-2.8l.1.1a1.7 1.7 0 001.8.3H9a1.7 1.7 0 001-1.5V3a2 2 0 014 0v.1a1.7 1.7 0 001 1.5 1.7 1.7 0 001.8-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.8V9a1.7 1.7 0 001.5 1H21a2 2 0 010 4h-.1a1.7 1.7 0 00-1.5 1z"/>
  </Ic>
);
const IFolder = (p) => <Ic {...p}><path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"/></Ic>;
const IImage = (p) => (
  <Ic {...p}>
    <rect x="3" y="4" width="18" height="16" rx="2"/>
    <circle cx="9" cy="10" r="1.5"/>
    <path d="M4 18l5-5 4 4 3-3 4 4"/>
  </Ic>
);
const IAlbums = (p) => (
  <Ic {...p}>
    <rect x="6" y="3" width="14" height="14" rx="2"/>
    <path d="M4 7v12a2 2 0 002 2h12"/>
  </Ic>
);
const IZoomReset = (p) => (
  <Ic {...p}>
    <path d="M3 8V4h4"/><path d="M21 8V4h-4"/><path d="M3 16v4h4"/><path d="M21 16v4h-4"/>
    <circle cx="12" cy="12" r="3"/>
  </Ic>
);
const IChevR = (p) => <Ic {...p}><polyline points="9 6 15 12 9 18"/></Ic>;
const IArrow = (p) => <Ic {...p}><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></Ic>;
const ILibrary = (p) => (
  <Ic {...p}>
    <rect x="3" y="6" width="8" height="8" rx="1.5"/>
    <rect x="13" y="6" width="8" height="8" rx="1.5"/>
    <rect x="3" y="16" width="8" height="5" rx="1.5"/>
    <rect x="13" y="16" width="8" height="5" rx="1.5"/>
  </Ic>
);
const IDrag = (p) => <Ic {...p}><circle cx="9" cy="6" r="1"/><circle cx="9" cy="12" r="1"/><circle cx="9" cy="18" r="1"/><circle cx="15" cy="6" r="1"/><circle cx="15" cy="12" r="1"/><circle cx="15" cy="18" r="1"/></Ic>;
const ILoader = (p) => (
  <Ic {...p}>
    <path d="M12 2a10 10 0 0110 10"/>
  </Ic>
);
const IArrowDown = (p) => <Ic {...p}><line x1="12" y1="5" x2="12" y2="19"/><polyline points="5 12 12 19 19 12"/></Ic>;

Object.assign(window, {
  IPin, IPinFilled, ITrash, ICloud, ICloudOff, ICheck, IX, IUndo, ISettings,
  IFolder, IImage, IAlbums, IZoomReset, IChevR, IArrow, ILibrary, IDrag, ILoader, IArrowDown,
});
