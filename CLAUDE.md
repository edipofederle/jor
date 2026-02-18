# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start dev server with hot-reload (serves on http://localhost:8080)
npx shadow-cljs watch app

# Production build
npx shadow-cljs release app

# Open a browser REPL (after `watch` is running)
npx shadow-cljs cljs-repl app
```

There are no tests in this project.

## Architecture

**jor** is a ClojureScript single-page app for visualising woodworking joints in 3D using Three.js. Stack: shadow-cljs + Reagent 1.2 + re-frame 1.4 + Three.js 0.165.

### Data flow

```
re-frame db  →  subscriptions  →  add-watch (in canvas component)  →  three/rebuild-joint!
```

The Three.js scene is **not** managed by re-frame. It lives in a private `defonce state` atom in `scene/three.cljs`. The canvas Reagent component (`views/app.cljs`) bridges the two worlds: it calls `rf/subscribe` for `:active-joint`, `:merged-joint-params`, and `:explode-factor`, then uses `add-watch` on those reactions so that REPL dispatches also trigger scene rebuilds. All three watches are debounced through a single `js/setTimeout 0` to coalesce rapid multi-sub updates into a single `rebuild-joint!` call.

### Joint definition schema

Every joint is a plain map with these keys:

```clojure
{:id          keyword
 :label       string
 :doc         string
 :image       string                  ; optional — relative path e.g. "images/joints/foo.jpg"
 :params      {keyword number ...}    ; default params (all dimensions in mm)
 :derived-fn  (fn [params] [[label value-str] ...])  ; optional — computed cut measurements for sidebar
 :min-explode float                   ; minimum explode factor to avoid Z-fighting
 :parts       [{:id kw :label str :explode-dir [x y z]} ...]
 :build-fn    (fn [params] {part-id THREE.Group ...})
 :dims-fn     (fn [params] {part-id [{:from [x y z] :to [x y z]
                                      :offset-dir [x y z] :offset-dist n
                                      :label str} ...]})  ; optional — 3D dimension lines
 :cut-seq     [{:step n :label str :part kw-or-nil} ...]}
```

`joints/registry.cljs` holds the master map and ordered list. To add a joint, create a new namespace under `src/jor/joints/`, define a `definition` var following the schema, and require + register it in the registry.

**`:derived-fn`** returns `[[label value-string] ...]` of computed workshop measurements (shoulder widths, cut depths, etc.) shown in the sidebar "Key measurements" section. Values that aren't whole numbers should be formatted with `.toFixed 1`.

**`:dims-fn`** returns a map of `{part-id [dim-spec ...]}`. Dim groups are added as children of their part groups (tagged `userData.jorDims = true`) so they move correctly during explode/animate. Only Mortise & Tenon currently has this; the "dims" toolbar button is disabled for joints without it.

**`:image`** — drop a `.jpg`/`.png` into `public/images/joints/` and set this to the relative path. Missing files are silently hidden via `on-error`.

**`:cut-seq`** — `:part nil` means both parts are involved (e.g. "Glue and clamp"). The sidebar groups consecutive same-part steps under a part header.

### Coordinate conventions

Each joint picks its own axis orientation; conventions are documented in comments at the top of each joint file. General rule: interlocking geometry must be centred at the world origin so the explode system can separate it cleanly.

### Explode system

`geometry/explode.cljs` computes per-part XYZ offsets from `:explode-dir` vectors scaled by `factor * 80 mm`. `rebuild-joint!` clamps to `(max user-factor (:min-explode joint-def))`. When adding a joint, set `:min-explode` to `protrusion_mm / (2 * 80)`.

### Three.js renderer

`scene/three.cljs` owns:
- `WebGLRenderer` (`logarithmicDepthBuffer: true` for depth precision)
- `CSS2DRenderer` — overlaid on canvas for dimension label HTML elements; resized together with the WebGL renderer via `ResizeObserver`
- `OrbitControls`, `PerspectiveCamera 45°`
- RAF render loop calling `advance-anim!` then both renderers each frame

`rebuild-joint!` disposes old mesh geometries before rebuilding. Three.js objects must never be put into re-frame's app-db.

### Animation

Ping-pong explode animation runs entirely inside the Three.js state atom (`:anim {:playing? false :factor 0.0 :dir 1}`). `advance-anim!` increments the factor 0.008/frame and bounces at 0 and 1. It calls `apply-explode!` (position-only, no geometry rebuild) so it's cheap enough to run at 60 fps. The toolbar button toggles `three/toggle-anim!`; when stopping, it syncs the re-frame `:explode-factor` to wherever the animation landed.

### Part highlighting (cut sequence)

`three/highlight-step!` dims non-active part groups when a cut step is selected:
- Traverses each part group's meshes, clones materials (to avoid mutating shared instances), sets `transparent=true`, `depthWrite=false`, `opacity=0.15`, `needsUpdate=true` on non-active parts
- Originals saved in `mesh.userData.origMat`; restored on deselect or joint switch
- `:highlighted-part` stored in Three.js state so `rebuild-joint!` reapplies highlighting after geometry rebuilds
- `events/joints.cljs` calls `three/highlight-step!` directly from `:set-cut-step` and `:select-joint` event handlers (pragmatic side-effect in db handler — consistent with how toolbar calls three functions from views)

### Materials

`scene/materials.cljs` holds a lazy-initialised cache of shared `THREE.MeshLambertMaterial` instances (`:wood-light`, `:wood-dark`, `:highlight`). Because they are shared, **never mutate them directly** — always `.clone` before changing properties (opacity, colour, etc.).

### Sidebar sections

The sidebar (`views/sidebar.cljs`) renders up to four sections per joint:
1. **About** — joint photo (if `:image` set) + `:doc` text
2. **Key measurements** — from `:derived-fn`; derived workshop values (shoulders, cut depths, etc.)
3. **Steps** — `:cut-seq` grouped by part with accent-coloured group headers; clicking a step triggers `highlight-step!`

### Params / overrides

`db.cljs` stores per-joint overrides as `{joint-id {param-key value}}` under `:joint-params`. The `:merged-joint-params` subscription merges these over defaults. The Three.js scene rebuilds automatically via the watches.
