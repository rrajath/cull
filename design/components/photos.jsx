// Placeholder "photos" — CSS-generated imagery that looks like real photos
// at thumbnail scale. Each returns a div with an inline style background.
// NO emoji, NO drawn SVG imagery. Gradient blobs + subtle noise feel organic.

const PHOTOS = [
  // id, label (for post-it / debug), css background
  { id: 'p01', label: 'Portrait · golden hour',
    bg: 'radial-gradient(at 30% 20%, #ffd29c 0%, #d8855a 30%, #4a2918 70%, #1a0f08 100%)' },
  { id: 'p02', label: 'Portrait · same subject, blink',
    bg: 'radial-gradient(at 35% 22%, #f8c89a 0%, #c8784e 32%, #3e2214 72%, #160a06 100%)' },
  { id: 'p03', label: 'Mountain · dawn',
    bg: 'linear-gradient(180deg, #2a3f6b 0%, #6b7fa8 35%, #d9c2a0 55%, #8a6d4e 75%, #3e2f1f 100%)' },
  { id: 'p04', label: 'Mountain · dawn (slightly softer)',
    bg: 'linear-gradient(180deg, #34456c 0%, #7788a8 35%, #e2cdaa 55%, #8f7354 75%, #45342a 100%)' },
  { id: 'p05', label: 'Forest trail',
    bg: 'radial-gradient(at 50% 80%, #4a6b2e 0%, #2d4420 40%, #1a2812 75%, #0a0f08 100%)' },
  { id: 'p06', label: 'Forest trail · blurred',
    bg: 'radial-gradient(at 50% 80%, #5a7a3a 0%, #3a5628 40%, #223015 75%, #0f1508 100%)' },
  { id: 'p07', label: 'Ocean · long exposure',
    bg: 'linear-gradient(180deg, #0a1a2a 0%, #1e3a5a 40%, #5a7a9a 70%, #d0d8e0 100%)' },
  { id: 'p08', label: 'City lights',
    bg: 'radial-gradient(at 40% 60%, #ff8a3a 0%, #8a2a4a 35%, #1a0a2a 80%)' },
  { id: 'p09', label: 'Desert',
    bg: 'linear-gradient(180deg, #f4c98a 0%, #d8965a 35%, #8a4a2a 70%, #2a1a0a 100%)' },
  { id: 'p10', label: 'Desert · duplicate',
    bg: 'linear-gradient(180deg, #f2c686 0%, #d4925a 35%, #864628 70%, #281808 100%)' },
  { id: 'p11', label: 'Portrait · profile',
    bg: 'radial-gradient(at 60% 30%, #e8b89a 0%, #a8684a 35%, #4a2a1a 75%, #180a06 100%)' },
  { id: 'p12', label: 'Still life',
    bg: 'linear-gradient(135deg, #d8a878 0%, #8a5a3a 50%, #2a1a0e 100%)' },
  { id: 'p13', label: 'Sky',
    bg: 'linear-gradient(180deg, #6a8aca 0%, #c8a8e8 60%, #f8c8a8 100%)' },
  { id: 'p14', label: 'Fog · trees',
    bg: 'linear-gradient(180deg, #8a9a9a 0%, #4a5a5a 50%, #1a2020 100%)' },
  { id: 'p15', label: 'Flower macro',
    bg: 'radial-gradient(at 50% 50%, #f8a8c8 0%, #c848a8 35%, #4a1838 75%, #1a0810 100%)' },
  { id: 'p16', label: 'Snow field',
    bg: 'linear-gradient(180deg, #e8eef4 0%, #c4d0dc 50%, #8a9aa8 100%)' },
  { id: 'p17', label: 'Street · night',
    bg: 'radial-gradient(at 30% 70%, #ffaa4a 0%, #5a2a4a 40%, #0a0a1a 90%)' },
  { id: 'p18', label: 'Portrait · candid',
    bg: 'radial-gradient(at 45% 35%, #f0c0a0 0%, #b86848 40%, #2a1410 80%)' },
  { id: 'p19', label: 'Leaves',
    bg: 'radial-gradient(at 70% 30%, #8aba4a 0%, #3a6a28 45%, #0f1808 90%)' },
  { id: 'p20', label: 'Beach',
    bg: 'linear-gradient(180deg, #a8d8e8 0%, #e8d4a8 60%, #c89a6a 100%)' },
];

// Subtle film-grain overlay
const GRAIN = `url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='120' height='120'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' seed='3'/><feColorMatrix values='0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 0.08 0'/></filter><rect width='100%25' height='100%25' filter='url(%23n)'/></svg>")`;

// A photo "tile" — the placeholder image. Accepts a photo object + display props.
function Photo({ photo, style, grayscale = false, grain = true, children }) {
  const p = typeof photo === 'number' ? PHOTOS[photo % PHOTOS.length] : photo;
  return (
    <div style={{
      position: 'relative',
      backgroundImage: p.bg,
      backgroundSize: 'cover',
      filter: grayscale ? 'grayscale(1) brightness(0.85)' : 'none',
      overflow: 'hidden',
      ...style,
    }}>
      {grain && (
        <div style={{
          position: 'absolute', inset: 0,
          backgroundImage: GRAIN,
          backgroundSize: '120px 120px',
          mixBlendMode: 'overlay',
          opacity: 0.55,
          pointerEvents: 'none',
        }}/>
      )}
      {children}
    </div>
  );
}

Object.assign(window, { PHOTOS, Photo });
