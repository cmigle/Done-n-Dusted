import { useId, type CSSProperties } from 'react'

/*
 * Pixel art drawn from text grids: one character is one 4 px pixel, '.' is empty.
 * Colours come from CSS variables through the px-* classes, so the art follows the theme.
 */
const PX = 4

const CLASS: Record<string, string> = {
  S: 'px-sun',
  y: 'px-sun-2',
  M: 'px-moon',
  c: 'px-moon-2',
  C: 'px-cloud',
  s: 'px-cloud-2',
  t: 'px-star',
  g: 'px-grass',
  G: 'px-grass-2',
  p: 'px-flower-1',
  o: 'px-flower-2',
  w: 'px-flower-3',
  '1': 'px-fire-1',
  '2': 'px-fire-2',
  '3': 'px-fire-3',
  W: 'px-wood',
}

/** One <rect> per run of equal pixels in a row. */
function pixels(rows: string[], dy = 0) {
  const rects = []
  for (let y = 0; y < rows.length; y++) {
    const row = rows[y]
    let x = 0
    while (x < row.length) {
      const ch = row[x]
      if (!CLASS[ch]) {
        x++
        continue
      }
      let w = 1
      while (row[x + w] === ch) w++
      rects.push(<rect key={`${y}-${x}`} className={CLASS[ch]} x={x} y={y + dy} width={w} height={1} />)
      x += w
    }
  }
  return rects
}

function Art({ rows, className, style }: { rows: string[]; className: string; style?: CSSProperties }) {
  const w = rows[0].length
  const h = rows.length
  return (
    <svg className={className} style={style} viewBox={`0 0 ${w} ${h}`} width={w * PX} height={h * PX} shapeRendering="crispEdges">
      {pixels(rows)}
    </svg>
  )
}

const SUN = [
  '.......yy.......',
  '.......yy.......',
  '..y..........y..',
  '...y..SSSS..y...',
  '.....SSSSSS.....',
  '....SSSSSSSS....',
  '...SSSSSSSSSS...',
  'yy.SSSSSSSSSS.yy',
  'yy.SSSSSSSSSS.yy',
  '...SSSSSSSSSS...',
  '....SSSSSSSS....',
  '.....SSSSSS.....',
  '...y..SSSS..y...',
  '..y..........y..',
  '.......yy.......',
  '.......yy.......',
]

const MOON = [
  '................',
  '......MMMM......',
  '....MMMMMMMM....',
  '...MMMMMMMcMM...',
  '..MMMMMMMcccMM..',
  '..MMcMMMMMcMMM..',
  '.MMcccMMMMMMMMM.',
  '.MMMcMMMMMMMMMM.',
  '.MMMMMMMMMMcMMM.',
  '.MMMMMMMMMcccMM.',
  '..MMMMMMMMMcMM..',
  '..MMMMcMMMMMMM..',
  '...MMcccMMMMM...',
  '....MMcMMMMM....',
  '......MMMM......',
  '................',
]

const CLOUD = [
  '.....CCCC.......',
  '...CCCCCCCC.....',
  '..CCCCCCCCCCC...',
  '.CCCCCCCCCCCCCC.',
  'CCCCCCCCCCCCCCCC',
  '.ssssssssssssss.',
]

const STAR = ['.t.', 'ttt', '.t.']

// Three flame frames over the same logs; CSS shows one at a time.
const FLAMES = [
  ['.....1......', '....121.....', '....121..1..', '...12321....', '..1233321...', '..1233321...', '...12221....', '....111.....'],
  ['....1.......', '...121......', '...121......', '...1231.....', '..123321....', '..1233321...', '...12221....', '....111.....'],
  ['......1.....', '.....121....', '..1..121....', '....1231....', '...123321...', '..1233321...', '...12221....', '....111.....'],
]
const LOGS = ['.WWWW..WWWW.', '..WWWWWWWW..']

const GRASS = [
  '....p...........w.......',
  '...pop.........wow......',
  '..g.p..g....g...w...g...',
  '.gg.g..gg..ggg..g..ggg.g',
  'GgGgGGgGGgGgGGgGGgGgGGgG',
  'GGGGGGGGGGGGGGGGGGGGGGGG',
]
const FLOWER = new Set(['p', 'o', 'w'])

/** A strip of pixel grass across the full width; the flowers show only in the day theme. */
export function GrassStrip({ className = '' }: { className?: string }) {
  const id = `grass-${useId().replace(/[^a-zA-Z0-9]/g, '')}`
  const blades = GRASS.map((row) => [...row].map((ch) => (FLOWER.has(ch) ? '.' : ch)).join(''))
  const flowers = GRASS.map((row) => [...row].map((ch) => (FLOWER.has(ch) ? ch : '.')).join(''))
  const w = GRASS[0].length
  const h = GRASS.length
  return (
    <svg className={`grass ${className}`} width="100%" height={h * PX} aria-hidden="true" focusable="false">
      <defs>
        <pattern id={id} width={w * PX} height={h * PX} patternUnits="userSpaceOnUse">
          <g transform={`scale(${PX})`} shapeRendering="crispEdges">
            {pixels(blades)}
            <g className="scene-day">{pixels(flowers)}</g>
          </g>
        </pattern>
      </defs>
      <rect width="100%" height={h * PX} fill={`url(#${id})`} />
    </svg>
  )
}

// Stars: [left %, top px, twinkle delay s]. They stay in the band above the heading, so text never sits on them.
const STARS: [number, number, number][] = [
  [4, 20, 0],
  [12, 64, 1.2],
  [21, 12, 2.1],
  [29, 48, 0.6],
  [38, 84, 1.7],
  [47, 24, 0.3],
  [55, 70, 2.6],
  [63, 16, 1.1],
  [71, 56, 2.2],
  [79, 96, 0.8],
  [88, 104, 1.5],
  [96, 64, 2.9],
]

/** The landing hero backdrop: sun, drifting clouds and flowers by day; moon, stars and a campfire by night. */
export function HeroScene() {
  return (
    <div className="scene" aria-hidden="true">
      <Art rows={SUN} className="scene__orb scene-day" />
      <Art rows={MOON} className="scene__orb scene-night" />
      <Art rows={CLOUD} className="scene__cloud scene__cloud--1 scene-day" />
      <Art rows={CLOUD} className="scene__cloud scene__cloud--2 scene-day" />
      <Art rows={CLOUD} className="scene__cloud scene__cloud--3 scene-day" />
      {STARS.map(([x, y, delay]) => (
        <Art
          key={`${x}-${y}`}
          rows={STAR}
          className="scene__star scene-night"
          style={{ left: `${x}%`, top: y, animationDelay: `${delay}s` }}
        />
      ))}
      <svg className="scene__fire scene-night" viewBox="0 0 12 10" width={12 * PX} height={10 * PX} shapeRendering="crispEdges">
        {FLAMES.map((frame, i) => (
          <g key={i} className={`flame flame--${i + 1}`}>
            {pixels(frame)}
          </g>
        ))}
        {pixels(LOGS, 8)}
      </svg>
      <GrassStrip className="scene__ground" />
    </div>
  )
}
